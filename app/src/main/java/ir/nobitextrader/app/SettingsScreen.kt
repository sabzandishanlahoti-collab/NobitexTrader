
package ir.nobitextrader.app

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

private const val SETTINGS_PREFS = "nobitex_trader_settings"

data class TraderSettings(
    val useEnglish: Boolean = false,
    val darkTheme: Boolean = false,
    val autoScanEnabled: Boolean = false,
    val refreshSeconds: Int = 30,
    val requestTimeoutSeconds: Int = 10,
    val maxMarketsPerBatch: Int = 10,
    val maxSpreadPercent: Float = 1f
)

object TraderSettingsStore {
    private fun prefs(context: Context) =
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): TraderSettings {
        val p = prefs(context)
        return TraderSettings(
            useEnglish = p.getBoolean("use_english", false),
            darkTheme = p.getBoolean("dark_theme", false),
            autoScanEnabled = p.getBoolean("auto_scan_enabled", false),
            refreshSeconds = p.getInt("refresh_seconds", 30).coerceIn(5, 3600),
            requestTimeoutSeconds = p.getInt("timeout_seconds", 10).coerceIn(1, 60),
            maxMarketsPerBatch = p.getInt("markets_per_batch", 10).coerceIn(1, 50),
            maxSpreadPercent = p.getFloat("max_spread_percent", 1f).coerceIn(0.1f, 10f)
        )
    }

    fun save(context: Context, settings: TraderSettings) {
        prefs(context).edit()
            .putBoolean("use_english", settings.useEnglish)
            .putBoolean("dark_theme", settings.darkTheme)
            .putBoolean("auto_scan_enabled", settings.autoScanEnabled)
            .putInt("refresh_seconds", settings.refreshSeconds.coerceIn(5, 3600))
            .putInt("timeout_seconds", settings.requestTimeoutSeconds.coerceIn(1, 60))
            .putInt("markets_per_batch", settings.maxMarketsPerBatch.coerceIn(1, 50))
            .putFloat(
                "max_spread_percent",
                settings.maxSpreadPercent.coerceIn(0.1f, 10f)
            )
            .apply()
    }

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var settings by remember {
        mutableStateOf(TraderSettingsStore.load(context))
    }

    fun updateSettings(updated: TraderSettings) {
        settings = updated
        TraderSettingsStore.save(context, updated)
    }

    val english = settings.useEnglish

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (english) "Trading Bot Settings" else "تنظیمات ربات معامله‌گر",
            style = MaterialTheme.typography.headlineMedium
        )

        HorizontalDivider()

        Text(
            text = if (english) "Appearance" else "ظاهر برنامه",
            style = MaterialTheme.typography.titleMedium
        )

        SettingSwitch(
            title = if (english) "English language" else "زبان انگلیسی",
            checked = settings.useEnglish,
            onCheckedChange = {
                updateSettings(settings.copy(useEnglish = it))
            }
        )

        SettingSwitch(
            title = if (english) "Dark theme" else "تم تاریک",
            checked = settings.darkTheme,
            onCheckedChange = {
                updateSettings(settings.copy(darkTheme = it))
            }
        )

        HorizontalDivider()

        Text(
            text = if (english) "Automatic Scanning" else "اسکن خودکار",
            style = MaterialTheme.typography.titleMedium
        )

        SettingSwitch(
            title = if (english) "Enable automatic scanning" else "فعال‌سازی اسکن خودکار",
            checked = settings.autoScanEnabled,
            onCheckedChange = {
                updateSettings(settings.copy(autoScanEnabled = it))
            }
        )

        Text(
            text = if (english) {
                "Refresh interval: ${settings.refreshSeconds} seconds"
            } else {
                "فاصله بین اسکن‌ها: ${settings.refreshSeconds} ثانیه"
            }
        )

        Slider(
            value = settings.refreshSeconds.toFloat(),
            onValueChange = {
                updateSettings(
                    settings.copy(
                        refreshSeconds = it.toInt().coerceIn(5, 3600)
                    )
                )
            },
            valueRange = 5f..3600f,
            steps = 0
        )

        HorizontalDivider()

        Text(
            text = if (english) "Nobitex Connection" else "اتصال به نوبیتکس",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = if (english) {
                "Request timeout: ${settings.requestTimeoutSeconds} seconds"
            } else {
                "مهلت پاسخ درخواست: ${settings.requestTimeoutSeconds} ثانیه"
            }
        )

        Slider(
            value = settings.requestTimeoutSeconds.toFloat(),
            onValueChange = {
                updateSettings(
                    settings.copy(
                        requestTimeoutSeconds = it.toInt().coerceIn(1, 60)
                    )
                )
            },
            valueRange = 1f..60f,
            steps = 58
        )

        HorizontalDivider()

        Text(
            text = if (english) "Market Filters" else "فیلترهای بازار",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = if (english) {
                "Markets per batch: ${settings.maxMarketsPerBatch}"
            } else {
                "تعداد بازار در هر دسته: ${settings.maxMarketsPerBatch}"
            }
        )

        Slider(
            value = settings.maxMarketsPerBatch.toFloat(),
            onValueChange = {
                updateSettings(
                    settings.copy(
                        maxMarketsPerBatch = it.toInt().coerceIn(1, 50)
                    )
                )
            },
            valueRange = 1f..50f,
            steps = 48
        )

        Text(
            text = if (english) {
                String.format(
                    Locale.US,
                    "Maximum spread: %.1f%%",
                    settings.maxSpreadPercent
                )
            } else {
                String.format(
                    Locale.US,
                    "حداکثر اختلاف قیمت: %.1f٪",
                    settings.maxSpreadPercent
                )
            }
        )

        Slider(
            value = settings.maxSpreadPercent,
            onValueChange = {
                updateSettings(
                    settings.copy(
                        maxSpreadPercent = it.coerceIn(0.1f, 10f)
                    )
                )
            },
            valueRange = 0.1f..10f,
            steps = 98
        )

        HorizontalDivider()

        Text(
            text = if (english) "Settings Information" else "وضعیت تنظیمات",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = if (english) {
                "Settings are saved on this device. Options affect actual scanning only when the scanner reads these saved values."
            } else {
                "تنظیمات روی دستگاه ذخیره می‌شوند. برای اثرگذاری روی اسکن واقعی، موتور اسکن باید این مقادیر را بخواند."
            },
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedButton(
            onClick = {
                TraderSettingsStore.reset(context)
                updateSettings(TraderSettings())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (english) "Reset to defaults" else "بازگردانی تنظیمات پیش‌فرض")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (english) "Back" else "بازگشت")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
