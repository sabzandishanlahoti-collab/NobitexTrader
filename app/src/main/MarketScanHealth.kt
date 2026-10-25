
package ir.nobitextrader.app.scanner

enum class MarketScanHealthStatus {
    NO_DATA,
    HEALTHY,
    PARTIAL_FAILURE,
    FAILED
}

data class MarketScanHealthReport(
    val status: MarketScanHealthStatus,
    val totalMarkets: Int,
    val successfulMarkets: Int,
    val failedMarkets: Int,
    val successRatePercent: Double
)

object MarketScanHealth {

    fun evaluate(
        results: List<MarketScanResult>
    ): MarketScanHealthReport {
        val total = results.size
        val successful = results.count { it.success }
        val failed = total - successful

        val rate = if (total == 0) {
            0.0
        } else {
            successful * 100.0 / total
        }

        val status = when {
            total == 0 ->
                MarketScanHealthStatus.NO_DATA

            successful == 0 ->
                MarketScanHealthStatus.FAILED

            failed > 0 ->
                MarketScanHealthStatus.PARTIAL_FAILURE

            else ->
                MarketScanHealthStatus.HEALTHY
        }

        return MarketScanHealthReport(
            status = status,
            totalMarkets = total,
            successfulMarkets = successful,
            failedMarkets = failed,
            successRatePercent = rate
        )
    }
}
