
package ir.nobitextrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import ir.nobitextrader.app.data.MarketStats
import ir.nobitextrader.app.data.NobitexCandlesApi
import ir.nobitextrader.app.data.NobitexMarketApi
import ir.nobitextrader.app.scanner.MarketScannerScreen
import ir.nobitextrader.app.scanner.MarketScannerUiState
import ir.nobitextrader.app.scanner.MarketScannerViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppScreen {
    HOME,
    SETTINGS,
    SCANNER
}


@androidx.compose.runtime.Composable
private fun AfaBrandHeader() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "A",
            fontSize = 46.sp,
            fontFamily = FontFamily.Cursive,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            style = TextStyle(
                shadow = Shadow(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    offset = Offset.Zero,
                    blurRadius = 18f
                )
            )
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "NOBITEX TRADER  A.F.A SNIPER",
            maxLines = 2,
            softWrap = true,
            fontSize = 14.sp,
            letterSpacing = (-0.45).sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = TextStyle(
                shadow = Shadow(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    offset = Offset.Zero,
                    blurRadius = 9f
                )
            )
        )
    }
}



@androidx.compose.runtime.Composable
private fun DashboardScreen(
    english: Boolean,
    marketResult: MarketStats?,
    marketLoading: Boolean,
    marketError: String?,
    scannerState: MarketScannerUiState,
    onOpenSettings: () -> Unit,
    onOpenScanner: () -> Unit,
    onScanMarkets: () -> Unit,
    onRefreshMarket: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AfaBrandHeader()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    if (english) "TRADING TERMINAL" else "ترمینال معاملاتی",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    if (english) "NOBITEX • AFA SNIPER" else "نوبیتکس • آفا اسنایپر",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            androidx.compose.material3.AssistChip(
                onClick = {},
                label = { Text(if (english) "READ ONLY" else "فقط مشاهده") },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (english) "ACCOUNT OVERVIEW" else "نمای کلی سرمایه",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    title = if (english) "INITIAL CAPITAL" else "سرمایه اولیه",
                    value = "—",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (english) "IN POSITIONS" else "سرمایه در گردش",
                    value = "—",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    title = if (english) "NET PROFIT" else "سود خالص",
                    value = "—",
                    accent = AfaColorSchemes.profit,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (english) "NET LOSS" else "ضرر خالص",
                    value = "—",
                    accent = AfaColorSchemes.loss,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransferMetricCard(
                title = if (english) "DEPOSITS" else "واریزها",
                value = "—",
                caption = if (english) "Verified account records" else "سوابق تأییدشده حساب",
                accent = AfaColorSchemes.profit,
                modifier = Modifier.weight(1f)
            )
            TransferMetricCard(
                title = if (english) "WITHDRAWALS" else "برداشت‌ها",
                value = "—",
                caption = if (english) "Verified account records" else "سوابق تأییدشده حساب",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (english) "ENGINE CONTROL" else "کنترل موتور معاملاتی",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            if (english) "NOT CONNECTED" else "اتصال اجرا برقرار نیست",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (english) "Order execution is not enabled" else "ثبت سفارش هنوز فعال نشده است",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                        Text("▶ START", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                        Text("Ⅱ STOP", fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                        Text("✕ KILL", fontSize = 11.sp)
                    }
                }
                Text(
                    if (english) "Controls unlock only after verified private API, order state and emergency-close tests." else "این کلیدها پس از تکمیل و آزمایش API خصوصی، وضعیت سفارش‌ها و توقف اضطراری فعال می‌شوند.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (english) "MARKET RADAR" else "رادار بازار",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (english) "Independent full-screen scanner" else "اسکنر مستقل در صفحه تمام‌صفحه",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Text("◎", color = MaterialTheme.colorScheme.primary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onScanMarkets,
                        enabled = !scannerState.isScanning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (scannerState.isScanning) {
                                if (english) "SCANNING…" else "در حال اسکن…"
                            } else {
                                if (english) "SCAN ALL" else "اسکن همه بازار"
                            },
                            fontSize = 11.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenScanner,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            if (english) "OPEN SCANNER →" else "صفحه اسکن ←",
                            fontSize = 11.sp
                        )
                    }
                }
                if (scannerState.isScanning || scannerState.totalCount > 0) {
                    Text(
                        if (english) {
                            "Progress: ${scannerState.completedCount} / ${scannerState.totalCount}"
                        } else {
                            "پیشرفت: ${scannerState.completedCount} / ${scannerState.totalCount}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                if (scannerState.results.isNotEmpty()) {
                    val successful = scannerState.results.count { it.success }
                    val failed = scannerState.results.size - successful
                    Text(
                        if (english) {
                            "Valid books: $successful   •   Failed: $failed"
                        } else {
                            "دفتر سفارش معتبر: $successful   •   ناموفق: $failed"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    scannerState.results
                        .asSequence()
                        .filter { it.success }
                        .take(3)
                        .forEach { result ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    result.symbol,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    (if (english) "Spread " else "اختلاف ") +
                                        String.format(Locale.US, "%.3f%%", result.spreadPercent),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                }
                scannerState.errorMessage?.let { message ->
                    Text(message, color = AfaColorSchemes.loss, fontSize = 11.sp)
                }
                val analyzedMarkets = scannerState.analysisBySymbol.entries
                    .filter { it.value.result != null || it.value.error != null || it.value.isLoading }
                    .take(4)
                if (analyzedMarkets.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        if (english) "STRATEGY ANALYSIS" else "خلاصه تحلیل استراتژی",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    analyzedMarkets.forEach { (symbol, analysisState) ->
                        val decision = analysisState.result?.decision
                        val status = when {
                            analysisState.isLoading -> if (english) "ANALYZING" else "در حال تحلیل"
                            analysisState.error != null -> if (english) "ERROR" else "خطا"
                            decision?.isActionable == true -> decision.direction.name
                            else -> if (english) "NO TRADE" else "بدون معامله"
                        }
                        val statusColor = when {
                            analysisState.isLoading -> MaterialTheme.colorScheme.onSurfaceVariant
                            analysisState.error != null -> AfaColorSchemes.loss
                            decision?.isActionable == true -> AfaColorSchemes.profit
                            else -> AfaColorSchemes.warning
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                symbol,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                status,
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (decision?.isActionable == true) {
                            Text(
                                (if (english) "Agreement " else "توافق ") +
                                    decision.agreeingStrategyIds.size +
                                    " • " +
                                    String.format(Locale.US, "%.1f%%", decision.confidence * 100.0),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        if (english) "Analysis signals only — no order has been placed." else "این‌ها فقط خروجی تحلیل هستند؛ هیچ سفارشی ثبت نشده است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (english) "LIVE MARKET SNAPSHOT" else "نمای لحظه‌ای بازار",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = onRefreshMarket, enabled = !marketLoading) {
                        Text(if (marketLoading) "…" else if (english) "REFRESH" else "بروزرسانی", fontSize = 11.sp)
                    }
                }
                if (marketResult == null) {
                    Text(
                        if (english) "No market snapshot loaded yet." else "هنوز اطلاعاتی از بازار دریافت نشده است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                } else {
                    Text("BTC / IRT  •  ${marketResult.market}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        (if (english) "Last: " else "آخرین قیمت: ") + marketResult.latest,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        (if (english) "Best bid: " else "بهترین خرید: ") + marketResult.bestBuy +
                            "     " + (if (english) "Best ask: " else "بهترین فروش: ") + marketResult.bestSell,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        (if (english) "24h change: " else "تغییر روزانه: ") + marketResult.dayChange + "%",
                        color = if (marketResult.dayChange >= 0.0) AfaColorSchemes.profit else AfaColorSchemes.loss,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                marketError?.let {
                    Text(it, color = AfaColorSchemes.loss, fontSize = 11.sp)
                }
            }
        }

        SectionPanel(
            title = if (english) "OPEN POSITIONS" else "پوزیشن‌های باز",
            subtitle = if (english) "Live filled positions will appear here" else "پوزیشن‌های واقعی پس از ثبت و تأیید معامله نمایش داده می‌شوند",
            empty = if (english) "No verified positions to display." else "فعلاً پوزیشن تأییدشده‌ای برای نمایش وجود ندارد."
        )
        SectionPanel(
            title = if (english) "TRADE HISTORY" else "تاریخچه معاملات",
            subtitle = if (english) "BUY / SELL • PRICE • FEE • REALIZED P&L" else "خرید / فروش • قیمت • کارمزد • سود و زیان قطعی",
            empty = if (english) "No completed trades recorded yet." else "هنوز معامله تکمیل‌شده‌ای ثبت نشده است."
        )
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(if (english) "SETTINGS" else "تنظیمات")
        }
        Text(
            if (english) "Account balance, profit and trade history require verified authenticated exchange data." else "موجودی حساب، سود و تاریخچه معاملات به داده احراز هویت‌شده و تأییدشده صرافی نیاز دارند.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@androidx.compose.runtime.Composable
private fun MetricCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2
            )
            Text(
                value,
                color = accent,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun TransferMetricCard(
    title: String,
    value: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                color = accent,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun SectionPanel(title: String, subtitle: String, empty: String) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(empty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var settings by remember {
                mutableStateOf(TraderSettingsStore.load(this@MainActivity))
            }

            var screen by remember {
                mutableStateOf(AppScreen.HOME)
            }

            // ViewModel is owned by this Activity and survives
            // Compose recompositions and screen navigation.
            val scannerViewModel = remember {
                ViewModelProvider(this@MainActivity)[
                    MarketScannerViewModel::class.java
                ]
            }

            val scope = rememberCoroutineScope()
            val english = settings.useEnglish
            val scannerState by scannerViewModel.uiState.collectAsState()

            var marketResult by remember {
                mutableStateOf<MarketStats?>(null)
            }
            var marketLoading by remember {
                mutableStateOf(false)
            }
            var marketError by remember {
                mutableStateOf<String?>(null)
            }

            var rawLoading by remember {
                mutableStateOf(false)
            }
            var rawResponse by remember {
                mutableStateOf<String?>(null)
            }
            var rawError by remember {
                mutableStateOf<String?>(null)
            }

            var candlesLoading by remember {
                mutableStateOf(false)
            }
            var candleCount by remember {
                mutableStateOf<Int?>(null)
            }
            var lastCandleTime by remember {
                mutableStateOf<String?>(null)
            }
            var candleError by remember {
                mutableStateOf<String?>(null)
            }

            BackHandler(enabled = screen != AppScreen.HOME) {
                if (screen == AppScreen.SETTINGS) {
                    settings = TraderSettingsStore.load(
                        this@MainActivity
                    )
                }
                screen = AppScreen.HOME
            }

            AfaTheme(darkTheme = settings.darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (screen) {
                        AppScreen.SETTINGS -> {
                            SettingsScreen(
                                modifier = Modifier.fillMaxSize(),
                                onBack = {
                                    settings = TraderSettingsStore.load(
                                        this@MainActivity
                                    )
                                    screen = AppScreen.HOME
                                }
                            )
                        }

                        AppScreen.SCANNER -> {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Button(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ),
                                    onClick = {
                                        screen = AppScreen.HOME
                                    }
                                ) {
                                    Text(
                                        if (english) {
                                            "Back to home"
                                        } else {
                                            "بازگشت به صفحه اصلی"
                                        }
                                    )
                                }

                                MarketScannerScreen(
                                    viewModel = scannerViewModel,
                                    modifier = Modifier
                                        .fillMaxSize()
                                )
                            }
                        }

                        AppScreen.HOME -> {
                            DashboardScreen(
                                english = english,
                                marketResult = marketResult,
                                marketLoading = marketLoading,
                                marketError = marketError,
                                scannerState = scannerState,
                                onOpenSettings = { screen = AppScreen.SETTINGS },
                                onOpenScanner = { screen = AppScreen.SCANNER },
                                onScanMarkets = { scannerViewModel.scanMarkets() },
                                onRefreshMarket = {
                                    scope.launch {
                                        marketLoading = true
                                        marketError = null
                                        try {
                                            marketResult = NobitexMarketApi.fetchMarketStats()
                                        } catch (e: Exception) {
                                            marketError = e.message ?: "Unknown market data error"
                                        } finally {
                                            marketLoading = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
