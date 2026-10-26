
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

            val spreads = successful.mapNotNull {
                it.spreadPercent?.takeIf { value ->
                    value.isFinite() && value >= 0.0
                }
            }

            val bestSpread = successful
                .filter {
                    it.spreadPercent != null &&
                        it.spreadPercent.isFinite() &&
                        it.spreadPercent >= 0.0
                }
                .minByOrNull { it.spreadPercent!! }

            return MarketScanSummary(
                totalMarkets = results.size,
                successfulMarkets = successful.size,
                failedMarkets = results.count { !it.success },
                averageSpreadPercent = spreads
                    .takeIf { it.isNotEmpty() }
                    ?.average(),
                bestSpreadSymbol = bestSpread?.symbol,
                bestSpreadPercent = bestSpread?.spreadPercent,
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
