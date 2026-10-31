package ir.nobitextrader.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal data class PaperPosition(val symbol:String,val quantity:Double,val averageEntry:Double)
internal data class PaperTrade(val id:String,val side:String,val symbol:String,val quantity:Double,val price:Double,val total:Double,val timestamp:Long)
internal data class PaperPortfolio(val cash:Double,val positions:List<PaperPosition>,val trades:List<PaperTrade>) {
    fun quantity(symbol:String)=positions.firstOrNull{it.symbol==symbol}?.quantity?:0.0
    fun averageEntry(symbol:String)=positions.firstOrNull{it.symbol==symbol}?.averageEntry?:0.0
}
internal object PaperTradingStore {
    private const val PREFS="afa_paper_wallet_v1"
    private const val INITIAL_CASH=1_000_000_000.0 // virtual IRR only
    fun load(context:Context):PaperPortfolio {
        val p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        val pj=JSONArray(p.getString("positions","[]"))
        val tj=JSONArray(p.getString("trades","[]"))
        val positions=buildList { for(i in 0 until pj.length()){val x=pj.getJSONObject(i);add(PaperPosition(x.getString("symbol"),x.getDouble("quantity"),x.getDouble("averageEntry")))} }
        val trades=buildList { for(i in 0 until tj.length()){val x=tj.getJSONObject(i);add(PaperTrade(x.getString("id"),x.getString("side"),x.getString("symbol"),x.getDouble("quantity"),x.getDouble("price"),x.getDouble("total"),x.getLong("timestamp")))} }
        return PaperPortfolio((p.getString("cash",INITIAL_CASH.toString()) ?: INITIAL_CASH.toString()).toDouble(),positions,trades)
    }
    @Synchronized fun execute(context:Context,side:String,symbol:String,quantity:Double,price:Double):PaperPortfolio {
        require(side=="BUY"||side=="SELL") { "Invalid side" }
        require(symbol.isNotBlank()&&quantity.isFinite()&&quantity>0.0) { "Enter a valid quantity" }
        require(price.isFinite()&&price>0.0) { "No valid market price is available" }
        val old=load(context);val value=quantity*price
        require(value.isFinite()&&value>0.0) { "Invalid order value" }
        val oldQty=old.quantity(symbol);val oldAvg=old.averageEntry(symbol)
        val cash:Double;val qty:Double;val avg:Double
        if(side=="BUY"){
            require(value<=old.cash) { "Insufficient virtual cash" }
            cash=old.cash-value;qty=oldQty+quantity;avg=((oldQty*oldAvg)+value)/qty
        } else {
            require(quantity<=oldQty+1e-12) { "Insufficient virtual position" }
            cash=old.cash+value;qty=(oldQty-quantity).coerceAtLeast(0.0);avg=if(qty<=1e-12)0.0 else oldAvg
        }
        val positions=old.positions.filterNot{it.symbol==symbol}.toMutableList()
        if(qty>1e-12)positions+=PaperPosition(symbol,qty,avg)
        val trade=PaperTrade(UUID.randomUUID().toString(),side,symbol,quantity,price,value,System.currentTimeMillis())
        val trades=(listOf(trade)+old.trades).take(2000)
        val pj=JSONArray().apply{positions.forEach{put(JSONObject().put("symbol",it.symbol).put("quantity",it.quantity).put("averageEntry",it.averageEntry))}}
        val tj=JSONArray().apply{trades.forEach{put(JSONObject().put("id",it.id).put("side",it.side).put("symbol",it.symbol).put("quantity",it.quantity).put("price",it.price).put("total",it.total).put("timestamp",it.timestamp))}}
        check(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("cash",cash.toString()).putString("positions",pj.toString()).putString("trades",tj.toString()).commit()) { "Could not save virtual transaction" }
        return PaperPortfolio(cash,positions,trades)
    }
}
