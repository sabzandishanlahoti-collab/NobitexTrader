
package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaliperStrategyTest {

    private val strategy = ScaliperStrategy()

    @Test
    fun bullishMomentumWithVolumeConfirmationReturnsLong() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = risingCandles()
            )
        )

        assertEquals(TradeDirection.LONG, result.direction)
        assertEquals(SignalReason.SETUP_CONFIRMED, result.reason)
        assertNotNull(result.referencePrice)
        assertNotNull(result.stopLossPrice)
        assertNotNull(result.takeProfitPrice)
        assertTrue(result.confidence in 0.0..1.0)
        assertTrue(result.stopLossPrice!! < result.referencePrice!!)
        assertTrue(result.takeProfitPrice!! > result.referencePrice!!)
    }

    @Test
    fun bearishMomentumWithVolumeConfirmationReturnsShort() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = fallingCandles()
            )
        )

        assertEquals(TradeDirection.SHORT, result.direction)
        assertEquals(SignalReason.SETUP_CONFIRMED, result.reason)
        assertNotNull(result.referencePrice)
        assertNotNull(result.stopLossPrice)
        assertNotNull(result.takeProfitPrice)
        assertTrue(result.confidence in 0.0..1.0)
        assertTrue(result.stopLossPrice!! > result.referencePrice!!)
        assertTrue(result.takeProfitPrice!! < result.referencePrice!!)
        assertTrue(result.takeProfitPrice!! > 0.0)
    }

    @Test
    fun insufficientCandlesReturnsFlatSignal() {
        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = risingCandles().take(13)
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
                candles = risingCandles(),
                enabled = false
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    @Test
    fun insufficientVolumeConfirmationDoesNotCreateSignal() {
        val candles = risingCandles().mapIndexed { index, candle ->
            if (index == 13) {
                candle.copy(volume = 100.0)
            } else {
                candle.copy(volume = 100.0)
            }
        }

        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.NO_SETUP, result.reason)
    }

    @Test
    fun invalidCandleReturnsInvalidMarketData() {
        val candles = risingCandles().toMutableList()
        candles[5] = candles[5].copy(
            high = 90.0,
            low = 95.0
        )

        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INVALID_MARKET_DATA, result.reason)
    }

    @Test
    fun nonChronologicalCandlesReturnInvalidMarketData() {
        val candles = risingCandles().toMutableList()
        candles[5] = candles[5].copy(timestampMillis = 1L)

        val result = strategy.evaluate(
            StrategyContext(
                symbol = "BTCUSDT",
                candles = candles
            )
        )

        assertEquals(TradeDirection.FLAT, result.direction)
        assertEquals(SignalReason.INVALID_MARKET_DATA, result.reason)
    }

    private fun risingCandles(): List<Candle> {
        val closes = listOf(
            100.0, 100.2, 100.4, 100.6, 100.8,
            101.0, 101.2, 101.4, 101.6, 101.8,
            102.0, 102.2, 102.4, 103.0
        )

        return closes.mapIndexed { index, close ->
            val previous = if (index == 0) close - 0.1 else closes[index - 1]
            Candle(
                timestampMillis = (index + 1).toLong() * 60_000L,
                open = previous,
                high = maxOf(previous, close) + 0.05,
                low = minOf(previous, close) - 0.05,
                close = close,
                volume = if (index == closes.lastIndex) 300.0 else 100.0
            )
        }
    }

    private fun fallingCandles(): List<Candle> {
        val closes = listOf(
            103.0, 102.8, 102.6, 102.4, 102.2,
            102.0, 101.8, 101.6, 101.4, 101.2,
            101.0, 100.8, 100.6, 100.0
        )

        return closes.mapIndexed { index, close ->
            val previous = if (index == 0) close + 0.1 else closes[index - 1]
            Candle(
                timestampMillis = (index + 1).toLong() * 60_000L,
                open = previous,
                high = maxOf(previous, close) + 0.05,
                low = minOf(previous, close) - 0.05,
                close = close,
                volume = if (index == closes.lastIndex) 300.0 else 100.0
            )
        }
    }
}
