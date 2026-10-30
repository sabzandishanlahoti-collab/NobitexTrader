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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedButtonDefaults
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
    val successfulCount = visibleResults.count { it.success }
    val failedCount = visibleResults.count { !it.success }
    val filteredOutCount =
        (state.totalCount - visibleResults.size).coerceAtLeast(0)

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
                colors = OutlinedButtonDefaults.colors(
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
                    colors = OutlinedButtonDefaults.colors(contentColor = TerminalRed),
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
            items(
                items = visibleResults,
                key = { "${it.symbol}_${it.success}" }
            ) { result ->
                MarketResultCard(result)
            }
        }
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
        colors = OutlinedButtonDefaults.colors(contentColor = TerminalGreen),
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
            colors = OutlinedButtonDefaults.colors(contentColor = TerminalGreen),
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
            colors = OutlinedButtonDefaults.colors(contentColor = TerminalMuted),
            shape = RoundedCornerShape(9.dp)
        ) {
            Text(text, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MarketResultCard(
    result: MarketScanResult
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
                Text(
                    if (result.success) "داده دریافت شد" else "خطا در دریافت",
                    color = if (result.success) TerminalGreen else TerminalRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
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
                    DataValue("عمق خرید ۵ سطح", formatNumber(result.bidLiquidityTop5))
                    DataValue("عمق فروش ۵ سطح", formatNumber(result.askLiquidityTop5))
                }
            }
        }
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
