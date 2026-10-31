
package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.data.NobitexOrderBookApi
import ir.nobitextrader.app.data.OrderBookSnapshot
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CancellationException

enum class MarketSortMode {
    SPREAD_LOW_TO_HIGH,
    BID_LIQUIDITY_HIGH_TO_LOW,
    ASK_LIQUIDITY_HIGH_TO_LOW,
    BUY_PRESSURE_HIGH_TO_LOW,
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

    private val symbolPattern = Regex("[A-Z0-9]{3,20}")

    val defaultMarkets = listOf(
        "BTCIRT", "ETHIRT", "LTCIRT", "XRPIRT", "TRXIRT",
        "DOGEIRT", "ADAIRT", "LINKIRT", "ETCIRT", "BNBIRT"
    )

    suspend fun scan(
        symbols: List<String> = defaultMarkets,
        policy: MarketRefreshPolicy = MarketRefreshPolicy.DEFAULT,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
        onResult: ((MarketScanResult) -> Unit)? = null
    ): List<MarketScanResult> {
        val validSymbols = symbols
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.matches(symbolPattern) }
            .distinct()
            .take(policy.maxMarketsPerBatch)

        val total = validSymbols.size
        var completed = 0
        val results = mutableListOf<MarketScanResult>()

        onProgress?.invoke(0, total)

        for (symbol in validSymbols) {
            try {
                val snapshot = NobitexOrderBookApi.fetch(
                    symbol = symbol,
                    timeoutMillis = policy.requestTimeoutMillis
                )
                val result = toScanResult(snapshot)
                results.add(result)
                onResult?.invoke(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                results.add(
                    failedResult(
                        symbol = symbol,
                        message = e.message
                            ?: "خطای دریافت اطلاعات بازار"
                    ).also { onResult?.invoke(it) }
                )
            } finally {
                completed++
                onProgress?.invoke(completed, total)
            }
        }

        return results
    }

    /**
     * Scans every valid order book returned by the exchange's all endpoint.
     * maxMarketsPerBatch is retained for compatibility with the policy,
     * but does not cap the total results of scanAll().
     */
    suspend fun scanAll(
        policy: MarketRefreshPolicy = MarketRefreshPolicy.DEFAULT,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
        onResult: ((MarketScanResult) -> Unit)? = null
    ): List<MarketScanResult> {
        val snapshots = NobitexOrderBookApi.fetchAll(
            timeoutMillis = policy.requestTimeoutMillis,
            maxMarkets = policy.maxMarketsPerBatch
        )

        val validSnapshots = snapshots
            .filter { it.symbol.matches(symbolPattern) }
            .distinctBy { it.symbol }

        val total = validSnapshots.size
        var completed = 0
        val results = mutableListOf<MarketScanResult>()

        onProgress?.invoke(0, total)

        for (snapshot in validSnapshots) {
            try {
                val result = toScanResult(snapshot)
                results.add(result)
                onResult?.invoke(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                results.add(
                    failedResult(
                        symbol = snapshot.symbol,
                        message = e.message
                            ?: "خطای پردازش دفتر سفارش"
                    ).also { onResult?.invoke(it) }
                )
            } finally {
                completed++
                onProgress?.invoke(completed, total)
            }
        }

        if (results.none { it.success }) {
            throw IOException("هیچ دفتر سفارش معتبری دریافت نشد.")
        }

        return results
    }

    fun sortResults(
        results: List<MarketScanResult>,
        mode: MarketSortMode
    ): List<MarketScanResult> = MarketRanking.rank(results, mode)

    fun successfulCount(results: List<MarketScanResult>): Int =
        results.count { it.success }

    fun failedCount(results: List<MarketScanResult>): Int =
        results.count { !it.success }

    private fun failedResult(
        symbol: String,
        message: String
    ) = MarketScanResult(
        symbol = symbol,
        lastPrice = 0.0,
        bestBid = 0.0,
        bestAsk = 0.0,
        spreadPercent = 0.0,
        bidLiquidityTop5 = 0.0,
        askLiquidityTop5 = 0.0,
        success = false,
        error = message
    )

    private fun toScanResult(
        snapshot: OrderBookSnapshot
    ): MarketScanResult {
        val bids = snapshot.bids
            .filter {
                it.price.isFinite() &&
                    it.amount.isFinite() &&
                    it.price > 0.0 &&
                    it.amount > 0.0
            }
            .sortedByDescending { it.price }

        val asks = snapshot.asks
            .filter {
                it.price.isFinite() &&
                    it.amount.isFinite() &&
                    it.price > 0.0 &&
                    it.amount > 0.0
            }
            .sortedBy { it.price }

        val bestBid = bids.firstOrNull()?.price ?: 0.0
        val bestAsk = asks.firstOrNull()?.price ?: 0.0
        val lastPrice = snapshot.lastTradePrice

        val validBook = bestBid.isFinite() &&
            bestAsk.isFinite() &&
            bestBid > 0.0 &&
            bestAsk > 0.0 &&
            bestAsk >= bestBid

        if (!validBook) {
            return failedResult(
                snapshot.symbol,
                "دفتر سفارش نامعتبر یا قیمت خرید و فروش ناسازگار است."
            )
        }

        if (!lastPrice.isFinite() || lastPrice <= 0.0) {
            return failedResult(
                snapshot.symbol,
                "قیمت آخرین معامله معتبر نیست."
            )
        }

        val spreadPercent =
            ((bestAsk - bestBid) / bestBid) * 100.0

        val bidLiquidity = bids.take(5).sumOf {
            it.price * it.amount
        }

        val askLiquidity = asks.take(5).sumOf {
            it.price * it.amount
        }

        if (!spreadPercent.isFinite() ||
            !bidLiquidity.isFinite() ||
            !askLiquidity.isFinite()
        ) {
            return failedResult(
                snapshot.symbol,
                "محاسبات دفتر سفارش مقدار غیرمعتبر تولید کرد."
            )
        }

        return MarketScanResult(
            symbol = snapshot.symbol,
            lastPrice = lastPrice,
            bestBid = bestBid,
            bestAsk = bestAsk,
            spreadPercent = spreadPercent.coerceAtLeast(0.0),
            bidLiquidityTop5 = bidLiquidity,
            askLiquidityTop5 = askLiquidity,
            success = true
        )
    }
}
