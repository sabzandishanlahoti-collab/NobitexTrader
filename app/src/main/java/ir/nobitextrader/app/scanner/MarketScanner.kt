package ir.nobitextrader.app.scanner

import ir.nobitextrader.app.data.NobitexOrderBookApi
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
                val book = NobitexOrderBookApi.fetch(symbol)
                results.add(book.toScanResult())
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

    fun scanAll(): List<MarketScanResult> {
        return try {
            NobitexOrderBookApi.fetchAll()
                .distinctBy { it.symbol }
                .map { it.toScanResult() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw IllegalStateException(
                e.message ?: "Failed to fetch all Nobitex order books",
                e
            )
        }
    }

    private fun ir.nobitextrader.app.data.OrderBookSnapshot
        .toScanResult(): MarketScanResult {
        return MarketScanResult(
            symbol = symbol,
            lastPrice = lastTradePrice,
            bestBid = bestBid,
            bestAsk = bestAsk,
            spreadPercent = spreadPercent,
            bidLiquidityTop5 = bids
                .take(5)
                .sumOf { it.amount },
            askLiquidityTop5 = asks
                .take(5)
                .sumOf { it.amount },
            success = bids.isNotEmpty() && asks.isNotEmpty(),
            error = if (bids.isEmpty() || asks.isEmpty()) {
                "Order book has no valid bid or ask levels"
            } else {
                null
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
    ): Int {
        return results.count { it.success }
    }

    fun failedCount(
        results: List<MarketScanResult>
    ): Int {
        return results.count { !it.success }
    }
}
