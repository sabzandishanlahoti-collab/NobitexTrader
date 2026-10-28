
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

    private const val TIMEOUT_MS = 10_000

    suspend fun fetchRawMarketStatsResponse(): String =
        withContext(Dispatchers.IO) {
            requestStatsResponse()
        }

    suspend fun fetchMarketStats(): MarketStats =
        withContext(Dispatchers.IO) {
            val response = requestStatsResponse()

            val root = try {
                JSONObject(response)
            } catch (e: Exception) {
                throw IOException(
                    "Invalid Nobitex JSON: ${response.take(500)}",
                    e
                )
            }

            val apiStatus = root.optString("status")
            if (apiStatus != "ok") {
                throw IOException(
                    "Nobitex status=$apiStatus; ${response.take(500)}"
                )
            }

            val statsObject = root.optJSONObject("stats")
                ?: throw IOException(
                    "Missing stats object: ${response.take(500)}"
                )

            val marketObject =
                statsObject.optJSONObject("btc-rls")
                    ?: statsObject.optJSONObject("btc-irt")
                    ?: throw IOException(
                        "BTC market missing. Available markets: " +
                            statsObject.keys().asSequence().toList()
                                .joinToString()
                    )

            val result = MarketStats(
                market = "BTCIRT",
                latest = readNumber(marketObject, "latest"),
                bestBuy = readNumber(marketObject, "bestBuy"),
                bestSell = readNumber(marketObject, "bestSell"),
                dayLow = readNumber(marketObject, "dayLow"),
                dayHigh = readNumber(marketObject, "dayHigh"),
                dayChange = readNumber(marketObject, "dayChange"),
                isClosed = readBoolean(marketObject, "isClosed")
            )

            val errors = MarketStatsValidator.validate(result)
            if (errors.isNotEmpty()) {
                throw IOException(
                    "Invalid BTC market data: ${errors.joinToString()}; " +
                        "raw=$marketObject"
                )
            }

            result
        }

    private fun requestStatsResponse(): String {
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(STATS_URL).openConnection()
                as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NobitexTrader/1.0")
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = stream
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                throw IOException(
                    "Nobitex HTTP $code: ${body.take(1000)}"
                )
            }

            if (body.isBlank()) {
                throw IOException("Nobitex returned an empty response")
            }

            return body
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Nobitex request failed: " +
                    (e.message ?: e.javaClass.simpleName),
                e
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun readNumber(
        json: JSONObject,
        key: String
    ): Double {
        if (!json.has(key) || json.isNull(key)) {
            throw IOException("Missing '$key': $json")
        }

        val value = when (val raw = json.get(key)) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull()
                ?: throw IOException("Invalid numeric field '$key': $raw")
            else -> throw IOException("Invalid type for '$key': $raw")
        }

        if (!value.isFinite()) {
            throw IOException("Non-finite field '$key': $value")
        }

        return value
    }

    private fun readBoolean(
        json: JSONObject,
        key: String
    ): Boolean {
        if (!json.has(key) || json.isNull(key)) {
            throw IOException("Missing '$key': $json")
        }

        return when (val raw = json.get(key)) {
            is Boolean -> raw
            is String -> when (raw.trim().lowercase()) {
                "true" -> true
                "false" -> false
                else -> throw IOException("Invalid Boolean '$key': $raw")
            }
            is Number -> when (raw.toInt()) {
                1 -> true
                0 -> false
                else -> throw IOException("Invalid Boolean '$key': $raw")
            }
            else -> throw IOException("Invalid type for '$key': $raw")
        }
    }
}
