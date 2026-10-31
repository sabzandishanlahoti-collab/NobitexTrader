package ir.nobitextrader.app

import ir.nobitextrader.app.scanner.MarketScanResult
import ir.nobitextrader.app.strategy.StrategyDecision
import ir.nobitextrader.app.strategy.StrategyId
import ir.nobitextrader.app.strategy.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaperAutoTradingPolicyTest {
    private val market = MarketScanResult(
        symbol = "BTCIRT",
        lastPrice = 100.0,
        bestBid = 99.0,
        bestAsk = 101.0,
        spreadPercent = 2.02,
        bidLiquidityTop5 = 100000.0,
        askLiquidityTop5 = 100000.0,
        success = true
    )

    private val portfolio = PaperPortfolio(
        cash = 1_000_000.0,
        positions = listOf(PaperPosition("BTCIRT", 2.0, 90.0)),
        trades = emptyList(),
        realizedPnl = 0.0
    )

    private fun decision(direction: TradeDirection) = StrategyDecision(
        symbol = "BTCIRT",
        direction = direction,
        confidence = 0.9,
        agreeingStrategyIds = listOf(StrategyId.FVG, StrategyId.FMC),
        longStrategyIds = if (direction == TradeDirection.LONG) listOf(StrategyId.FVG, StrategyId.FMC) else emptyList(),
        shortStrategyIds = if (direction == TradeDirection.SHORT) listOf(StrategyId.FVG, StrategyId.FMC) else emptyList(),
        reason = "test consensus",
        isActionable = direction != TradeDirection.FLAT
    )

    private fun plan(
        direction: TradeDirection = TradeDirection.LONG,
        snapshot: MarketScanResult = market,
        fee: Double = 0.001,
        budget: Double = 10_000.0,
        observedAt: Long = 100_000L,
        now: Long = 101_000L
    ) = PaperAutoTradingPolicy.plan(
        decision(direction), snapshot, portfolio, fee, budget,
        observedAt, now, maxSpreadPercent = 3.0
    )

    @Test fun longUsesBestAskAndStaysWithinBudget() {
        val result = plan()
        assertTrue(result is PaperAutoTradePlan.Approved)
        result as PaperAutoTradePlan.Approved
        assertEquals("BUY", result.side)
        assertEquals(101.0, result.limitPrice, 0.0)
        assertTrue(result.estimatedValueIrr <= 10_000.0)
    }

    @Test fun shortSellsOnlyExistingVirtualPositionAtBestBid() {
        val result = plan(TradeDirection.SHORT)
        assertTrue(result is PaperAutoTradePlan.Approved)
        result as PaperAutoTradePlan.Approved
        assertEquals("SELL", result.side)
        assertEquals(99.0, result.limitPrice, 0.0)
        assertTrue(result.quantity <= 2.0)
    }

    @Test fun rejectsStaleMarketData() {
        val result = plan(observedAt = 1L, now = 20_000L)
        assertTrue(result is PaperAutoTradePlan.Rejected)
        assertTrue((result as PaperAutoTradePlan.Rejected).reason.contains("stale"))
    }

    @Test fun rejectsWideSpread() {
        val result = plan(snapshot = market.copy(spreadPercent = 4.0))
        assertTrue(result is PaperAutoTradePlan.Rejected)
        assertTrue((result as PaperAutoTradePlan.Rejected).reason.contains("Spread"))
    }

    @Test fun rejectsShortWithoutPosition() {
        val empty = portfolio.copy(positions = emptyList())
        val result = PaperAutoTradingPolicy.plan(
            decision(TradeDirection.SHORT), market.copy(spreadPercent = 0.5),
            empty, 0.001, 10_000.0, 100_000L, 101_000L
        )
        assertTrue(result is PaperAutoTradePlan.Rejected)
        assertTrue((result as PaperAutoTradePlan.Rejected).reason.contains("No virtual position"))
    }

    @Test fun rejectsUnconfiguredFeeAndNonActionableDecision() {
        assertTrue(plan(fee = 0.0) is PaperAutoTradePlan.Rejected)
        assertTrue(plan(direction = TradeDirection.FLAT) is PaperAutoTradePlan.Rejected)
    }
}
