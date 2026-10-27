package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FvgStrategyTest {

    private val strategy = FvgStrategy(
        minGapPercent = 0.05,
        rewardRiskRatio = 2.0
    )

    @Test
    fun bullishFvgReturnsLongSignal() {
        val candles = listOf(
            candle(1, 100.0, 101.0, 99.5, 100.0),
            candle(2, 101.0, 104.0, 100.5, 103.0),
            candle(3, 103.0, 105.0, 102.0, 104.0)
        )

        val result = strategy.evaluate(
            StrategyContext("BTCUSDT", candles)
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
    fun bearishFvgReturnsShortSignal() {
        val candles = listOf(
            candle(1, 105.0, 106.0, 104.0, 105.0),
            candle(2, 104.0, 104.5, 100.0, 101.0),
            candle(3, 101.0, 102.0, 99.0, 100.0)
        )

        val result = strategy.evaluate(
            StrategyContext("BTCUSDT", candles)
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
    fun insufficientCandlesReturnsInsufficientData() {
        val result = strategy.evaluate(
            StrategyContext(
                "BTCUSDT",
                listOf(
                    candle(1, 100.0, 101.0, 99.0, 100.0),
                    candle(2, 100.0, 102.0, 99.5, 101.0)
                )
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INSUFFICIENT_DATA, result.reason)
    }

    @Test
    fun disabledStrategyDoesNotGenerateEntry() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = listOf(
                    candle(1, 100.0, 101.0, 99.5, 100.0),
                    candle(2, 101.0, 104.0, 100.5, 103.0),
                    candle(3, 103.0, 105.0, 102.0, 104.0)
                ),
                enabled = false
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    @Test
    fun noGapReturnsFlatSignal() {
        val candles = listOf(
            candle(1, 100.0, 102.0, 98.0, 100.0),
            candle(2, 100.0, 103.0, 99.0, 101.0),
            candle(3, 101.0, 102.0, 100.0, 101.5)
        )

        val result = strategy.evaluate(
            StrategyContext("BTCUSDT", candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    @Test
    fun invalidCandleReturnsInvalidMarketData() {
        val candles = listOf(
            candle(1, 100.0, 101.0, 99.5, 100.0),
            candle(2, 101.0, 104.0, 100.5, 103.0),
            candle(3, 103.0, 102.0, 99.0, 104.0)
        )

        val result = strategy.evaluate(
            StrategyContext("BTCUSDT", candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INVALID_MARKET_DATA, result.reason)
    }

    @Test
    fun nonChronologicalCandlesReturnInvalidMarketData() {
        val candles = listOf(
            candle(2, 100.0, 101.0, 99.5, 100.0),
            candle(1, 101.0, 104.0, 100.5, 103.0),
            candle(3, 103.0, 105.0, 102.0, 104.0)
        )

        val result = strategy.evaluate(
            StrategyContext("BTCUSDT", candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INVALID_MARKET_DATA, result.reason)
    }

    private fun candle(
        timestamp: Long,
        open: Double,
        high: Double,
        low: Double,
        close: Double
    ) = Candle(
        timestampMillis = timestamp,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = 100.0
    )
}
