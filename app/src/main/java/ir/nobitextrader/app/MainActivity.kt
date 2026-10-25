
package ir.nobitextrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ir.nobitextrader.app.data.MarketStats
import ir.nobitextrader.app.data.NobitexMarketApi

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val result = remember {
                    mutableStateOf<MarketStats?>(null)
                }
                val loading = remember {
                    mutableStateOf(false)
                }
                val error = remember {
                    mutableStateOf<String?>(null)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NobitexTrader",
                            fontSize = 28.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "ربات معاملات نوبیتکس",
                            fontSize = 21.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            enabled = !loading.value,
                            onClick = {
                                scope.launch {
                                    loading.value = true
                                    error.value = null

                                    try {
                                        result.value = withContext(
                                            Dispatchers.IO
                                        ) {
                                            NobitexMarketApi
                                                .fetchMarketStats()
                                        }
                                    } catch (e: Exception) {
                                        error.value =
                                            e.message
                                                ?: "خطای نامشخص در دریافت اطلاعات"
                                    } finally {
                                        loading.value = false
                                    }
                                }
                            }
                        ) {
                            Text(
                                if (loading.value)
                                    "در حال دریافت..."
                                else
                                    "دریافت قیمت واقعی BTC/IRT"
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (loading.value) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("در حال اتصال به نوبیتکس...")
                        }

                        result.value?.let { stats ->
                            Text(
                                text = "وضعیت دریافت: موفق",
                                fontSize = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("بازار: ${stats.market}")
                            Text("آخرین قیمت: ${stats.latest} ریال")
                            Text("بهترین قیمت خرید: ${stats.bestBuy} ریال")
                            Text("بهترین قیمت فروش: ${stats.bestSell} ریال")
                            Text("کمترین قیمت روز: ${stats.dayLow} ریال")
                            Text("بیشترین قیمت روز: ${stats.dayHigh} ریال")
                            Text("تغییر روزانه: ${stats.dayChange}%")
                            Text(
                                if (stats.isClosed)
                                    "وضعیت بازار: بسته"
                                else
                                    "وضعیت بازار: باز"
                            )
                        }

                        error.value?.let { message ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "دریافت اطلاعات ناموفق بود:\n$message",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "در این مرحله فقط اطلاعات عمومی بازار خوانده می‌شود؛ سفارشی ثبت نمی‌شود.",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
