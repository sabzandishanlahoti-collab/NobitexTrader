package ir.nobitextrader.app.scanner

data class MarketRefreshPolicy(
    val refreshIntervalMillis: Long = DEFAULT_INTERVAL_MS,
    val requestTimeoutMillis: Long = 10_000L,
    val maxMarketsPerBatch: Int = 10
) {
    init {
        require(refreshIntervalMillis in MIN_REFRESH_INTERVAL_MS..MAX_REFRESH_INTERVAL_MS) {
            "refreshIntervalMillis must be between 5 seconds and 1 hour"
        }
        require(requestTimeoutMillis in 1_000L..60_000L) {
            "requestTimeoutMillis must be between 1 and 60 seconds"
        }
        require(maxMarketsPerBatch in 1..50) {
            "maxMarketsPerBatch must be between 1 and 50"
        }
    }

    companion object {
        const val DEFAULT_INTERVAL_MS = 30_000L
        const val MIN_REFRESH_INTERVAL_MS = 5_000L
        const val MAX_REFRESH_INTERVAL_MS = 3_600_000L

        /**
         * Keeps a supplied refresh interval within the supported range.
         */
        fun normalizeInterval(intervalMillis: Long): Long {
            return intervalMillis.coerceIn(
                MIN_REFRESH_INTERVAL_MS,
                MAX_REFRESH_INTERVAL_MS
            )
        }

        val DEFAULT = MarketRefreshPolicy()

        fun fromRefreshSeconds(
            refreshSeconds: Long,
            requestTimeoutMillis: Long = DEFAULT.requestTimeoutMillis,
            maxMarketsPerBatch: Int = DEFAULT.maxMarketsPerBatch
        ): MarketRefreshPolicy {
            require(refreshSeconds <= Long.MAX_VALUE / 1_000L) {
                "refreshSeconds is too large"
            }

            return MarketRefreshPolicy(
                refreshIntervalMillis = refreshSeconds * 1_000L,
                requestTimeoutMillis = requestTimeoutMillis,
                maxMarketsPerBatch = maxMarketsPerBatch
            )
        }
    }
}
