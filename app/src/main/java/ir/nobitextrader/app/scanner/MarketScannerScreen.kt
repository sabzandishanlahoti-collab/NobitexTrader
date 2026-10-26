
package ir.nobitextrader.app.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "اسکن بازارهای نوبیتکس",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "نتایج از داده‌های دریافتی API نمایش داده می‌شوند؛ " +
                "بازارهای ناموفق جداگانه مشخص می‌شوند.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                enabled = !state.isScanning,
                onClick = {
                    viewModel.scanMarkets()
                }
            ) {
                Text(
                    if (state.isScanning) {
                        "در حال اسکن..."
                    } else {
                        "اسکن همه بازارها"
                    }
                )
            }

            OutlinedButton(
                enabled = !state.isScanning &&
                    state.results.isNotEmpty(),
                onClick = {
                    viewModel.clearResults()
                }
            ) {
                Text("پاک‌کردن")
            }
        }

        if (state.isScanning) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator()
                Text("در حال دریافت دفتر سفارش بازارها...")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val successful = state.results.count { it.success }
        val failed = state.results.count { !it.success }

        Text("تعداد نتایج: ${state.results.size}")
        Text("موفق: $successful   ناموفق: $failed")

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.results,
                key = { it.symbol }
            ) { result ->
                MarketResultCard(result)
            }
        }
    }
}

@Composable
private fun MarketResultCard(
    result: MarketScanResult
) {
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
                .padding(12.dp)
        ) {
            Text(
                text = result.symbol,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = if (result.success) {
                    "وضعیت: دریافت موفق دفتر سفارش"
                } else {
                    "وضعیت: ناموفق"
                },
                style = MaterialTheme.typography.bodyMedium
            )

            if (result.success) {
                Text(
                    text = "آخرین قیمت: ${
                        formatNumber(result.lastPrice)
                    }"
                )

                Text(
                    text = "بهترین خرید: ${
                        result.bestBid?.let(::formatNumber) ?: "ناموجود"
                    }"
                )

                Text(
                    text = "بهترین فروش: ${
                        result.bestAsk?.let(::formatNumber) ?: "ناموجود"
                    }"
                )

                Text(
                    text = "اختلاف قیمت: ${
                        result.spreadPercent?.let {
                            String.format(Locale.US, "%.4f%%", it)
                        } ?: "ناموجود"
                    }"
                )

                Text(
                    text = "حجم خرید ۵ سطح اول: ${
                        formatNumber(result.bidLiquidityTop5)
                    }"
                )

                Text(
                    text = "حجم فروش ۵ سطح اول: ${
                        formatNumber(result.askLiquidityTop5)
                    }"
                )
            }

            result.error?.let { message ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "علت: $message",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatNumber(value: Double): String {
    return String.format(Locale.US, "%,.8f", value)
        .trimEnd('0')
        .trimEnd('.')
}
