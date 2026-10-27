
package ir.nobitextrader.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class MarketStats(
    val market: String,
    val latest: Double,
    val bestBuy: Double,
    val bestSell: Double,
    val dayLow: Double,
    val dayHigh: Double,
    val dayChange: Double,
    val isClosed: Boolean
)

object NobitexMarketApi {

    private const val STATS_URL =
        "https://apiv2.nobitex.ir/market/stats?srcCurrency=btc&dstCurrency=rls"

    suspend fun fetchMarketStats(): MarketStats =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null

            try {
                connection = (URL(STATS_URL).openConnection()
                    as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                }

                val statusCode = connection.responseCode

                if (statusCode !in 200..299) {
                    throw IOException(
                        "Nobitex stats request failed: HTTP $statusCode"
                    )
                }

                val response = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val root = try {
                    JSONObject(response)
                } catch (e: Exception) {
                    throw IOException("Nobitex returned invalid JSON", e)
                }

                val apiStatus = root.optString("status")
                if (apiStatus != "ok") {
                    throw IOException(
                        "Nobitex API status was not ok: $apiStatus"
                    )
                }

                val statsObject = root.optJSONObject("stats")
                    ?: throw IOException(
                        "Nobitex response is missing the stats object"
                    )

                val marketObject =
                    statsObject.optJSONObject("btc-rls")
                        ?: statsObject.optJSONObject("btc-irt")
                        ?: throw IOException(
                            "BTC market data is missing. Available markets: " +
                                statsObject.keys().asSequence().toList()
                                    .joinToString()
                        )

                val marketStats = MarketStats(
                    market = "BTCIRT",
                    latest = marketStatsNumber(marketObject, "latest"),
                    bestBuy = marketStatsNumber(marketObject, "bestBuy"),
                    bestSell = marketStatsNumber(marketObject, "bestSell"),
                    dayLow = marketStatsNumber(marketObject, "dayLow"),
                    dayHigh = marketStatsNumber(marketObject, "dayHigh"),
                    dayChange = marketStatsNumber(marketObject, "dayChange"),
                    isClosed = marketStatsBoolean(marketObject, "isClosed")
                )

                val validationErrors =
                    MarketStatsValidator.validate(marketStats)

                if (validationErrors.isNotEmpty()) {
                    throw IOException(
                        "Invalid BTC market data: " +
                            validationErrors.joinToString("; ")
                    )
                }

                marketStats
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                throw IOException(
                    "Unable to fetch Nobitex market stats",
                    e
                )
            } finally {
                connection?.disconnect()
            }
        }

    private fun marketStatsNumber(
        json: JSONObject,
        key: String
    ): Double {
        if (!json.has(key) || json.isNull(key)) {
            throw IOException("Nobitex response is missing '$key'")
        }

        val value = when (val raw = json.get(key)) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
                ?: throw IOException(
                    "Nobitex field '$key' is not numeric"
                )
            else -> throw IOException(
                "Nobitex field '$key' has an invalid type"
            )
        }

        if (!value.isFinite()) {
            throw IOException("Nobitex field '$key' is not finite")
        }

        return value
    }

    private fun marketStatsBoolean(
        json: JSONObject,
        key: String
    ): Boolean {
        if (!json.has(key) || json.isNull(key)) {
            throw IOException("Nobitex response is missing '$key'")
        }

        return when (val raw = json.get(key)) {
            is Boolean -> raw
            is String -> when (raw.trim().lowercase()) {
                "true" -> true
                "false" -> false
                else -> throw IOException(
                    "Nobitex field '$key' is not a valid Boolean"
                )
            }
            else -> throw IOException(
                "Nobitex field '$key' has an invalid type"
            )
        }
    }
}
