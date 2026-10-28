

package ir.nobitextrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import ir.nobitextrader.app.data.MarketStats
import ir.nobitextrader.app.data.NobitexCandlesApi
import ir.nobitextrader.app.data.NobitexMarketApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppScreen {
    HOME,
    SETTINGS
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

            BackHandler(enabled = screen == AppScreen.SETTINGS) {
                settings = TraderSettingsStore.load(this@MainActivity)
                screen = AppScreen.HOME
            }

            MaterialTheme(
                colorScheme = if (settings.darkTheme) {
                    darkColorScheme()
                } else {
                    lightColorScheme()
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

                        AppScreen.HOME -> {
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
                                    text = if (english) {
                                        "Nobitex Trading Bot"
                                    } else {
                                        "ربات معاملات نوبیتکس"
                                    },
                                    fontSize = 21.sp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        screen = AppScreen.SETTINGS
                                    }
                                ) {
                                    Text(
                                        if (english) "Settings"
                                        else "تنظیمات"
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !marketLoading,
                                    onClick = {
                                        scope.launch {
                                            marketLoading = true
                                            marketError = null

                                            try {
                                                marketResult =
                                                    NobitexMarketApi.fetchMarketStats()
                                            } catch (e: Exception) {
                                                marketError =
                                                    e.message
                                                        ?: if (english) {
                                                            "Unknown market data error"
                                                        } else {
                                                            "خطای نامشخص دریافت اطلاعات بازار"
                                                        }
                                            } finally {
                                                marketLoading = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        when {
                                            marketLoading && english ->
                                                "Loading market..."
                                            marketLoading ->
                                                "دریافت اطلاعات بازار..."
                                            english ->
                                                "Fetch live BTC/IRT market data"
                                            else ->
                                                "دریافت اطلاعات واقعی BTC/IRT"
                                        }
                                    )
                                }

                                marketResult?.let { stats ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        if (english) "Market data received"
                                        else "دریافت اطلاعات بازار موفق بود"
                                    )
                                    Text("${if (english) "Market" else "بازار"}: ${stats.market}")
                                    Text("${if (english) "Last price" else "آخرین قیمت"}: ${stats.latest}")
                                    Text("${if (english) "Best buy" else "بهترین خرید"}: ${stats.bestBuy}")
                                    Text("${if (english) "Best sell" else "بهترین فروش"}: ${stats.bestSell}")
                                    Text("${if (english) "Daily change" else "تغییر روزانه"}: ${stats.dayChange}%")
                                }

                                marketError?.let { message ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !rawLoading,
                                    onClick = {
                                        scope.launch {
                                            rawLoading = true
                                            rawResponse = null
                                            rawError = null

                                            try {
                                                rawResponse =
                                                    NobitexMarketApi
                                                        .fetchRawMarketStatsResponse()
                                            } catch (e: Exception) {
                                                rawError =
                                                    e.message
                                                        ?: "Unknown raw API error"
                                            } finally {
                                                rawLoading = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        when {
                                            rawLoading && english ->
                                                "Fetching raw API response..."
                                            rawLoading ->
                                                "در حال دریافت پاسخ خام..."
                                            english ->
                                                "Show raw Nobitex API response"
                                            else ->
                                                "نمایش پاسخ خام نوبیتکس"
                                        }
                                    )
                                }

                                rawResponse?.let { response ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        if (english) {
                                            "Raw API response:"
                                        } else {
                                            "پاسخ خام API:"
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = response,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                rawError?.let { message ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (english) {
                                            "Raw API request failed: $message"
                                        } else {
                                            "خطای دریافت پاسخ خام: $message"
                                        },
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !candlesLoading,
                                    onClick = {
                                        scope.launch {
                                            candlesLoading = true
                                            candleError = null
                                            candleCount = null
                                            lastCandleTime = null

                                            try {
                                                val nowSeconds =
                                                    System.currentTimeMillis() / 1000L

                                                val candles =
                                                    NobitexCandlesApi.fetchCandles(
                                                        symbol = "BTCIRT",
                                                        resolution = "60",
                                                        fromUnixSeconds =
                                                            nowSeconds - 24L * 60L * 60L,
                                                        toUnixSeconds = nowSeconds
                                                    )

                                                candleCount = candles.size

                                                lastCandleTime =
                                                    candles.lastOrNull()?.let { candle ->
                                                        SimpleDateFormat(
                                                            "yyyy-MM-dd HH:mm:ss",
                                                            Locale.getDefault()
                                                        ).format(
                                                            Date(candle.timestampMillis)
                                                        )
                                                    }
                                            } catch (e: Exception) {
                                                candleError =
                                                    e.message
                                                        ?: if (english) {
                                                            "Unknown candle API error"
                                                        } else {
                                                            "خطای نامشخص دریافت کندل"
                                                        }
                                            } finally {
                                                candlesLoading = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        when {
                                            candlesLoading && english ->
                                                "Fetching live candles..."
                                            candlesLoading ->
                                                "در حال دریافت کندل..."
                                            english ->
                                                "Test live BTC/IRT candles"
                                            else ->
                                                "آزمایش دریافت کندل واقعی BTC/IRT"
                                        }
                                    )
                                }

                                if (candlesLoading || rawLoading) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CircularProgressIndicator()
                                }

                                candleCount?.let { count ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        if (english) {
                                            "Candle request completed"
                                        } else {
                                            "درخواست کندل پایان یافت"
                                        }
                                    )
                                    Text(
                                        if (english) {
                                            "Candles received: $count"
                                        } else {
                                            "تعداد کندل دریافتی: $count"
                                        }
                                    )

                                    lastCandleTime?.let { time ->
                                        Text(
                                            if (english) {
                                                "Latest candle time: $time"
                                            } else {
                                                "زمان آخرین کندل: $time"
                                            }
                                        )
                                    }

                                    if (count == 0) {
                                        Text(
                                            if (english) {
                                                "The API returned no candles for this interval."
                                            } else {
                                                "API برای این بازه هیچ کندلی برنگرداند."
                                            }
                                        )
                                    }
                                }

                                candleError?.let { message ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (english) {
                                            "Candle request failed: $message"
                                        } else {
                                            "خطای دریافت کندل: $message"
                                        },
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = if (english) {
                                        "Read-only tests. No orders are placed."
                                    } else {
                                        "این آزمایش‌ها فقط اطلاعات بازار و کندل را می‌خوانند و سفارشی ثبت نمی‌کنند."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

                                                val candles =
                                                    NobitexCandlesApi.fetchCandles(
                                                        symbol = "BTCIRT",
                                                        resolution = "60",
                                                        fromUnixSeconds =
                                                            nowSeconds - 24L * 60L * 60L,
                                                        toUnixSeconds = nowSeconds
                                                    )

                                                candleCount = candles.size

                                                lastCandleTime =
                                                    candles.lastOrNull()?.let { candle ->
                                                        SimpleDateFormat(
                                                            "yyyy-MM-dd HH:mm:ss",
                                                            Locale.getDefault()
                                                        ).format(
                                                            Date(candle.timestampMillis)
                                                        )
                                                    }
                                            } catch (e: Exception) {
                                                candleError =
                                                    e.message
                                                        ?: if (english) {
                                                            "Unknown candle API error"
                                                        } else {
                                                            "خطای نامشخص دریافت کندل"
                                                        }
                                            } finally {
                                                candlesLoading = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        when {
                                            candlesLoading && english ->
                                                "Fetching live candles..."
                                            candlesLoading ->
                                                "در حال دریافت کندل..."
                                            english ->
                                                "Test live BTC/IRT candles"
                                            else ->
                                                "آزمایش دریافت کندل واقعی BTC/IRT"
                                        }
                                    )
                                }

                                if (candlesLoading) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CircularProgressIndicator()
                                }

                                candleCount?.let { count ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        if (english) {
                                            "Candle request completed"
                                        } else {
                                            "درخواست کندل پایان یافت"
                                        }
                                    )
                                    Text(
                                        if (english) {
                                            "Candles received: $count"
                                        } else {
                                            "تعداد کندل دریافتی: $count"
                                        }
                                    )

                                    lastCandleTime?.let { time ->
                                        Text(
                                            if (english) {
                                                "Latest candle time: $time"
                                            } else {
                                                "زمان آخرین کندل: $time"
                                            }
                                        )
                                    }

                                    if (count == 0) {
                                        Text(
                                            if (english) {
                                                "The API returned no candles for this interval."
                                            } else {
                                                "API برای این بازه هیچ کندلی برنگرداند."
                                            }
                                        )
                                    }
                                }

                                candleError?.let { message ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (english) {
                                            "Candle request failed: $message"
                                        } else {
                                            "خطای دریافت کندل: $message"
                                        },
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = if (english) {
                                        "Read-only tests. No orders are placed."
                                    } else {
                                        "این آزمایش‌ها فقط اطلاعات بازار و کندل را می‌خوانند و سفارشی ثبت نمی‌کنند."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
