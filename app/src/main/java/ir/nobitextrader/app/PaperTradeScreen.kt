package ir.nobitextrader.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.data.MarketStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PaperTradeScreen(english:Boolean,market:MarketStats?) {
    val context= LocalContext.current
    var portfolio by remember { mutableStateOf(PaperTradingStore.load(context)) }
    var quantityText by remember { mutableStateOf("0.001") }
    var message by remember { mutableStateOf<String?>(null) }
    val price=market?.latest
    val btcQty=portfolio.quantity("BTCIRT")
    val unrealized=if(price!=null)(price-portfolio.averageEntry("BTCIRT"))*btcQty else null
    Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("MANUAL PAPER TRADE",color=MaterialTheme.colorScheme.primary,fontSize=18.sp)
        Text(if(english)"VIRTUAL WALLET • PUBLIC NOBITEX PRICE • NO REAL ORDERS" else "کیف پول مجازی • قیمت عمومی نوبیتکس • بدون سفارش واقعی",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp)
        Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(english)"VIRTUAL CASH (IRR)" else "موجودی مجازی (ریال)",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp)
                Text(String.format(Locale.US,"%,.0f",portfolio.cash),color=MaterialTheme.colorScheme.primary,fontSize=22.sp)
                Text((if(english)"BTC quantity: " else "مقدار بیت‌کوین: ")+String.format(Locale.US,"%.8f",btcQty),color=MaterialTheme.colorScheme.onSurface)
                Text((if(english)"Unrealized P/L: " else "سود/زیان تحقق‌نیافته: ")+(unrealized?.let{String.format(Locale.US,"%,.0f IRR",it)}?: "—"),color=when{unrealized==null->MaterialTheme.colorScheme.onSurfaceVariant;unrealized>=0->AfaColorSchemes.profit;else->AfaColorSchemes.loss})
                Text((if(english)"Market price: " else "قیمت بازار: ")+(price?.let{String.format(Locale.US,"%,.0f IRR",it)}?:"Unavailable"),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)
            }
        }
        OutlinedTextField(value=quantityText,onValueChange={quantityText=it;message=null},modifier=Modifier.fillMaxWidth(),label={Text(if(english)"BTC quantity" else "مقدار BTC")},singleLine=true)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onClick={
                try {
                    val p=price?:error("No current public price")
                    portfolio=PaperTradingStore.execute(context,"BUY","BTCIRT",quantityText.trim().toDouble(),p)
                    message=if(english)"Virtual buy recorded." else "خرید مجازی ثبت شد."
                } catch(e:Exception){message=e.message?: "Trade rejected"}
            },modifier=Modifier.weight(1f),enabled=price!=null){Text(if(english)"BUY • PAPER" else "خرید مجازی")}
            OutlinedButton(onClick={
                try {
                    val p=price?:error("No current public price")
                    portfolio=PaperTradingStore.execute(context,"SELL","BTCIRT",quantityText.trim().toDouble(),p)
                    message=if(english)"Virtual sell recorded." else "فروش مجازی ثبت شد."
                } catch(e:Exception){message=e.message?: "Trade rejected"}
            },modifier=Modifier.weight(1f),enabled=price!=null){Text(if(english)"SELL • PAPER" else "فروش مجازی")}
        }
        message?.let{Text(it,color=if(it.contains("recorded")||it.contains("ثبت"))AfaColorSchemes.profit else AfaColorSchemes.loss,fontSize=11.sp)}
        Text(if(english)"RECENT PAPER TRANSACTIONS" else "آخرین معاملات مجازی",color=MaterialTheme.colorScheme.primary,fontSize=12.sp)
        if(portfolio.trades.isEmpty()) Text(if(english)"No virtual transactions yet." else "هنوز معامله مجازی ثبت نشده است.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            items(portfolio.trades.take(30),key={it.id}) { trade ->
                Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                        Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                            Text(trade.side,color=if(trade.side=="BUY")AfaColorSchemes.profit else AfaColorSchemes.loss,fontSize=11.sp)
                            Text(String.format(Locale.US,"%.8f BTC",trade.quantity),color=MaterialTheme.colorScheme.onSurface,fontSize=10.sp)
                            Text(SimpleDateFormat("MM-dd HH:mm:ss",Locale.getDefault()).format(Date(trade.timestamp)),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp)
                        }
                        Text(String.format(Locale.US,"%,.0f IRR",trade.total),color=MaterialTheme.colorScheme.onSurface,fontSize=11.sp)
                    }
                }
            }
        }
        Text(if(english)"Paper ledger only. Fee model is not configured yet; displayed P/L excludes fees." else "فقط دفتر مجازی. کارمزد هنوز تنظیم نشده و سودوزیان نمایش‌داده‌شده بدون کارمزد است.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=9.sp)
    }
}
