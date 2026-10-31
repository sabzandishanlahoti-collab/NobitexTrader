package ir.nobitextrader.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class PaperPosition(
    val symbol: String,
    val quantity: Double,
    val averageEntry: Double
)

internal data class PaperTrade(
    val id: String,
    val side: String,
    val symbol: String,
    val quantity: Double,
    val price: Double,
    val total: Double,
    val timestamp: Long,
    val realizedPnl: Double = 0.0
)

internal data class PaperPortfolio(
    val cash: Double,
    val positions: List<PaperPosition>,
    val trades: List<PaperTrade>,
    val realizedPnl: Double
) {
    fun quantity(symbol: String): Double =
        positions.firstOrNull { it.symbol == symbol }?.quantity ?: 0.0

    fun averageEntry(symbol: String): Double =
        positions.firstOrNull { it.symbol == symbol }?.averageEntry ?: 0.0
}

internal object PaperTradingStore {
    private const val PREFS = "afa_paper_wallet_v1"
    private const val INITIAL_CASH = 1_000_000_000.0 // virtual IRR only

    fun load(context: Context): PaperPortfolio {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val positionsJson = JSONArray(prefs.getString("positions", "[]"))
        val tradesJson = JSONArray(prefs.getString("trades", "[]"))

        val positions = buildList {
            for (index in 0 until positionsJson.length()) {
                val item = positionsJson.getJSONObject(index)
                add(
                    PaperPosition(
                        item.getString("symbol"),
                        item.getDouble("quantity"),
                        item.getDouble("averageEntry")
                    )
                )
            }
        }

        val trades = buildList {
            for (index in 0 until tradesJson.length()) {
                val item = tradesJson.getJSONObject(index)
                add(
                    PaperTrade(
                        id = item.getString("id"),
                        side = item.getString("side"),
                        symbol = item.getString("symbol"),
                        quantity = item.getDouble("quantity"),
                        price = item.getDouble("price"),
                        total = item.getDouble("total"),
                        timestamp = item.getLong("timestamp"),
                        realizedPnl = item.optDouble("realizedPnl", 0.0)
                    )
                )
            }
        }

        // Older installations have no cumulative key. Their retained trade records
        // are the only recoverable source; older sells without realizedPnl count as 0.
        val realizedPnl = if (prefs.contains("realizedPnl")) {
            (prefs.getString("realizedPnl", "0") ?: "0").toDouble()
        } else {
            trades.sumOf { it.realizedPnl }
        }

        val cash = (prefs.getString("cash", INITIAL_CASH.toString())
            ?: INITIAL_CASH.toString()).toDouble()

        return PaperPortfolio(cash, positions, trades, realizedPnl)
    }

    @Synchronized
    fun execute(
        context: Context,
        side: String,
        symbol: String,
        quantity: Double,
        price: Double
    ): PaperPortfolio {
        require(side == "BUY" || side == "SELL") { "Invalid side" }
        require(symbol.isNotBlank() && quantity.isFinite() && quantity > 0.0) {
            "Enter a valid quantity"
        }
        require(price.isFinite() && price > 0.0) { "No valid market price is available" }

        val old = load(context)
        val value = quantity * price
        require(value.isFinite() && value > 0.0) { "Invalid order value" }

        val oldQuantity = old.quantity(symbol)
        val oldAverage = old.averageEntry(symbol)

        val cash: Double
        val newQuantity: Double
        val newAverage: Double
        val tradeRealizedPnl: Double

        if (side == "BUY") {
            require(value <= old.cash) { "Insufficient virtual cash" }
            cash = old.cash - value
            newQuantity = oldQuantity + quantity
            newAverage = ((oldQuantity * oldAverage) + value) / newQuantity
            tradeRealizedPnl = 0.0
        } else {
            require(quantity <= oldQuantity + 1e-12) {
                "Insufficient virtual position"
            }
            cash = old.cash + value
            newQuantity = (oldQuantity - quantity).coerceAtLeast(0.0)
            newAverage = if (newQuantity <= 1e-12) 0.0 else oldAverage
            tradeRealizedPnl = (price - oldAverage) * quantity
        }

        val positions = old.positions.filterNot { it.symbol == symbol }.toMutableList()
        if (newQuantity > 1e-12) {
            positions += PaperPosition(symbol, newQuantity, newAverage)
        }

        val trade = PaperTrade(
            id = UUID.randomUUID().toString(),
            side = side,
            symbol = symbol,
            quantity = quantity,
            price = price,
            total = value,
            timestamp = System.currentTimeMillis(),
            realizedPnl = tradeRealizedPnl
        )
        val trades = (listOf(trade) + old.trades).take(2000)
        val cumulativeRealizedPnl = old.realizedPnl + tradeRealizedPnl

        val positionsJson = JSONArray().apply {
            positions.forEach {
                put(
                    JSONObject()
                        .put("symbol", it.symbol)
                        .put("quantity", it.quantity)
                        .put("averageEntry", it.averageEntry)
                )
            }
        }
        val tradesJson = JSONArray().apply {
            trades.forEach {
                put(
                    JSONObject()
                        .put("id", it.id)
                        .put("side", it.side)
                        .put("symbol", it.symbol)
                        .put("quantity", it.quantity)
                        .put("price", it.price)
                        .put("total", it.total)
                        .put("timestamp", it.timestamp)
                        .put("realizedPnl", it.realizedPnl)
                )
            }
        }

        check(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("cash", cash.toString())
                .putString("positions", positionsJson.toString())
                .putString("trades", tradesJson.toString())
                .putString("realizedPnl", cumulativeRealizedPnl.toString())
                .commit()
        ) {
            "Could not save virtual transaction"
        }

        return PaperPortfolio(cash, positions, trades, cumulativeRealizedPnl)
    }
}
