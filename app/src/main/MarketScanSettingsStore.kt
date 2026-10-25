
package ir.nobitextrader.app.scanner

import android.content.Context

class MarketScanSettingsStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        "market_scan_settings",
        Context.MODE_PRIVATE
    )

    fun getRefreshIntervalMillis(): Long =
        preferences.getLong("refresh_interval_ms", 30_000L)

    fun setRefreshIntervalMillis(value: Long) {
        require(value in 5_000L..3_600_000L)
        preferences.edit()
            .putLong("refresh_interval_ms", value)
            .apply()
    }

    fun getMinimumSpreadPercent(): Double =
        preferences.getString("minimum_spread_percent", "0.0")
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?: 0.0

    fun setMinimumSpreadPercent(value: Double) {
        require(value.isFinite() && value >= 0.0)
        preferences.edit()
            .putString("minimum_spread_percent", value.toString())
            .apply()
    }

    fun getMaximumMarkets(): Int =
        preferences.getInt("maximum_markets", 10)
            .coerceIn(1, 50)

    fun setMaximumMarkets(value: Int) {
        require(value in 1..50)
        preferences.edit()
            .putInt("maximum_markets", value)
            .apply()
    }
}
