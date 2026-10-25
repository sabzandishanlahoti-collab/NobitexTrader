
package ir.nobitextrader.app.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {

    private val engine = RiskEngine()

    private fun request(
        equity: Double = 1000.0,
        peak: Double = 1000.0,
        dayStart: Double = 1000.0,
        freeCapital: Double = 1000.0,
        openRisk: Double = 0.0,
        stopLossPct: Double = 1.0,
        roundTripCostsPct: Double = 0.1,
        spreadPct: Double = 0.1,
        marketDataValid: Boolean = true,
        liquiditySufficient: Boolean = true,
        killSwitchActive: Boolean = false
    ) = RiskRequest(
        equityUsd = equity,
        peakEquityUsd = peak,
        dayStartEquityUsd = dayStart,
        freeCapitalUsd = freeCapital,
        openRiskUsd = openRisk,
        stopLossPct = stopLossPct,
        roundTripCostsPct = roundTripCostsPct,
        spreadPct = spreadPct,
        marketDataValid = marketDataValid,
        liquiditySufficient = liquiditySufficient,
        killSwitchActive = killSwitchActive
    )

    @Test
    fun approvesValidRequest() {
        val result = engine.evaluate(request())

        assertTrue(result.approved)
        assertFalse(result.protectiveMode)
        assertTrue(result.riskBudgetUsd > 0.0)
    }

    @Test
    fun protectiveModeReducesRiskBudget() {
        val result = engine.evaluate(
            request(
                equity = 930.0,
                peak = 1000.0,
                dayStart = 930.0
            )
        )

        assertTrue(result.approved)
        assertTrue(result.protectiveMode)
        assertEquals(2.325, result.riskBudgetUsd, 0.001)
    }

    @Test
    fun rejectsActiveKillSwitch() {
        val result = engine.evaluate(
            request(killSwitchActive = true)
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsInvalidMarketData() {
        val result = engine.evaluate(
            request(marketDataValid = false)
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsInsufficientLiquidity() {
        val result = engine.evaluate(
            request(liquiditySufficient = false)
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsSpreadAboveLimit() {
        val result = engine.evaluate(
            request(spreadPct = 1.0)
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsMaximumDrawdown() {
        val result = engine.evaluate(
            request(
                equity = 890.0,
                peak = 1000.0,
                dayStart = 1000.0
            )
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsDailyLossLimit() {
        val result = engine.evaluate(
            request(
                equity = 980.0,
                peak = 1000.0,
                dayStart = 1000.0
            )
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsNoFreeCapital() {
        val result = engine.evaluate(
            request(freeCapital = 0.0)
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsExcessiveOpenRisk() {
        val result = engine.evaluate(
            request(openRisk = 20.0)
        )

        assertFalse(result.approved)
    }

    @Test
    fun rejectsInvalidEquity() {
        val result = engine.evaluate(
            request(equity = 0.0)
        )

        assertFalse(result.approved)
    }
}
