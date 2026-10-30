package ir.nobitextrader.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketRankingTest {

    private fun result(
        symbol: String,
        spread: Double = 1.0,
        bid: Double = 100.0,
        ask: Double = 100.0,
        success: Boolean = true
    ) = MarketScanResult(
        symbol = symbol,
        lastPrice = 100.0,
        bestBid = 99.0,
        bestAsk = 101.0,
        spreadPercent = spread,
        bidLiquidityTop5 = bid,
        askLiquidityTop5 = ask,
        success = success,
        error = if (success) null else "request failed"
    )

    @Test
    fun balancedRankingUsesSpreadBidAndAskDepth() {
        val input = listOf(
            result("WIDE", spread = 2.0, bid = 1_000.0, ask = 1_000.0),
            result("BALANCED", spread = 0.5, bid = 500.0, ask = 500.0),
            result("THIN", spread = 1.0, bid = 10.0, ask = 10.0)
        )
        val ranked = MarketRanking.rank(input)
        assertEquals("BALANCED", ranked.first().symbol)
        assertEquals(3, ranked.size)
    }

    @Test
    fun failedMarketsAlwaysRemainAfterSuccessfulMarkets() {
        val input = listOf(
            result("FAILED", spread = 0.0, bid = 0.0, ask = 0.0, success = false),
            result("VALID", spread = 1.0, bid = 100.0, ask = 100.0)
        )
        val ranked = MarketRanking.rank(input)
        assertEquals("VALID", ranked.first().symbol)
        assertEquals("FAILED", ranked.last().symbol)
    }

    @Test
    fun invalidMetricsAreNotAllowedToWinBalancedRanking() {
        val input = listOf(
            result("NAN", spread = Double.NaN, bid = 10_000.0, ask = 10_000.0),
            result("VALID", spread = 1.0, bid = 100.0, ask = 100.0)
        )
        val ranked = MarketRanking.rank(input)
        assertEquals("VALID", ranked.first().symbol)
        assertEquals("NAN", ranked.last().symbol)
    }

    @Test
    fun stableRankingPreservesInputOrderForEqualScores() {
        val input = listOf(result("FIRST"), result("SECOND"), result("THIRD"))
        assertEquals(
            listOf("FIRST", "SECOND", "THIRD"),
            MarketRanking.rank(input).map { it.symbol }
        )
    }

    @Test
    fun spreadModeSortsAscendingAndKeepsFailuresLast() {
        val input = listOf(
            result("WIDE", spread = 2.0),
            result("NARROW", spread = 0.2),
            result("FAILED", spread = 0.0, success = false)
        )
        val ranked = MarketRanking.rank(input, MarketSortMode.SPREAD_LOW_TO_HIGH)
        assertEquals(listOf("NARROW", "WIDE", "FAILED"), ranked.map { it.symbol })
        assertFalse(ranked.last().success)
    }

    @Test
    fun liquidityModesSortDescending() {
        val input = listOf(
            result("LOW", bid = 10.0, ask = 20.0),
            result("HIGH", bid = 100.0, ask = 200.0)
        )
        assertEquals(
            "HIGH",
            MarketRanking.rank(input, MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW).first().symbol
        )
        assertEquals(
            "HIGH",
            MarketRanking.rank(input, MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW).first().symbol
        )
    }

    @Test
    fun emptyAndSingleItemInputsAreSafe() {
        assertTrue(MarketRanking.rank(emptyList()).isEmpty())
        assertEquals(
            listOf("ONLY"),
            MarketRanking.rank(listOf(result("ONLY"))).map { it.symbol }
        )
    }
}
