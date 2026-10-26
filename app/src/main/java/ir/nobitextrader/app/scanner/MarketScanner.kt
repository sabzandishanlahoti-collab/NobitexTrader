
package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.data.NobitexOrderBookApi
import java.io.IOException

data class MarketScanResult(
    val symbol: String,
    val lastPrice: Double,
    val bestBid: Double?,
    val bestAsk: Double?,
    val spreadPercent: Double?,
    val bidLiquidityTop5: Double,
    val askLiquidityTop5: Double,
    val success: Boolean,
    val error: String? = null
)

object MarketScanner {

    /**
     * Retained for callers that explicitly request these markets.
     * The default scan now discovers markets from Nobitex.
     */
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

    /**
     * Fetches all public order books in one API request.
     * Must be called on a background thread.
     */
    fun scanAll(): List<MarketScanResult> {
        return try {
            val books = NobitexOrderBookApi.fetchAll()

            books.map { book ->
                MarketScanResult(
                    symbol = book.symbol,
                    lastPrice = book.lastTradePrice,
                    bestBid = book.bestBid,
                    bestAsk = book.bestAsk,
                    spreadPercent = book.spreadPercent,
                    bidLiquidityTop5 = book.bids
                        .take(5)
                        .sumOf { it.amount },
                    askLiquidityTop5 = book.asks
                        .take(5)
                        .sumOf { it.amount },
                    success = true
                )
            }
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }

            throw IOException(
                e.message ?: "Failed to fetch Nobitex markets",
                e
            )
        }
    }

    /**
     * Scans explicitly supplied symbols using individual requests.
     */
    fun scan(
        symbols: List<String> = defaultMarkets
    ): List<MarketScanResult> {
        require(symbols.size <= 50) {
            "Scan is limited to 50 markets per call"
        }

        val results = mutableListOf<MarketScanResult>()
        val seen = mutableSetOf<String>()

        for (rawSymbol in symbols) {
            val symbol = rawSymbol.trim().uppercase()

            if (!symbol.matches(Regex("[A-Z0-9]{3,20}"))) {
                results.add(
                    failure(symbol, "Invalid market symbol")
                )
                continue
            }

            if (!seen.add(symbol)) continue

            try {
                val book = NobitexOrderBookApi.fetch(symbol)

                results.add(
                    MarketScanResult(
                        symbol = book.symbol,
                        lastPrice = book.lastTradePrice,
                        bestBid = book.bestBid,
                        bestAsk = book.bestAsk,
                        spreadPercent = book.spreadPercent,
                        bidLiquidityTop5 = book.bids
                            .take(5)
                            .sumOf { it.amount },
                        askLiquidityTop5 = book.asks
                            .take(5)
                            .sumOf { it.amount },
                        success = true
                    )
                )
            } catch (e: Exception) {
                results.add(
                    failure(
                        symbol,
                        e.message ?: "Unknown market data error"
                    )
                )
            }
        }

        return results
    }

    private fun failure(
        symbol: String,
        message: String
    ) = MarketScanResult(
        symbol = symbol,
        lastPrice = 0.0,
        bestBid = null,
        bestAsk = null,
        spreadPercent = null,
        bidLiquidityTop5 = 0.0,
        askLiquidityTop5 = 0.0,
        success = false,
        error = message
    )

    fun rankBySpread(
        results: List<MarketScanResult>
    ): List<MarketScanResult> {
        return results
            .filter {
                it.success &&
                    it.spreadPercent != null &&
                    it.spreadPercent.isFinite() &&
                    it.spreadPercent >= 0.0 &&
                    it.bestBid != null &&
                    it.bestAsk != null
            }
            .sortedBy { it.spreadPercent }
    }

    fun successfulCount(
        results: List<MarketScanResult>
    ): Int = results.count { it.success }

    fun failedCount(
        results: List<MarketScanResult>
    ): Int = results.count { !it.success }
}
