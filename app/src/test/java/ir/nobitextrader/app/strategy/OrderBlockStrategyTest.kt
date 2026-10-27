package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderBlockStrategyTest {

    private val strategy = OrderBlockStrategy(
        lookbackCandles = 3,
        displacementPercent = 0.20,
        minimumBodyToRangeRatio = 0.60,
        rewardRiskRatio = 2.0
    )

    @Test
    fun bullishOrderBlockRetestReturnsLong() {
        val candles = listOf(
            candle(1, 100.8, 101.0, 100.0, 100.2),
            candle(2, 100.0, 100.2, 99.0, 99.2),
            candle(3, 99.5, 102.0, 99.4, 101.8),
            candle(4, 101.5, 102.0, 100.8, 101.7),
            candle(5, 101.7, 102.2, 101.0, 101.8),
            candle(6, 102.0, 102.1, 100.1, 101.5)
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
    fun bearishOrderBlockRetestReturnsShort() {
        val candles = listOf(
            candle(1, 99.2, 100.0, 99.0, 99.8),
            candle(2, 100.0, 101.0, 99.8, 100.8),
            candle(3, 100.5, 100.6, 98.0, 98.2),
            candle(4, 98.5, 99.0, 98.0, 98.3),
            candle(5, 98.4, 98.8, 97.8, 98.0),
            candle(6, 98.0, 100.1, 97.9, 98.5)
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
                symbol = "BTCUSDT",
                candles = listOf(
                    candle(1, 100.0, 101.0, 99.0, 100.0),
                    candle(2, 100.0, 102.0, 99.5, 101.0)
                )
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INSUFFICIENT_DATA, result.reason)
    }

    @Test
    fun disabledStrategyReturnsFlatSignal() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = validCandles(),
                enabled = false
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    @Test
    fun invalidCandleReturnsInvalidMarketData() {
        val candles = listOf(
            candle(1, 100.0, 101.0, 99.0, 100.0),
            candle(2, 100.0, 102.0, 99.5, 101.0),
            candle(3, 101.0, 99.0, 100.0, 102.0),
            candle(4, 102.0, 103.0, 101.0, 102.5),
            candle(5, 102.5, 103.0, 102.0, 102.8),
            candle(6, 102.8, 103.0, 102.0, 102.5)
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
            candle(1, 100.0, 101.0, 99.0, 100.0),
            candle(3, 100.0, 102.0, 99.5, 101.0),
            candle(2, 101.0, 102.0, 100.0, 101.5),
            candle(4, 101.5, 102.0, 100.8, 101.7),
            candle(5, 101.7, 102.2, 101.0, 101.8),
            candle(6, 102.0, 102.1, 100.1, 101.5)
        )

        val result = strategy.evaluate(
            StrategyContext("BTCUSDT", candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INVALID_MARKET_DATA, result.reason)
    }

    private fun validCandles(): List<Candle> = listOf(
        candle(1, 100.8, 101.0, 100.0, 100.2),
        candle(2, 100.0, 100.2, 99.0, 99.2),
        candle(3, 99.5, 102.0, 99.4, 101.8),
        candle(4, 101.5, 102.0, 100.8, 101.7),
        candle(5, 101.7, 102.2, 101.0, 101.8),
        candle(6, 102.0, 102.1, 100.1, 101.5)
    )

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
