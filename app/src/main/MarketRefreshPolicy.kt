
package ir.nobitextrader.app.scanner

data class MarketRefreshPolicy(
    val refreshIntervalMillis: Long = 30_000L,
    val requestTimeoutMillis: Long = 10_000L,
    val maxMarketsPerBatch: Int = 10
) {
    init {
        require(refreshIntervalMillis >= 5_000L)
        require(requestTimeoutMillis in 1_000L..60_000L)
        require(maxMarketsPerBatch in 1..50)
    }
}
