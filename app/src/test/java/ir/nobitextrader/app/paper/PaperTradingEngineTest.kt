package ir.nobitextrader.app.paper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperTradingEngineTest {
    @Test fun buyUsesAskAndDebitsFee() {
        val start = PaperAccount.create(1_000.0)
        val bought = PaperTradingEngine.buy(start, "btcirt", 100.0, 500.0, 0.001, 10L)
        assertEquals(499.5, bought.cash, 1e-9)
        assertEquals(5.0, bought.positions.getValue("BTCIRT").quantity, 1e-9)
        assertEquals(0.5, bought.trades.single().fee, 1e-9)
        assertEquals(100.1, bought.positions.getValue("BTCIRT").averageEntryPrice, 1e-9)
    }

    @Test fun sellUsesBidAndRealizesPnlAfterBothFees() {
        val bought = PaperTradingEngine.buy(PaperAccount.create(1_000.0), "BTCIRT", 100.0, 500.0, 0.001, 10L)
        val sold = PaperTradingEngine.sell(bought, "BTCIRT", 110.0, 5.0, 0.001, 20L)
        assertEquals(1_048.95, sold.cash, 1e-8)
        assertEquals(48.95, sold.realizedPnl, 1e-8)
        assertTrue(sold.positions.isEmpty())
        assertEquals(PaperSide.SELL, sold.trades.last().side)
    }

    @Test fun partialSellKeepsProportionalCostBasis() {
        val bought = PaperTradingEngine.buy(PaperAccount.create(1_000.0), "ETHIRT", 10.0, 500.0, 0.001, 1L)
        val sold = PaperTradingEngine.sell(bought, "ETHIRT", 11.0, 20.0, 0.001, 2L)
        val remaining = sold.positions.getValue("ETHIRT")
        assertEquals(30.0, remaining.quantity, 1e-9)
        assertEquals(300.3, remaining.costBasis, 1e-8)
        assertEquals(19.58, sold.realizedPnl, 1e-8)
    }

    @Test fun rejectsOversellAndInsufficientCash() {
        val bought = PaperTradingEngine.buy(PaperAccount.create(100.0), "BTCIRT", 10.0, 50.0)
        try {
            PaperTradingEngine.sell(bought, "BTCIRT", 10.0, 6.0)
            throw AssertionError("Oversell should fail")
        } catch (_: IllegalArgumentException) { }
        try {
            PaperTradingEngine.buy(bought, "ETHIRT", 10.0, 100.0)
            throw AssertionError("Insufficient cash should fail")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun paperEngineControlsFollowSafeTransitions() {
        assertEquals(PaperEngineMode.RUNNING, PaperEngineControl.start(PaperEngineMode.STOPPED))
        assertEquals(PaperEngineMode.STOPPED, PaperEngineControl.stop())
        assertEquals(PaperEngineMode.KILLED, PaperEngineControl.kill())
        assertEquals(PaperEngineMode.RUNNING, PaperEngineControl.start(PaperEngineMode.KILLED))
    }

    @Test fun valuationMarksOpenPositionsAtBidAndIncludesExitFeeEstimate() {
        val bought = PaperTradingEngine.buy(PaperAccount.create(1_000.0), "BTCIRT", 100.0, 500.0)
        val value = PaperTradingEngine.value(bought, mapOf("BTCIRT" to 110.0))
        assertEquals(1_048.95, value.totalEquity, 1e-8)
        assertEquals(48.95, value.unrealizedPnl, 1e-8)
    }
}
