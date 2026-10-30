package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyOrchestratorTest {

    private fun context(
        symbol: String = "BTCIRT",
        candles: List<Candle> = listOf(
            Candle(1_000L, 100.0, 105.0, 95.0, 101.0, 10.0),
            Candle(2_000L, 101.0, 106.0, 96.0, 102.0, 12.0)
        )
    ) = StrategyContext(symbol = symbol, candles = candles)

    private fun signal(
        id: StrategyId,
        symbol: String = "BTCIRT",
        direction: TradeDirection = TradeDirection.LONG,
        reason: SignalReason = SignalReason.SETUP_CONFIRMED
    ) = StrategySignal(
        strategyId = id,
        symbol = symbol,
        direction = direction,
        confidence = 0.8,
        reason = reason,
        referencePrice = 102.0,
        stopLossPrice = 100.0,
        takeProfitPrice = 106.0
    )

    private class StubStrategy(
        override val id: StrategyId,
        private val result: (StrategyContext) -> StrategySignal
    ) : TradingStrategy {
        var calls: Int = 0
            private set

        override fun evaluate(context: StrategyContext): StrategySignal {
            calls++
            return result(context)
        }
    }

    @Test
    fun evaluatesEveryStrategyAndPreservesRegistrationOrder() {
        val first = StubStrategy(StrategyId.FVG) { signal(StrategyId.FVG) }
        val second = StubStrategy(StrategyId.MICRO_SCALPER) {
            signal(StrategyId.MICRO_SCALPER)
        }

        val result = StrategyOrchestrator(listOf(first, second)).evaluate(context())

        assertEquals(1, first.calls)
        assertEquals(1, second.calls)
        assertEquals(2, result.expectedStrategyCount)
        assertEquals(2, result.attemptedStrategyCount)
        assertEquals(
            listOf(StrategyId.FVG, StrategyId.MICRO_SCALPER),
            result.signals.map { it.strategyId }
        )
        assertTrue(result.isInputValid)
        assertTrue(result.isComplete)
        assertNull(result.inputError)
    }

    @Test
    fun strategyExceptionDoesNotPreventOtherStrategiesFromRunning() {
        val failing = StubStrategy(StrategyId.FVG) {
            throw IllegalStateException("strategy failed")
        }
        val working = StubStrategy(StrategyId.MICRO_SCALPER) {
            signal(StrategyId.MICRO_SCALPER)
        }

        val result = StrategyOrchestrator(listOf(failing, working)).evaluate(context())

        assertEquals(1, failing.calls)
        assertEquals(1, working.calls)
        assertEquals(2, result.attemptedStrategyCount)
        assertEquals(1, result.failures.size)
        assertEquals(StrategyId.FVG, result.failures.single().strategyId)
        assertEquals("strategy failed", result.failures.single().message)
        assertEquals(1, result.signals.size)
        assertFalse(result.isComplete)
    }

    @Test
    fun rejectsSignalWithWrongStrategyId() {
        val strategy = StubStrategy(StrategyId.FVG) {
            signal(StrategyId.MICRO_SCALPER)
        }

        val result = StrategyOrchestrator(listOf(strategy)).evaluate(context())

        assertTrue(result.signals.isEmpty())
        assertEquals(1, result.failures.size)
        assertEquals(StrategyId.FVG, result.failures.single().strategyId)
        assertTrue(result.failures.single().message.contains("strategy ID"))
        assertFalse(result.isComplete)
    }

    @Test
    fun rejectsSignalForDifferentSymbol() {
        val strategy = StubStrategy(StrategyId.FVG) {
            signal(StrategyId.FVG, symbol = "ETHIRT")
        }

        val result = StrategyOrchestrator(listOf(strategy)).evaluate(context())

        assertTrue(result.signals.isEmpty())
        assertEquals(1, result.failures.size)
        assertTrue(result.failures.single().message.contains("symbol"))
        assertFalse(result.isComplete)
    }

    @Test
    fun invalidCandleRejectsWholeBatchBeforeExecution() {
        val strategy = StubStrategy(StrategyId.FVG) { signal(StrategyId.FVG) }
        val invalid = Candle(3_000L, 100.0, 90.0, 95.0, 101.0, 10.0)

        val result = StrategyOrchestrator(listOf(strategy))
            .evaluate(context(candles = listOf(invalid)))

        assertEquals(0, strategy.calls)
        assertEquals(0, result.attemptedStrategyCount)
        assertTrue(result.signals.isEmpty())
        assertFalse(result.isInputValid)
        assertTrue(result.inputError!!.contains("invalid candle"))
    }

    @Test
    fun nonChronologicalCandlesRejectWholeBatch() {
        val strategy = StubStrategy(StrategyId.FVG) { signal(StrategyId.FVG) }

        val result = StrategyOrchestrator(listOf(strategy))
            .evaluate(context(candles = context().candles.reversed()))

        assertEquals(0, strategy.calls)
        assertEquals(0, result.attemptedStrategyCount)
        assertFalse(result.isInputValid)
        assertTrue(result.inputError!!.contains("chronological"))
    }

    @Test
    fun blankSymbolRejectsWholeBatch() {
        val strategy = StubStrategy(StrategyId.FVG) { signal(StrategyId.FVG, symbol = "") }

        val result = StrategyOrchestrator(listOf(strategy)).evaluate(context(symbol = " "))

        assertEquals(0, strategy.calls)
        assertFalse(result.isInputValid)
        assertTrue(result.inputError!!.contains("blank"))
    }

    @Test
    fun flatAndNonSetupSignalsAreNotActionable() {
        val flat = StubStrategy(StrategyId.FVG) {
            signal(
                id = StrategyId.FVG,
                symbol = it.symbol,
                direction = TradeDirection.FLAT,
                reason = SignalReason.NO_SETUP
            )
        }

        val result = StrategyOrchestrator(listOf(flat)).evaluate(context())

        assertTrue(result.isComplete)
        assertTrue(result.actionableSignals.isEmpty())
    }

    @Test
    fun confirmedDirectionalSignalIsActionable() {
        val strategy = StubStrategy(StrategyId.FVG) {
            signal(id = StrategyId.FVG, symbol = it.symbol)
        }

        val result = StrategyOrchestrator(listOf(strategy)).evaluate(context())

        assertEquals(1, result.actionableSignals.size)
        assertEquals(TradeDirection.LONG, result.actionableSignals.single().direction)
    }

    @Test
    fun duplicateStrategyIdsAreRejectedAtConstruction() {
        val first = StubStrategy(StrategyId.FVG) { signal(StrategyId.FVG) }
        val second = StubStrategy(StrategyId.FVG) { signal(StrategyId.FVG) }

        try {
            StrategyOrchestrator(listOf(first, second))
            throw AssertionError("Expected duplicate strategy IDs to fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("Duplicate strategy IDs"))
        }
    }

    @Test
    fun emptyStrategyListIsRejectedAtConstruction() {
        try {
            StrategyOrchestrator(emptyList())
            throw AssertionError("Expected empty strategy list to fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("At least one"))
        }
    }
}
