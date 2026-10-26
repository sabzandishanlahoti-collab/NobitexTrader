
package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.data.NobitexOrderBookApi
import ir.nobitextrader.app.data.OrderBookSnapshot
import java.io.IOException
import java.util.Locale
import kotlin.math.max

enum class MarketSortMode {
    SPREAD_LOW_TO_HIGH,
    BID_LIQUIDITY_HIGH_TO_LOW,
    ASK_LIQUIDITY_HIGH_TO_LOW
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

    val defaultMarkets: List<String> = listOf(
        "BTCIRT",
        "ETHIRT",
        "LTCIRT",
        "XRPIRT",
        "TRXIRT",
        "DOGEIRT",
        "ADAIRT",
        "LINKIRT",
        "ETCIRT",
        "BNBIRT"
    )

    suspend fun scan(
        symbols: List<String> = defaultMarkets
    ): List<MarketScanResult> {
        val normalizedSymbols = symbols
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.matches(Regex("[A-Z0-9]{3,20}")) }
            .distinct()

        if (normalizedSymbols.isEmpty()) {
            return emptyList()
        }

        return normalizedSymbols.map { symbol ->
            try {
                toScanResult(NobitexOrderBookApi.fetch(symbol))
            } catch (exception: Exception) {
                MarketScanResult(
                    symbol = symbol,
                    lastPrice = 0.0,
                    bestBid = 0.0,
                    bestAsk = 0.0,
                    spreadPercent = 0.0,
                    bidLiquidityTop5 = 0.0,
                    askLiquidityTop5 = 0.0,
                    success = false,
                    error = exception.message ?: "خطای دریافت اطلاعات بازار"
                )
            }
        }
    }

    suspend fun scanAll(): List<MarketScanResult> {
        val snapshots = NobitexOrderBookApi.fetchAll()

        val results = snapshots
            .filter { it.symbol.matches(Regex("[A-Z0-9]{3,20}")) }
            .distinctBy { it.symbol }
            .map { snapshot -> toScanResult(snapshot) }

        if (results.isEmpty()) {
            throw IOException("اطلاعات معتبر بازار از نوبیتکس دریافت نشد.")
        }

        return results
    }

    fun sortResults(
        results: List<MarketScanResult>,
        mode: MarketSortMode
    ): List<MarketScanResult> {
        val successful = results.filter { it.success }
        val failed = results.filterNot { it.success }

        val sortedSuccessful = when (mode) {
            MarketSortMode.SPREAD_LOW_TO_HIGH ->
                successful.sortedBy { it.spreadPercent }

            MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedByDescending { it.bidLiquidityTop5 }

            MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW ->
                successful.sortedByDescending { it.askLiquidityTop5 }
        }

        return sortedSuccessful + failed
    }

    fun successfulCount(results: List<MarketScanResult>): Int =
        results.count { it.success }

    fun failedCount(results: List<MarketScanResult>): Int =
        results.count { !it.success }

    private fun toScanResult(
        snapshot: OrderBookSnapshot
    ): MarketScanResult {
        val bids = snapshot.bids
            .filter { it.price > 0.0 && it.amount >= 0.0 }
            .sortedByDescending { it.price }

        val asks = snapshot.asks
            .filter { it.price > 0.0 && it.amount >= 0.0 }
            .sortedBy { it.price }

        val bestBid = bids.firstOrNull()?.price ?: 0.0
        val bestAsk = asks.firstOrNull()?.price ?: 0.0

        val validBook = bestBid > 0.0 &&
            bestAsk > 0.0 &&
            bestAsk >= bestBid

        val spread = if (validBook) {
            ((bestAsk - bestBid) / bestBid) * 100.0
        } else {
            0.0
        }

        val bidLiquidity = bids.take(5).sumOf {
            it.price * it.amount
        }

        val askLiquidity = asks.take(5).sumOf {
            it.price * it.amount
        }

        return MarketScanResult(
            symbol = snapshot.symbol,
            lastPrice = snapshot.lastTradePrice,
            bestBid = bestBid,
            bestAsk = bestAsk,
            spreadPercent = max(0.0, spread),
            bidLiquidityTop5 = bidLiquidity,
            askLiquidityTop5 = askLiquidity,
            success = validBook
        )
    }
}
