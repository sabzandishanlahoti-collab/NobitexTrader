
package ir.nobitextrader.app.scanner

enum class MarketRankingMode {
    LOWEST_SPREAD,
    HIGHEST_BID_LIQUIDITY,
    HIGHEST_ASK_LIQUIDITY
}

object MarketScanRanking {

    fun rank(
        results: List<MarketScanResult>,
        mode: MarketRankingMode
    ): List<MarketScanResult> {
        val eligible = results.filter { result ->
            result.success &&
                result.lastPrice > 0.0 &&
                result.bestBid != null &&
                result.bestAsk != null &&
                result.bestBid > 0.0 &&
                result.bestAsk >= result.bestBid
        }

        return when (mode) {
            MarketRankingMode.LOWEST_SPREAD ->
                eligible
                    .filter { it.spreadPercent != null }
                    .sortedBy { it.spreadPercent }

            MarketRankingMode.HIGHEST_BID_LIQUIDITY ->
                eligible.sortedByDescending {
                    it.bidLiquidityTop5
                }

            MarketRankingMode.HIGHEST_ASK_LIQUIDITY ->
                eligible.sortedByDescending {
                    it.askLiquidityTop5
                }
        }
    }
}
