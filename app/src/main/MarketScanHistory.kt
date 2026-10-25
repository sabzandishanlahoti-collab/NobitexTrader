
package ir.nobitextrader.app.scanner

data class MarketScanHistoryEntry(
    val timestampMillis: Long,
    val results: List<MarketScanResult>
)

class MarketScanHistory(
    private val maxEntries: Int = 20
) {
    init {
        require(maxEntries in 1..500)
    }

    private val entries =
        ArrayDeque<MarketScanHistoryEntry>()

    @Synchronized
    fun add(
        results: List<MarketScanResult>,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        require(timestampMillis >= 0L)

        entries.addFirst(
            MarketScanHistoryEntry(
                timestampMillis = timestampMillis,
                results = results.toList()
            )
        )

        while (entries.size > maxEntries) {
            entries.removeLast()
        }
    }

    @Synchronized
    fun latest(): MarketScanHistoryEntry? =
        entries.firstOrNull()

    @Synchronized
    fun all(): List<MarketScanHistoryEntry> =
        entries.toList()

    @Synchronized
    fun clear() {
        entries.clear()
    }

    @Synchronized
    fun size(): Int = entries.size
}
