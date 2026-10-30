
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "A",
            fontSize = 46.sp,
            fontFamily = FontFamily.Cursive,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF35DDE0),
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xFF00DDE8).copy(alpha = 0.85f),
                    offset = Offset.Zero,
                    blurRadius = 18f
                )
            )
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "NOBITEX. TRADER.AFA.ESNIPER",
            maxLines = 1,
            softWrap = false,
            fontSize = 14.sp,
            letterSpacing = (-0.45).sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFEAFBFC),
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0xFF35DDE0).copy(alpha = 0.55f),
                    offset = Offset.Zero,
                    blurRadius = 9f
                )
            )
        )
    }
}


private val AfaCyan = Color(0xFF35DDE0)
private val AfaPanel = Color(0xFF101B2B)
private val AfaMuted = Color(0xFFA9C2CC)
private val AfaProfit = Color(0xFF35D6B0)
private val AfaLoss = Color(0xFFFF647C)

@androidx.compose.runtime.Composable
private fun DashboardScreen(
    english: Boolean,
    marketResult: MarketStats?,
    marketLoading: Boolean,
    marketError: String?,
    onOpenSettings: () -> Unit,
    onOpenScanner: () -> Unit,
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
                    if (english) "NOBITEX • AFA ESNIPER" else "نوبیتکس • آفا اسنایپر",
                    color = AfaMuted,
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
                color = AfaCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    title = if (english) "INITIAL CAPITAL" else "سرمایه اولیه",
                    value = "—",
                    accent = AfaCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (english) "IN POSITIONS" else "سرمایه در گردش",
                    value = "—",
                    accent = AfaCyan,
                    modifier = Modifier.weight(1f)
                )
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    title = if (english) "NET PROFIT" else "سود خالص",
                    value = "—",
                    accent = AfaProfit,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (english) "NET LOSS" else "ضرر خالص",
                    value = "—",
                    accent = AfaLoss,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AfaPanel),
            shape = RoundedCornerShape(18.dp)
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
                        color = Color(0xFF4B3B20),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            if (english) "NOT CONNECTED" else "اتصال اجرا برقرار نیست",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color(0xFFFFD18A),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (english) "Order execution is not enabled" else "ثبت سفارش هنوز فعال نشده است",
                        color = AfaMuted,
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
                    color = AfaMuted,
                    fontSize = 10.sp
                )
            }
        }

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AfaPanel),
            shape = RoundedCornerShape(18.dp)
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
                            color = AfaMuted,
                            fontSize = 10.sp
                        )
                    }
                    Text("◎", color = AfaCyan, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onOpenScanner, modifier = Modifier.fillMaxWidth()) {
                    Text(if (english) "OPEN MARKET SCANNER  →" else "ورود به صفحه اسکن بازار  ←")
                }
            }
        }

        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AfaPanel),
            shape = RoundedCornerShape(18.dp)
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
                        Text(if (marketLoading) "…" else if (english) "REFRESH" else "بروزرسانی", fontSize = 10.sp)
                    }
                }
                if (marketResult == null) {
                    Text(
                        if (english) "No market snapshot loaded yet." else "هنوز اطلاعاتی از بازار دریافت نشده است.",
                        color = AfaMuted,
                        fontSize = 12.sp
                    )
                } else {
                    Text("BTC / IRT  •  ${marketResult.market}", color = AfaCyan, fontWeight = FontWeight.Bold)
                    Text(
                        (if (english) "Last: " else "آخرین قیمت: ") + marketResult.latest,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        (if (english) "Best bid: " else "بهترین خرید: ") + marketResult.bestBuy +
                            "     " + (if (english) "Best ask: " else "بهترین فروش: ") + marketResult.bestSell,
                        color = AfaMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        (if (english) "24h change: " else "تغییر روزانه: ") + marketResult.dayChange + "%",
                        color = if (marketResult.dayChange >= 0.0) AfaProfit else AfaLoss,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                marketError?.let {
                    Text(it, color = AfaLoss, fontSize = 11.sp)
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
            color = AfaMuted,
            fontSize = 10.sp
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
        colors = CardDefaults.cardColors(containerColor = AfaPanel),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, color = AfaMuted, fontSize = 10.sp, maxLines = 1)
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
        }
    }
}

@androidx.compose.runtime.Composable
private fun SectionPanel(title: String, subtitle: String, empty: String) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AfaPanel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AfaCyan, fontSize = 9.sp)
            HorizontalDivider(color = Color(0xFF29404E))
            Text(empty, color = AfaMuted, fontSize = 11.sp)
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

            MaterialTheme(
                colorScheme = if (settings.darkTheme) {
                    darkColorScheme(
                        primary = Color(0xFF35DDE0),
                        onPrimary = Color(0xFF00191B),
                        secondary = Color(0xFF8CECEF),
                        background = Color(0xFF070D18),
                        surface = Color(0xFF101B2B),
                        surfaceVariant = Color(0xFF19283A),
                        onBackground = Color(0xFFEAFBFC),
                        onSurface = Color(0xFFEAFBFC),
                        onSurfaceVariant = Color(0xFFA9C2CC)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF008F98),
                        secondary = Color(0xFF087E88),
                        background = Color(0xFFF3FAFB),
                        surface = Color(0xFFFFFFFF),
                        onBackground = Color(0xFF102329),
                        onSurface = Color(0xFF102329)
                    )
                }
            ) {
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
                                onOpenSettings = { screen = AppScreen.SETTINGS },
                                onOpenScanner = { screen = AppScreen.SCANNER },
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
