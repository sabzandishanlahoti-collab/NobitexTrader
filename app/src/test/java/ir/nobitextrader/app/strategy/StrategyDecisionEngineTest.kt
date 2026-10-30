package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrategyDecisionEngineTest {

    private fun signal(
        id: StrategyId,
        direction: TradeDirection = TradeDirection.LONG,
        confidence: Double = 0.8,
        reason: SignalReason = SignalReason.SETUP_CONFIRMED
    ) = StrategySignal(
        strategyId = id,
        symbol = "BTCIRT",
        direction = direction,
        confidence = confidence,
        reason = reason
    )

    private fun batch(
        signals: List<StrategySignal>,
        inputError: String? = null,
        failures: List<StrategyExecutionFailure> = emptyList()
    ) = StrategyBatchResult(
        symbol = "BTCIRT",
        expectedStrategyCount = signals.size + failures.size,
        attemptedStrategyCount = signals.size + failures.size,
        signals = signals,
        failures = failures,
        inputError = inputError
    )

    @Test
    fun twoConfirmedLongSignalsProduceActionableLongDecision() {
        val result = StrategyDecisionEngine().decide(
            batch(
                listOf(
                    signal(StrategyId.FVG, confidence = 0.8),
                    signal(StrategyId.ORDER_BLOCK, confidence = 0.6)
                )
            )
        )

        assertTrue(result.isActionable)
        assertEquals("BTCIRT", result.symbol)
        assertEquals(TradeDirection.LONG, result.direction)
        assertEquals(0.7, result.confidence, 0.000001)
        assertEquals(
            listOf(StrategyId.FVG, StrategyId.ORDER_BLOCK),
            result.agreeingStrategyIds
        )
        assertEquals(TradeDirection.LONG, result.candidateDirection ?: result.direction)
    }

    @Test
    fun twoConfirmedShortSignalsProduceActionableShortDecision() {
        val result = StrategyDecisionEngine().decide(
            batch(
                listOf(
                    signal(StrategyId.FVG, TradeDirection.SHORT),
                    signal(StrategyId.LIQUIDITY_SWEEP, TradeDirection.SHORT)
                )
            )
        )

        assertTrue(result.isActionable)
        assertEquals(TradeDirection.SHORT, result.direction)
        assertEquals(2, result.agreeingStrategyIds.size)
    }

    @Test
    fun oneConfirmedSignalDoesNotMeetDefaultAgreement() {
        val result = StrategyDecisionEngine().decide(
            batch(listOf(signal(StrategyId.FVG)))
        )

        assertFalse(result.isActionable)
        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(TradeDirection.LONG, result.candidateDirection)
        assertEquals(listOf(StrategyId.FVG), result.agreeingStrategyIds)
        assertTrue(result.reason.contains("Not enough agreeing"))
    }

    @Test
    fun opposingConfirmedSignalsAlwaysProduceNoTrade() {
        val result = StrategyDecisionEngine().decide(
            batch(
                listOf(
                    signal(StrategyId.FVG, TradeDirection.LONG),
                    signal(StrategyId.ORDER_BLOCK, TradeDirection.LONG),
                    signal(StrategyId.LIQUIDITY_SWEEP, TradeDirection.SHORT)
                )
            )
        )

        assertFalse(result.isActionable)
        assertEquals(TradeDirection.FLAT, result.direction)
        assertTrue(result.reason.contains("Conflicting"))
        assertEquals(2, result.longStrategyIds.size)
        assertEquals(listOf(StrategyId.LIQUIDITY_SWEEP), result.shortStrategyIds)
    }

    @Test
    fun nonConfirmedSignalsDoNotCountTowardAgreement() {
        val result = StrategyDecisionEngine().decide(
            batch(
                listOf(
                    signal(StrategyId.FVG),
                    signal(
                        StrategyId.ORDER_BLOCK,
                        reason = SignalReason.NO_SETUP
                    )
                )
            )
        )

        assertFalse(result.isActionable)
        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(TradeDirection.LONG, result.candidateDirection)
        assertEquals(listOf(StrategyId.FVG), result.agreeingStrategyIds)
        assertTrue(result.reason.contains("Not enough agreeing"))
    }

    @Test
    fun invalidInputAlwaysProducesNoTrade() {
        val result = StrategyDecisionEngine().decide(
            batch(
                signals = listOf(
                    signal(StrategyId.FVG),
                    signal(StrategyId.ORDER_BLOCK)
                ),
                inputError = "Invalid candle data"
            )
        )

        assertFalse(result.isActionable)
        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals("Invalid candle data", result.reason)
    }

    @Test
    fun strategyFailureDoesNotDiscardAgreementFromWorkingStrategies() {
        val failure = StrategyExecutionFailure(
            strategyId = StrategyId.WHALE,
            message = "Trade-flow feed unavailable"
        )
        val result = StrategyDecisionEngine().decide(
            batch(
                signals = listOf(
                    signal(StrategyId.FVG),
                    signal(StrategyId.ORDER_BLOCK)
                ),
                failures = listOf(failure)
            )
        )

        assertTrue(result.isActionable)
        assertEquals(TradeDirection.LONG, result.direction)
        assertEquals(2, result.agreeingStrategyIds.size)
    }

    @Test
    fun customAgreementThresholdIsRespected() {
        val result = StrategyDecisionEngine(minimumAgreement = 3).decide(
            batch(
                listOf(
                    signal(StrategyId.FVG),
                    signal(StrategyId.ORDER_BLOCK)
                )
            )
        )

        assertFalse(result.isActionable)
        assertTrue(result.reason.contains("2/3"))
    }

    @Test
    fun zeroAgreementThresholdIsRejected() {
        try {
            StrategyDecisionEngine(minimumAgreement = 0)
            throw AssertionError("Expected minimum agreement validation to fail")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("at least 1"))
        }
    }
}
