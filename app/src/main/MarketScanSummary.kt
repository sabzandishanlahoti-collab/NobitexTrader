
package ir.nobitextrader.app.scanner

data class MarketScanSummary(
    val total: Int,
    val successful: Int,
    val failed: Int,
    val ranked: Int
) {
    companion object {
        fun from(
            results: List<MarketScanResult>
        ): MarketScanSummary {
            return MarketScanSummary(
                total = results.size,
                successful = results.count { it.success },
                failed = results.count { !it.success },
                ranked = MarketScanner.rankBySpread(results).size
            )
        }
    }
}
