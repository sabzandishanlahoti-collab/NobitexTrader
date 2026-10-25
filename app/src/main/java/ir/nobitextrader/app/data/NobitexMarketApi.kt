
package ir.nobitextrader.app.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class MarketStats(
    val market: String,
    val latest: String,
    val bestBuy: String,
    val bestSell: String,
    val dayLow: String,
    val dayHigh: String,
    val dayChange: String,
    val isClosed: Boolean
)

object NobitexMarketApi {

    private const val BASE_URL =
        "https://apiv2.nobitex.ir/market/stats"

    /**
     * Fetches public market statistics.
     *
     * IMPORTANT:
     * Call this function on a background thread,
     * never directly on the Android main thread.
     *
     * Defaults to BTC/IRT (BTC/RLS in Nobitex API).
     */
    @Throws(IOException::class)
    fun fetchMarketStats(
        srcCurrency: String = "btc",
        dstCurrency: String = "rls"
    ): MarketStats {

        require(srcCurrency.matches(Regex("[a-zA-Z0-9]+"))) {
            "Invalid source currency"
        }

        require(dstCurrency.matches(Regex("[a-zA-Z0-9]+"))) {
            "Invalid destination currency"
        }

        val source = srcCurrency.lowercase()
        val destination = dstCurrency.lowercase()

        val query = "srcCurrency=${
            URLEncoder.encode(source, "UTF-8")
        }&dstCurrency=${
            URLEncoder.encode(destination, "UTF-8")
        }"

        val connection =
            (URL("$BASE_URL?$query").openConnection()
                as HttpURLConnection)

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val statusCode = connection.responseCode

            val responseStream =
                if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                } ?: throw IOException(
                    "Nobitex HTTP error: $statusCode"
                )

            val responseText = responseStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            if (statusCode !in 200..299) {
                throw IOException(
                    "Nobitex HTTP $statusCode: $responseText"
                )
            }

            val root = JSONObject(responseText)

            if (root.optString("status") != "ok") {
                throw IOException(
                    "Nobitex API returned an unsuccessful status"
                )
            }

            val stats = root.optJSONObject("stats")
                ?: throw IOException(
                    "Missing stats in Nobitex response"
                )

            val marketKey = "$source-$destination"

            val market = stats.optJSONObject(marketKey)
                ?: throw IOException(
                    "Market not found: $marketKey"
                )

            return MarketStats(
                market = marketKey,
                latest = market.optString("latest"),
                bestBuy = market.optString("bestBuy"),
                bestSell = market.optString("bestSell"),
                dayLow = market.optString("dayLow"),
                dayHigh = market.optString("dayHigh"),
                dayChange = market.optString("dayChange"),
                isClosed = market.optBoolean("isClosed", false)
            )
        } finally {
            connection.disconnect()
        }
    }
}
