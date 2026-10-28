
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
 * Read-only client for Nobitex candle history.
 * Does not submit orders.
 */
object NobitexCandlesApi {

    private const val BASE_URL =
        "https://apiv2.nobitex.ir/market/udf/history"

    private const val CONNECTION_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

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

        val encodedSymbol = URLEncoder.encode(
            symbol,
            Charsets.UTF_8.name()
        )
        val encodedResolution = URLEncoder.encode(
            resolution,
            Charsets.UTF_8.name()
        )

        val url = URL(
            "$BASE_URL?symbol=$encodedSymbol" +
                "&resolution=$encodedResolution" +
                "&from=$fromUnixSeconds" +
                "&to=$toUnixSeconds"
        )

        var connection: HttpURLConnection? = null

        try {
            connection = (url.openConnection()
                as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty(
                    "User-Agent",
                    "TraderBot/NobitexTrader"
                )
            }

            val statusCode = connection.responseCode
            val contentType =
                connection.getHeaderField("Content-Type") ?: "unknown"

            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = stream?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()

            if (statusCode !in 200..299) {
                throw IOException(
                    "Nobitex candle request failed: HTTP $statusCode; " +
                        "content-type=$contentType; " +
                        "body=${body.take(300)}"
                )
            }

            val root = try {
                JSONObject(body)
            } catch (e: Exception) {
                throw IOException(
                    "Nobitex returned invalid candle JSON; " +
                        "content-type=$contentType; " +
                        "body=${body.take(300)}",
                    e
                )
            }

            when (root.optString("s")) {
                "no_data" -> return@withContext emptyList<Candle>()

                "error" -> throw IOException(
                    "Nobitex candle API error: " +
                        root.optString(
                            "errmsg",
                            "No error message supplied"
                        )
                )

                "ok" -> Unit

                else -> throw IOException(
                    "Unexpected Nobitex candle response status: " +
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
                throw IOException(
                    "Candle response arrays have different lengths"
                )
            }

            val candles = ArrayList<Candle>(count)

            for (index in 0 until count) {
                val timestampSeconds = try {
                    times.getLong(index)
                } catch (e: Exception) {
                    throw IOException(
                        "Invalid candle timestamp at index $index",
                        e
                    )
                }

                if (
                    timestampSeconds <= 0L ||
                    timestampSeconds > Long.MAX_VALUE / 1000L
                ) {
                    throw IOException(
                        "Invalid candle timestamp at index $index"
                    )
                }

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

                candles.add(candle)
            }

            candles.sortedBy { it.timestampMillis }
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(
                "Unable to fetch Nobitex candle history: " +
                    (e.message ?: e.javaClass.simpleName),
                e
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun requiredArray(
        root: JSONObject,
        key: String
    ): JSONArray {
        return root.optJSONArray(key)
            ?: throw IOException(
                "Candle response is missing array '$key'"
            )
    }

    private fun numberAt(
        array: JSONArray,
        index: Int,
        field: String
    ): Double {
        val value = try {
            array.getDouble(index)
        } catch (e: Exception) {
            throw IOException(
                "Candle field '$field' is not numeric at index $index",
                e
            )
        }

        if (!value.isFinite()) {
            throw IOException(
                "Candle field '$field' is not finite at index $index"
            )
        }

        return value
    }
}
