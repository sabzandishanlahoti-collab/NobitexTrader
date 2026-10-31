package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.data.PublicTrade
import ir.nobitextrader.app.data.TradeSide
import org.junit.Assert.assertEquals
import org.junit.Test

class PublicTradeFlowSummaryTest {

    @Test
    fun rejectsBuyQuoteVolumeSumOverflow() {
        val trades = listOf(
            trade(time = 1L, price = 1e200, volume = 1e108, side = TradeSide.BUY),
            trade(time = 2L, price = 1e200, volume = 1e108, side = TradeSide.BUY)
        )

        assertIllegalArgument {
            PublicTradeFlowSummary.from(trades)
        }
    }

    @Test
    fun rejectsSellQuoteVolumeSumOverflow() {
        val trades = listOf(
            trade(time = 1L, price = 1e200, volume = 1e108, side = TradeSide.SELL),
            trade(time = 2L, price = 1e200, volume = 1e108, side = TradeSide.SELL)
        )

        assertIllegalArgument {
            PublicTradeFlowSummary.from(trades)
        }
    }

    @Test
    fun summarizesFiniteBuyAndSellFlow() {
        val summary = PublicTradeFlowSummary.from(
            listOf(
                trade(time = 10L, price = 10.0, volume = 2.0, side = TradeSide.BUY),
                trade(time = 30L, price = 10.0, volume = 3.0, side = TradeSide.BUY),
                trade(time = 20L, price = 5.0, volume = 1.0, side = TradeSide.SELL),
                trade(time = 40L, price = 7.0, volume = 1.0, side = TradeSide.SELL)
            )
        )

        assertEquals(2, summary.buyCount)
        assertEquals(2, summary.sellCount)
        assertEquals(50.0, summary.buyQuoteVolume, 0.0)
        assertEquals(12.0, summary.sellQuoteVolume, 0.0)
        assertEquals(38.0, summary.netQuoteFlow, 0.0)
        assertEquals(40L, summary.latestTradeTimeMillis)
    }

    private fun trade(
        time: Long,
        price: Double,
        volume: Double,
        side: TradeSide
    ) = PublicTrade(
        timeMillis = time,
        price = price,
        volume = volume,
        type = side
    )

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected overflow rejection.
        }
    }
}
