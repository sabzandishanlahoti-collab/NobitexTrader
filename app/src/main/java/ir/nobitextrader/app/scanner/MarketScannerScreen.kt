package ir.nobitextrader.app.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.TraderSettingsStore
import java.util.Locale

private val TerminalBackground = Color(0xFF07110F)
private val TerminalPanel = Color(0xFF0D1B17)
private val TerminalPanelAlt = Color(0xFF10231D)
private val TerminalBorder = Color(0xFF1D4034)
private val TerminalGreen = Color(0xFF35D69A)
private val TerminalMuted = Color(0xFF8AA69A)
private val TerminalText = Color(0xFFE4F2EB)
private val TerminalRed = Color(0xFFFF7777)
private val TerminalAmber = Color(0xFFFFC66D)

@Composable
fun MarketScannerScreen(
    viewModel: MarketScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.applySettings(TraderSettingsStore.load(context))
    }

    val visibleResults = state.results
    val successfulCount = state.successfulCount
    val visibleSuccessfulCount = visibleResults.count { it.success }
    val failedCount = state.failedCount
    // Count only successful markets excluded by the active filters.
    val filteredOutCount =
        (successfulCount - visibleSuccessfulCount).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalPanel, RoundedCornerShape(16.dp))
                .border(1.dp, TerminalBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "NOBITEX TERMINAL",
                    color = TerminalGreen,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "موتور پایش بازار",
                    color = TerminalMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (state.isScanning) "SCANNING" else "STANDBY",
                        color = if (state.isScanning) TerminalGreen else TerminalMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (state.isScanning) TerminalGreen else TerminalMuted,
                                RoundedCornerShape(50)
                            )
                    )
                }
                Text(
                    text = "REST SNAPSHOT",
                    color = TerminalMuted,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.scanMarkets() },
                enabled = !state.isScanning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TerminalGreen,
                    contentColor = Color(0xFF062017),
                    disabledContainerColor = TerminalBorder,
                    disabledContentColor = TerminalMuted
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (state.isScanning) "در حال اسکن..." else "شروع اسکن بازارها",
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = { viewModel.clearResults() },
                enabled = !state.isScanning && state.results.isNotEmpty(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TerminalMuted,
                    disabledContentColor = TerminalBorder
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("پاک‌کردن")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            MetricPanel(
                title = "دریافت‌شده",
                value = state.totalCount.toString(),
                modifier = Modifier.weight(1f),
                valueColor = TerminalText
            )
            MetricPanel(
                title = "موفق",
                value = successfulCount.toString(),
                modifier = Modifier.weight(1f),
                valueColor = TerminalGreen
            )
            MetricPanel(
                title = "ناموفق",
                value = failedCount.toString(),
                modifier = Modifier.weight(1f),
                valueColor = if (failedCount > 0) TerminalRed else TerminalMuted
            )
        }

        val scanSummary = MarketScanSummary.from(visibleResults)
        val bidLiquidity = scanSummary.totalBidLiquidity
        val askLiquidity = scanSummary.totalAskLiquidity
        val liquidityDifference = bidLiquidity - askLiquidity
        val averageSpread = scanSummary.averageSpreadPercent

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            MetricPanel(
                title = "نقدینگی خرید ۵ سطح",
                value = formatQuoteLiquidity(bidLiquidity),
                modifier = Modifier.weight(1f),
                valueColor = TerminalGreen
            )
            MetricPanel(
                title = "نقدینگی فروش ۵ سطح",
                value = formatQuoteLiquidity(askLiquidity),
                modifier = Modifier.weight(1f),
                valueColor = TerminalRed
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            MetricPanel(
                title = "اختلاف نقدینگی خرید/فروش",
                value = formatQuoteLiquidity(kotlin.math.abs(liquidityDifference)) +
                    if (liquidityDifference >= 0.0) " +" else " -",
                modifier = Modifier.weight(1f),
                valueColor = if (liquidityDifference >= 0.0) TerminalGreen else TerminalRed
            )
            MetricPanel(
                title = "میانگین اختلاف قیمت",
                value = averageSpread?.let {
                    String.format(Locale.US, "%.3f%%", it)
                } ?: "—",
                modifier = Modifier.weight(1f),
                valueColor = TerminalAmber
            )
        }
        Text(
            "این ارقام نقدینگی ۵ ردیف اول دفتر سفارش بازارهای موفقِ قابل‌نمایش هستند؛ " +
                "نه تعداد معاملات انجام‌شده، نه ورود/خروج قطعی سرمایه و نه سود حساب.",
            color = TerminalMuted,
            fontSize = 10.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = TerminalPanel),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, TerminalBorder)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "به‌روزرسانی خودکار",
                        color = TerminalText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (state.autoRefreshEnabled) "فعال" else "خاموش",
                        color = if (state.autoRefreshEnabled) TerminalGreen else TerminalMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "فاصله اسکن: ${state.refreshIntervalMs / 1000} ثانیه",
                    color = TerminalMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TerminalChoiceButton("۳۰ ثانیه", modifier = Modifier.weight(1f)) {
                        viewModel.setRefreshInterval(30_000L)
                    }
                    TerminalChoiceButton("۶۰ ثانیه", modifier = Modifier.weight(1f)) {
                        viewModel.setRefreshInterval(60_000L)
                    }
                    TerminalChoiceButton("۵ دقیقه", modifier = Modifier.weight(1f)) {
                        viewModel.setRefreshInterval(300_000L)
                    }
                }
                TerminalActionButton(
                    text = if (state.autoRefreshEnabled) {
                        "خاموش کردن به‌روزرسانی خودکار"
                    } else {
                        "روشن کردن به‌روزرسانی خودکار"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    outlined = true,
                    onClick = { viewModel.toggleAutoRefresh() }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                "مرتب‌سازی و فیلتر",
                color = TerminalGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortButton(
                    text = "ترکیبی",
                    selected = state.sortMode == MarketSortMode.BALANCED_PRIORITY,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setSortMode(MarketSortMode.BALANCED_PRIORITY) }
                SortButton(
                    text = "اختلاف کمتر",
                    selected = state.sortMode == MarketSortMode.SPREAD_LOW_TO_HIGH,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setSortMode(MarketSortMode.SPREAD_LOW_TO_HIGH) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortButton(
                    text = "تقاضای بیشتر",
                    selected = state.sortMode == MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setSortMode(MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW) }
                SortButton(
                    text = "عرضه بیشتر",
                    selected = state.sortMode == MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setSortMode(MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW) }
            }
            SortButton(
                text = "فشار خرید + معاملات",
                selected = state.sortMode == MarketSortMode.COMBINED_BUY_FLOW_HIGH_TO_LOW,
                modifier = Modifier.fillMaxWidth()
            ) { viewModel.setSortMode(MarketSortMode.COMBINED_BUY_FLOW_HIGH_TO_LOW) }
            SortButton(
                text = "فشار خرید بیشتر",
                selected = state.sortMode == MarketSortMode.BUY_PRESSURE_HIGH_TO_LOW,
                modifier = Modifier.fillMaxWidth()
            ) { viewModel.setSortMode(MarketSortMode.BUY_PRESSURE_HIGH_TO_LOW) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortButton(
                    text = "همه بازارها",
                    selected = state.filterMode == MarketResultFilterMode.ALL,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setFilterMode(MarketResultFilterMode.ALL) }
                SortButton(
                    text = "اختلاف زیر ۱٪",
                    selected = state.filterMode == MarketResultFilterMode.SPREAD_UNDER_ONE_PERCENT,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setFilterMode(MarketResultFilterMode.SPREAD_UNDER_ONE_PERCENT) }
            }
            SortButton(
                text = "فقط بازارهای دارای نقدینگی",
                selected = state.filterMode == MarketResultFilterMode.POSITIVE_LIQUIDITY,
                modifier = Modifier.fillMaxWidth()
            ) { viewModel.setFilterMode(MarketResultFilterMode.POSITIVE_LIQUIDITY) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "خروجی اسکن",
                color = TerminalGreen,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "${visibleResults.size} نمایش  •  $filteredOutCount فیلترشده",
                color = TerminalMuted,
                fontSize = 11.sp
            )
        }

        state.errorMessage?.let { message ->
            Text(
                text = "خطا: $message",
                color = TerminalRed,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A1518), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            )
        }

        if (state.isScanning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalPanelAlt, RoundedCornerShape(10.dp))
                    .padding(9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TerminalGreen,
                    strokeWidth = 2.dp
                )
                Text(
                    "دریافت و پردازش پاسخ بازار...",
                    color = TerminalMuted,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(
                    onClick = { viewModel.cancelScan() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalRed),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text("توقف")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(
                items = visibleResults,
                key = { _, result -> "${result.symbol}_${result.success}" }
            ) { index, result ->
                MarketResultCard(
                    result = result,
                    rank = if (result.success) index + 1 else null,
                    sortMode = state.sortMode,
                    analysisState = state.analysisBySymbol[result.symbol],
                    onAnalyze = { viewModel.analyzeMarket(result.symbol) },
                    tradeFlow = state.tradeFlowBySymbol[result.symbol],
                    tradeLoading = result.symbol in state.tradeLoadingSymbols,
                    tradeError = state.tradeErrorBySymbol[result.symbol],
                    onLoadTradeFlow = { viewModel.loadRecentTradeFlow(result.symbol, forceRefresh = true) }
                )
            }
        }
    }
}

