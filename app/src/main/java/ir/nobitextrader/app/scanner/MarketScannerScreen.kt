
package ir.nobitextrader.app.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun MarketScannerScreen(
    viewModel: MarketScannerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "اسکن بازار نوبیتکس",
            style = MaterialTheme.typography.titleLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.scanMarkets() },
                enabled = !state.isScanning,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.isScanning) "در حال اسکن..." else "اسکن همه بازارها")
            }

            OutlinedButton(
                onClick = { viewModel.clearResults() },
                enabled = !state.isScanning && state.results.isNotEmpty()
            ) {
                Text("پاک‌کردن")
            }
        }

        Text(
            text = "مرتب‌سازی نتایج:",
            style = MaterialTheme.typography.titleMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortButton(
                    text = "ترکیبی",
                    selected = state.sortMode == MarketSortMode.BALANCED_PRIORITY,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setSortMode(MarketSortMode.BALANCED_PRIORITY)
                }

                SortButton(
                    text = "اختلاف کمتر",
                    selected = state.sortMode == MarketSortMode.SPREAD_LOW_TO_HIGH,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setSortMode(MarketSortMode.SPREAD_LOW_TO_HIGH)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortButton(
                    text = "تقاضای بیشتر",
                    selected = state.sortMode ==
                        MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setSortMode(
                        MarketSortMode.BID_LIQUIDITY_HIGH_TO_LOW
                    )
                }

                SortButton(
                    text = "عرضه بیشتر",
                    selected = state.sortMode ==
                        MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW,
                    modifier = Modifier.weight(1f)
                ) {
                    viewModel.setSortMode(
                        MarketSortMode.ASK_LIQUIDITY_HIGH_TO_LOW
                    )
                }
            }
        }

        Text(
            text = "کل: ${state.totalCount} | موفق: ${
                MarketScanner.successfulCount(state.results)
            } | ناموفق: ${MarketScanner.failedCount(state.results)}"
        )

        state.errorMessage?.let { message ->
            Text(
                text = "خطا: $message",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.isScanning) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator()
                Text("در حال دریافت اطلاعات بازار...")
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { viewModel.cancelScan() }) {
                    Text("توقف")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.results,
                key = { "${it.symbol}_${it.success}" }
            ) { result ->
                MarketResultCard(result)
            }
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
        Button(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    }
}

@Composable
private fun MarketResultCard(result: MarketScanResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = result.symbol,
                style = MaterialTheme.typography.titleMedium
            )

            if (!result.success) {
                Text(
                    text = result.error ?: "داده معتبر نیست",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text("آخرین قیمت: ${formatNumber(result.lastPrice)}")
                Text("بهترین خرید: ${formatNumber(result.bestBid)}")
                Text("بهترین فروش: ${formatNumber(result.bestAsk)}")
                Text(
                    "اختلاف قیمت: ${
                        String.format(Locale.US, "%.4f", result.spreadPercent)
                    }٪"
                )
                Text(
                    "نقدینگی ۵ سفارش خرید: ${
                        formatNumber(result.bidLiquidityTop5)
                    }"
                )
                Text(
                    "نقدینگی ۵ سفارش فروش: ${
                        formatNumber(result.askLiquidityTop5)
                    }"
                )
            }
        }
    }
}

private fun formatNumber(value: Double): String {
    if (!value.isFinite()) return "—"

    return String.format(Locale.US, "%,.8f", value)
        .trimEnd('0')
        .trimEnd('.')
        .ifEmpty { "0" }
}
