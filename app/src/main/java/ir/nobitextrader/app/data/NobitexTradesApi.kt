package ir.nobitextrader.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * One publicly visible execution from Nobitex market data.
 *
 * This data does not identify the account or person behind the trade.
 */
data class PublicTrade(
    val timeMillis: Long,
    val price: Double,
    val volume: Double,
    val type: TradeSide
) {
    val quoteVolume: Double
        get() = price * volume
}

enum class TradeSide {
    BUY,
    SELL
}

/**
 * Read-only client for Nobitex public recent-trades endpoint.
 * GET /v2/trades/{symbol}; no account token is used.
 */
object NobitexTradesApi {

    private const val BASE_URL = "https://apiv2.nobitex.ir/v2/trades/"
    private const val TIMEOUT_MS = 10_000
    private val symbolPattern = Regex("[A-Z0-9]{3,20}")

    suspend fun fetchRecentTrades(
        symbol: String,
        timeoutMillis: Int = TIMEOUT_MS
    ): List<PublicTrade> = withContext(Dispatchers.IO) {
        val normalized = normalizeSymbol(symbol)
        require(timeoutMillis in 1_000..60_000) {
            "timeoutMillis must be between 1 and 60 seconds"
        }

        val connection = URL("$BASE_URL$normalized")
            .openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "NobitexTrader/1.0")

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                throw IOException("Nobitex HTTP $status: ${body.take(300)}")
            }

            parseResponse(body)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Nobitex recent-trades request failed: " +
                    (e.message ?: e.javaClass.simpleName),
                e
            )
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseResponse(body: String): List<PublicTrade> {
        if (body.isBlank()) throw IOException("Nobitex returned an empty trades response")

        val root = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw IOException("Nobitex trades response is not valid JSON", e)
        }

        if (root.optString("status") != "ok") {
            throw IOException(
                "Nobitex trades API status: ${root.optString("status", "missing")}"
            )
        }

        val trades = root.optJSONArray("trades")
            ?: throw IOException("Nobitex response is missing trades array")

        return parseTrades(trades)
    }

    internal fun parseTrades(array: JSONArray): List<PublicTrade> {
        val result = ArrayList<PublicTrade>(array.length())

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index)
                ?: throw IOException("Invalid trade entry at index $index")

            val time = readLong(item, "time", index)
            if (time <= 0L) {
                throw IOException("Invalid trade timestamp at index $index")
            }

            val price = readNumber(item, "price", index)
            val volume = readNumber(item, "volume", index)
            if (price <= 0.0 || volume < 0.0) {
                throw IOException("Non-positive price or negative volume at index $index")
            }

            val side = when (item.optString("type").lowercase(Locale.ROOT)) {
                "buy" -> TradeSide.BUY
                "sell" -> TradeSide.SELL
                else -> throw IOException("Unknown trade side at index $index")
            }

            result += PublicTrade(
                timeMillis = time,
                price = price,
                volume = volume,
                type = side
            )
        }

        return result
    }

    private fun normalizeSymbol(symbol: String): String {
        val normalized = symbol.trim().uppercase(Locale.ROOT)
        require(symbolPattern.matches(normalized)) { "Invalid market symbol" }
        return normalized
    }

    private fun readNumber(json: JSONObject, key: String, index: Int): Double {
        if (!json.has(key) || json.isNull(key)) {
            throw IOException("Missing trade field '$key' at index $index")
        }
        val value = when (val raw = json.get(key)) {
            is Number -> raw.toDouble()
            is String -> raw.trim().toDoubleOrNull()
                ?: throw IOException("Invalid trade field '$key' at index $index")
            else -> throw IOException("Invalid trade field '$key' at index $index")
        }
        if (!value.isFinite()) {
            throw IOException("Non-finite trade field '$key' at index $index")
        }
        return value
    }

    private fun readLong(json: JSONObject, key: String, index: Int): Long {
        val value = readNumber(json, key, index)
        if (value % 1.0 != 0.0 || value > Long.MAX_VALUE.toDouble()) {
            throw IOException("Invalid integer trade field '$key' at index $index")
        }
        return value.toLong()
    }
}
