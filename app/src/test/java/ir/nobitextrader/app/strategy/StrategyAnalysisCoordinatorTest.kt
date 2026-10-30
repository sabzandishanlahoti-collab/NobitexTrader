package ir.nobitextrader.app.strategy

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyAnalysisCoordinatorTest {

    private val candles = listOf(
        Candle(1_000L, 100.0, 105.0, 95.0, 101.0, 10.0),
        Candle(2_000L, 101.0, 106.0, 96.0, 102.0, 12.0),
        Candle(3_000L, 102.0, 107.0, 97.0, 103.0, 14.0)
    )

    private fun signal(
        id: StrategyId,
        symbol: String,
        direction: TradeDirection = TradeDirection.LONG
    ) = StrategySignal(
        strategyId = id,
        symbol = symbol,
        direction = direction,
        confidence = 0.8,
        reason = SignalReason.SETUP_CONFIRMED,
        referencePrice = 103.0,
        stopLossPrice = 100.0,
        takeProfitPrice = 109.0
    )

    private class StubStrategy(
        override val id: StrategyId,
        private val evaluator: (StrategyContext) -> StrategySignal
    ) : TradingStrategy {
        var calls = 0
            private set

        override fun evaluate(context: StrategyContext): StrategySignal {
            calls++
            return evaluator(context)
        }
    }

    @Test
    fun loadsCandlesAndPassesThemToAllStrategiesAndDecisionEngine() = runBlocking {
        var requestedSymbol: String? = null
        var requestedResolution: String? = null
        var requestedFrom: Long? = null
        var requestedTo: Long? = null

        val first = StubStrategy(StrategyId.FVG) {
            assertEquals(candles, it.candles)
            signal(StrategyId.FVG, it.symbol)
        }
        val second = StubStrategy(StrategyId.MICRO_SCALPER) {
            assertEquals(candles, it.candles)
            signal(StrategyId.MICRO_SCALPER, it.symbol)
        }

        val coordinator = StrategyAnalysisCoordinator(
            candleLoader = { symbol, resolution, from, to ->
                requestedSymbol = symbol
                requestedResolution = resolution
                requestedFrom = from
                requestedTo = to
                candles
            },
            strategies = listOf(first, second)
        )

        val result = coordinator.analyze(
            symbol = "BTCIRT",
            resolution = "5",
            fromUnixSeconds = 100L,
            toUnixSeconds = 200L
        )

        assertEquals("BTCIRT", requestedSymbol)
        assertEquals("5", requestedResolution)
        assertEquals(100L, requestedFrom)
        assertEquals(200L, requestedTo)
        assertEquals(candles, result.candles)
        assertEquals(1, first.calls)
        assertEquals(1, second.calls)
        assertTrue(result.isFetchSuccessful)
        assertTrue(result.isAnalysisAvailable)
        assertTrue(result.isActionable)
        assertEquals(TradeDirection.LONG, result.decision?.direction)
        assertEquals(2, result.decision?.agreeingStrategyIds?.size)
        assertNull(result.fetchError)
    }

    @Test
    fun insufficientCandlesProduceNoTradeWithoutFetchFailure() = runBlocking {
        val coordinator = StrategyAnalysisCoordinator(
            candleLoader = { _, _, _, _ -> emptyList() }
        )

        val result = coordinator.analyze(
            symbol = "BTCIRT",
            resolution = "60",
            fromUnixSeconds = 100L,
            toUnixSeconds = 200L
        )

        assertTrue(result.isFetchSuccessful)
        assertTrue(result.isAnalysisAvailable)
        assertFalse(result.isActionable)
        assertEquals(0, result.candles.size)
        assertEquals(TradeDirection.FLAT, result.decision?.direction)
        assertNotNull(result.batch)
        assertNull(result.fetchError)
    }

    @Test
    fun invalidCandlesAreRejectedBeforeAnyStrategyRuns() = runBlocking {
        val strategy = StubStrategy(StrategyId.FVG) {
            signal(StrategyId.FVG, it.symbol)
        }
        val invalid = listOf(
            Candle(1_000L, 100.0, 90.0, 95.0, 101.0, 10.0)
        )
        val coordinator = StrategyAnalysisCoordinator(
            candleLoader = { _, _, _, _ -> invalid },
            strategies = listOf(strategy)
        )

        val result = coordinator.analyze(
            "BTCIRT",
            "1",
            100L,
            200L
        )

        assertEquals(0, strategy.calls)
        assertTrue(result.isFetchSuccessful)
        assertTrue(result.isAnalysisAvailable)
        assertFalse(result.isActionable)
        assertFalse(result.batch!!.isInputValid)
        assertEquals(TradeDirection.FLAT, result.decision?.direction)
    }

    @Test
    fun apiFailureIsReportedAndDoesNotCreateFakeAnalysis() = runBlocking {
        val coordinator = StrategyAnalysisCoordinator(
            candleLoader = { _, _, _, _ ->
                throw IllegalStateException("Nobitex unavailable")
            }
        )

        val result = coordinator.analyze(
            "BTCIRT",
            "5",
            100L,
            200L
        )

        assertFalse(result.isFetchSuccessful)
        assertFalse(result.isAnalysisAvailable)
        assertFalse(result.isActionable)
        assertEquals("Nobitex unavailable", result.fetchError)
        assertNull(result.batch)
        assertNull(result.decision)
        assertTrue(result.candles.isEmpty())
    }

    @Test
    fun oneStrategyFailureDoesNotStopOtherStrategies() = runBlocking {
        val failing = StubStrategy(StrategyId.FVG) {
            throw IllegalStateException("FVG evaluation failed")
        }
        val working = StubStrategy(StrategyId.MICRO_SCALPER) {
            signal(StrategyId.MICRO_SCALPER, it.symbol)
        }
        val coordinator = StrategyAnalysisCoordinator(
            candleLoader = { _, _, _, _ -> candles },
            strategies = listOf(failing, working),
            decisionEngine = StrategyDecisionEngine(minimumAgreement = 1)
        )

        val result = coordinator.analyze(
            "BTCIRT",
            "5",
            100L,
            200L
        )

        assertEquals(1, failing.calls)
        assertEquals(1, working.calls)
        assertEquals(1, result.batch?.failures?.size)
        assertEquals(1, result.batch?.signals?.size)
        assertTrue(result.isAnalysisAvailable)
        assertTrue(result.isActionable)
        assertEquals(TradeDirection.LONG, result.decision?.direction)
    }

    @Test
    fun disabledAnalysisFetchesMarketDataButDoesNotEmitActionableSignals() = runBlocking {
        val strategy = StubStrategy(StrategyId.FVG) {
            signal(StrategyId.FVG, it.symbol)
        }
        val coordinator = StrategyAnalysisCoordinator(
            candleLoader = { _, _, _, _ -> candles },
            strategies = listOf(strategy)
        )

        val result = coordinator.analyze(
            symbol = "BTCIRT",
            resolution = "5",
            fromUnixSeconds = 100L,
            toUnixSeconds = 200L,
            enabled = false
        )

        assertEquals(1, strategy.calls)
        assertTrue(result.isFetchSuccessful)
        assertTrue(result.isAnalysisAvailable)
        assertFalse(result.isActionable)
        assertEquals(TradeDirection.FLAT, result.decision?.direction)
    }

    @Test
    fun defaultStrategiesExcludeWhaleWithoutRealTradeFlow() {
        val ids = StrategyAnalysisCoordinator.defaultStrategies().map { it.id }

        assertEquals(
            listOf(
                StrategyId.FMC,
                StrategyId.FVG,
                StrategyId.LIQUIDITY_SWEEP,
                StrategyId.ORDER_BLOCK,
                StrategyId.MICRO_SCALPER
            ),
            ids
        )
        assertFalse(ids.contains(StrategyId.WHALE))
    }
}
