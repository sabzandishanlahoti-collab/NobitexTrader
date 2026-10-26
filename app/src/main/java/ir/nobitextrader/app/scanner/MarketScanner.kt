
package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.data.NobitexOrderBookApi
import ir.nobitextrader.app.data.OrderBookSnapshot
import kotlinx.coroutines.CancellationException

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

    // Used only when a specific list of symbols is requested.
    // scanAll() obtains the actual markets from Nobitex.
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

    fun scan(
        symbols: List<String> = defaultMarkets
    ): List<MarketScanResult> {
        require(symbols.size <= 500) {
            "Scan is limited to 500 markets per call"
        }

        val results = mutableListOf<MarketScanResult>()
        val seen = mutableSetOf<String>()

        for (rawSymbol in symbols) {
            val symbol = rawSymbol.trim().uppercase()

            if (!symbol.matches(Regex("[A-Z0-9]{3,20}"))) {
                results.add(
                    failedResult(symbol, "Invalid market symbol")
                )
                continue
            }

            if (!seen.add(symbol)) continue

            try {
                results.add(
                    NobitexOrderBookApi.fetch(symbol).toScanResult()
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                results.add(
                    failedResult(
                        symbol,
                        e.message ?: "Unknown market data error"
                    )
                )
            }
        }

        return results
    }

    /**
     * Retrieves the market list from Nobitex's public all-order-books
     * endpoint. It does not treat the defaultMarkets list as all markets.
     */
    fun scanAll(): List<MarketScanResult> {
        val snapshots = try {
            NobitexOrderBookApi.fetchAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(
                e.message ?: "Failed to fetch Nobitex order books",
                e
            )
        }

        val uniqueSnapshots = snapshots
            .filter {
                it.symbol.matches(Regex("[A-Z0-9]{3,20}"))
            }
            .distinctBy { it.symbol }

        if (uniqueSnapshots.isEmpty()) {
            throw IllegalStateException(
                "Nobitex returned no valid market order books"
            )
        }

        return uniqueSnapshots.map { snapshot ->
            snapshot.toScanResult()
        }
    }

    private fun OrderBookSnapshot.toScanResult(): MarketScanResult {
        val bid = bestBid
        val ask = bestAsk

        val validBook =
            bids.isNotEmpty() &&
            asks.isNotEmpty() &&
            bid != null &&
            ask != null &&
            bid > 0.0 &&
            ask > 0.0 &&
            ask >= bid

        return MarketScanResult(
            symbol = symbol,
            lastPrice = lastTradePrice,
            bestBid = bid,
            bestAsk = ask,
            spreadPercent = if (validBook) spreadPercent else null,
            bidLiquidityTop5 = bids
                .take(5)
                .sumOf { it.amount },
            askLiquidityTop5 = asks
                .take(5)
                .sumOf { it.amount },
            success = validBook,
            error = if (validBook) {
                null
            } else {
                "Order book has missing or invalid bid/ask levels"
            }
        )
    }

    private fun failedResult(
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
