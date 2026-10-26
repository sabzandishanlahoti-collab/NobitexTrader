
package ir.nobitextrader.app.scanner

object MarketRefreshPolicy {
    const val DEFAULT_INTERVAL_MS: Long = 60_000L
    const val MIN_INTERVAL_MS: Long = 30_000L
    const val MAX_INTERVAL_MS: Long = 300_000L

    fun normalizeInterval(intervalMs: Long): Long {
        return intervalMs.coerceIn(
            MIN_INTERVAL_MS,
            MAX_INTERVAL_MS
        )
    }
}
