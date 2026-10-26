
package ir.nobitextrader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketStatsValidatorTest {

    @Test
    fun validStatsPassValidation() {
        assertTrue(
            MarketStatsValidator.isValid(sampleStats())
        )
    }

    @Test
    fun zeroLatestPriceFailsValidation() {
        val result = MarketStatsValidator.validate(
            sampleStats(latest = 0.0)
        )

        assertFalse(result.isEmpty())
        assertTrue(result.any { it.contains("Latest price") })
    }

    @Test
    fun crossedBidAndAskFailValidation() {
        val result = MarketStatsValidator.validate(
            sampleStats(bestBuy = 105.0, bestSell = 100.0)
        )

        assertTrue(
            result.any { it.contains("Best buy price exceeds") }
        )
    }

    @Test
    fun invalidDailyRangeFailsValidation() {
        val result = MarketStatsValidator.validate(
            sampleStats(dayLow = 120.0, dayHigh = 100.0)
        )

        assertTrue(
            result.any { it.contains("Daily low exceeds") }
        )
    }

    @Test
    fun blankMarketSymbolFailsValidation() {
        val result = MarketStatsValidator.validate(
            sampleStats(market = " ")
        )

        assertTrue(
            result.any { it.contains("Market symbol") }
        )
    }

    @Test
    fun multipleInvalidFieldsAreReported() {
        val result = MarketStatsValidator.validate(
            sampleStats(latest = -1.0, dayChange = Double.NaN)
        )

        assertEquals(2, result.size)
    }

    private fun sampleStats(
        market: String = "BTC/IRT",
        latest: Double = 100.0,
        bestBuy: Double = 99.0,
        bestSell: Double = 101.0,
        dayLow: Double = 90.0,
        dayHigh: Double = 110.0,
        dayChange: Double = 1.0
    ) = MarketStats(
        market = market,
        latest = latest,
        bestBuy = bestBuy,
        bestSell = bestSell,
        dayLow = dayLow,
        dayHigh = dayHigh,
        dayChange = dayChange,
        isClosed = false
    )
}
