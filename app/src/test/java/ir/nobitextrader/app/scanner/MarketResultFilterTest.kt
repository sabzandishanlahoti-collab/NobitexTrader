package ir.nobitextrader.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Test

class MarketResultFilterTest {

    private fun result(
        symbol: String,
        spread: Double = 0.5,
        bidLiquidity: Double = 10.0,
        askLiquidity: Double = 10.0,
        success: Boolean = true,
        lastPrice: Double = 100.0,
        bestBid: Double = 99.0,
        bestAsk: Double = 101.0
    ) = MarketScanResult(
        symbol = symbol,
        lastPrice = lastPrice,
        bestBid = bestBid,
        bestAsk = bestAsk,
        spreadPercent = spread,
        bidLiquidityTop5 = bidLiquidity,
        askLiquidityTop5 = askLiquidity,
        success = success
    )

    @Test
    fun allModeReturnsOnlyValidSuccessfulResults() {
        val input = listOf(
            result("BTCIRT"),
            result("FAILED", success = false),
            result("BADPRICE", lastPrice = 0.0),
            result("CROSSED", bestBid = 102.0, bestAsk = 101.0),
            result(symbol = " ")
        )

        assertEquals(
            listOf("BTCIRT"),
            MarketResultFilter.apply(input, MarketResultFilterMode.ALL)
                .map { it.symbol }
        )
    }

    @Test
    fun spreadFilterIncludesExactlyOnePercentBoundary() {
        val input = listOf(
            result("BELOW", spread = 0.99),
            result("BOUNDARY", spread = 1.0),
            result("ABOVE", spread = 1.01)
        )

        assertEquals(
            listOf("BELOW", "BOUNDARY"),
            MarketResultFilter.apply(
                input,
                MarketResultFilterMode.SPREAD_UNDER_ONE_PERCENT
            ).map { it.symbol }
        )
    }

    @Test
    fun positiveLiquidityFilterAcceptsEitherSide() {
        val input = listOf(
            result("BID", bidLiquidity = 1.0, askLiquidity = 0.0),
            result("ASK", bidLiquidity = 0.0, askLiquidity = 1.0),
            result("NONE", bidLiquidity = 0.0, askLiquidity = 0.0)
        )

        assertEquals(
            listOf("BID", "ASK"),
            MarketResultFilter.apply(
                input,
                MarketResultFilterMode.POSITIVE_LIQUIDITY
            ).map { it.symbol }
        )
    }

    @Test
    fun invalidNegativeOrNonFiniteMetricsAreRejected() {
        val input = listOf(
            result("NEGSPREAD", spread = -0.1),
            result("NEGBID", bidLiquidity = -1.0),
            result("NEGASK", askLiquidity = -1.0),
            result("INFINITESPREAD", spread = Double.POSITIVE_INFINITY),
            result("NANPRICE", lastPrice = Double.NaN)
        )

        assertEquals(
            emptyList<MarketScanResult>(),
            MarketResultFilter.apply(input, MarketResultFilterMode.ALL)
        )
    }
}
