package ir.nobitextrader.app.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacktestEngineTest {
    private fun c(t: Long, o: Double, h: Double, l: Double, close: Double) =
        Candle(t, o, h, l, close, 100.0)

    @Test
    fun signalEntersAtNextCandleOpenAndExitsAtTarget() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 100.0, 109.0, 99.0, 105.0),
            c(3, 105.0, 106.0, 104.0, 105.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { history ->
            if (history.size == 1) BacktestSignal(TradeDirection.LONG, 95.0, 108.0) else null
        }
        assertEquals(1, result.totalTrades)
        assertEquals(100.0, result.trades.single().entryPrice, 0.000001)
        assertEquals(108.0, result.trades.single().exitPrice, 0.000001)
        assertEquals(ExitReason.TAKE_PROFIT, result.trades.single().exitReason)
    }

    @Test
    fun stopLossWinsWhenBothStopAndTargetAreTouched() {
        val candles = listOf(c(1, 100.0, 101.0, 99.0, 100.0), c(2, 100.0, 110.0, 90.0, 100.0))
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 95.0, 105.0) else null }
        assertEquals(ExitReason.STOP_LOSS, result.trades.single().exitReason)
        assertTrue(result.trades.single().netPnl < 0.0)
    }

    @Test
    fun feesAndSlippageAreIncludedInNetResult() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 100.0, 110.0, 99.0, 106.0),
            c(3, 106.0, 107.0, 105.0, 106.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.001, slippageRate = 0.001
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 95.0, 105.0) else null }
        assertTrue(result.totalFees > 0.0)
        assertTrue(result.netPnl < 50.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnorderedCandles() {
        BacktestEngine().run(listOf(c(2, 100.0, 101.0, 99.0, 100.0), c(1, 100.0, 101.0, 99.0, 100.0))) { null }
    }

    @Test
    fun gapThroughStopUsesWorseOpeningPrice() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 90.0, 96.0, 89.0, 92.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 95.0, 120.0) else null }

        assertEquals(90.0, result.trades.single().exitPrice, 0.000001)
        assertEquals(ExitReason.STOP_LOSS, result.trades.single().exitReason)
    }

    @Test
    fun adverseIntrabarPriceContributesToUnrealizedMaxDrawdown() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 100.0, 101.0, 90.0, 100.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 80.0, 120.0) else null }

        // The position recovers by the close, but the intrabar 10% adverse move
        // on a 10% position allocation must still register as 1% account drawdown.
        assertEquals(0.01, result.maxDrawdown, 0.000001)
        assertEquals(ExitReason.END_OF_DATA, result.trades.single().exitReason)
    }

    @Test
    fun endOfDataLiquidationContributesToMaxDrawdown() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 100.0, 101.0, 89.0, 90.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 80.0, 120.0) else null }

        // Intrabar low of 89 is more adverse than the final close of 90.
        assertEquals(0.011, result.maxDrawdown, 0.000001)
        assertEquals(ExitReason.END_OF_DATA, result.trades.single().exitReason)
    }


    @Test
    fun signalCallbackCannotMutateOriginalCandleHistory() {
        val candles = mutableListOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 100.0, 111.0, 99.0, 105.0),
            c(3, 105.0, 106.0, 104.0, 105.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { history ->
            if (history.size == 1) {
                try {
                    (history as MutableList<Candle>)[0] = c(1, 700.0, 701.0, 699.0, 700.0)
                    throw AssertionError("Signal callback unexpectedly mutated its candle history")
                } catch (_: UnsupportedOperationException) {
                    // The callback receives a read-only snapshot.
                }
                BacktestSignal(TradeDirection.LONG, 95.0, 110.0)
            } else null
        }

        assertEquals(100.0, candles.first().close, 0.000001)
        assertEquals(100.0, result.trades.single().entryPrice, 0.000001)
        assertEquals(ExitReason.TAKE_PROFIT, result.trades.single().exitReason)
    }

}
