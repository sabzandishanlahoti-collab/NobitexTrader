package ir.nobitextrader.app.paper

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists the simulated wallet locally; it has no exchange credentials or network writes. */
object PaperAccountStore {
    private const val PREFS = "paper_trading_wallet"
    private const val KEY_ACCOUNT = "account_v1"

    fun load(context: Context): PaperAccount {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACCOUNT, null) ?: return PaperAccount.create()
        return try {
            val root = JSONObject(raw)
            val positionsJson = root.optJSONArray("positions") ?: JSONArray()
            val positions = linkedMapOf<String, PaperPosition>()
            for (i in 0 until positionsJson.length()) {
                val item = positionsJson.getJSONObject(i)
                val symbol = item.getString("symbol")
                positions[symbol] = PaperPosition(
                    symbol = symbol,
                    quantity = item.getDouble("quantity"),
                    averageEntryPrice = item.getDouble("averageEntryPrice"),
                    costBasis = item.getDouble("costBasis")
                )
            }
            val tradesJson = root.optJSONArray("trades") ?: JSONArray()
            val trades = ArrayList<PaperTrade>(tradesJson.length())
            for (i in 0 until tradesJson.length()) {
                val item = tradesJson.getJSONObject(i)
                trades += PaperTrade(
                    id = item.getLong("id"),
                    symbol = item.getString("symbol"),
                    side = PaperSide.valueOf(item.getString("side")),
                    quantity = item.getDouble("quantity"),
                    price = item.getDouble("price"),
                    fee = item.getDouble("fee"),
                    realizedPnl = item.getDouble("realizedPnl"),
                    timestampMillis = item.getLong("timestampMillis")
                )
            }
            PaperAccount(
                initialCash = root.getDouble("initialCash"),
                cash = root.getDouble("cash"),
                realizedPnl = root.getDouble("realizedPnl"),
                positions = positions,
                trades = trades
            )
        } catch (_: Exception) {
            // Do not overwrite a malformed saved wallet during load.
            PaperAccount.create()
        }
    }

    fun save(context: Context, account: PaperAccount) {
        val root = JSONObject()
            .put("initialCash", account.initialCash)
            .put("cash", account.cash)
            .put("realizedPnl", account.realizedPnl)
        val positions = JSONArray()
        account.positions.values.forEach { position ->
            positions.put(JSONObject()
                .put("symbol", position.symbol)
                .put("quantity", position.quantity)
                .put("averageEntryPrice", position.averageEntryPrice)
                .put("costBasis", position.costBasis))
        }
        val trades = JSONArray()
        account.trades.forEach { trade ->
            trades.put(JSONObject()
                .put("id", trade.id)
                .put("symbol", trade.symbol)
                .put("side", trade.side.name)
                .put("quantity", trade.quantity)
                .put("price", trade.price)
                .put("fee", trade.fee)
                .put("realizedPnl", trade.realizedPnl)
                .put("timestampMillis", trade.timestampMillis))
        }
        root.put("positions", positions).put("trades", trades)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACCOUNT, root.toString()).apply()
    }

    fun reset(context: Context, initialCash: Double = PaperAccount.DEFAULT_INITIAL_CASH) {
        save(context, PaperAccount.create(initialCash))
    }
}
