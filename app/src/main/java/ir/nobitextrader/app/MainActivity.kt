
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ir.nobitextrader.app.data.MarketStats
import ir.nobitextrader.app.data.NobitexMarketApi

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

            var result by remember {
                mutableStateOf<MarketStats?>(null)
            }

            var loading by remember {
                mutableStateOf(false)
            }

            var error by remember {
                mutableStateOf<String?>(null)
            }

            val english = settings.useEnglish

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
                                    enabled = !loading,
                                    onClick = {
                                        scope.launch {
                                            loading = true
                                            error = null

                                            try {
                                                result = withContext(
                                                    Dispatchers.IO
                                                ) {
                                                    NobitexMarketApi
                                                        .fetchMarketStats()
                                                }
                                            } catch (e: Exception) {
                                                error = e.message
                                                    ?: if (english) {
                                                        "Unknown market data error"
                                                    } else {
                                                        "خطای نامشخص در دریافت اطلاعات"
                                                    }
                                            } finally {
                                                loading = false
                                            }
                                        }
                                    }
                                ) {
                                    Text(
                                        when {
                                            loading && english ->
                                                "Loading..."
                                            loading ->
                                                "در حال دریافت..."
                                            english ->
                                                "Fetch live BTC/IRT market data"
                                            else ->
                                                "دریافت اطلاعات واقعی BTC/IRT"
                                        }
                                    )
                                }

                                if (loading) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        if (english) {
                                            "Connecting to Nobitex..."
                                        } else {
                                            "در حال اتصال به نوبیتکس..."
                                        }
                                    )
                                }

                                result?.let { stats ->
                                    Spacer(modifier = Modifier.height(16.dp))

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

                                error?.let { message ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = message,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = if (english) {
                                        "This test only reads public market data. It does not place orders."
                                    } else {
                                        "این آزمایش فقط اطلاعات عمومی بازار را می‌خواند و سفارشی ثبت نمی‌کند."
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
