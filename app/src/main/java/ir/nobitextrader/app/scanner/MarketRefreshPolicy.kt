package ir.nobitextrader.app.scanner

data class MarketRefreshPolicy(
    val refreshIntervalMillis: Long = 30_000L,
    val requestTimeoutMillis: Long = 10_000L,
    val maxMarketsPerBatch: Int = 10
) {
    init {
        require(refreshIntervalMillis in MIN_REFRESH_INTERVAL_MILLIS..MAX_REFRESH_INTERVAL_MILLIS) {
            "refreshIntervalMillis must be between 5 seconds and 1 hour"
        }

        require(requestTimeoutMillis in MIN_REQUEST_TIMEOUT_MILLIS..MAX_REQUEST_TIMEOUT_MILLIS) {
            "requestTimeoutMillis must be between 1 and 60 seconds"
        }

        require(maxMarketsPerBatch in MIN_MARKETS_PER_BATCH..MAX_MARKETS_PER_BATCH) {
            "maxMarketsPerBatch must be between 1 and 50"
        }
    }

    companion object {
        const val MIN_REFRESH_INTERVAL_MILLIS = 5_000L
        const val MAX_REFRESH_INTERVAL_MILLIS = 3_600_000L

        const val MIN_REQUEST_TIMEOUT_MILLIS = 1_000L
        const val MAX_REQUEST_TIMEOUT_MILLIS = 60_000L

        const val MIN_MARKETS_PER_BATCH = 1
        const val MAX_MARKETS_PER_BATCH = 50

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
