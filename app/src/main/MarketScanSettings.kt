
package ir.nobitextrader.app.scanner

data class MarketScanSettings(
    val symbols: List<String> = MarketScanner.defaultMarkets,
    val maxMarkets: Int = 10,
    val minimumSpreadPercent: Double = 0.0
) {
    init {
        require(maxMarkets in 1..50) {
            "maxMarkets must be between 1 and 50"
        }
        require(minimumSpreadPercent >= 0.0) {
            "minimumSpreadPercent cannot be negative"
        }
    }

    fun validatedSymbols(): List<String> {
        return symbols
            .map { it.trim().uppercase() }
            .filter { it.matches(Regex("[A-Z0-9]{3,20}")) }
            .distinct()
            .take(maxMarkets)
    }
}
