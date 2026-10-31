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

    private fun flow(
        buyCount: Int = 1,
        sellCount: Int = 1,
        buyVolume: Double = 100.0,
        sellVolume: Double = 100.0
    ) = PublicTradeFlowSummary(
        buyCount = buyCount,
        sellCount = sellCount,
        buyQuoteVolume = buyVolume,
        sellQuoteVolume = sellVolume,
        netQuoteFlow = buyVolume - sellVolume,
        latestTradeTimeMillis = 1_800_000_000_000L
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
            result("INVALID", spread = Double.NaN),
            result("NARROW", spread = 0.2),
            result("FAILED", spread = 0.0, success = false)
        )
        val ranked = MarketRanking.rank(input, MarketSortMode.SPREAD_LOW_TO_HIGH)
        assertEquals(
            listOf("NARROW", "WIDE", "INVALID", "FAILED"),
            ranked.map { it.symbol }
        )
        assertFalse(ranked.last().success)
    }

    @Test
    fun liquidityModesPlaceInvalidMeasurementsAfterValidMarkets() {
        val input = listOf(
            result("INVALID_BID", bid = Double.NaN, ask = 10_000.0),
            result("VALID_LOW", bid = 10.0, ask = 20.0),
            result("VALID_HIGH", bid = 100.0, ask = 200.0),
            result("INVALID_ASK", bid = 10_000.0, ask = Double.POSITIVE_INFINITY)
        )

        assertEquals(
            listOf("VALID_HIGH", "VALID_LOW", "INVALID_BID", "INVALID_ASK"),
            MarketRanking.rank(input, MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW)
                .map { it.symbol }
        )
        assertEquals(
            listOf("VALID_HIGH", "VALID_LOW", "INVALID_BID", "INVALID_ASK"),
            MarketRanking.rank(input, MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW)
                .map { it.symbol }
        )
    }

    @Test
    fun buyPressureRankingDoesNotPromoteInvalidOrEmptyDepth() {
        val input = listOf(
            result("INVALID", bid = Double.NaN, ask = 1.0),
            result("EMPTY", bid = 0.0, ask = 0.0),
            result("SELL_HEAVY", bid = 20.0, ask = 80.0),
            result("BUY_HEAVY", bid = 80.0, ask = 20.0)
        )

        assertEquals(
            listOf("BUY_HEAVY", "SELL_HEAVY", "INVALID", "EMPTY"),
            MarketRanking.rank(input, MarketSortMode.BUY_PRESSURE_HIGH_TO_LOW)
                .map { it.symbol }
        )
    }

    @Test
    fun combinedBuyFlowUsesBothDepthAndRealTradeBuyShare() {
        val input = listOf(
            result("A", bid = 90.0, ask = 10.0),
            result("B", bid = 50.0, ask = 50.0),
            result("NO_TRADES", bid = 99.0, ask = 1.0)
        )
        val flows = mapOf(
            "A" to flow(buyVolume = 10.0, sellVolume = 90.0),
            "B" to flow(buyVolume = 90.0, sellVolume = 10.0)
        )

        assertEquals(
            listOf("B", "A", "NO_TRADES"),
            MarketRanking.rank(
                input,
                MarketSortMode.COMBINED_BUY_FLOW_HIGH_TO_LOW,
                flows
            ).map { it.symbol }
        )
    }

    @Test
    fun combinedBuyFlowKeepsInvalidDepthAndFailedMarketsAtTheEnd() {
        val input = listOf(
            result("INVALID_DEPTH", bid = Double.NaN, ask = 100.0),
            result("NO_TRADES", bid = 60.0, ask = 40.0),
            result("WITH_TRADES", bid = 50.0, ask = 50.0),
            result("FAILED", bid = 0.0, ask = 0.0, success = false)
        )
        val flows = mapOf(
            "WITH_TRADES" to flow(buyVolume = 80.0, sellVolume = 20.0),
            "INVALID_DEPTH" to flow(buyVolume = 100.0, sellVolume = 0.0)
        )

        assertEquals(
            listOf("WITH_TRADES", "NO_TRADES", "INVALID_DEPTH", "FAILED"),
            MarketRanking.rank(
                input,
                MarketSortMode.COMBINED_BUY_FLOW_HIGH_TO_LOW,
                flows
            ).map { it.symbol }
        )
    }

    @Test
    fun zeroVolumeTradeFlowIsTreatedAsMissingData() {
        val input = listOf(
            result("ZERO_FLOW", bid = 99.0, ask = 1.0),
            result("REAL_FLOW", bid = 50.0, ask = 50.0)
        )
        val flows = mapOf(
            "ZERO_FLOW" to flow(buyCount = 5, sellCount = 0, buyVolume = 0.0, sellVolume = 0.0),
            "REAL_FLOW" to flow(buyVolume = 70.0, sellVolume = 30.0)
        )

        assertEquals(
            listOf("REAL_FLOW", "ZERO_FLOW"),
            MarketRanking.rank(
                input,
                MarketSortMode.COMBINED_BUY_FLOW_HIGH_TO_LOW,
                flows
            ).map { it.symbol }
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
