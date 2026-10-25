
package ir.nobitextrader.app.scanner

import java.util.Locale

object MarketScanFormatter {

    fun price(value: Double?): String {
        if (value == null || !value.isFinite()) {
            return "نامشخص"
        }

        return String.format(Locale.US, "%,.2f", value)
    }

    fun percent(value: Double?): String {
        if (value == null || !value.isFinite()) {
            return "نامشخص"
        }

        return String.format(Locale.US, "%.4f%%", value)
    }

    fun summary(
        summary: MarketScanSummary
    ): String {
        return "کل: ${summary.total} | " +
            "موفق: ${summary.successful} | " +
            "ناموفق: ${summary.failed} | " +
            "رتبه‌بندی‌شده: ${summary.ranked}"
    }
}
