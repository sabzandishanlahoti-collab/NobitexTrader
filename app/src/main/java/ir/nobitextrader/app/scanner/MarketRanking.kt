
package ir.nobitextrader.app.scanner

/**
 * Deterministic ranking for successful market scan results.
 *
 * This class only ranks data already returned by the scanner.
 * It does not generate prices or simulate market data.
 */
object MarketRanking {

    fun rank(
        results: List<MarketScanResult>,
        mode: MarketSortMode = MarketSortMode.BALANCED_PRIORITY
    ): List<MarketScanResult> {
        val successful = results.filter { it.success }
        val failed = results.filterNot { it.success }

        val ranked = when (mode) {
            MarketSortMode.SPREAD_LOW_TO_HIGH ->
                successful.sortedWith(
                    compareBy<MarketScanResult> { it.spreadPercent }
                        .thenBy { it.symbol }
                )

            MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedWith(
                    compareByDescending<MarketScanResult> {
                        it.bidLiquidityTop5
                    }.thenBy { it.symbol }
                )

            MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedWith(
                    compareByDescending<MarketScanResult> {
                        it.askLiquidityTop5
                    }.thenBy { it.symbol }
                )

            MarketSortMode.BALANCED_PRIORITY ->
                successful.sortedWith(
                    compareBy<MarketScanResult> {
                        balancedScore(it)
                    }.thenBy { it.symbol }
                )
        }

        return ranked + failed
    }

    private fun balancedScore(
        result: MarketScanResult
    ): Double {
        val spread = result.spreadPercent
        val bidLiquidity = result.bidLiquidityTop5
        val askLiquidity = result.askLiquidityTop5

        if (
            !spread.isFinite() ||
            !bidLiquidity.isFinite() ||
            !askLiquidity.isFinite() ||
            spread < 0.0 ||
            bidLiquidity < 0.0 ||
            askLiquidity < 0.0
        ) {
            return Double.MAX_VALUE
        }

        val totalLiquidity = bidLiquidity + askLiquidity
        if (!totalLiquidity.isFinite() || totalLiquidity <= 0.0) {
            return Double.MAX_VALUE
        }

        val liquidityImbalance =
            kotlin.math.abs(bidLiquidity - askLiquidity) / totalLiquidity

        // Lower spread and better bid/ask balance receive higher priority.
        // Liquidity contributes a bounded bonus so large values do not
        // overwhelm the spread and imbalance components.
        val liquidityBonus =
            1.0 / (1.0 + kotlin.math.ln(1.0 + totalLiquidity))

        return spread + liquidityImbalance - liquidityBonus
    }
}
