
package ir.nobitextrader.app.data

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
            return ask - bid
        }

    val spreadPercent: Double?
        get() {
            val bid = bestBid ?: return null
            val ask = bestAsk ?: return null
            val mid = (bid + ask) / 2.0
            if (mid <= 0.0) return null
            return ((ask - bid) / mid) * 100.0
        }
}

object NobitexOrderBookApi {

    private const val BASE_URL =
        "https://apiv2.nobitex.ir/v3/orderbook/"

    @Throws(IOException::class)
    fun fetch(
        symbol: String = "BTCIRT"
    ): OrderBookSnapshot {
        val normalizedSymbol = normalizeSymbol(symbol)

        val root = getJson("$BASE_URL$normalizedSymbol")

        return parseSnapshot(normalizedSymbol, root)
    }

    /**
     * Fetches all market order books through one public API request.
     * No prices or market symbols are generated locally.
     */
    @Throws(IOException::class)
    fun fetchAll(): List<OrderBookSnapshot> {
        val root = getJson("${BASE_URL}all")

        val markets = mutableListOf<OrderBookSnapshot>()
        val keys = root.keys()

        while (keys.hasNext()) {
            val symbol = keys.next()

            if (!symbol.matches(Regex("[A-Z0-9]{3,20}"))) {
                continue
            }

            val market = root.optJSONObject(symbol) ?: continue

            try {
                markets.add(parseSnapshot(symbol, market))
            } catch (_: IOException) {
                // Skip malformed market data; do not fabricate a result.
            }
        }

        if (markets.isEmpty()) {
            throw IOException(
                "Nobitex returned no valid market order books"
            )
        }

        return markets
    }

    private fun getJson(url: String): JSONObject {
        val connection =
            URL(url).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val statusCode = connection.responseCode

            val stream =
                if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                } ?: throw IOException(
                    "Nobitex HTTP error: $statusCode"
                )

            val response = stream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            if (statusCode !in 200..299) {
                throw IOException(
                    "Nobitex HTTP $statusCode: $response"
                )
            }

            val root = JSONObject(response)

            if (root.optString("status") != "ok") {
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

        require(
            normalized.matches(Regex("[A-Z0-9]{3,20}"))
        ) {
            "Invalid market symbol"
        }

        return normalized
    }

    private fun parseSnapshot(
        symbol: String,
        root: JSONObject
    ): OrderBookSnapshot {
        val bids = parseLevels(root.optJSONArray("bids"), "bids")
        val asks = parseLevels(root.optJSONArray("asks"), "asks")

        val lastPrice = root.optString(
            "lastTradePrice",
            ""
        ).toDoubleOrNull() ?: 0.0

        val lastUpdate = root.optLong("lastUpdate", 0L)

        return OrderBookSnapshot(
            symbol = symbol,
            lastTradePrice = lastPrice,
            lastUpdate = lastUpdate,
            bids = bids,
            asks = asks
        )
    }

    private fun parseLevels(
        array: JSONArray?,
        fieldName: String
    ): List<OrderBookLevel> {
        if (array == null) {
            throw IOException(
                "Missing order book field: $fieldName"
            )
        }

        val levels = mutableListOf<OrderBookLevel>()

        for (index in 0 until array.length()) {
            val item = array.optJSONArray(index) ?: continue
            if (item.length() < 2) continue

            val price = item.optString(0)
                .toDoubleOrNull() ?: continue

            val amount = item.optString(1)
                .toDoubleOrNull() ?: continue

            if (
                price.isFinite() &&
                amount.isFinite() &&
                price > 0.0 &&
                amount >= 0.0
            ) {
                levels.add(
                    OrderBookLevel(
                        price = price,
                        amount = amount
                    )
                )
            }
        }

        return levels
    }
}
