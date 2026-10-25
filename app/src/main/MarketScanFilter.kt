
package ir.nobitextrader.app.scanner

object MarketScanFilter {

    fun apply(
        results: List<MarketScanResult>,
        minimumSpreadPercent: Double = 0.0
    ): List<MarketScanResult> {
        require(minimumSpreadPercent >= 0.0)

        return results.filter { result ->
            result.success &&
                result.lastPrice > 0.0 &&
                result.spreadPercent != null &&
                result.spreadPercent >= minimumSpreadPercent &&
                result.bestBid != null &&
                result.bestAsk != null &&
                result.bestBid > 0.0 &&
                result.bestAsk >= result.bestBid
        }
    }
}
