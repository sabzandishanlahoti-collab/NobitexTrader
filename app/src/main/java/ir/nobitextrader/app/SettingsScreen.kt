
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val SETTINGS_PREFS = "nobitex_trader_settings"

data class TraderSettings(
    val useEnglish: Boolean = false,
    val darkTheme: Boolean = false,
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
            .putInt("refresh_seconds", settings.refreshSeconds.coerceIn(5, 3600))
            .putInt("timeout_seconds", settings.requestTimeoutSeconds.coerceIn(1, 60))
            .putInt("markets_per_batch", settings.maxMarketsPerBatch.coerceIn(1, 50))
            .putFloat("max_spread_percent", settings.maxSpreadPercent.coerceIn(0.1f, 10f))
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (settings.useEnglish) "Settings" else "تنظیمات",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = if (settings.useEnglish) "General" else "عمومی",
            style = MaterialTheme.typography.titleMedium
        )

        SettingSwitch(
            title = if (settings.useEnglish) "English language" else "زبان انگلیسی",
            checked = settings.useEnglish,
            onCheckedChange = {
                updateSettings(settings.copy(useEnglish = it))
            }
        )

        SettingSwitch(
            title = if (settings.useEnglish) "Dark theme preference" else "ترجیح تم تاریک",
            checked = settings.darkTheme,
            onCheckedChange = {
                updateSettings(settings.copy(darkTheme = it))
            }
        )

        Text(
            text = if (settings.useEnglish) "Exchange connection" else "اتصال به صرافی",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            if (settings.useEnglish) {
                "Request timeout: ${settings.requestTimeoutSeconds} seconds"
            } else {
                "مهلت درخواست: ${settings.requestTimeoutSeconds} ثانیه"
            }
        )

        Slider(
            value = settings.requestTimeoutSeconds.toFloat(),
            onValueChange = {
                updateSettings(settings.copy(requestTimeoutSeconds = it.toInt().coerceIn(1, 60)))
            },
            valueRange = 1f..60f,
            steps = 58
        )

        Text(
            text = if (settings.useEnglish) "Market scanner" else "اسکن بازارها",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            if (settings.useEnglish) {
                "Refresh interval: ${settings.refreshSeconds} seconds"
            } else {
                "فاصلهٔ به‌روزرسانی: ${settings.refreshSeconds} ثانیه"
            }
        )

        Slider(
            value = settings.refreshSeconds.toFloat(),
            onValueChange = {
                updateSettings(settings.copy(refreshSeconds = it.toInt().coerceIn(5, 3600)))
            },
            valueRange = 5f..300f,
            steps = 58
        )

        Text(
            if (settings.useEnglish) {
                "Markets per batch: ${settings.maxMarketsPerBatch}"
            } else {
                "تعداد بازار در هر دسته: ${settings.maxMarketsPerBatch}"
            }
        )

        Slider(
            value = settings.maxMarketsPerBatch.toFloat(),
            onValueChange = {
                updateSettings(settings.copy(maxMarketsPerBatch = it.toInt().coerceIn(1, 50)))
            },
            valueRange = 1f..50f,
            steps = 48
        )

        Text(
            if (settings.useEnglish) {
                "Maximum spread filter: ${"%.1f".format(settings.maxSpreadPercent)}%"
            } else {
                "حداکثر اختلاف قیمت برای فیلتر: ${"%.1f".format(settings.maxSpreadPercent)}٪"
            }
        )

        Slider(
            value = settings.maxSpreadPercent,
            onValueChange = {
                updateSettings(settings.copy(maxSpreadPercent = it.coerceIn(0.1f, 10f)))
            },
            valueRange = 0.1f..10f,
            steps = 98
        )

        Text(
            text = if (settings.useEnglish) "Advanced" else "پیشرفته",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = if (settings.useEnglish) {
                "These values are saved locally. They will affect scanning only after being connected to the scanner logic."
            } else {
                "این مقادیر روی دستگاه ذخیره می‌شوند. اثر آن‌ها بر اسکنر پس از اتصال به منطق اسکن اعمال خواهد شد."
            },
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedButton(
            onClick = {
                TraderSettingsStore.reset(context)
                settings = TraderSettings()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (settings.useEnglish) "Reset settings" else "بازنشانی تنظیمات")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (settings.useEnglish) "Back" else "بازگشت")
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
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
