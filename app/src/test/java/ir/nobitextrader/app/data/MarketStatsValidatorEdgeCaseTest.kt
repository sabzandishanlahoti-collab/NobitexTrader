
package ir.nobitextrader.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketStatsValidatorEdgeCaseTest {

    @Test
    fun nanLatestPriceIsRejected() {
        assertFalse(
            MarketStatsValidator.isValid(
                sampleStats(latest = Double.NaN)
            )
        )
    }

    @Test
    fun infiniteLatestPriceIsRejected() {
        assertFalse(
            MarketStatsValidator.isValid(
                sampleStats(latest = Double.POSITIVE_INFINITY)
            )
        )
    }

    @Test
    fun negativeInfiniteDailyChangeIsRejected() {
        assertFalse(
            MarketStatsValidator.isValid(
                sampleStats(dayChange = Double.NEGATIVE_INFINITY)
            )
        )
    }

    @Test
    fun nonPositiveBestBuyIsRejected() {
        assertFalse(
            MarketStatsValidator.isValid(
                sampleStats(bestBuy = 0.0)
            )
        )
    }

    @Test
    fun nonPositiveBestSellIsRejected() {
        assertFalse(
            MarketStatsValidator.isValid(
                sampleStats(bestSell = -1.0)
            )
        )
    }

    @Test
    fun validClosedMarketStatsRemainValid() {
        assertTrue(
            MarketStatsValidator.isValid(
                sampleStats(isClosed = true)
            )
        )
    }

    private fun sampleStats(
        latest: Double = 100.0,
        bestBuy: Double = 99.0,
        bestSell: Double = 101.0,
        dayLow: Double = 90.0,
        dayHigh: Double = 110.0,
        dayChange: Double = 1.0,
        isClosed: Boolean = false
    ) = MarketStats(
        market = "BTC/IRT",
        latest = latest,
        bestBuy = bestBuy,
        bestSell = bestSell,
        dayLow = dayLow,
        dayHigh = dayHigh,
        dayChange = dayChange,
        isClosed = isClosed
    )
}
