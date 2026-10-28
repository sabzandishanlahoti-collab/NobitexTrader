
package ir.nobitextrader.app.data

import ir.nobitextrader.app.strategy.Candle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Read-only Nobitex candle client.
 * Never places orders.
 */
object NobitexCandlesApi {

    private const val BASE_URL =
        "https://apiv2.nobitex.ir/market/udf/history"

    private const val TIMEOUT_MS = 10_000

    suspend fun fetchCandles(
        symbol: String,
        resolution: String,
        fromUnixSeconds: Long,
        toUnixSeconds: Long
    ): List<Candle> = withContext(Dispatchers.IO) {

        require(symbol.isNotBlank()) { "Symbol must not be blank" }
        require(resolution.isNotBlank()) { "Resolution must not be blank" }
        require(fromUnixSeconds >= 0L) { "Invalid start time" }
        require(toUnixSeconds > fromUnixSeconds) {
            "End time must be greater than start time"
        }

        val url = URL(
            "$BASE_URL?symbol=${encode(symbol)}" +
                "&resolution=${encode(resolution)}" +
                "&from=$fromUnixSeconds" +
                "&to=$toUnixSeconds"
        )

        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(
                "User-Agent",
                "NobitexTrader/1.0"
            )

            val status = connection.responseCode
            val stream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = stream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (status !in 200..299) {
                throw IOException(
                    "Nobitex HTTP $status; response=${body.take(250)}"
                )
            }

            parseResponse(body)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Nobitex candle request failed: " +
                    (e.message ?: e.javaClass.simpleName),
                e
            )
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseResponse(body: String): List<Candle> {
        if (body.isBlank()) {
            throw IOException("Nobitex returned an empty response")
        }

        val root = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw IOException(
                "Nobitex response is not valid JSON: ${body.take(200)}",
                e
            )
        }

        when (root.optString("s")) {
            "no_data" -> return emptyList()
            "ok" -> Unit
            "error" -> throw IOException(
                "Nobitex candle API error: " +
                    root.optString("errmsg", "No details provided")
            )
            else -> throw IOException(
                "Unexpected candle response status: " +
                    root.optString("s", "missing")
            )
        }

        val times = requiredArray(root, "t")
        val opens = requiredArray(root, "o")
        val highs = requiredArray(root, "h")
        val lows = requiredArray(root, "l")
        val closes = requiredArray(root, "c")
        val volumes = requiredArray(root, "v")

        val count = times.length()

        if (
            opens.length() != count ||
            highs.length() != count ||
            lows.length() != count ||
            closes.length() != count ||
            volumes.length() != count
        ) {
            throw IOException("Candle arrays have inconsistent lengths")
        }

        val result = ArrayList<Candle>(count)

        for (i in 0 until count) {
            val seconds = try {
                times.getLong(i)
            } catch (e: Exception) {
                throw IOException("Invalid timestamp at index $i", e)
            }

            if (seconds <= 0L || seconds > Long.MAX_VALUE / 1000L) {
                throw IOException("Out-of-range timestamp at index $i")
            }

            val candle = Candle(
                timestampMillis = seconds * 1000L,
                open = readNumber(opens, i, "o"),
                high = readNumber(highs, i, "h"),
                low = readNumber(lows, i, "l"),
                close = readNumber(closes, i, "c"),
                volume = readNumber(volumes, i, "v")
            )

            if (!candle.isValid()) {
                throw IOException("Invalid OHLCV candle at index $i")
            }

            result += candle
        }

        return result.sortedBy { it.timestampMillis }
    }

    private fun requiredArray(
        root: JSONObject,
        key: String
    ): JSONArray =
        root.optJSONArray(key)
            ?: throw IOException("Missing candle array '$key'")

    private fun readNumber(
        array: JSONArray,
        index: Int,
        field: String
    ): Double {
        val value = try {
            array.getDouble(index)
        } catch (e: Exception) {
            throw IOException(
                "Invalid numeric field '$field' at index $index",
                e
            )
        }

        if (!value.isFinite()) {
            throw IOException(
                "Non-finite field '$field' at index $index"
            )
        }

        return value
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
