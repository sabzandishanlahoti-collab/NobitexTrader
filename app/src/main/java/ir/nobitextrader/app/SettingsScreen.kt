package ir.nobitextrader.app

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private const val SETTINGS_PREFS = "nobitex_trader_settings"


data class TraderSettings(
    val useEnglish: Boolean = false,
    val darkTheme: Boolean = true,
    val autoScanEnabled: Boolean = false,
    val refreshSeconds: Int = 30,
    val requestTimeoutSeconds: Int = 10,
    val maxMarketsPerBatch: Int = 10,
    val maxSpreadPercent: Float = 1f,
    val paperFeePercent: Float = 0f,
    val strategyFmcEnabled: Boolean = true,
    val strategyFvgEnabled: Boolean = true,
    val strategyLiquiditySweepEnabled: Boolean = true,
    val strategyOrderBlockEnabled: Boolean = true,
    val strategyMicroScalperEnabled: Boolean = true,
    val minimumStrategyAgreement: Int = 2
)

object TraderSettingsStore {
    private fun prefs(context: Context) =
        context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): TraderSettings {
        val p = prefs(context)
        return TraderSettings(
            useEnglish = p.getBoolean("use_english", false),
            darkTheme = p.getBoolean("dark_theme", true),
            autoScanEnabled = p.getBoolean("auto_scan_enabled", false),
            refreshSeconds = p.getInt("refresh_seconds", 30).coerceIn(5, 3600),
            requestTimeoutSeconds = p.getInt("timeout_seconds", 10).coerceIn(1, 60),
            maxMarketsPerBatch = p.getInt("markets_per_batch", 10).coerceIn(1, 50),
            maxSpreadPercent = p.getFloat("max_spread_percent", 1f).coerceIn(0.1f, 10f),
            paperFeePercent = p.getFloat("paper_fee_percent", 0f).coerceIn(0f, 2f),
            strategyFmcEnabled = p.getBoolean("strategy_fmc_enabled", true),
            strategyFvgEnabled = p.getBoolean("strategy_fvg_enabled", true),
            strategyLiquiditySweepEnabled = p.getBoolean("strategy_liquidity_sweep_enabled", true),
            strategyOrderBlockEnabled = p.getBoolean("strategy_order_block_enabled", true),
            strategyMicroScalperEnabled = p.getBoolean("strategy_micro_scalper_enabled", true),
            minimumStrategyAgreement = p.getInt("minimum_strategy_agreement", 2).coerceIn(1, 5)
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
            .putFloat("max_spread_percent", settings.maxSpreadPercent.coerceIn(0.1f, 10f))
            .putFloat("paper_fee_percent", settings.paperFeePercent.coerceIn(0f, 2f))
            .putBoolean("strategy_fmc_enabled", settings.strategyFmcEnabled)
            .putBoolean("strategy_fvg_enabled", settings.strategyFvgEnabled)
            .putBoolean("strategy_liquidity_sweep_enabled", settings.strategyLiquiditySweepEnabled)
            .putBoolean("strategy_order_block_enabled", settings.strategyOrderBlockEnabled)
            .putBoolean("strategy_micro_scalper_enabled", settings.strategyMicroScalperEnabled)
            .putInt("minimum_strategy_agreement", settings.minimumStrategyAgreement.coerceIn(1, 5))
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
    val dark = settings.darkTheme
    val scheme = AfaColorSchemes.create(dark)

    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = if (english) "CONTROL CENTER" else "مرکز کنترل",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (english) "Trading Settings" else "تنظیمات تریدر",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (english) {
                        "Configure the scanner and app preferences"
                    } else {
                        "مدیریت ظاهر برنامه و پارامترهای اسکن بازار"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(
                title = if (english) "APPEARANCE" else "ظاهر و زبان",
                subtitle = if (english) "Personalize the interface" else "شخصی‌سازی محیط برنامه",
                dark = dark
            ) {
                SettingSwitch(
                    title = if (english) "English language" else "زبان انگلیسی",
                    description = if (english) "Switch interface language" else "تغییر زبان رابط کاربری",
                    checked = settings.useEnglish,
                    onCheckedChange = { updateSettings(settings.copy(useEnglish = it)) }
                )
                SectionDivider()
                SettingSwitch(
                    title = if (english) "Dark theme" else "تم تاریک",
                    description = if (english) "Use the low-glare dark interface" else "استفاده از محیط تیره و کم‌نور",
                    checked = settings.darkTheme,
                    onCheckedChange = { updateSettings(settings.copy(darkTheme = it)) }
                )
            }

            SettingsSection(
                title = if (english) "MARKET SCANNER" else "اسکن بازار",
                subtitle = if (english) "Scanning behavior and refresh interval" else "کنترل اجرای اسکن و فاصله به‌روزرسانی",
                dark = dark
            ) {
                SettingSwitch(
                    title = if (english) "Automatic scanning" else "اسکن خودکار",
                    description = if (english) "Allow scheduled refresh cycles" else "فعال‌سازی چرخه‌های اسکن خودکار",
                    checked = settings.autoScanEnabled,
                    onCheckedChange = { updateSettings(settings.copy(autoScanEnabled = it)) }
                )
                SectionDivider()
                SettingSlider(
                    title = if (english) "Refresh interval" else "فاصله بین اسکن‌ها",
                    valueLabel = if (english) "${settings.refreshSeconds} sec" else "${settings.refreshSeconds} ثانیه",
                    value = settings.refreshSeconds.toFloat(),
                    valueRange = 5f..3600f,
                    steps = 0,
                    onValueChange = {
                        updateSettings(settings.copy(refreshSeconds = it.toInt().coerceIn(5, 3600)))
                    }
                )
                Text(
                    text = if (english) "Range: 5 seconds to 60 minutes" else "محدوده مجاز: ۵ ثانیه تا ۶۰ دقیقه",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(
                title = if (english) "NOBITEX CONNECTION" else "اتصال به نوبیتکس",
                subtitle = if (english) "Public market data request settings" else "پارامترهای دریافت داده عمومی بازار",
                dark = dark
            ) {
                SettingSlider(
                    title = if (english) "Request timeout" else "مهلت پاسخ درخواست",
                    valueLabel = if (english) "${settings.requestTimeoutSeconds} sec" else "${settings.requestTimeoutSeconds} ثانیه",
                    value = settings.requestTimeoutSeconds.toFloat(),
                    valueRange = 1f..60f,
                    steps = 58,
                    onValueChange = {
                        updateSettings(settings.copy(requestTimeoutSeconds = it.toInt().coerceIn(1, 60)))
                    }
                )
                SectionDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (english) "Market data mode" else "نوع دریافت داده",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (english) "REST snapshot" else "دریافت لحظه‌ایِ تصویری (REST Snapshot)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusTag(
                        text = if (english) "HTTP" else "HTTP",
                        dark = dark
                    )
                }
                Text(
                    text = if (english) {
                        "This label describes the current data method; it does not indicate a verified live connection."
                    } else {
                        "این برچسب روش فعلی دریافت داده را نشان می‌دهد و به معنی تأیید اتصال زنده نیست."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(
                title = if (english) "MARKET FILTERS" else "فیلترهای بازار",
                subtitle = if (english) "Selected-scan limits and market filters" else "محدودیت اسکن انتخابی و فیلترهای بازار",
                dark = dark
            ) {
                Text(
                    text = if (english) {
                        "This limit applies only when scanning selected symbols. Full-market scanning continues to retrieve all valid markets returned by Nobitex."
                    } else {
                        "این محدودیت فقط هنگام اسکن نمادهای انتخابی اعمال می‌شود. اسکن کامل بازار همچنان همه بازارهای معتبرِ برگشتی از نوبیتکس را دریافت می‌کند."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SectionDivider()
                SettingSlider(
                    title = if (english) "Maximum symbols in selected scan" else "حداکثر نمادها در اسکن انتخابی",
                    valueLabel = settings.maxMarketsPerBatch.toString(),
                    value = settings.maxMarketsPerBatch.toFloat(),
                    valueRange = 1f..50f,
                    steps = 48,
                    onValueChange = {
                        updateSettings(settings.copy(maxMarketsPerBatch = it.toInt().coerceIn(1, 50)))
                    }
                )
                SectionDivider()
                SettingSlider(
                    title = if (english) "Maximum spread" else "حداکثر اختلاف قیمت",
                    valueLabel = String.format(
                        Locale.US,
                        if (english) "%.1f%%" else "%.1f٪",
                        settings.maxSpreadPercent
                    ),
                    value = settings.maxSpreadPercent,
                    valueRange = 0.1f..10f,
                    steps = 98,
                    onValueChange = {
                        updateSettings(settings.copy(maxSpreadPercent = it.coerceIn(0.1f, 10f)))
                    }
                )
            }

            SettingsSection(
                title = if (english) "STRATEGY CONTROL" else "کنترل استراتژی‌ها",
                subtitle = if (english) "Enable or disable each strategy independently" else "هر استراتژی را مستقل روشن یا خاموش کن",
                dark = dark
            ) {
                SettingSwitch(
                    title = if (english) "FMC" else "استراتژی FMC",
                    description = if (english) "FMC candle setup evaluation" else "بررسی الگوی کندلی FMC",
                    checked = settings.strategyFmcEnabled,
                    onCheckedChange = { updateSettings(settings.copy(strategyFmcEnabled = it)) }
                )
                SectionDivider()
                SettingSwitch(
                    title = if (english) "FVG" else "استراتژی FVG",
                    description = if (english) "Fair Value Gap setup evaluation" else "بررسی ناحیه عدم تعادل قیمت",
                    checked = settings.strategyFvgEnabled,
                    onCheckedChange = { updateSettings(settings.copy(strategyFvgEnabled = it)) }
                )
                SectionDivider()
                SettingSwitch(
                    title = if (english) "Liquidity Sweep" else "جمع‌آوری نقدینگی",
                    description = if (english) "Liquidity sweep setup evaluation" else "بررسی شکار حدضرر و نقدینگی",
                    checked = settings.strategyLiquiditySweepEnabled,
                    onCheckedChange = { updateSettings(settings.copy(strategyLiquiditySweepEnabled = it)) }
                )
                SectionDivider()
                SettingSwitch(
                    title = if (english) "Order Block" else "اوردر بلاک",
                    description = if (english) "Order block setup evaluation" else "بررسی نواحی سفارش",
                    checked = settings.strategyOrderBlockEnabled,
                    onCheckedChange = { updateSettings(settings.copy(strategyOrderBlockEnabled = it)) }
                )
                SectionDivider()
                SettingSwitch(
                    title = if (english) "Micro Scalper" else "میکرو اسکالپر",
                    description = if (english) "Short-term candle setup evaluation" else "بررسی موقعیت‌های کوتاه‌مدت",
                    checked = settings.strategyMicroScalperEnabled,
                    onCheckedChange = { updateSettings(settings.copy(strategyMicroScalperEnabled = it)) }
                )
                SectionDivider()
                SettingSlider(
                    title = if (english) "Minimum agreeing strategies" else "حداقل استراتژی‌های هم‌نظر",
                    valueLabel = settings.minimumStrategyAgreement.toString(),
                    value = settings.minimumStrategyAgreement.toFloat(),
                    valueRange = 1f..5f,
                    steps = 3,
                    onValueChange = {
                        updateSettings(settings.copy(minimumStrategyAgreement = it.toInt().coerceIn(1, 5)))
                    }
                )
                Text(
                    text = if (english) {
                        "Only enabled strategies are evaluated. If all are disabled, the analysis reports that no strategy is active. Lower agreement can increase signals and risk."
                    } else {
                        "فقط استراتژی‌های روشن بررسی می‌شوند. اگر همه خاموش باشند، تحلیل علت را اعلام می‌کند. کاهش حدنصاب می‌تواند تعداد سیگنال‌ها و ریسک را افزایش دهد."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(
                title = if (english) "PAPER TRADING COSTS" else "هزینه‌های معاملات مجازی",
                subtitle = if (english) "Set the fee rate from your current exchange fee tier" else "نرخ کارمزد را مطابق سطح کارمزدی فعلی حسابت وارد کن",
                dark = dark
            ) {
                SettingSlider(
                    title = if (english) "Fee per side" else "کارمزد هر سمت معامله",
                    valueLabel = String.format(Locale.US, "%.2f%%", settings.paperFeePercent),
                    value = settings.paperFeePercent,
                    valueRange = 0f..2f,
                    steps = 199,
                    onValueChange = {
                        updateSettings(settings.copy(paperFeePercent = it.coerceIn(0f, 2f)))
                    }
                )
                Text(
                    if (settings.paperFeePercent <= 0f) {
                        if (english) "Fee is not configured. Paper BUY/SELL will remain locked." else "کارمزد تنظیم نشده؛ خرید و فروش مجازی قفل می‌ماند."
                    } else {
                        if (english) "Applied to both BUY and SELL notional. Enter the rate shown for your account; this is not fetched from Nobitex." else "این نرخ هم برای خرید و هم فروش اعمال می‌شود. نرخ حساب خودت را وارد کن؛ برنامه آن را از نوبیتکس دریافت نمی‌کند."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(
                title = if (english) "PAPER TRADING COSTS" else "هزینه‌های معاملات مجازی",
                subtitle = if (english) "Configure the local paper-ledger fee" else "تنظیم کارمزد دفتر معاملات مجازی",
                dark = dark
            ) {
                SettingSlider(
                    title = if (english) "Fee per side" else "کارمزد هر سمت معامله",
                    valueLabel = String.format(Locale.US, "%.2f%%", settings.paperFeePercent),
                    value = settings.paperFeePercent,
                    valueRange = 0f..2f,
                    steps = 199,
                    onValueChange = {
                        updateSettings(settings.copy(paperFeePercent = it.coerceIn(0f, 2f)))
                    }
                )
                Text(
                    text = if (english) {
                        "Applied separately to every buy and sell. Default is 0% until you set the rate for your account. This is a simulation setting, not a verified Nobitex fee quote."
                    } else {
                        "برای هر خرید و فروش جداگانه اعمال می‌شود. پیش‌فرض صفر است تا نرخ مربوط به حساب خودت را تعیین کنی. این تنظیم شبیه‌سازی است و نرخ قطعی کارمزد نوبیتکس نیست."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(
                title = if (english) "LOCAL STORAGE" else "ذخیره‌سازی و بازنشانی",
                subtitle = if (english) "Preferences are stored on this device" else "تنظیمات فعلی روی همین دستگاه ذخیره می‌شوند",
                dark = dark
            ) {
                Text(
                    text = if (english) {
                        "Scanner parameters affect real scanning only after the scanner reads these saved values. API credentials are not requested or stored on this screen."
                    } else {
                        "پارامترهای اسکن زمانی روی اسکن واقعی اثر دارند که موتور اسکن آن‌ها را بخواند. در این صفحه کلید API دریافت یا ذخیره نمی‌شود."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        TraderSettingsStore.reset(context)
                        updateSettings(TraderSettings())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(if (english) "Restore defaults" else "بازگردانی تنظیمات پیش‌فرض")
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (english) "Done" else "بازگشت",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    dark: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun SettingSlider(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                activeTickColor = MaterialTheme.colorScheme.onPrimaryContainer,
                inactiveTickColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun StatusTag(
    text: String,
    dark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
}
