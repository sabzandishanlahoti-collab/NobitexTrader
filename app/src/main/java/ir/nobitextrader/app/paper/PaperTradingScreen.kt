package ir.nobitextrader.app.paper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import ir.nobitextrader.app.data.NobitexOrderBookApi
import ir.nobitextrader.app.data.OrderBookSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PaperTradingScreen(
    modifier: Modifier = Modifier,
    english: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var account by remember { mutableStateOf(PaperAccountStore.load(context)) }
    var symbol by remember { mutableStateOf("BTCIRT") }
    var quoteAmount by remember { mutableStateOf("10000000") }
    var sellQuantity by remember { mutableStateOf("") }
    var orderBook by remember { mutableStateOf<OrderBookSnapshot?>(null) }
    var bidPrices by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun refreshMarket() {
        val requestedSymbol = symbol.trim().uppercase(Locale.ROOT)
        if (!requestedSymbol.matches(Regex("[A-Z0-9]{3,20}"))) {
            error = if (english) "Enter a valid market symbol" else "نماد بازار معتبر نیست"
            return
        }
        scope.launch {
            loading = true
            error = null
            info = null
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    val allBooks = if (account.positions.isNotEmpty()) {
                        runCatching { NobitexOrderBookApi.fetchAll() }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    val selected = allBooks.firstOrNull { it.symbol == requestedSymbol }
                        ?: NobitexOrderBookApi.fetch(requestedSymbol)
                    val prices = allBooks.mapNotNull { book ->
                        book.bestBid?.let { book.symbol to it }
                    }.toMap().toMutableMap()
                    selected.bestBid?.let { prices[selected.symbol] = it }
                    selected to prices.toMap()
                }
                val result = refreshed.first
                orderBook = result
                bidPrices = refreshed.second
                symbol = result.symbol
                info = if (english) "Live order book updated" else "دفتر سفارش زنده به‌روزرسانی شد"
            } catch (e: Exception) {
                error = e.message ?: if (english) "Market data unavailable" else "دریافت داده بازار ناموفق بود"
            } finally {
                loading = false
            }
        }
    }

    fun save(next: PaperAccount) {
        account = next
        PaperAccountStore.save(context, next)
    }

    val portfolio = remember(account, bidPrices) {
        PaperTradingEngine.value(account, bidPrices)
    }

    LaunchedEffect(Unit) { refreshMarket() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            if (english) "PAPER TRADING — VIRTUAL MONEY" else "معاملات آزمایشی — فقط پول مجازی",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            if (english) "No order is sent to Nobitex. Quotes are read-only live market data."
            else "هیچ سفارشی به نوبیتکس ارسال نمی‌شود؛ فقط قیمت زنده بازار خوانده می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (english) "Virtual wallet" else "کیف پول مجازی", style = MaterialTheme.typography.titleMedium)
                Text((if (english) "Available cash: " else "موجودی نقد: ") + money(account.cash))
                Text((if (english) "Open positions value: " else "ارزش دارایی باز: ") + money(portfolio.positionsValue))
                Text((if (english) "Total equity: " else "ارزش کل حساب: ") + money(portfolio.totalEquity))
                if (portfolio.unpricedPositionCount > 0) {
                    Text(
                        if (english) "Unpriced open positions: ${portfolio.unpricedPositionCount} — valuation is partial"
                        else "دارایی بدون قیمت زنده: ${portfolio.unpricedPositionCount} — ارزش کل فعلاً ناقص است",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text((if (english) "Realized P/L: " else "سود و زیان قطعی: ") + money(account.realizedPnl))
                Text((if (english) "Unrealized P/L (estimated): " else "سود و زیان شناور (تخمینی): ") + money(portfolio.unrealizedPnl))
                Text(
                    (if (english) "Starting balance: " else "سرمایه اولیه: ") + money(account.initialCash),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        OutlinedTextField(
            value = symbol,
            onValueChange = { symbol = it.uppercase(Locale.ROOT) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (english) "Nobitex symbol (e.g. BTCIRT)" else "نماد نوبیتکس (مثلاً BTCIRT)") },
            singleLine = true
        )
        Button(onClick = { refreshMarket() }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) {
                if (english) "Refreshing..." else "در حال دریافت..."
            } else {
                if (english) "Refresh live bid / ask" else "به‌روزرسانی قیمت خرید و فروش زنده"
            })
        }

        orderBook?.let { book ->
            Text(if (english) "Market: ${book.symbol}" else "بازار: ${book.symbol}")
            Text((if (english) "Best ask (virtual buy fill): " else "بهترین فروشنده (قیمت خرید مجازی): ") + money(book.bestAsk ?: 0.0))
            Text((if (english) "Best bid (virtual sell fill): " else "بهترین خریدار (قیمت فروش مجازی): ") + money(book.bestBid ?: 0.0))
            Text(
                (if (english) "Last update: " else "آخرین به‌روزرسانی: ") +
                    if (book.lastUpdate > 0L) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(book.lastUpdate)) else "—",
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = quoteAmount,
            onValueChange = { quoteAmount = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (english) "Buy amount (quote currency)" else "مبلغ خرید (ریال)") },
            singleLine = true
        )
        Button(
            onClick = {
                val book = orderBook
                val amount = quoteAmount.trim().replace(",", "").toDoubleOrNull()
                if (book?.bestAsk == null || amount == null) {
                    error = if (english) "Refresh the market and enter a valid buy amount" else "قیمت زنده بگیر و مبلغ خرید معتبر وارد کن"
                } else {
                    try {
                        save(PaperTradingEngine.buy(account, book.symbol, book.bestAsk!!, amount))
                        info = if (english) "Virtual buy filled at best ask" else "خرید مجازی با بهترین قیمت فروشنده ثبت شد"
                        error = null
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            enabled = !loading && orderBook?.bestAsk != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (english) "BUY — PAPER" else "خرید مجازی") }

        OutlinedTextField(
            value = sellQuantity,
            onValueChange = { sellQuantity = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (english) "Sell quantity" else "مقدار فروش") },
            singleLine = true
        )
        Button(
            onClick = {
                val book = orderBook
                val quantity = sellQuantity.trim().replace(",", "").toDoubleOrNull()
                if (book?.bestBid == null || quantity == null) {
                    error = if (english) "Refresh the market and enter a valid sell quantity" else "قیمت زنده بگیر و مقدار فروش معتبر وارد کن"
                } else {
                    try {
                        save(PaperTradingEngine.sell(account, book.symbol, book.bestBid!!, quantity))
                        info = if (english) "Virtual sell filled at best bid" else "فروش مجازی با بهترین قیمت خریدار ثبت شد"
                        error = null
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            },
            enabled = !loading && orderBook?.bestBid != null,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (english) "SELL — PAPER" else "فروش مجازی") }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        info?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Text(if (english) "Open positions" else "دارایی‌های باز", style = MaterialTheme.typography.titleMedium)
        if (account.positions.isEmpty()) {
            Text(if (english) "No open positions" else "هنوز دارایی بازی وجود ندارد")
        } else {
            account.positions.values.forEach { position ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp)) {
                        Text(position.symbol)
                        Text((if (english) "Quantity: " else "مقدار: ") + position.quantity.toString())
                        Text((if (english) "Average entry incl. fees: " else "میانگین بهای تمام‌شده با کارمزد: ") + money(position.averageEntryPrice))
                        Text((if (english) "Cost basis: " else "بهای تمام‌شده: ") + money(position.costBasis))
                    }
                }
            }
        }

        Text(if (english) "Trade history" else "تاریخچه معاملات", style = MaterialTheme.typography.titleMedium)
        if (account.trades.isEmpty()) {
            Text(if (english) "No paper trades yet" else "هنوز معامله مجازی ثبت نشده")
        } else {
            account.trades.asReversed().take(30).forEach { trade ->
                Text(
                    "${if (trade.side == PaperSide.BUY) "BUY" else "SELL"}  ${trade.symbol}  ${trade.quantity} @ ${money(trade.price)}  | fee ${money(trade.fee)}" +
                        if (trade.side == PaperSide.SELL) " | P/L ${money(trade.realizedPnl)}" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

private fun money(value: Double): String =
    String.format(Locale.US, "%,.2f", value)
