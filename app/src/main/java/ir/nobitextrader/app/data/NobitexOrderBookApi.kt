package ir.nobitextrader.app.data

import ir.nobitextrader.app.scanner.MarketRefreshPolicy
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class OrderBookLevel(
    val price: Double,
    val amount: Double
)

data class OrderBookSnapshot(
    val symbol: String,
    val lastTradePrice: Double,
    val lastUpdate: Long,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>
) {
    val bestBid: Double?
        get() = bids.firstOrNull()?.price

    val bestAsk: Double?
        get() = asks.firstOrNull()?.price

    val spread: Double?
        get() {
            val bid = bestBid ?: return null
            val ask = bestAsk ?: return null
            if (!bid.isFinite() || !ask.isFinite()) return null
            if (bid <= 0.0 || ask < bid) return null
            return ask - bid
        }

    val spreadPercent: Double?
        get() {
            val bid = bestBid ?: return null
            val ask = bestAsk ?: return null

            if (!bid.isFinite() || !ask.isFinite()) return null
            if (bid <= 0.0 || ask < bid) return null

            val mid = bid / 2.0 + ask / 2.0
            if (!mid.isFinite() || mid <= 0.0) return null

            val result = ((ask - bid) / mid) * 100.0
            return result.takeIf { it.isFinite() && it >= 0.0 }
        }
}

object NobitexOrderBookApi {

    private const val BASE_URL =
        "https://apiv2.nobitex.ir/v3/orderbook/"

    private val symbolPattern = Regex("[A-Z0-9]{3,20}")

    @Throws(IOException::class)
    fun fetch(
        symbol: String = "BTCIRT",
        timeoutMillis: Long =
            MarketRefreshPolicy.DEFAULT.requestTimeoutMillis
    ): OrderBookSnapshot {
        val normalizedSymbol = normalizeSymbol(symbol)
        val root = getJson(
            url = "$BASE_URL$normalizedSymbol",
            timeoutMillis = timeoutMillis
        )
        return parseSnapshot(normalizedSymbol, root)
    }

    @Throws(IOException::class)
    fun fetchAll(
        timeoutMillis: Long =
            MarketRefreshPolicy.DEFAULT.requestTimeoutMillis,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): List<OrderBookSnapshot> {
        val root = getJson(
            url = "${BASE_URL}all",
            timeoutMillis = timeoutMillis
        )

        val validEntries = mutableListOf<Pair<String, JSONObject>>()
        val keys = root.keys()

        while (keys.hasNext()) {
            val symbol = keys.next()
            if (!symbolPattern.matches(symbol)) continue

            val market = root.optJSONObject(symbol) ?: continue
            validEntries.add(symbol to market)
        }

        val total = validEntries.size
        val markets = mutableListOf<OrderBookSnapshot>()

        onProgress?.invoke(0, total)

        validEntries.forEachIndexed { index, (symbol, market) ->
            try {
                markets.add(parseSnapshot(symbol, market))
            } catch (_: IOException) {
                // Ignore malformed market entries.
            } finally {
                onProgress?.invoke(index + 1, total)
            }
        }

        if (markets.isEmpty()) {
            throw IOException(
                "Nobitex returned no valid market order books"
            )
        }

        return markets.distinctBy { it.symbol }
    }

    private fun getJson(
        url: String,
        timeoutMillis: Long
    ): JSONObject {
        if (timeoutMillis !in 1_000L..60_000L) {
            throw IllegalArgumentException(
                "timeoutMillis must be between 1 and 60 seconds"
            )
        }

        val timeout = timeoutMillis.toInt()
        val connection =
            URL(url).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val statusCode = connection.responseCode

            if (statusCode !in 200..299) {
                val errorBody = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()

                throw IOException(
                    "Nobitex HTTP $statusCode: $errorBody"
                )
            }

            val response = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val root = try {
                JSONObject(response)
            } catch (e: Exception) {
                throw IOException(
                    "Nobitex returned invalid JSON",
                    e
                )
            }

            if (root.optString("status", "") != "ok") {
                throw IOException(
                    "Nobitex returned an unsuccessful status"
                )
            }

            return root
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeSymbol(symbol: String): String {
        val normalized = symbol.trim().uppercase()

        if (!symbolPattern.matches(normalized)) {
            throw IllegalArgumentException("Invalid market symbol")
        }

        return normalized
    }

    private fun parseSnapshot(
        symbol: String,
        root: JSONObject
    ): OrderBookSnapshot {
        val bids = parseLevels(
            root.optJSONArray("bids"),
            "bids"
        ).sortedByDescending { it.price }

        val asks = parseLevels(
            root.optJSONArray("asks"),
            "asks"
        ).sortedBy { it.price }

        if (bids.isEmpty() || asks.isEmpty()) {
            throw IOException(
                "Empty or invalid order book for $symbol"
            )
        }

        if (bids.first().price > asks.first().price) {
            throw IOException("Crossed order book for $symbol")
        }

        val lastPrice = readRequiredNumber(root, "lastTradePrice")

        if (lastPrice <= 0.0) {
            throw IOException("Invalid last trade price for $symbol")
        }

        val lastUpdate = root.optLong("lastUpdate", 0L)
        if (lastUpdate < 0L) {
            throw IOException(
                "Invalid last update timestamp for $symbol"
            )
        }

        return OrderBookSnapshot(
            symbol = symbol,
            lastTradePrice = lastPrice,
            lastUpdate = lastUpdate,
            bids = bids,
            asks = asks
        )
    }

    private fun readRequiredNumber(
        json: JSONObject,
        key: String
    ): Double {
        if (!json.has(key) || json.isNull(key)) {
            throw IOException("Missing order book field: $key")
        }

        val value = when (val raw = json.get(key)) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
                ?: throw IOException("Invalid numeric field: $key")
            else -> throw IOException("Invalid numeric field: $key")
        }

        if (!value.isFinite()) {
            throw IOException("Non-finite numeric field: $key")
        }

        return value
    }

    private fun parseLevels(
        array: JSONArray?,
        fieldName: String
    ): List<OrderBookLevel> {
        if (array == null) {
            throw IOException("Missing order book field: $fieldName")
        }

        val levels = mutableListOf<OrderBookLevel>()

        for (index in 0 until array.length()) {
            val item = array.optJSONArray(index) ?: continue
            if (item.length() < 2) continue

            val price = readLevelNumber(item, 0) ?: continue
            val amount = readLevelNumber(item, 1) ?: continue

            if (
                price.isFinite() &&
                amount.isFinite() &&
                price > 0.0 &&
                amount > 0.0
            ) {
                levels.add(OrderBookLevel(price, amount))
            }
        }

        return levels
    }

    private fun readLevelNumber(
        item: JSONArray,
        index: Int
    ): Double? {
        val raw = item.opt(index)

        val value = when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull()
            else -> null
        } ?: return null

        return value.takeIf { it.isFinite() }
    }
}
