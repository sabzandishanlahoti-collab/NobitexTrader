
package ir.nobitextrader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketStatsValidationTest {

    @Test
    fun validMarketStatsPreserveAllValues() {
        val stats = MarketStats(
            market = "BTC/IRT",
            latest = 3_500_000_000.0,
            bestBuy = 3_490_000_000.0,
            bestSell = 3_510_000_000.0,
            dayLow = 3_400_000_000.0,
            dayHigh = 3_600_000_000.0,
            dayChange = 2.5,
            isClosed = false
        )

        assertEquals("BTC/IRT", stats.market)
        assertEquals(3_500_000_000.0, stats.latest, 0.0)
        assertEquals(3_490_000_000.0, stats.bestBuy, 0.0)
        assertEquals(3_510_000_000.0, stats.bestSell, 0.0)
        assertEquals(3_400_000_000.0, stats.dayLow, 0.0)
        assertEquals(3_600_000_000.0, stats.dayHigh, 0.0)
        assertEquals(2.5, stats.dayChange, 0.0)
        assertFalse(stats.isClosed)
    }

    @Test
    fun closedMarketStatusIsPreserved() {
        val stats = sampleStats(isClosed = true)

        assertTrue(stats.isClosed)
    }

    @Test
    fun marketChangeCanBeNegative() {
        val stats = sampleStats(dayChange = -3.25)

        assertEquals(-3.25, stats.dayChange, 0.0)
    }

    @Test
    fun zeroPriceIsRepresentedWithoutMutation() {
        val stats = sampleStats(latest = 0.0)

        assertEquals(0.0, stats.latest, 0.0)
    }

    @Test
    fun marketSymbolIsPreserved() {
        val stats = sampleStats(market = "BTC/IRT")

        assertEquals("BTC/IRT", stats.market)
    }

    private fun sampleStats(
        market: String = "BTC/IRT",
        latest: Double = 100.0,
        dayChange: Double = 1.0,
        isClosed: Boolean = false
    ) = MarketStats(
        market = market,
        latest = latest,
        bestBuy = 99.0,
        bestSell = 101.0,
        dayLow = 90.0,
        dayHigh = 110.0,
        dayChange = dayChange,
        isClosed = isClosed
    )
}
