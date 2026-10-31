package ir.nobitextrader.app.scanner

/**
 * Deterministic ranking for successful market scan results.
 *
 * Balanced priority uses only live order-book measurements:
 * 50% spread quality, 25% bid depth and 25% ask depth.
 * Failed or invalid measurements are never promoted above valid markets.
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
                    compareBy<MarketScanResult> { safeNonNegative(it.spreadPercent) }
                )

            MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedWith(
                    compareByDescending<MarketScanResult> {
                        safeNonNegative(it.bidLiquidityTop5)
                    }
                )

            MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedWith(
                    compareByDescending<MarketScanResult> {
                        safeNonNegative(it.askLiquidityTop5)
                    }
                )

            MarketSortMode.BUY_PRESSURE_HIGH_TO_LOW ->
                successful.sortedWith(
                    compareByDescending<MarketScanResult> { result ->
                        val bid = safeNonNegative(result.bidLiquidityTop5)
                        val ask = safeNonNegative(result.askLiquidityTop5)
                        val total = bid + ask
                        if (total.isFinite() && total > 0.0) bid / total else -1.0
                    }
                )

            MarketSortMode.BALANCED_PRIORITY ->
                rankBalanced(successful)
        }

        // Kotlin's sorted operations are stable: equal metrics retain scan order.
        return ranked + failed
    }

    private fun rankBalanced(
        results: List<MarketScanResult>
    ): List<MarketScanResult> {
        if (results.size < 2) return results

        val valid = results.filter(::hasValidRankingMetrics)
        val invalid = results.filterNot(::hasValidRankingMetrics)
        if (valid.size < 2) return valid + invalid

        val minSpread = valid.minOf { it.spreadPercent }
        val maxSpread = valid.maxOf { it.spreadPercent }
        val maxBid = valid.maxOf { it.bidLiquidityTop5 }
        val maxAsk = valid.maxOf { it.askLiquidityTop5 }

        fun lowerIsBetter(value: Double, min: Double, max: Double): Double {
            if (max <= min) return 1.0
            return ((max - value) / (max - min)).coerceIn(0.0, 1.0)
        }

        fun higherIsBetter(value: Double, max: Double): Double {
            if (max <= 0.0) return 0.0
            return (value / max).coerceIn(0.0, 1.0)
        }

        return valid.sortedByDescending { result ->
            val spreadScore = lowerIsBetter(
                result.spreadPercent,
                minSpread,
                maxSpread
            )
            val bidScore = higherIsBetter(result.bidLiquidityTop5, maxBid)
            val askScore = higherIsBetter(result.askLiquidityTop5, maxAsk)

            spreadScore * 0.50 + bidScore * 0.25 + askScore * 0.25
        } + invalid
    }

    private fun hasValidRankingMetrics(result: MarketScanResult): Boolean =
        result.spreadPercent.isFinite() &&
            result.spreadPercent >= 0.0 &&
            result.bidLiquidityTop5.isFinite() &&
            result.bidLiquidityTop5 >= 0.0 &&
            result.askLiquidityTop5.isFinite() &&
            result.askLiquidityTop5 >= 0.0

    private fun safeNonNegative(value: Double): Double =
        if (value.isFinite() && value >= 0.0) value else Double.MAX_VALUE
}