private fun formatQuoteLiquidity(value: Double): String {
    if (!value.isFinite()) return "—"
    return when {
        value >= 1_000_000_000.0 ->
            String.format(Locale.US, "%.2f B", value / 1_000_000_000.0)
        value >= 1_000_000.0 ->
            String.format(Locale.US, "%.2f M", value / 1_000_000.0)
        value >= 1_000.0 ->
            String.format(Locale.US, "%.1f K", value / 1_000.0)
        else -> String.format(Locale.US, "%.0f", value)
    }
}

@Composable
private fun MetricPanel(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .background(TerminalPanel, RoundedCornerShape(12.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = TerminalMuted, fontSize = 10.sp)
        Text(
            value,
            color = valueColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TerminalChoiceButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalGreen),
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(text, fontSize = 11.sp)
    }
}

@Composable
private fun TerminalActionButton(
    text: String,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    onClick: () -> Unit
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalGreen),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = TerminalGreen,
                contentColor = Color(0xFF062017)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SortButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(
                containerColor = TerminalGreen,
                contentColor = Color(0xFF062017)
            ),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalMuted),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(text, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MarketResultCard(
    result: MarketScanResult,
    rank: Int?,
    sortMode: MarketSortMode,
    analysisState: StrategyAnalysisUiState?,
    onAnalyze: () -> Unit,
    tradeFlow: PublicTradeFlowSummary?,
    tradeLoading: Boolean,
    tradeError: String?,
    onLoadTradeFlow: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (result.success) TerminalBorder else Color(0xFF63343A),
                shape = RoundedCornerShape(13.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) TerminalPanel else Color(0xFF241518)
        ),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    result.symbol,
                    color = TerminalText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (rank != null) {
                        Text(
                            "اولویت #$rank",
                            color = TerminalGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        if (result.success) "داده دریافت شد" else "خطا در دریافت",
                        color = if (result.success) TerminalGreen else TerminalRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (result.success && rank != null) {
                val rankingBasis = when (sortMode) {
                    MarketSortMode.BALANCED_PRIORITY ->
                        "اولویت ترکیبی: اختلاف قیمت ۵۰٪ • عمق خرید ۲۵٪ • عمق فروش ۲۵٪"
                    MarketSortMode.SPREAD_LOW_TO_HIGH ->
                        "مرتب‌شده بر اساس کمترین اختلاف قیمت"
                    MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW ->
                        "مرتب‌شده بر اساس بیشترین عمق خرید"
                    MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW ->
                        "مرتب‌شده بر اساس بیشترین عمق فروش"
                    MarketSortMode.BUY_PRESSURE_HIGH_TO_LOW ->
                        "مرتب‌شده بر اساس سهم عمق خرید از مجموع عمق خرید و فروش"
                    MarketSortMode.COMBINED_BUY_FLOW_HIGH_TO_LOW ->
                        "میانگین سهم عمق خرید و سهم ارزش خرید از معاملات عمومی اخیر؛ نمادهای بدون داده در انتها"
                }
                Text(
                    rankingBasis,
                    color = TerminalMuted,
                    fontSize = 10.sp
                )
            }

            if (!result.success) {
                Text(
                    result.error ?: "داده معتبر نیست",
                    color = TerminalRed,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DataValue("آخرین قیمت", formatNumber(result.lastPrice))
                    DataValue("بهترین خرید", formatNumber(result.bestBid))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DataValue("بهترین فروش", formatNumber(result.bestAsk))
                    DataValue(
                        "اختلاف قیمت",
                        String.format(Locale.US, "%.4f%%", result.spreadPercent)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DataValue("عمق خرید ۵ سطح", formatQuoteLiquidity(result.bidLiquidityTop5))
                    DataValue("عمق فروش ۵ سطح", formatQuoteLiquidity(result.askLiquidityTop5))
                }
                val combinedDepth = result.bidLiquidityTop5 + result.askLiquidityTop5
                val bidDepthShare = if (combinedDepth > 0.0) {
                    result.bidLiquidityTop5 / combinedDepth * 100.0
                } else {
                    Double.NaN
                }
                val depthImbalance = result.bidLiquidityTop5 - result.askLiquidityTop5
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TerminalPanelAlt, RoundedCornerShape(9.dp))
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DataValue(
                        "سهم عمق خرید",
                        bidDepthShare.takeIf { it.isFinite() }
                            ?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"
                    )
                    DataValue(
                        "برتری عمق",
                        (if (depthImbalance >= 0.0) "+" else "−") +
                            formatQuoteLiquidity(kotlin.math.abs(depthImbalance))
                    )
                }
                Text(
                    "سهم عمق خرید از مجموع ۱۰ سطح برتر دفتر سفارش؛ شاخص فشار سفارش است، نه پیش‌بینی قطعی قیمت.",
                    color = TerminalMuted,
                    fontSize = 9.sp
                )

                OutlinedButton(
                    onClick = onLoadTradeFlow,
                    enabled = !tradeLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TerminalAmber,
                        disabledContentColor = TerminalMuted
                    ),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(if (tradeLoading) "دریافت معاملات عمومی..." else "بررسی معاملات انجام‌شده")
                }
                tradeError?.let {
                    Text("خطای معاملات: $it", color = TerminalRed, fontSize = 10.sp)
                }
                tradeFlow?.let { flow ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(TerminalPanelAlt, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("جریان معاملات عمومی اخیر • ${flow.buyCount + flow.sellCount} معامله",
                            color = TerminalAmber, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DataValue("تعداد خرید", flow.buyCount.toString())
                            DataValue("تعداد فروش", flow.sellCount.toString())
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DataValue("ارزش خرید", formatQuoteLiquidity(flow.buyQuoteVolume))
                            DataValue("ارزش فروش", formatQuoteLiquidity(flow.sellQuoteVolume))
                        }
                        DataValue("خالص خرید منهای فروش",
                            (if (flow.netQuoteFlow >= 0.0) "+" else "") + formatQuoteLiquidity(flow.netQuoteFlow))
                        Text(
                            flow.latestTradeTimeMillis?.let { "آخرین معامله: " +
                                java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date(it)) }
                                ?: "معامله‌ای در پاسخ اخیر موجود نبود",
                            color = TerminalMuted, fontSize = 10.sp
                        )
                        Text("نمونه معاملات اخیر API عمومی؛ نه کل معاملات روز و نه سود حساب شخصی.",
                            color = TerminalMuted, fontSize = 9.sp)
                    }
                }

                OutlinedButton(
                    onClick = onAnalyze,
                    enabled = analysisState?.isLoading != true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TerminalGreen,
                        disabledContentColor = TerminalMuted
                    ),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        if (analysisState?.isLoading == true) {
                            "در حال تحلیل کندل‌ها..."
                        } else {
                            "تحلیل استراتژی‌ها (۱۵ دقیقه)"
                        },
                        fontSize = 12.sp
                    )
                }

                analysisState?.let { analysis ->
                    StrategyAnalysisPanel(analysis)
                }
            }
        }
    }
}

