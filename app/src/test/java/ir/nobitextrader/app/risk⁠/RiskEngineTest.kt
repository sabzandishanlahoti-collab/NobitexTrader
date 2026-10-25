
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
        stopLossPct: Double = 2.0,
        costsPct: Double = 0.1,
        spreadPct: Double = 0.1,
        validData: Boolean = true,
        sufficientLiquidity: Boolean = true,
        killSwitch: Boolean = false
    ) = RiskRequest(
        equityUsd = equity,
        peakEquityUsd = peak,
        dayStartEquityUsd = dayStart,
        freeCapitalUsd = freeCapital,
        openRiskUsd = openRisk,
        stopLossPct = stopLossPct,
        estimatedRoundTripCostsPct = costsPct,
        spreadPct = spreadPct,
        marketDataValid = validData,
        liquiditySufficient = sufficientLiquidity,
        killSwitchActive = killSwitch
    )

    @Test
    fun validTradeIsApprovedWithinLimits() {
        val result = engine.evaluate(request())

        assertTrue(result.approved)
        assertEquals(
            238.095238,
            result.positionSizeUsd,
            0.01
        )
        assertFalse(result.protectiveMode)
    }

    @Test
    fun killSwitchRejectsTrade() {
        val result = engine.evaluate(
            request(killSwitch = true)
        )

        assertFalse(result.approved)
        assertTrue(result.reason.contains("KILL"))
    }

    @Test
    fun invalidMarketDataRejectsTrade() {
        val result = engine.evaluate(
            request(validData = false)
        )

        assertFalse(result.approved)
    }

    @Test
    fun insufficientLiquidityRejectsTrade() {
        val result = engine.evaluate(
            request(sufficientLiquidity = false)
        )

        assertFalse(result.approved)
    }

    @Test
    fun excessiveSpreadRejectsTrade() {
        val result = engine.evaluate(
            request(spreadPct = 0.8)
        )

        assertFalse(result.approved)
    }

    @Test
    fun invalidStopLossRejectsTrade() {
        val result = engine.evaluate(
            request(stopLossPct = 0.0)
        )

        assertFalse(result.approved)
    }

    @Test
    fun maximumDrawdownLocksNewTrades() {
        val result = engine.evaluate(
            request(equity = 900.0, peak = 1000.0)
        )

        assertFalse(result.approved)
        assertEquals(10.0, result.drawdownPct, 0.0001)
    }

    @Test
    fun dailyLossLimitRejectsTrade() {
        val result = engine.evaluate(
            request(equity = 980.0, dayStart = 1000.0)
        )

        assertFalse(result.approved)
        assertEquals(2.0, result.dailyLossPct, 0.0001)
    }

    @Test
    fun protectiveModeReducesRiskBudget() {
        val result = engine.evaluate(
            request(equity = 930.0, peak = 1000.0)
        )

        assertTrue(result.approved)
        assertTrue(result.protectiveMode)
        assertEquals(4.65, result.riskBudgetUsd, 0.001)
    }

    @Test
    fun totalOpenRiskLimitRejectsAdditionalTrade() {
        val result = engine.evaluate(
            request(openRisk = 15.0)
        )

        assertFalse(result.approved)
    }

    @Test
    fun positionSizeNeverExceedsFreeCapital() {
        val result = engine.evaluate(
            request(freeCapital = 100.0)
        )

        assertTrue(result.approved)
        assertTrue(result.positionSizeUsd <= 100.0)
    }

    @Test
    fun invalidConfigurationIsRejected() {
        val invalidConfig = RiskConfig(
            maxDrawdownPct = 5.0,
            protectiveDrawdownPct = 7.0
        )

        var rejected = false

        try {
            RiskEngine(invalidConfig)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
    }
}
