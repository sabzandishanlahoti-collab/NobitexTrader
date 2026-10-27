package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FmcStrategyTest {

    private val strategy = FmcStrategy(
        lookbackCandles = 3,
        minimumBodyPercent = 0.15,
        minimumVolumeRatio = 1.5,
        rewardRiskRatio = 2.0,
        stopLossPercent = 0.50
    )

    @Test
    fun insufficientCandlesReturnsInsufficientData() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = listOf(
                    candle(1, 100.0, 101.0, 99.0, 100.5, 100.0),
                    candle(2, 100.5, 101.0, 100.0, 100.7, 110.0)
                )
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INSUFFICIENT_DATA, result.reason)
    }

    @Test
    fun bullishMomentumWithHighVolumeReturnsLongSignal() {
        val candles = listOf(
            candle(1, 100.0, 100.5, 99.5, 100.1, 100.0),
            candle(2, 100.1, 100.6, 99.8, 100.2, 100.0),
            candle(3, 100.2, 100.7, 100.0, 100.3, 100.0),
            candle(4, 100.3, 101.2, 100.2, 101.0, 200.0)
        )

        val result = strategy.evaluate(
            StrategyContext(symbol = "BTCUSDT", candles = candles)
        )

        assertEquals(TradeDirection.LONG, result.direction)
        assertEquals(SignalReason.SETUP_CONFIRMED, result.reason)
        assertNotNull(result.referencePrice)
        assertNotNull(result.stopLossPrice)
        assertNotNull(result.takeProfitPrice)

        assertTrue(result.stopLossPrice!! < result.referencePrice!!)
        assertTrue(result.takeProfitPrice!! > result.referencePrice!!)
        assertTrue(result.confidence in 0.5..0.9)
    }

    @Test
    fun bearishMomentumWithHighVolumeReturnsShortSignal() {
        val candles = listOf(
            candle(1, 100.0, 100.5, 99.5, 100.1, 100.0),
            candle(2, 100.1, 100.6, 99.8, 100.2, 100.0),
            candle(3, 100.2, 100.7, 100.0, 100.3, 100.0),
            candle(4, 101.0, 101.1, 99.8, 100.0, 200.0)
        )

        val result = strategy.evaluate(
            StrategyContext(symbol = "BTCUSDT", candles = candles)
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
    fun lowVolumeReturnsNoSetup() {
        val candles = listOf(
            candle(1, 100.0, 100.5, 99.5, 100.1, 100.0),
            candle(2, 100.1, 100.6, 99.8, 100.2, 100.0),
            candle(3, 100.2, 100.7, 100.0, 100.3, 100.0),
            candle(4, 100.3, 101.2, 100.2, 101.0, 100.0)
        )

        val result = strategy.evaluate(
            StrategyContext(symbol = "BTCUSDT", candles = candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    @Test
    fun disabledStrategyReturnsNoSetup() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = validCandles(),
                enabled = false
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
        assertTrue(result.details.contains("disabled", ignoreCase = true))
    }

    @Test
    fun invalidCandleReturnsInvalidMarketData() {
        val candles = listOf(
            candle(1, 100.0, 100.5, 99.5, 100.1, 100.0),
            candle(2, 100.1, 100.6, 99.8, 100.2, 100.0),
            candle(3, 100.2, 100.7, 100.0, 100.3, 100.0),
            candle(4, 100.3, 99.0, 100.2, 101.0, 200.0)
        )

        val result = strategy.evaluate(
            StrategyContext(symbol = "BTCUSDT", candles = candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INVALID_MARKET_DATA, result.reason)
    }

    @Test
    fun nonChronologicalCandlesReturnInvalidMarketData() {
        val candles = listOf(
            candle(1, 100.0, 100.5, 99.5, 100.1, 100.0),
            candle(3, 100.1, 100.6, 99.8, 100.2, 100.0),
            candle(2, 100.2, 100.7, 100.0, 100.3, 100.0),
            candle(4, 100.3, 101.2, 100.2, 101.0, 200.0)
        )

        val result = strategy.evaluate(
            StrategyContext(symbol = "BTCUSDT", candles = candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INVALID_MARKET_DATA, result.reason)
    }

    @Test
    fun flatLatestCandleDoesNotGenerateDirectionalSignal() {
        val candles = listOf(
            candle(1, 100.0, 100.5, 99.5, 100.1, 100.0),
            candle(2, 100.1, 100.6, 99.8, 100.2, 100.0),
            candle(3, 100.2, 100.7, 100.0, 100.3, 100.0),
            candle(4, 100.5, 101.0, 100.0, 100.5, 200.0)
        )

        val result = strategy.evaluate(
            StrategyContext(symbol = "BTCUSDT", candles = candles)
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertNotEquals(SignalReason.SETUP_CONFIRMED, result.reason)
    }

    private fun validCandles(): List<Candle> = listOf(
        candle(1, 100.0, 100.5, 99.5, 100.1, 100.0),
        candle(2, 100.1, 100.6, 99.8, 100.2, 100.0),
        candle(3, 100.2, 100.7, 100.0, 100.3, 100.0),
        candle(4, 100.3, 101.2, 100.2, 101.0, 200.0)
    )

    private fun candle(
        timestamp: Long,
        open: Double,
        high: Double,
        low: Double,
        close: Double,
        volume: Double
    ) = Candle(
        timestampMillis = timestamp,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume
    )
}
