
package ir.nobitextrader.app.risk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskEngineSafetyTest {

    private fun validRequest(
        equity: Double = 10_000.0,
        peakEquity: Double = 10_000.0,
        dayStartEquity: Double = 10_000.0,
        killSwitch: Boolean = false,
        marketDataValid: Boolean = true,
        liquiditySufficient: Boolean = true,
        spread: Double = 0.1,
        openRisk: Double = 0.0,
        freeCapital: Double = 5_000.0
    ) = RiskRequest(
        equityUsd = equity,
        peakEquityUsd = peakEquity,
        dayStartEquityUsd = dayStartEquity,
        freeCapitalUsd = freeCapital,
        openRiskUsd = openRisk,
        stopLossPct = 2.0,
        estimatedRoundTripCostsPct = 0.2,
        spreadPct = spread,
        marketDataValid = marketDataValid,
        liquiditySufficient = liquiditySufficient,
        killSwitchActive = killSwitch
    )

    @Test
    fun killSwitchMustRejectTrade() {
        val result = RiskEngine().evaluate(
            validRequest(killSwitch = true)
        )

        assertFalse(result.approved)
        assertTrue(result.reason.contains("KILL"))
    }

    @Test
    fun staleMarketDataMustRejectTrade() {
        val result = RiskEngine().evaluate(
            validRequest(marketDataValid = false)
        )

        assertFalse(result.approved)
    }

    @Test
    fun insufficientLiquidityMustRejectTrade() {
        val result = RiskEngine().evaluate(
            validRequest(liquiditySufficient = false)
        )

        assertFalse(result.approved)
    }

    @Test
    fun excessiveSpreadMustRejectTrade() {
        val result = RiskEngine().evaluate(
            validRequest(spread = 0.8)
        )

        assertFalse(result.approved)
    }

    @Test
    fun dailyLossLimitMustRejectTrade() {
        val result = RiskEngine().evaluate(
            validRequest(
                equity = 9_800.0,
                dayStartEquity = 10_000.0
            )
        )

        assertFalse(result.approved)
        assertTrue(result.dailyLossPct >= 1.5)
    }

    @Test
    fun validRequestMustProducePositivePositionSize() {
        val result = RiskEngine().evaluate(validRequest())

        assertTrue(result.approved)
        assertTrue(result.positionSizeUsd > 0.0)
        assertTrue(result.riskBudgetUsd > 0.0)
    }

    @Test
    fun positionMustNotExceedFreeCapital() {
        val result = RiskEngine().evaluate(validRequest())

        assertTrue(result.approved)
        assertTrue(result.positionSizeUsd <= 5_000.0)
    }


    @Test
    fun protectiveDrawdownModeHalvesThePerTradeRiskBudget() {
        val result = RiskEngine().evaluate(
            validRequest(
                equity = 9_300.0,
                peakEquity = 10_000.0,
                dayStartEquity = 9_300.0
            )
        )

        assertTrue(result.approved)
        assertTrue(result.protectiveMode)
        assertEquals(23.25, result.riskBudgetUsd, 0.000001)
    }

    @Test
    fun remainingOpenRiskAllowanceCapsTheNextTradeBudget() {
        val result = RiskEngine().evaluate(
            validRequest(openRisk = 110.0)
        )

        assertTrue(result.approved)
        assertEquals(40.0, result.riskBudgetUsd, 0.000001)
        assertTrue(result.reason.contains("reduced size"))
    }

    @Test
    fun exhaustedTotalOpenRiskAllowanceRejectsTrade() {
        val result = RiskEngine().evaluate(
            validRequest(openRisk = 150.0)
        )

        assertFalse(result.approved)
        assertTrue(result.reason.contains("open-risk"))
    }

    @Test
    fun positionCapitalLimitCapsCalculatedPositionSize() {
        val engine = RiskEngine(
            RiskConfig(maxPositionCapitalPct = 5.0)
        )

        val result = engine.evaluate(validRequest())

        assertTrue(result.approved)
        assertEquals(500.0, result.positionSizeUsd, 0.000001)
    }

    @Test
    fun freeCapitalAboveEquityIsRejected() {
        val result = RiskEngine().evaluate(
            validRequest(freeCapital = 10_001.0)
        )

        assertFalse(result.approved)
        assertTrue(result.reason.contains("exceeds account equity"))
    }

    @Test
    fun invalidConfigurationMustBeRejected() {
        val invalidConfig = RiskConfig(
            riskPerTradePct = -1.0
        )

        assertFalse(invalidConfig.validate())
    }
}
