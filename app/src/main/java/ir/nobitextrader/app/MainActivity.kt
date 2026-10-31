
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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppScreen {
    HOME,
    SETTINGS,
    SCANNER,
    BTC_CHART,
    PAPER_TRADE,
    HISTORY,
    WHALE
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
    marketUpdatedAt: Long?,
    marketConnected: Boolean,
    scannerState: MarketScannerUiState,
    paperPortfolio: PaperPortfolio,
    onOpenSettings: () -> Unit,
    onOpenScanner: () -> Unit,
    onScanMarkets: () -> Unit,
    onStartEngine: () -> Unit,
    onStopEngine: () -> Unit,
    onKillEngine: () -> Unit,
    onRefreshMarket: () -> Unit
) {
    val paperPositions = paperPortfolio.positions.filter { it.quantity > 0.0 }
    val paperInvestedCost = paperPositions.sumOf { it.quantity * it.averageEntry }
    val nowMillis = System.currentTimeMillis()
    val freshBtcPrice = marketResult?.latest?.takeIf {
        it.isFinite() && it > 0.0 &&
            marketUpdatedAt != null && nowMillis - marketUpdatedAt <= 60_000L
    }
    val paperPositionMarks = paperPositions.map { position ->
        val scannedResult = scannerState.rawResults.firstOrNull {
            it.symbol == position.symbol && it.success &&
                it.observedAtMillis > 0L &&
                nowMillis - it.observedAtMillis <= 120_000L
        }
        val scannedPrice = scannedResult?.lastPrice
        val currentPrice = if (position.symbol == "BTCIRT") {
            freshBtcPrice ?: scannedPrice
        } else {
            scannedPrice
        }
        position to currentPrice?.takeIf { it.isFinite() && it > 0.0 }
    }
    val allPaperPositionsMarked = paperPositionMarks.all { it.second != null }
    val paperUnrealizedPnl = if (allPaperPositionsMarked) {
        paperPositionMarks.sumOf { (position, price) ->
            (price!! - position.averageEntry) * position.quantity
        }
    } else null
    val paperTotalPnl = paperUnrealizedPnl?.plus(paperPortfolio.realizedPnl)
    val paperPositionValue = if (allPaperPositionsMarked) {
        paperPositionMarks.sumOf { (position, price) -> position.quantity * price!! }
    } else paperInvestedCost
    val paperPositionColor = when {
        paperUnrealizedPnl == null -> MaterialTheme.colorScheme.onSurfaceVariant
        paperUnrealizedPnl >= 0.0 -> AfaColorSchemes.profit
        else -> AfaColorSchemes.loss
    }

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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectionStatusIndicator(
                label = if (english) "NOBITEX DATA" else "داده نوبیتکس",
                connected = marketConnected,
                detail = if (english) "PUBLIC API" else "API عمومی"
            )
            ConnectionStatusIndicator(
                label = if (english) "PRIVATE TRADING" else "معاملات حساب",
                connected = false,
                detail = if (english) "NOT AUTHENTICATED" else "احراز هویت نشده"
            )
        }
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
                    "A.F.A SNIPER • PERSONAL TERMINAL",
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
                if (english) "PAPER ACCOUNT OVERVIEW" else "نمای کلی سرمایه مجازی",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    title = if (english) "AVAILABLE CASH" else "سرمایه آزاد مجازی",
                    value = String.format(Locale.US, "%,.0f IRR", paperPortfolio.cash),
                    accent = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (english) "IN POSITIONS" else "سرمایه در گردش",
                    value = String.format(Locale.US, "%,.0f IRR", paperPositionValue),
                    accent = paperPositionColor,
                    modifier = Modifier.weight(1f)
                )
            }
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    title = if (english) "NET PROFIT" else "سود خالص",
                    value = paperTotalPnl?.let {
                        String.format(Locale.US, "%,.0f IRR", it.coerceAtLeast(0.0))
                    } ?: "—",
                    accent = if ((paperTotalPnl ?: 0.0) > 0.0) AfaColorSchemes.profit
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (english) "NET LOSS" else "ضرر خالص",
                    value = paperTotalPnl?.let {
                        String.format(Locale.US, "%,.0f IRR", (-it).coerceAtLeast(0.0))
                    } ?: "—",
                    accent = if ((paperTotalPnl ?: 0.0) < 0.0) AfaColorSchemes.loss
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    if (english) "SCANNER ENGINE CONTROL" else "کنترل موتور اسکن",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Surface(
                        color = if (scannerState.autoRefreshEnabled || scannerState.isScanning)
                            Color(0xFFE0F4E8) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            if (scannerState.autoRefreshEnabled || scannerState.isScanning) {
                                if (english) "SCANNER RUNNING" else "اسکنر فعال"
                            } else {
                                if (english) "SCANNER STOPPED" else "اسکنر متوقف"
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = if (scannerState.autoRefreshEnabled || scannerState.isScanning)
                                Color(0xFF168A45) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (english) "PUBLIC MARKET DATA • PAPER MODE ONLY"
                        else "فقط کنترل اسکن داده عمومی • بدون سفارش واقعی",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Button(
                        onClick = onStartEngine,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF168A45),
                            contentColor = Color.White
                        )
                    ) {
                        Text("START", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onStopEngine,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFDADA),
                            contentColor = Color(0xFF9E4141)
                        )
                    ) {
                        Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onKillEngine,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        )
                    ) {
                        Text("KILL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Text(
                    if (english) "START runs the scanner and auto-refresh. STOP pauses scanning. KILL cancels scanner, analysis and public-trade jobs. These controls do not send or cancel real exchange orders."
                    else "START اسکن و تازه‌سازی خودکار را آغاز می‌کند. STOP اسکن را متوقف می‌کند. KILL اسکن، تحلیل و دریافت معاملات عمومی را لغو می‌کند. این کلیدها سفارش واقعی صرافی را ثبت یا لغو نمی‌کنند.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
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
                Text(
                    if (scannerState.autoRefreshEnabled) {
                        if (english) {
                            "AUTO SCAN ON • every ${scannerState.refreshIntervalMs / 1_000L}s"
                        } else {
                            "اسکن خودکار روشن • هر ${scannerState.refreshIntervalMs / 1_000L} ثانیه"
                        }
                    } else {
                        if (english) "AUTO SCAN OFF • manual scans only" else "اسکن خودکار خاموش • فقط اسکن دستی"
                    },
                    color = if (scannerState.autoRefreshEnabled) AfaColorSchemes.profit else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
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
                if (scannerState.totalCount > 0 || scannerState.results.isNotEmpty() || scannerState.rawResults.isNotEmpty()) {
                    Text(
                        if (english) {
                            "Valid books: ${scannerState.successfulCount}   •   Failed: ${scannerState.failedCount}"
                        } else {
                            "بازارهای دارای دفتر سفارش معتبر: ${scannerState.successfulCount}   •   ناموفق: ${scannerState.failedCount}"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (english) "SYMBOL" else "نماد", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        Text(if (english) "LAST PRICE (IRR)" else "آخرین قیمت (ریال)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        Text(if (english) "SPREAD" else "اختلاف", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                    val dashboardMarkets = scannerState.rawResults
                        .asSequence()
                        .filter { it.success && it.lastPrice.isFinite() && it.lastPrice > 0.0 }
                        .take(20)
                        .toList()
                    dashboardMarkets.forEach { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    result.symbol,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    String.format(Locale.US, "%,.0f IRR", result.lastPrice),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    String.format(Locale.US, "%.3f%%", result.spreadPercent),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    if (dashboardMarkets.isEmpty() && scannerState.successfulCount > 0) {
                        Text(
                            if (english) "API reported ${scannerState.successfulCount} valid books, but no priced rows reached the dashboard. This is a display/data mismatch; run SCAN ALL again."
                            else "API تعداد ${scannerState.successfulCount} دفتر سفارش معتبر گزارش کرده، اما ردیف قیمت‌دار به داشبورد نرسیده است. این ناهماهنگی نمایش داده است؛ SCAN ALL را دوباره اجرا کنید.",
                            color = AfaColorSchemes.loss,
                            fontSize = 10.sp
                        )
                    }
                } else if (!scannerState.isScanning) {
                    Text(
                        if (english) "No scan results yet. Run a scan to populate this panel."
                        else "هنوز نتیجه‌ای ثبت نشده؛ برای تکمیل این بخش اسکن بازار را اجرا کنید.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
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
                if (marketLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(18.dp).height(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (marketResult == null) {
                                if (english) "Loading public market data…" else "در حال دریافت داده عمومی بازار…"
                            } else {
                                if (english) "Refreshing public market data…" else "در حال بروزرسانی داده بازار…"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                if (marketResult == null) {
                    if (!marketLoading) {
                        Text(
                            if (english) "No public market snapshot is available. Check your connection and retry." else "داده‌ای از بازار دریافت نشده است. اتصال اینترنت را بررسی و دوباره تلاش کنید.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text("BTC / IRT  •  ${marketResult.market}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val rising = marketResult.dayChange >= 0.0
                        Text(
                            if (rising) "▲" else "▼",
                            color = if (rising) AfaColorSchemes.profit else AfaColorSchemes.loss,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            (if (english) "Last: " else "آخرین قیمت: ") + String.format(Locale.US, "%,.0f", marketResult.latest),
                            color = if (rising) AfaColorSchemes.profit else AfaColorSchemes.loss,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        (if (english) "Best bid: " else "بهترین خرید: ") + marketResult.bestBuy +
                            "     " + (if (english) "Best ask: " else "بهترین فروش: ") + marketResult.bestSell,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        (if (english) "24h change: " else "تغییر روزانه: ") +
                            String.format(Locale.US, "%.2f%%", marketResult.dayChange),
                        color = if (marketResult.dayChange >= 0.0) AfaColorSchemes.profit else AfaColorSchemes.loss,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        (if (english) "24h range: " else "بازه ۲۴ ساعته: ") +
                            String.format(Locale.US, "%,.0f – %,.0f", marketResult.dayLow, marketResult.dayHigh),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        if (english) {
                            if (marketResult.isClosed) "Market status: CLOSED" else "Market status: OPEN"
                        } else {
                            if (marketResult.isClosed) "وضعیت بازار: بسته" else "وضعیت بازار: باز"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                marketUpdatedAt?.let { updatedAt ->
                    Text(
                        (if (english) "Last successful update: " else "آخرین به‌روزرسانی موفق: ") +
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(updatedAt)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
                marketError?.let {
                    Text(it, color = AfaColorSchemes.loss, fontSize = 11.sp)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    if (english) "OPEN PAPER POSITIONS" else "نمادهای خریداری‌شده مجازی",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (english) "LOCAL PAPER WALLET • arrows compare current price with average entry"
                    else "کیف پول مجازی محلی • فلش‌ها قیمت فعلی را با میانگین خرید مقایسه می‌کنند",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                if (paperPositionMarks.isEmpty()) {
                    Text(
                        if (english) "No virtual positions yet. Paper buys will appear here."
                        else "هنوز نماد خریداری‌شده‌ای در کیف پول مجازی نیست؛ خریدهای آزمایشی اینجا نمایش داده می‌شوند.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else {
                    paperPositionMarks.forEach { (position, currentPrice) ->
                        val pnl = currentPrice?.let {
                            (it - position.averageEntry) * position.quantity
                        }
                        val positionColor = when {
                            pnl == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            pnl > 0.0 -> AfaColorSchemes.profit
                            pnl < 0.0 -> AfaColorSchemes.loss
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(position.symbol, color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        String.format(Locale.US, "%.8f", position.quantity),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        if (currentPrice != null) {
                                            (if (english) "LAST: " else "قیمت: ") +
                                                String.format(Locale.US, "%,.0f IRR", currentPrice)
                                        } else {
                                            if (english) "WAITING FOR FRESH PRICE" else "در انتظار قیمت تازه"
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        when {
                                            pnl == null -> "—"
                                            pnl > 0.0 -> "↑ " + String.format(Locale.US, "%+,.0f IRR", pnl)
                                            pnl < 0.0 -> "↓ " + String.format(Locale.US, "%+,.0f IRR", pnl)
                                            else -> "• 0 IRR"
                                        },
                                        color = positionColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        (if (english) "ENTRY: " else "میانگین خرید: ") +
                                            String.format(Locale.US, "%,.0f", position.averageEntry),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    if (english) "These are virtual positions saved on this device. Real Nobitex account positions require authenticated private-account access."
                    else "این‌ها پوزیشن‌های مجازی ذخیره‌شده روی همین دستگاه هستند. نمایش پوزیشن واقعی نوبیتکس به اتصال امن حساب خصوصی نیاز دارد.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
        SectionPanel(
            title = if (english) "REAL ACCOUNT POSITIONS" else "پوزیشن‌های حساب واقعی",
            subtitle = if (english) "Authenticated filled positions" else "پوزیشن‌های واقعی تأییدشده",
            empty = if (english) "Private account is not connected. Real positions are unavailable." else "حساب خصوصی متصل نیست؛ پوزیشن‌های واقعی در دسترس نیستند."
        )
        SectionPanel(
            title = if (english) "TRADE HISTORY" else "تاریخچه معاملات",
            subtitle = if (english) "BUY / SELL • PRICE • FEE • REALIZED P&L" else "خرید / فروش • قیمت • کارمزد • سود و زیان قطعی",
            empty = if (english) "Authenticated trade history is not connected." else "تاریخچه معاملات حساب هنوز به منبع احراز هویت‌شده متصل نیست."
        )
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
            Text(if (english) "SETTINGS" else "تنظیمات")
        }
        Text(
            if (english) {
                "PUBLIC DATA ONLY • Account balances, transfers, positions and realized P&L remain unavailable until secure private-account access is implemented."
            } else {
                "فقط داده عمومی • موجودی، واریز و برداشت، پوزیشن‌ها و سود و زیان قطعی تا زمان پیاده‌سازی اتصال امن حساب خصوصی قابل دریافت نیستند."
            },
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
private fun ConnectionStatusIndicator(
    label: String,
    connected: Boolean,
    detail: String
) {
    val statusColor = if (connected) AfaColorSchemes.profit else AfaColorSchemes.loss
    Card(
        modifier = Modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Surface(
                modifier = Modifier.width(8.dp).height(8.dp),
                color = statusColor,
                shape = RoundedCornerShape(50)
            ) {}
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = statusColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
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

@androidx.compose.runtime.Composable
private fun TerminalBottomNavigation(
    selected: AppScreen,
    english: Boolean,
    onSelect: (AppScreen) -> Unit
) {
    val items = listOf(
        AppScreen.HOME to if (english) "HOME" else "خانه",
        AppScreen.SCANNER to if (english) "SCAN" else "اسکن",
        AppScreen.BTC_CHART to if (english) "BTC" else "بیت‌کوین",
        AppScreen.PAPER_TRADE to if (english) "PAPER" else "مجازی",
        AppScreen.HISTORY to if (english) "HISTORY" else "تاریخچه",
        AppScreen.WHALE to if (english) "WHALE" else "نهنگ",
        AppScreen.SETTINGS to if (english) "SET" else "تنظیمات"
    )
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (destination, label) ->
            TextButton(
                onClick = { onSelect(destination) },
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 1.dp, vertical = 5.dp)
            ) {
                Text(
                    label,
                    color = if (selected == destination) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp,
                    fontWeight = if (selected == destination) FontWeight.ExtraBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
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
            var paperPortfolio by remember {
                mutableStateOf(PaperTradingStore.load(this@MainActivity))
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
            var marketUpdatedAt by remember {
                mutableStateOf<Long?>(null)
            }
            var marketConnected by remember {
                mutableStateOf(false)
            }

            LaunchedEffect(screen) {
                if (screen == AppScreen.HOME) {
                    paperPortfolio = PaperTradingStore.load(this@MainActivity)
                }
            }

            LaunchedEffect(screen) {
                if (screen == AppScreen.HOME || screen == AppScreen.PAPER_TRADE) {
                    while (true) {
                        marketLoading = marketResult == null
                        marketError = null
                        try {
                            marketResult = NobitexMarketApi.fetchMarketStats()
                            marketUpdatedAt = System.currentTimeMillis()
                            marketConnected = true
                        } catch (e: Exception) {
                            marketConnected = false
                            marketError = e.message ?: "Unknown market data error"
                        } finally {
                            marketLoading = false
                        }
                        delay(15_000L)
                    }
                }
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (screen) {
                                AppScreen.SETTINGS -> {
                                    SettingsScreen(
                                        modifier = Modifier.fillMaxSize(),
                                        onBack = {
                                            settings = TraderSettingsStore.load(this@MainActivity)
                                            screen = AppScreen.HOME
                                        }
                                    )
                                }
                                AppScreen.SCANNER -> {
                                    MarketScannerScreen(
                                        viewModel = scannerViewModel,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                AppScreen.BTC_CHART -> BtcChartScreen(english = english)
                                AppScreen.PAPER_TRADE -> PaperTradeScreen(english = english, market = marketResult, marketUpdatedAt = marketUpdatedAt, feeRatePercent = settings.paperFeePercent.toDouble())
                                AppScreen.HISTORY -> PaperHistoryScreen(english = english)
                                AppScreen.WHALE -> SectionPanel(
                                    title = if (english) "WHALE TRACKER" else "ردیاب نهنگ",
                                    subtitle = if (english) "PUBLIC ON-CHAIN DATA ONLY" else "فقط داده عمومی روی زنجیره",
                                    empty = if (english) "No verified chain data source is configured. Exchange order books do not identify individual traders." else "منبع معتبر داده زنجیره‌ای تنظیم نشده است؛ دفتر سفارش صرافی هویت معامله‌گران را مشخص نمی‌کند."
                                )
                                AppScreen.HOME -> {
                                    DashboardScreen(
                                        english = english,
                                        marketResult = marketResult,
                                        marketLoading = marketLoading,
                                        marketError = marketError,
                                        marketUpdatedAt = marketUpdatedAt,
                                        marketConnected = marketConnected,
                                        scannerState = scannerState,
                                        paperPortfolio = paperPortfolio,
                                        onOpenSettings = { screen = AppScreen.SETTINGS },
                                        onOpenScanner = { screen = AppScreen.SCANNER },
                                        onScanMarkets = { scannerViewModel.scanMarkets() },
                                        onStartEngine = { scannerViewModel.startScannerEngine() },
                                        onStopEngine = { scannerViewModel.stopScannerEngine() },
                                        onKillEngine = { scannerViewModel.killScannerEngine() },
                                        onRefreshMarket = {
                                            scope.launch {
                                                marketLoading = true
                                                marketError = null
                                                try {
                                                    marketResult = NobitexMarketApi.fetchMarketStats()
                                                    marketUpdatedAt = System.currentTimeMillis()
                                                    marketConnected = true
                                                } catch (e: Exception) {
                                                    marketConnected = false
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
                        TerminalBottomNavigation(
                            selected = screen,
                            english = english,
                            onSelect = { destination ->
                                if (destination == AppScreen.SETTINGS) {
                                    settings = TraderSettingsStore.load(this@MainActivity)
                                }
                                screen = destination
                            }
                        )
                    }
                }
            }
        }
    }
}
