package ir.nobitextrader.app.scanner

/**
 * Deterministic ranking for successful market scan results.
 *
 * Invalid measurements are always placed after valid measurements.
 * Combined flow ranking requires both a valid order book and real public
 * trade volume; markets without recent trades remain visible but rank later.
 */
object MarketRanking {

    fun rank(
        results: List<MarketScanResult>,
        mode: MarketSortMode = MarketSortMode.BALANCED_PRIORITY,
        tradeFlowBySymbol: Map<String, PublicTradeFlowSummary> = emptyMap()
    ): List<MarketScanResult> {
        val successful = results.filter { it.success }
        val failed = results.filterNot { it.success }

        val ranked = when (mode) {
            MarketSortMode.SPREAD_LOW_TO_HIGH ->
                rankValidFirst(
                    successful,
                    isValid = { it.spreadPercent.isFinite() && it.spreadPercent >= 0.0 },
                    comparator = compareBy { it.spreadPercent }
                )

            MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW ->
                rankValidFirst(
                    successful,
                    isValid = { it.bidLiquidityTop5.isFinite() && it.bidLiquidityTop5 >= 0.0 },
                    comparator = compareByDescending { it.bidLiquidityTop5 }
                )

            MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW ->
                rankValidFirst(
                    successful,
                    isValid = { it.askLiquidityTop5.isFinite() && it.askLiquidityTop5 >= 0.0 },
                    comparator = compareByDescending { it.askLiquidityTop5 }
                )

            MarketSortMode.BUY_PRESSURE_HIGH_TO_LOW ->
                rankValidFirst(
                    successful,
                    isValid = ::hasValidDepth,
                    comparator = compareByDescending { buyDepthShare(it) }
                )

            MarketSortMode.COMBINED_BUY_FLOW_HIGH_TO_LOW ->
                rankCombinedBuyFlow(successful, tradeFlowBySymbol)

            MarketSortMode.BALANCED_PRIORITY ->
                rankBalanced(successful)
        }

        // Successful markets always remain above failed scans.
        return ranked + failed
    }

    private fun <T> rankValidFirst(
        results: List<T>,
        isValid: (T) -> Boolean,
        comparator: Comparator<T>
    ): List<T> {
        val valid = results.filter(isValid)
        val invalid = results.filterNot(isValid)
        return valid.sortedWith(comparator) + invalid
    }

    private fun rankCombinedBuyFlow(
        results: List<MarketScanResult>,
        flows: Map<String, PublicTradeFlowSummary>
    ): List<MarketScanResult> {
        val validDepth = results.filter(::hasValidDepth)
        val invalidDepth = results.filterNot(::hasValidDepth)

        val withTradeData = validDepth.filter { result ->
            flows[result.symbol]?.let(::hasValidTradeFlow) == true
        }
        val withoutTradeData = validDepth.filterNot { result ->
            flows[result.symbol]?.let(::hasValidTradeFlow) == true
        }

        val rankedWithTrades = withTradeData.sortedByDescending { result ->
            val flow = flows.getValue(result.symbol)
            val depthShare = buyDepthShare(result)
            val tradeTotal = flow.buyQuoteVolume + flow.sellQuoteVolume
            val tradeBuyShare = flow.buyQuoteVolume / tradeTotal

            // Equal weighting avoids allowing a single large order-book
            // snapshot or a single large public trade to dominate alone.
            (depthShare + tradeBuyShare) / 2.0
        }

        return rankedWithTrades + withoutTradeData + invalidDepth
    }

    private fun hasValidTradeFlow(flow: PublicTradeFlowSummary): Boolean {
        if (flow.buyCount < 0 || flow.sellCount < 0) return false
        if (flow.buyCount.toLong() + flow.sellCount.toLong() <= 0L) return false
        if (!flow.buyQuoteVolume.isFinite() || flow.buyQuoteVolume < 0.0) return false
        if (!flow.sellQuoteVolume.isFinite() || flow.sellQuoteVolume < 0.0) return false

        val total = flow.buyQuoteVolume + flow.sellQuoteVolume
        return total.isFinite() && total > 0.0
    }

    private fun hasValidDepth(result: MarketScanResult): Boolean {
        if (!result.bidLiquidityTop5.isFinite() || result.bidLiquidityTop5 < 0.0) return false
        if (!result.askLiquidityTop5.isFinite() || result.askLiquidityTop5 < 0.0) return false
        val total = result.bidLiquidityTop5 + result.askLiquidityTop5
        return total.isFinite() && total > 0.0
    }

    private fun buyDepthShare(result: MarketScanResult): Double {
        val total = result.bidLiquidityTop5 + result.askLiquidityTop5
        return result.bidLiquidityTop5 / total
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
}
