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
    fun longGapThroughStopSkipsStaleEntrySignal() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 90.0, 96.0, 89.0, 92.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 95.0, 120.0) else null }

        assertEquals(0, result.totalTrades)
        assertTrue(result.trades.isEmpty())
        assertEquals(10_000.0, result.finalEquity, 0.000001)
    }

    @Test
    fun longGapBeyondTargetSkipsMissedEntry() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 110.0, 112.0, 109.0, 111.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 95.0, 105.0) else null }

        assertEquals(0, result.totalTrades)
        assertEquals(10_000.0, result.finalEquity, 0.000001)
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
            c(2, 100.0, 106.0, 99.0, 105.0),
            c(3, 105.0, 106.0, 104.0, 105.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { history ->
            if (history.size == 2) {
                // Mutate the callback's multi-candle snapshot. The source list
                // used by the backtest must remain unchanged.
                (history as MutableList<Candle>)[0] =
                    c(1, 700.0, 701.0, 699.0, 700.0)
                BacktestSignal(TradeDirection.LONG, 95.0, 110.0)
            } else null
        }

        assertEquals(100.0, candles.first().close, 0.000001)
        assertEquals(105.0, result.trades.single().entryPrice, 0.000001)
        assertEquals(ExitReason.END_OF_DATA, result.trades.single().exitReason)
    }


    @Test
    fun shortPositionEntersAtNextOpenAndExitsAtTarget() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 100.0, 101.0, 99.0, 100.0),
            c(3, 100.0, 101.0, 91.0, 94.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { history ->
            if (history.size == 1) BacktestSignal(TradeDirection.SHORT, 105.0, 92.0) else null
        }

        assertEquals(1, result.totalTrades)
        assertEquals(100.0, result.trades.single().entryPrice, 0.000001)
        assertEquals(92.0, result.trades.single().exitPrice, 0.000001)
        assertEquals(ExitReason.TAKE_PROFIT, result.trades.single().exitReason)
        assertTrue(result.trades.single().netPnl > 0.0)
    }

    @Test
    fun shortGapThroughStopSkipsStaleEntrySignal() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 110.0, 112.0, 109.0, 111.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { history ->
            if (history.size == 1) BacktestSignal(TradeDirection.SHORT, 105.0, 80.0) else null
        }

        assertEquals(0, result.totalTrades)
        assertEquals(10_000.0, result.finalEquity, 0.000001)
    }

    @Test
    fun shortGapBeyondTargetSkipsMissedEntry() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 90.0, 91.0, 88.0, 89.0),
            c(3, 89.0, 90.0, 88.0, 89.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { history ->
            if (history.size == 1) BacktestSignal(TradeDirection.SHORT, 105.0, 95.0) else null
        }

        assertEquals(0, result.totalTrades)
        assertEquals(10_000.0, result.finalEquity, 0.000001)
    }

    @Test
    fun entrySlippageCannotPushLongEntryThroughTarget() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 104.9, 105.0, 104.0, 104.8)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.002
        )) { if (it.size == 1) BacktestSignal(TradeDirection.LONG, 95.0, 105.0) else null }

        assertEquals(0, result.totalTrades)
    }

    @Test
    fun shortGapThroughStopUsesWorseOpeningPrice() {
        val candles = listOf(
            c(1, 100.0, 101.0, 99.0, 100.0),
            c(2, 100.0, 101.0, 99.0, 100.0),
            c(3, 110.0, 112.0, 109.0, 111.0)
        )
        val result = BacktestEngine().run(candles, BacktestConfig(
            initialCapital = 10_000.0, positionNotional = 1_000.0,
            feeRate = 0.0, slippageRate = 0.0
        )) { history ->
            if (history.size == 1) BacktestSignal(TradeDirection.SHORT, 105.0, 80.0) else null
        }

        assertEquals(110.0, result.trades.single().exitPrice, 0.000001)
        assertEquals(ExitReason.STOP_LOSS, result.trades.single().exitReason)
        assertTrue(result.trades.single().netPnl < 0.0)
    }

}
