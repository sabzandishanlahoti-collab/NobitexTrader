
package ir.nobitextrader.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only client for Nobitex candle history.
 *
 * No orders are submitted by this class.
 */
object NobitexCandlesApi {

    private const val BASE_URL =
        "https://apiv2.nobitex.ir/market/udf/history"

    private const val CONNECTION_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    /**
     * Fetches historical candles for a market.
     *
     * @param symbol Market symbol, for example BTCIRT.
     * @param resolution UDF resolution, for example 60 or 240.
     * @param fromUnixSeconds Start time in Unix seconds.
     * @param toUnixSeconds End time in Unix seconds.
     */
    suspend fun fetchCandles(
        symbol: String,
        resolution: String,
        fromUnixSeconds: Long,
        toUnixSeconds: Long
    ): List<Candle> = withContext(Dispatchers.IO) {

        require(symbol.isNotBlank()) {
            "Market symbol must not be blank"
        }
        require(resolution.isNotBlank()) {
            "Resolution must not be blank"
        }
        require(fromUnixSeconds >= 0L) {
            "Start time must not be negative"
        }
        require(toUnixSeconds > fromUnixSeconds) {
            "End time must be greater than start time"
        }

        val url = URL(
            "$BASE_URL?symbol=$symbol" +
                "&resolution=$resolution" +
                "&from=$fromUnixSeconds" +
                "&to=$toUnixSeconds"
        )

        var connection: HttpURLConnection? = null

        try {
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }

            val statusCode = connection.responseCode

            if (statusCode !in 200..299) {
                throw IOException(
                    "Nobitex candle request failed: HTTP $statusCode"
                )
            }

            val body = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val root = try {
                JSONObject(body)
            } catch (e: Exception) {
                throw IOException(
                    "Nobitex returned invalid candle JSON",
                    e
                )
            }

            if (root.optString("s") != "ok") {
                throw IOException(
                    "Nobitex candle response status was not ok: " +
                        root.optString("s")
                )
            }

            val times = root.optJSONArray("t")
                ?: throw IOException("Candle response is missing 't'")
            val opens = root.optJSONArray("o")
                ?: throw IOException("Candle response is missing 'o'")
            val highs = root.optJSONArray("h")
                ?: throw IOException("Candle response is missing 'h'")
            val lows = root.optJSONArray("l")
                ?: throw IOException("Candle response is missing 'l'")
            val closes = root.optJSONArray("c")
                ?: throw IOException("Candle response is missing 'c'")
            val volumes = root.optJSONArray("v")
                ?: throw IOException("Candle response is missing 'v'")

            val count = times.length()

            if (
                opens.length() != count ||
                highs.length() != count ||
                lows.length() != count ||
                closes.length() != count ||
                volumes.length() != count
            ) {
                throw IOException(
                    "Candle response arrays have different lengths"
                )
            }

            val candles = ArrayList<Candle>(count)

            for (index in 0 until count) {
                val timestampSeconds = times.getLong(index)

                val candle = Candle(
                    timestampMillis = timestampSeconds * 1000L,
                    open = numberAt(opens, index, "o"),
                    high = numberAt(highs, index, "h"),
                    low = numberAt(lows, index, "l"),
                    close = numberAt(closes, index, "c"),
                    volume = numberAt(volumes, index, "v")
                )

                if (!candle.isValid()) {
                    throw IOException(
                        "Invalid candle at response index $index"
                    )
                }

                candles += candle
            }

            candles.sortedBy { it.timestampMillis }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Unable to fetch Nobitex candle history",
                e
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun numberAt(
        array: JSONArray,
        index: Int,
        field: String
    ): Double {
        val value = array.getDouble(index)

        if (!value.isFinite()) {
            throw IOException(
                "Candle field '$field' is not finite at index $index"
            )
        }

        return value
    }
}
