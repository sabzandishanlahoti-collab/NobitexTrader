package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhalePressureStrategyTest {

    private val strategy = WhalePressureStrategy(
        minimumTrades = 4,
        minimumImbalance = 0.20,
        minimumLargeTradeCount = 1,
        largeTradeMultiplier = 1.5,
        rewardRiskRatio = 2.0,
        stopLossPercent = 0.50
    )

    @Test
    fun strongBuyPressureReturnsLong() {
        val trades = listOf(
            trade(1, 10.0, WhalePressureStrategy.Aggressor.BUY),
            trade(2, 10.0, WhalePressureStrategy.Aggressor.BUY),
            trade(3, 1.0, WhalePressureStrategy.Aggressor.SELL),
            trade(4, 1.0, WhalePressureStrategy.Aggressor.SELL)
        )

        strategy.updateTradeFlow(
            WhalePressureStrategy.TradeFlow(
                trades = trades,
                referencePrice = 100.0
            )
        )

        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles()
            )
        )

        assertEquals(TradeDirection.LONG, result.direction)
        assertEquals(SignalReason.SETUP_CONFIRMED, result.reason)
        assertNotNull(result.referencePrice)
        assertNotNull(result.stopLossPrice)
        assertNotNull(result.takeProfitPrice)
        assertTrue(result.stopLossPrice!! < result.referencePrice!!)
        assertTrue(result.takeProfitPrice!! > result.referencePrice!!)
    }

    @Test
    fun strongSellPressureReturnsShort() {
        val trades = listOf(
            trade(1, 10.0, WhalePressureStrategy.Aggressor.SELL),
            trade(2, 10.0, WhalePressureStrategy.Aggressor.SELL),
            trade(3, 1.0, WhalePressureStrategy.Aggressor.BUY),
            trade(4, 1.0, WhalePressureStrategy.Aggressor.BUY)
        )

        strategy.updateTradeFlow(
            WhalePressureStrategy.TradeFlow(
                trades = trades,
                referencePrice = 100.0
            )
        )

        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles()
            )
        )

        assertEquals(TradeDirection.SHORT, result.direction)
        assertEquals(SignalReason.SETUP_CONFIRMED, result.reason)
        assertNotNull(result.referencePrice)
        assertNotNull(result.stopLossPrice)
        assertNotNull(result.takeProfitPrice)
        assertTrue(result.stopLossPrice!! > result.referencePrice!!)
        assertTrue(result.takeProfitPrice!! < result.referencePrice!!)
        assertTrue(result.takeProfitPrice!! > 0.0)
    }

    @Test
    fun balancedTradeFlowReturnsNoSetup() {
        val trades = listOf(
            trade(1, 2.0, WhalePressureStrategy.Aggressor.BUY),
            trade(2, 2.0, WhalePressureStrategy.Aggressor.SELL),
            trade(3, 2.0, WhalePressureStrategy.Aggressor.BUY),
            trade(4, 2.0, WhalePressureStrategy.Aggressor.SELL)
        )

        strategy.updateTradeFlow(
            WhalePressureStrategy.TradeFlow(
                trades = trades,
                referencePrice = 100.0
            )
        )

        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles()
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    @Test
    fun missingTradeFlowReturnsFlatSignal() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles()
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INSUFFICIENT_DATA, result.reason)
    }

    @Test
    fun disabledStrategyReturnsFlatSignal() {
        val trades = listOf(
            trade(1, 10.0, WhalePressureStrategy.Aggressor.BUY),
            trade(2, 10.0, WhalePressureStrategy.Aggressor.BUY),
            trade(3, 1.0, WhalePressureStrategy.Aggressor.SELL),
            trade(4, 1.0, WhalePressureStrategy.Aggressor.SELL)
        )

        strategy.updateTradeFlow(
            WhalePressureStrategy.TradeFlow(
                trades = trades,
                referencePrice = 100.0
            )
        )

        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles(),
                enabled = false
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    private fun trade(
        timestamp: Long,
        quantity: Double,
        aggressor: WhalePressureStrategy.Aggressor
    ) = WhalePressureStrategy.MarketTrade(
        timestampMillis = timestamp,
        price = 100.0,
        quantity = quantity,
        aggressor = aggressor
    )

    private fun candles(): List<Candle> = listOf(
        Candle(1, 100.0, 101.0, 99.0, 100.0, 100.0),
        Candle(2, 100.0, 101.5, 99.5, 101.0, 120.0),
        Candle(3, 101.0, 102.0, 100.0, 101.5, 110.0),
        Candle(4, 101.5, 102.0, 100.5, 101.0, 105.0),
        Candle(5, 101.0, 102.0, 100.0, 101.5, 115.0)
    )
}
