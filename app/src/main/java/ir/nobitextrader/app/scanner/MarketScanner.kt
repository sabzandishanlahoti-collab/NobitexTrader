
package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.data.NobitexOrderBookApi
import ir.nobitextrader.app.data.OrderBookSnapshot
import java.io.IOException
import java.util.Locale

enum class MarketSortMode {
    SPREAD_LOW_TO_HIGH,
    BID_LIQUIDITY_HIGH_TO_LOW,
    ASK_LIQUIDITY_HIGH_TO_LOW,
    BALANCED_PRIORITY
}

data class MarketScanResult(
    val symbol: String,
    val lastPrice: Double,
    val bestBid: Double,
    val bestAsk: Double,
    val spreadPercent: Double,
    val bidLiquidityTop5: Double,
    val askLiquidityTop5: Double,
    val success: Boolean,
    val error: String? = null
)

object MarketScanner {

    val defaultMarkets = listOf(
        "BTCIRT", "ETHIRT", "LTCIRT", "XRPIRT", "TRXIRT",
        "DOGEIRT", "ADAIRT", "LINKIRT", "ETCIRT", "BNBIRT"
    )

    suspend fun scan(
        symbols: List<String> = defaultMarkets
    ): List<MarketScanResult> {
        val validSymbols = symbols
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.matches(Regex("[A-Z0-9]{3,20}")) }
            .distinct()

        return validSymbols.map { symbol ->
            try {
                toScanResult(NobitexOrderBookApi.fetch(symbol))
            } catch (e: Exception) {
                MarketScanResult(
                    symbol = symbol,
                    lastPrice = 0.0,
                    bestBid = 0.0,
                    bestAsk = 0.0,
                    spreadPercent = 0.0,
                    bidLiquidityTop5 = 0.0,
                    askLiquidityTop5 = 0.0,
                    success = false,
                    error = e.message ?: "خطای دریافت اطلاعات بازار"
                )
            }
        }
    }

    suspend fun scanAll(): List<MarketScanResult> {
        val results = NobitexOrderBookApi.fetchAll()
            .filter { it.symbol.matches(Regex("[A-Z0-9]{3,20}")) }
            .distinctBy { it.symbol }
            .map { toScanResult(it) }

        if (results.isEmpty()) {
            throw IOException("اطلاعات معتبر بازار دریافت نشد.")
        }

        return results
    }

    fun sortResults(
        results: List<MarketScanResult>,
        mode: MarketSortMode
    ): List<MarketScanResult> {
        val successful = results.filter { it.success }
        val failed = results.filterNot { it.success }

        val sorted = when (mode) {
            MarketSortMode.SPREAD_LOW_TO_HIGH ->
                successful.sortedBy { it.spreadPercent }

            MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedByDescending { it.bidLiquidityTop5 }

            MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedByDescending { it.askLiquidityTop5 }

            MarketSortMode.BALANCED_PRIORITY ->
                sortByBalancedPriority(successful)
        }

        return sorted + failed
    }

    private fun sortByBalancedPriority(
        results: List<MarketScanResult>
    ): List<MarketScanResult> {
        if (results.size < 2) return results

        val minSpread = results.minOf { it.spreadPercent }
        val maxSpread = results.maxOf { it.spreadPercent }
        val maxBid = results.maxOf { it.bidLiquidityTop5 }
        val maxAsk = results.maxOf { it.askLiquidityTop5 }

        fun normalizedLowerIsBetter(
            value: Double,
            min: Double,
            max: Double
        ): Double {
            if (max <= min) return 1.0
            return (max - value) / (max - min)
        }

        fun normalizedHigherIsBetter(
            value: Double,
            max: Double
        ): Double {
            if (max <= 0.0) return 0.0
            return (value / max).coerceIn(0.0, 1.0)
        }

        return results.sortedByDescending { result ->
            val spreadScore = normalizedLowerIsBetter(
                result.spreadPercent,
                minSpread,
                maxSpread
            )
            val bidScore = normalizedHigherIsBetter(
                result.bidLiquidityTop5,
                maxBid
            )
            val askScore = normalizedHigherIsBetter(
                result.askLiquidityTop5,
                maxAsk
            )

            (spreadScore * 0.50) +
                (bidScore * 0.25) +
                (askScore * 0.25)
        }
    }

    fun successfulCount(results: List<MarketScanResult>) =
        results.count { it.success }

    fun failedCount(results: List<MarketScanResult>) =
        results.count { !it.success }

    private fun toScanResult(
        snapshot: OrderBookSnapshot
    ): MarketScanResult {
        val bids = snapshot.bids
            .filter { it.price.isFinite() && it.amount.isFinite() &&
                it.price > 0.0 && it.amount >= 0.0 }
            .sortedByDescending { it.price }

        val asks = snapshot.asks
            .filter { it.price.isFinite() && it.amount.isFinite() &&
                it.price > 0.0 && it.amount >= 0.0 }
            .sortedBy { it.price }

        val bestBid = bids.firstOrNull()?.price ?: 0.0
        val bestAsk = asks.firstOrNull()?.price ?: 0.0

        val validBook = bestBid > 0.0 &&
            bestAsk > 0.0 &&
            bestAsk >= bestBid

        val spreadPercent = if (validBook) {
            ((bestAsk - bestBid) / bestBid) * 100.0
        } else {
            0.0
        }

        return MarketScanResult(
            symbol = snapshot.symbol,
            lastPrice = snapshot.lastTradePrice,
            bestBid = bestBid,
            bestAsk = bestAsk,
            spreadPercent = spreadPercent.coerceAtLeast(0.0),
            bidLiquidityTop5 = bids.take(5).sumOf {
                it.price * it.amount
            },
            askLiquidityTop5 = asks.take(5).sumOf {
                it.price * it.amount
            },
            success = validBook
        )
    }
}
