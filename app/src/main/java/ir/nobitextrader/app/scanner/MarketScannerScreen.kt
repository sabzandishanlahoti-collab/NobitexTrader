package ir.nobitextrader.app.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "اسکنر بازار نوبیتکس",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                enabled = !state.isScanning,
                onClick = { viewModel.scanMarkets() },
                modifier = Modifier.weight(1f)
            ) {
                Text("اسکن همه بازارها")
            }

            Button(
                enabled = state.isScanning,
                onClick = { viewModel.cancelScan() },
                modifier = Modifier.weight(1f)
            ) {
                Text("توقف")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (state.isScanning) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text("در حال دریافت اردربوک بازارهای نوبیتکس...")
            }
        }

        Text("کل بازارهای دریافت‌شده: ${state.totalCount}")
        Text("بازارهای موفق: ${state.results.count { it.success }}")
        Text("بازارهای ناموفق: ${state.results.count { !it.success }}")

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.results,
                key = { it.symbol }
            ) { item ->
                MarketResultCard(item)
            }
        }
    }
}

@Composable
private fun MarketResultCard(
    item: MarketScanResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = item.symbol,
                style = MaterialTheme.typography.titleMedium
            )

            if (!item.success) {
                Text(
                    text = item.error ?: "دریافت اطلاعات ناموفق بود",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = if (item.lastPrice > 0.0) {
                        "آخرین قیمت: ${formatNumber(item.lastPrice)}"
                    } else {
                        "آخرین قیمت: در پاسخ اردربوک موجود نیست"
                    }
                )

                Text("بهترین خرید: ${formatNullable(item.bestBid)}")
                Text("بهترین فروش: ${formatNullable(item.bestAsk)}")

                Text(
                    "فاصله خرید و فروش: ${
                        item.spreadPercent?.let {
                            String.format(Locale.US, "%.4f%%", it)
                        } ?: "نامشخص"
                    }"
                )

                Text(
                    "حجم ۵ سطح خرید: ${formatNumber(item.bidLiquidityTop5)}"
                )

                Text(
                    "حجم ۵ سطح فروش: ${formatNumber(item.askLiquidityTop5)}"
                )
            }
        }
    }
}

private fun formatNullable(value: Double?): String {
    return value?.let { formatNumber(it) } ?: "نامشخص"
}

private fun formatNumber(value: Double): String {
    return String.format(Locale.US, "%,.4f", value)
}
