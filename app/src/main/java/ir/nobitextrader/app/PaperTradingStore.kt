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
    val realizedPnl: Double = 0.0,
    val feeAmount: Double = 0.0
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
    private const val INITIAL_CASH = 1_000_000_000.0

    fun load(context: Context): PaperPortfolio {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val positionsJson = JSONArray(prefs.getString("positions", "[]"))
        val tradesJson = JSONArray(prefs.getString("trades", "[]"))

        val positions = buildList {
            for (index in 0 until positionsJson.length()) {
                val item = positionsJson.getJSONObject(index)
                add(PaperPosition(
                    item.getString("symbol"),
                    item.getDouble("quantity"),
                    item.getDouble("averageEntry")
                ))
            }
        }

        val trades = buildList {
            for (index in 0 until tradesJson.length()) {
                val item = tradesJson.getJSONObject(index)
                add(PaperTrade(
                    id = item.getString("id"),
                    side = item.getString("side"),
                    symbol = item.getString("symbol"),
                    quantity = item.getDouble("quantity"),
                    price = item.getDouble("price"),
                    total = item.getDouble("total"),
                    timestamp = item.getLong("timestamp"),
                    realizedPnl = item.optDouble("realizedPnl", 0.0),
                    feeAmount = item.optDouble("feeAmount", 0.0)
                ))
            }
        }

        // Legacy sells have no recoverable fee or historical net P/L.
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
        price: Double,
        feeRate: Double
    ): PaperPortfolio {
        require(side == "BUY" || side == "SELL") { "Invalid side" }
        require(symbol.isNotBlank() && quantity.isFinite() && quantity > 0.0) {
            "Enter a valid quantity"
        }
        require(price.isFinite() && price > 0.0) { "No valid market price is available" }
        require(feeRate.isFinite() && feeRate in 0.0..0.02) { "Fee rate must be between 0% and 2%" }

        val old = load(context)
        val value = quantity * price
        require(value.isFinite() && value > 0.0) { "Invalid order value" }
        val fee = value * feeRate
        require(fee.isFinite()) { "Invalid fee amount" }

        val calculation = PaperTradingMath.calculate(
            side = side,
            oldCash = old.cash,
            oldQuantity = old.quantity(symbol),
            oldAverageEntry = old.averageEntry(symbol),
            quantity = quantity,
            price = price,
            feeRate = feeRate
        )
        val cash = calculation.cash
        val newQuantity = calculation.quantity
        val newAverage = calculation.averageEntry
        val tradeRealizedPnl = calculation.realizedPnl
        require(cash.isFinite() && cash >= -1e-8) { "Invalid virtual cash balance" }

        val positions = old.positions.filterNot { it.symbol == symbol }.toMutableList()
        if (newQuantity > 1e-12) positions += PaperPosition(symbol, newQuantity, newAverage)

        val trade = PaperTrade(
            id = UUID.randomUUID().toString(),
            side = side,
            symbol = symbol,
            quantity = quantity,
            price = price,
            total = value,
            timestamp = System.currentTimeMillis(),
            realizedPnl = tradeRealizedPnl,
            feeAmount = fee
        )
        val trades = (listOf(trade) + old.trades).take(2000)
        val cumulativeRealizedPnl = old.realizedPnl + tradeRealizedPnl

        val positionsJson = JSONArray().apply {
            positions.forEach {
                put(JSONObject()
                    .put("symbol", it.symbol)
                    .put("quantity", it.quantity)
                    .put("averageEntry", it.averageEntry))
            }
        }
        val tradesJson = JSONArray().apply {
            trades.forEach {
                put(JSONObject()
                    .put("id", it.id)
                    .put("side", it.side)
                    .put("symbol", it.symbol)
                    .put("quantity", it.quantity)
                    .put("price", it.price)
                    .put("total", it.total)
                    .put("timestamp", it.timestamp)
                    .put("realizedPnl", it.realizedPnl)
                    .put("feeAmount", it.feeAmount))
            }
        }

        check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("cash", cash.toString())
            .putString("positions", positionsJson.toString())
            .putString("trades", tradesJson.toString())
            .putString("realizedPnl", cumulativeRealizedPnl.toString())
            .commit()) { "Could not save virtual transaction" }

        return PaperPortfolio(cash, positions, trades, cumulativeRealizedPnl)
    }
}
