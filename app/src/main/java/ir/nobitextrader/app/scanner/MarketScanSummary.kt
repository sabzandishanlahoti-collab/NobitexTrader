
package ir.nobitextrader.app.scanner

data class MarketScanSummary(
    val totalMarkets: Int = 0,
    val successfulMarkets: Int = 0,
    val failedMarkets: Int = 0,
    val averageSpreadPercent: Double? = null,
    val bestSpreadSymbol: String? = null,
    val bestSpreadPercent: Double? = null,
    val totalBidLiquidity: Double = 0.0,
    val totalAskLiquidity: Double = 0.0
) {
    companion object {
        fun from(
            results: List<MarketScanResult>
        ): MarketScanSummary {
            val successful = results.filter { it.success }

            val validSpreads = successful.mapNotNull { result ->
                result.spreadPercent
                    ?.takeIf { it.isFinite() && it >= 0.0 }
                    ?.let { spread -> result to spread }
            }

            val bestSpread = validSpreads.minByOrNull { it.second }

            return MarketScanSummary(
                totalMarkets = results.size,
                successfulMarkets = successful.size,
                failedMarkets = results.count { !it.success },
                averageSpreadPercent = validSpreads
                    .map { it.second }
                    .takeIf { it.isNotEmpty() }
                    ?.average(),
                bestSpreadSymbol = bestSpread?.first?.symbol,
                bestSpreadPercent = bestSpread?.second,
                totalBidLiquidity = successful.sumOf {
                    it.bidLiquidityTop5
                },
                totalAskLiquidity = successful.sumOf {
                    it.askLiquidityTop5
                }
            )
        }
    }
}
