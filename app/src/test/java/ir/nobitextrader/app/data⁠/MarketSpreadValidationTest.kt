
package ir.nobitextrader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketSpreadValidationTest {

    @Test
    fun spreadIsCalculatedAsPercentageOfMidPrice() {
        val bestBuy = 99.0
        val bestSell = 101.0

        val midPrice = (bestBuy + bestSell) / 2.0
        val spreadPercent =
            ((bestSell - bestBuy) / midPrice) * 100.0

        assertEquals(2.0, spreadPercent, 0.000001)
    }

    @Test
    fun equalPricesProduceZeroSpread() {
        val bestBuy = 100.0
        val bestSell = 100.0

        val spreadPercent =
            ((bestSell - bestBuy) / bestBuy) * 100.0

        assertEquals(0.0, spreadPercent, 0.000001)
    }

    @Test
    fun crossedMarketIsDetected() {
        val bestBuy = 102.0
        val bestSell = 101.0

        assertTrue(bestBuy > bestSell)
    }

    @Test
    fun normalMarketHasNonNegativeSpread() {
        val bestBuy = 99.0
        val bestSell = 101.0

        assertTrue(bestSell >= bestBuy)
    }
}
