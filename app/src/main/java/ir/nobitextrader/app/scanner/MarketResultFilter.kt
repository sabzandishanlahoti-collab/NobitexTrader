
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
        return results.filter { result ->
            if (!isValidResult(result)) {
                return@filter false
            }

            when (mode) {
                MarketResultFilterMode.ALL -> true

                MarketResultFilterMode.SPREAD_UNDER_ONE_PERCENT ->
                    result.spreadPercent <= 1.0

                MarketResultFilterMode.POSITIVE_LIQUIDITY ->
                    result.bidLiquidityTop5 > 0.0 ||
                        result.askLiquidityTop5 > 0.0
            }
        }
    }

    private fun isValidResult(
        result: MarketScanResult
    ): Boolean {
        if (!result.success) return false

        if (result.symbol.isBlank()) return false

        if (!result.lastPriceOrValid()) return false

        if (
            !result.bestBid.isFinite() ||
            result.bestBid <= 0.0
        ) {
            return false
        }

        if (
            !result.bestAsk.isFinite() ||
            result.bestAsk < result.bestBid
        ) {
            return false
        }

        if (
            !result.spreadPercent.isFinite() ||
            result.spreadPercent < 0.0
        ) {
            return false
        }

        if (
            !result.bidLiquidityTop5.isFinite() ||
            result.bidLiquidityTop5 < 0.0
        ) {
            return false
        }

        if (
            !result.askLiquidityTop5.isFinite() ||
            result.askLiquidityTop5 < 0.0
        ) {
            return false
        }

        return true
    }

    private fun MarketScanResult.lastPriceOrValid(): Boolean {
        return lastPrice.isFinite() && lastPrice > 0.0
    }
}
