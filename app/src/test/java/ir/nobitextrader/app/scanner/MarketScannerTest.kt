
package ir.nobitextrader.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketScannerTest {

    private fun result(
        symbol: String,
        spread: Double,
        bidLiquidity: Double,
        askLiquidity: Double,
        success: Boolean = true
    ): MarketScanResult {
        return MarketScanResult(
            symbol = symbol,
            lastPrice = 100.0,
            bestBid = 99.0,
            bestAsk = 101.0,
            spreadPercent = spread,
            bidLiquidityTop5 = bidLiquidity,
            askLiquidityTop5 = askLiquidity,
            success = success,
            error = if (success) null else "Invalid market data"
        )
    }

    @Test
    fun spreadSortPlacesLowestSpreadFirst() {
        val input = listOf(
            result("BTCIRT", 2.0, 100.0, 100.0),
            result("ETHIRT", 0.5, 100.0, 100.0),
            result("LTCIRT", 1.0, 100.0, 100.0)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.SPREAD_LOW_TO_HIGH
        )

        assertEquals(
            listOf("ETHIRT", "LTCIRT", "BTCIRT"),
            sorted.map { it.symbol }
        )
    }

    @Test
    fun failedResultsAreKeptAfterSuccessfulResults() {
        val input = listOf(
            result("BADIRT", 0.1, 100.0, 100.0, success = false),
            result("BTCIRT", 1.0, 100.0, 100.0)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.SPREAD_LOW_TO_HIGH
        )

        assertTrue(sorted.first().success)
        assertFalse(sorted.last().success)
        assertEquals("BTCIRT", sorted.first().symbol)
        assertEquals("BADIRT", sorted.last().symbol)
    }

    @Test
    fun bidLiquiditySortPlacesHighestFirst() {
        val input = listOf(
            result("BTCIRT", 1.0, 100.0, 100.0),
            result("ETHIRT", 1.0, 500.0, 100.0),
            result("LTCIRT", 1.0, 250.0, 100.0)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW
        )

        assertEquals(
            listOf("ETHIRT", "LTCIRT", "BTCIRT"),
            sorted.map { it.symbol }
        )
    }

    @Test
    fun askLiquiditySortPlacesHighestFirst() {
        val input = listOf(
            result("BTCIRT", 1.0, 100.0, 100.0),
            result("ETHIRT", 1.0, 100.0, 700.0),
            result("LTCIRT", 1.0, 100.0, 300.0)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW
        )

        assertEquals(
            listOf("ETHIRT", "LTCIRT", "BTCIRT"),
            sorted.map { it.symbol }
        )
    }

    @Test
    fun successAndFailureCountsAreAccurate() {
        val input = listOf(
            result("BTCIRT", 1.0, 100.0, 100.0),
            result("ETHIRT", 1.0, 100.0, 100.0),
            result("BADIRT", 0.0, 0.0, 0.0, success = false)
        )

        assertEquals(2, MarketScanner.successfulCount(input))
        assertEquals(1, MarketScanner.failedCount(input))
    }
    @Test
    fun balancedPriorityKeepsSuccessfulResultsAndRanksThemBeforeFailures() {
        val input = listOf(
            result("LOWDEPTHIRT", 0.8, 10.0, 10.0),
            result("BALANCEDIRT", 0.4, 100.0, 100.0),
            result("WIDESPREADIRT", 2.0, 500.0, 500.0),
            result("FAILEDIRT", 0.0, 0.0, 0.0, success = false)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.BALANCED_PRIORITY
        )

        assertEquals("BALANCEDIRT", sorted.first().symbol)
        assertFalse(sorted.last().success)
        assertEquals("FAILEDIRT", sorted.last().symbol)
        assertEquals(4, sorted.size)
    }

    @Test
    fun balancedPriorityHandlesEqualMetricsWithoutDivisionErrors() {
        val input = listOf(
            result("BTCIRT", 1.0, 100.0, 100.0),
            result("ETHIRT", 1.0, 100.0, 100.0)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.BALANCED_PRIORITY
        )

        assertEquals(2, sorted.size)
        assertTrue(sorted.all { it.success })
    }

    @Test
    fun equalSortMetricsPreserveOriginalOrderAndFailedOrder() {
        val input = listOf(
            result("FAILFIRST", 0.0, 0.0, 0.0, success = false),
            result("BTCIRT", 1.0, 100.0, 100.0),
            result("ETHIRT", 1.0, 100.0, 100.0),
            result("FAILSECOND", 0.0, 0.0, 0.0, success = false)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.SPREAD_LOW_TO_HIGH
        )

        assertEquals(
            listOf("BTCIRT", "ETHIRT", "FAILFIRST", "FAILSECOND"),
            sorted.map { it.symbol }
        )
    }

    @Test
    fun balancedPriorityPreservesOrderWhenEveryScoreIsEqual() {
        val input = listOf(
            result("BTCIRT", 1.0, 100.0, 100.0),
            result("ETHIRT", 1.0, 100.0, 100.0),
            result("LTCIRT", 1.0, 100.0, 100.0)
        )

        val sorted = MarketScanner.sortResults(
            input,
            MarketSortMode.BALANCED_PRIORITY
        )

        assertEquals(
            listOf("BTCIRT", "ETHIRT", "LTCIRT"),
            sorted.map { it.symbol }
        )
    }

}
