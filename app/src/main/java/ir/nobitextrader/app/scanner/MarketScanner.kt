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
 * Performs real public order-book requests.  
 *  
 * Call from a background thread, not the UI thread.  
 * Unsupported or unavailable markets are reported as failures.  
 * No simulated prices or fallback market data are generated.  
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
                MarketScanResult(  
                    symbol = symbol,  
                    lastPrice = 0.0,  
                    bestBid = null,  
                    bestAsk = null,  
                    spreadPercent = null,  
                    bidLiquidityTop5 = 0.0,  
                    askLiquidityTop5 = 0.0,  
                    success = false,  
                    error = "Invalid market symbol"  
                )  
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
                MarketScanResult(  
                    symbol = symbol,  
                    lastPrice = 0.0,  
                    bestBid = null,  
                    bestAsk = null,  
                    spreadPercent = null,  
                    bidLiquidityTop5 = 0.0,  
                    askLiquidityTop5 = 0.0,  
                    success = false,  
                    error = e.message  
                        ?: "Unknown market data error"  
                )  
            )  
        }  
    }  

    return results  
}  

/**  
 * Returns successfully fetched markets with a valid spread,  
 * ordered from smallest spread to largest.  
 *  
 * A narrow spread alone does not make a market profitable.  
 */  
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
