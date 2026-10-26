
package ir.nobitextrader.app.scanner

enum class MarketResultFilterMode {
    ALL,
    SPREAD_UNDER_ONE_PERCENT,
    POSITIVE_LIQUIDITY
}

object MarketResultFilter {

    fun apply(
        results: List<MarketScanResult>,
        mode: MarketResultFilterMode
    ): List<MarketScanResult> {
        return when (mode) {
            MarketResultFilterMode.ALL -> results

            MarketResultFilterMode.SPREAD_UNDER_ONE_PERCENT ->
                results.filter { result ->
                    result.success &&
                        result.spreadPercent.isFinite() &&
                        result.spreadPercent >= 0.0 &&
                        result.spreadPercent <= 1.0
                }

            MarketResultFilterMode.POSITIVE_LIQUIDITY ->
                results.filter { result ->
                    result.success &&
                        (result.bidLiquidityTop5 > 0.0 ||
                            result.askLiquidityTop5 > 0.0)
                }
        }
    }
}
