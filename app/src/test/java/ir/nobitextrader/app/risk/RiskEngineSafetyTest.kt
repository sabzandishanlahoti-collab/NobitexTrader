
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
        openRisk: Double = 0.0
    ) = RiskRequest(
        equityUsd = equity,
        peakEquityUsd = peakEquity,
        dayStartEquityUsd = dayStartEquity,
        freeCapitalUsd = 5_000.0,
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
    fun invalidConfigurationMustBeRejected() {
        val invalidConfig = RiskConfig(
            riskPerTradePct = -1.0
        )

        assertFalse(invalidConfig.validate())
    }
}
