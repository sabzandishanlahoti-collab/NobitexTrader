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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.data.MarketStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PAPER_PRICE_MAX_AGE_MS = 45_000L

@Composable
internal fun PaperTradeScreen(
    english: Boolean,
    market: MarketStats?,
    marketUpdatedAt: Long?,
    feeRatePercent: Double
) {
    val context = LocalContext.current
    var portfolio by remember { mutableStateOf(PaperTradingStore.load(context)) }
    var quantityText by remember { mutableStateOf("0.001") }
    var message by remember { mutableStateOf<String?>(null) }
    var autoRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(5_000L)
        }
    }

    val price = market?.latest?.takeIf { it.isFinite() && it > 0.0 }
    val priceAgeMs = marketUpdatedAt?.let { (nowMillis - it).coerceAtLeast(0L) }
    val priceIsFresh = price != null && priceAgeMs != null &&
        priceAgeMs <= PAPER_PRICE_MAX_AGE_MS
    val feeIsConfigured = feeRatePercent.isFinite() && feeRatePercent > 0.0 && feeRatePercent <= 2.0
    val paperTradeEnabled = priceIsFresh && feeIsConfigured
    val btcQty = portfolio.quantity("BTCIRT")
    val positionMarketValue = if (priceIsFresh) price!! * btcQty else null
    val totalEquity = positionMarketValue?.let { portfolio.cash + it }
    val unrealized = if (priceIsFresh && feeIsConfigured) {
        val gross = (price!! - portfolio.averageEntry("BTCIRT")) * btcQty
        val estimatedExitFee = price * btcQty * feeRatePercent / 100.0
        gross - estimatedExitFee
    } else null
    val priceText = when {
        price == null -> if (english) "Unavailable" else "در دسترس نیست"
        !priceIsFresh -> if (english) "STALE — trading disabled" else "کهنه — معامله غیرفعال"
        else -> String.format(Locale.US, "%,.0f IRR", price)
    }

    Column(
        Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("MANUAL PAPER TRADE", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
        Text(
            if (english) "VIRTUAL WALLET • PUBLIC NOBITEX PRICE • NO REAL ORDERS"
            else "کیف پول مجازی • قیمت عمومی نوبیتکس • بدون سفارش واقعی",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (english) "VIRTUAL CASH (IRR)" else "موجودی مجازی (ریال)",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(String.format(Locale.US, "%,.0f", portfolio.cash),
                    color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
                Text(
                    (if (english) "BTC quantity: " else "مقدار بیت‌کوین: ") +
                        String.format(Locale.US, "%.8f", btcQty),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    (if (english) "POSITION VALUE: " else "ارزش دارایی در گردش: ") +
                        (positionMarketValue?.let { String.format(Locale.US, "%,.0f IRR", it) } ?: "—"),
                    color = when {
                        unrealized == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        unrealized >= 0.0 -> AfaColorSchemes.profit
                        else -> AfaColorSchemes.loss
                    },
                    fontSize = 12.sp
                )
                if (portfolio.positions.isNotEmpty()) {
                    Text(
                        if (english) "OPEN POSITIONS" else "نمادهای خریداری‌شده و وضعیت آن‌ها",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                    portfolio.positions.forEach { position ->
                        val currentPrice = if (position.symbol == "BTCIRT" && priceIsFresh) price else null
                        val positionPnl = currentPrice?.let {
                            (it - position.averageEntry) * position.quantity -
                                (it * position.quantity * feeRatePercent / 100.0)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                position.symbol,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                when {
                                    positionPnl == null -> if (english) "PRICE UNAVAILABLE" else "قیمت در دسترس نیست"
                                    positionPnl > 0.0 -> "↑ " + (if (english) "UP" else "صعودی")
                                    positionPnl < 0.0 -> "↓ " + (if (english) "DOWN" else "نزولی")
                                    else -> if (english) "FLAT" else "بدون تغییر"
                                },
                                color = when {
                                    positionPnl == null || positionPnl == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                    positionPnl > 0.0 -> AfaColorSchemes.profit
                                    else -> AfaColorSchemes.loss
                                },
                                fontSize = 11.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                        Text(
                            (if (english) "Qty " else "مقدار ") +
                                String.format(Locale.US, "%.8f", position.quantity) +
                                (positionPnl?.let { " • " + String.format(Locale.US, "%+,.0f IRR", it) } ?: ""),
                            color = positionPnl?.let {
                                if (it >= 0.0) AfaColorSchemes.profit else AfaColorSchemes.loss
                            } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
                Text(
                    (if (english) "TOTAL VIRTUAL EQUITY: " else "کل سرمایه مجازی: ") +
                        (totalEquity?.let { String.format(Locale.US, "%,.0f IRR", it) } ?: "—"),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
                Text(
                    (if (english) "TOTAL NET P/L: " else "سود/زیان خالص کل: ") +
                        (if (unrealized != null) String.format(
                            Locale.US, "%+,.0f IRR", portfolio.realizedPnl + unrealized
                        ) else "—"),
                    color = when {
                        unrealized == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        portfolio.realizedPnl + unrealized >= 0.0 -> AfaColorSchemes.profit
                        else -> AfaColorSchemes.loss
                    },
                    fontSize = 13.sp
                )
                Text(
                    (if (english) "Unrealized P/L: " else "سود/زیان تحقق‌نیافته: ") +
                        (unrealized?.let { String.format(Locale.US, "%,.0f IRR", it) } ?: "—"),
                    color = when {
                        unrealized == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        unrealized >= 0 -> AfaColorSchemes.profit
                        else -> AfaColorSchemes.loss
                    }
                )
                Text(
                    (if (english) "Market price: " else "قیمت بازار: ") + priceText,
                    color = if (priceIsFresh) MaterialTheme.colorScheme.onSurfaceVariant
                    else AfaColorSchemes.loss,
                    fontSize = 11.sp
                )
                if (priceIsFresh && priceAgeMs != null) {
                    Text(
                        (if (english) "Updated " else "به‌روزرسانی ") +
                            "${priceAgeMs / 1_000L}s " +
                            (if (english) "ago" else "پیش"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }
        }

        if (!feeIsConfigured) {
            Text(
                if (english) "FEE NOT CONFIGURED • SET YOUR ACCOUNT RATE IN SETTINGS" else "کارمزد تنظیم نشده • نرخ حساب خودت را در تنظیمات وارد کن",
                color = AfaColorSchemes.loss,
                fontSize = 11.sp
            )
        }

        if (!priceIsFresh) {
            Text(
                if (english) "MARKET PRICE STALE • BUY/SELL LOCKED • WAIT FOR A SUCCESSFUL REFRESH"
                else "قیمت بازار تازه نیست • خرید و فروش قفل است • منتظر دریافت موفق قیمت جدید بمانید",
                color = AfaColorSchemes.loss,
                fontSize = 11.sp
            )
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = { quantityText = it; message = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (english) "BTC quantity" else "مقدار BTC") },
            singleLine = true
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    try {
                        val p = price?.takeIf { priceIsFresh } ?: error("Market price is stale or unavailable")
                        portfolio = PaperTradingStore.execute(
                            context, "BUY", "BTCIRT", quantityText.trim().toDouble(), p, feeRatePercent / 100.0
                        )
                        message = if (english) "Virtual buy recorded." else "خرید مجازی ثبت شد."
                    } catch (e: Exception) {
                        message = e.message ?: "Trade rejected"
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = paperTradeEnabled
            ) { Text(if (english) "BUY • PAPER" else "خرید مجازی") }
            OutlinedButton(
                onClick = {
                    try {
                        val p = price?.takeIf { priceIsFresh } ?: error("Market price is stale or unavailable")
                        portfolio = PaperTradingStore.execute(
                            context, "SELL", "BTCIRT", quantityText.trim().toDouble(), p, feeRatePercent / 100.0
                        )
                        message = if (english) "Virtual sell recorded." else "فروش مجازی ثبت شد."
                    } catch (e: Exception) {
                        message = e.message ?: "Trade rejected"
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = paperTradeEnabled
            ) { Text(if (english) "SELL • PAPER" else "فروش مجازی") }
        }
        Button(
            onClick = {
                if (!autoRunning) {
                    autoRunning = true
                    message = if (english) "Running one BTC strategy cycle…" else "در حال اجرای یک چرخه تحلیل و معامله BTC…"
                    scope.launch {
                        try {
                            when (val result = PaperAutoTradingRunner.runBtcOnce(
                                context,
                                TraderSettingsStore.load(context)
                            )) {
                                is PaperAutoTradingRunner.Result.Executed -> {
                                    portfolio = result.portfolio
                                    message = if (english) {
                                        "AUTO PAPER ${result.side} recorded: ${String.format(Locale.US, "%.8f", result.quantity)} BTC at ${String.format(Locale.US, "%,.0f", result.price)} IRR."
                                    } else {
                                        "خریدوفروش خودکار مجازی ثبت شد: ${result.side}، مقدار ${String.format(Locale.US, "%.8f", result.quantity)} BTC با قیمت ${String.format(Locale.US, "%,.0f", result.price)} ریال."
                                    }
                                }
                                is PaperAutoTradingRunner.Result.Rejected -> {
                                    message = result.reason
                                }
                            }
                        } catch (e: Exception) {
                            message = e.message ?: if (english) "Automatic paper cycle failed." else "چرخه خودکار مجازی ناموفق بود."
                        } finally {
                            autoRunning = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = feeIsConfigured && !autoRunning
        ) {
            Text(
                when {
                    autoRunning && english -> "RUNNING BTC PAPER CYCLE…"
                    autoRunning -> "در حال اجرا…"
                    english -> "RUN ONE AUTO PAPER CYCLE • BTC"
                    else -> "اجرای یک چرخه خریدوفروش خودکار مجازی • BTC"
                }
            )
        }
        Text(
            if (english) {
                "One-shot test only • closed 15-minute candles + fresh BTC order book • max order 1% of cash, capped at 10,000,000 IRR • no real orders."
            } else {
                "فقط اجرای آزمایشی تک‌مرحله‌ای • کندل بسته‌شده ۱۵ دقیقه‌ای + دفتر سفارش تازه BTC • سقف سفارش ۱٪ موجودی و حداکثر ۱۰٬۰۰۰٬۰۰۰ ریال • بدون سفارش واقعی."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
        message?.let {
            Text(
                it,
                color = if (it.contains("recorded") || it.contains("ثبت"))
                    AfaColorSchemes.profit else AfaColorSchemes.loss,
                fontSize = 11.sp
            )
        }
        Text(if (english) "RECENT PAPER TRANSACTIONS" else "آخرین معاملات مجازی",
            color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        if (portfolio.trades.isEmpty()) {
            Text(if (english) "No virtual transactions yet." else "هنوز معامله مجازی ثبت نشده است.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(portfolio.trades.take(30), key = { it.id }) { trade ->
                Card(Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(trade.side,
                                color = if (trade.side == "BUY") AfaColorSchemes.profit else AfaColorSchemes.loss,
                                fontSize = 11.sp)
                            Text(String.format(Locale.US, "%.8f BTC", trade.quantity),
                                color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                            Text(SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                                .format(Date(trade.timestamp)),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        }
                        Text(String.format(Locale.US, "%,.0f IRR", trade.total),
                            color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                    }
                }
            }
        }
        Text(
            if (english) "Paper ledger only • fee ${String.format(Locale.US, "%.2f", feeRatePercent)}% per side • P/L includes recorded fees."
            else "دفتر مجازی • کارمزد هر سمت ${String.format(Locale.US, "%.2f", feeRatePercent)}٪ • سودوزیان شامل کارمزد ثبت‌شده است.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
    }
}
