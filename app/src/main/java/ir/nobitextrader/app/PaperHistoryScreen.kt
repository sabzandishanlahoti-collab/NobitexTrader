package ir.nobitextrader.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PaperHistoryScreen(english: Boolean) {
    val context = LocalContext.current
    val portfolio = remember { PaperTradingStore.load(context) }
    val sellTrades = portfolio.trades.filter { it.side == "SELL" }
    val winningSells = sellTrades.count { it.realizedPnl > 0.0 }
    val losingSells = sellTrades.count { it.realizedPnl < 0.0 }
    val winRate = if (sellTrades.isEmpty()) null else winningSells * 100.0 / sellTrades.size
    val pnlColor = if (portfolio.realizedPnl >= 0.0) AfaColorSchemes.profit else AfaColorSchemes.loss

    Column(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            if (english) "PAPER TRADE HISTORY" else "تاریخچه معاملات مجازی",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp
        )
        Text(
            if (english) "PERSISTED LOCAL LEDGER • VIRTUAL IRR" else "دفتر ذخیره‌شده محلی • ریال مجازی",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (english) "CUMULATIVE REALIZED P/L • NET OF RECORDED FEES" else "سود/زیان تحقق‌یافته کل • پس از کسر کارمزدهای ثبت‌شده",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    String.format(Locale.US, "%,.0f IRR", portfolio.realizedPnl),
                    color = pnlColor,
                    fontSize = 22.sp
                )
                Text(
                    if (english) "Current trades include configured fees; legacy trades may not." else "معاملات جدید شامل کارمزد تنظیم‌شده‌اند؛ معاملات قدیمی ممکن است بدون کارمزد باشند.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryStatCard(
                if (english) "ALL RECORDS" else "کل رکوردها",
                portfolio.trades.size.toString(),
                Modifier.weight(1f)
            )
            HistoryStatCard(
                if (english) "SELLS" else "فروش‌ها",
                sellTrades.size.toString(),
                Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HistoryStatCard(
                if (english) "WINS / LOSSES" else "سودده / زیان‌ده",
                "$winningSells / $losingSells",
                Modifier.weight(1f)
            )
            HistoryStatCard(
                if (english) "WIN RATE" else "درصد برد",
                winRate?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—",
                Modifier.weight(1f)
            )
        }

        Text(
            if (english) "LATEST TRANSACTIONS (UP TO 2,000)" else "آخرین معاملات (حداکثر ۲۰۰۰ رکورد)",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp
        )

        if (portfolio.trades.isEmpty()) {
            Text(
                if (english) "No paper trades recorded yet." else "هنوز معاملهٔ مجازی ثبت نشده است.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(portfolio.trades, key = { it.id }) { trade ->
                    val isSell = trade.side == "SELL"
                    val tradeColor = when {
                        !isSell -> MaterialTheme.colorScheme.onSurface
                        trade.realizedPnl > 0.0 -> AfaColorSchemes.profit
                        trade.realizedPnl < 0.0 -> AfaColorSchemes.loss
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "${trade.side} • ${trade.symbol}",
                                    color = if (isSell) AfaColorSchemes.loss else AfaColorSchemes.profit,
                                    fontSize = 11.sp
                                )
                                Text(
                                    String.format(Locale.US, "%.8f", trade.quantity),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp
                                )
                                Text(
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                        .format(Date(trade.timestamp)),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                                Text(
                                    String.format(Locale.US, "%,.0f IRR", trade.total),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp
                                )
                                Text(
                                    String.format(Locale.US, "Fee: %,.0f IRR", trade.feeAmount),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp
                                )
                                if (isSell) {
                                    Text(
                                        String.format(Locale.US, "%+,.0f IRR", trade.realizedPnl),
                                        color = tradeColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            if (english) "Win rate is calculated from recorded sell transactions only. Legacy trades recorded before fee tracking do not include historical fees; their old P/L is not retroactively adjusted. This is not a live-execution history." else "درصد برد فقط از فروش‌های ثبت‌شده محاسبه می‌شود. معاملات قدیمی پیش از ثبت کارمزد، بدون کسر کارمزد تاریخی باقی می‌مانند و به‌صورت عطف‌به‌ماسبق اصلاح نمی‌شوند. این تاریخچه مربوط به اجرای واقعی سفارش نیست.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun HistoryStatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp)
        }
    }
}