@Composable
private fun StrategyAnalysisPanel(
    state: StrategyAnalysisUiState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TerminalPanelAlt, RoundedCornerShape(10.dp))
            .border(1.dp, TerminalBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "تحلیل کندلی مستقل • بازه ۴۸ ساعت",
            color = TerminalGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        if (state.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = TerminalGreen,
                    strokeWidth = 2.dp
                )
                Text("دریافت کندل و ارزیابی استراتژی‌ها...", color = TerminalMuted, fontSize = 11.sp)
            }
        }

        state.error?.let {
            Text("خطای تحلیل: $it", color = TerminalRed, fontSize = 11.sp)
        }

        state.result?.let { analysis ->
            Text(
                "تعداد کندل دریافتی: ${analysis.candles.size}",
                color = TerminalMuted,
                fontSize = 11.sp
            )

            if (!analysis.isFetchSuccessful) {
                Text(
                    "دریافت داده ناموفق: ${analysis.fetchError}",
                    color = TerminalRed,
                    fontSize = 11.sp
                )
            } else {
                val decision = analysis.decision
                if (decision == null) {
                    Text("NO TRADE • تصمیمی در دسترس نیست", color = TerminalAmber, fontWeight = FontWeight.Bold)
                } else {
                    val directionLabel = when {
                        decision.isActionable && decision.direction.name == "LONG" -> "LONG"
                        decision.isActionable && decision.direction.name == "SHORT" -> "SHORT"
                        decision.candidateDirection?.name == "LONG" -> "نامزد LONG"
                        decision.candidateDirection?.name == "SHORT" -> "نامزد SHORT"
                        else -> "NO TRADE"
                    }
                    Text(
                        directionLabel,
                        color = if (decision.isActionable) TerminalGreen else TerminalAmber,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "توافق استراتژی‌ها: ${decision.agreeingStrategyIds.size}",
                        color = TerminalText,
                        fontSize = 11.sp
                    )
                    if (decision.isActionable) {
                        Text(
                            "میانگین اطمینان: ${String.format(Locale.US, "%.1f", decision.confidence * 100.0)}٪",
                            color = TerminalGreen,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        decision.reason,
                        color = TerminalMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Text(
            "فقط تحلیل و سیگنال؛ هیچ سفارش یا معامله‌ای ثبت نمی‌شود.",
            color = TerminalAmber,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DataValue(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = TerminalMuted, fontSize = 10.sp)
        Text(
            value,
            color = TerminalText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return "—"

    return String.format(Locale.US, "%,.8f", value)
        .trimEnd('0')
        .trimEnd('.')
        .ifEmpty { "0" }
}
