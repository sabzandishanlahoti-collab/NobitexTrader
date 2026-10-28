
package ir.nobitextrader.app.data

object MarketStatsValidator {

    fun validate(stats: MarketStats): List<String> {
        val errors = mutableListOf<String>()

        if (stats.market.isBlank()) {
            errors += "Market name is empty"
        }

        val prices = listOf(
            "latest" to stats.latest,
            "bestBuy" to stats.bestBuy,
            "bestSell" to stats.bestSell,
            "dayLow" to stats.dayLow,
            "dayHigh" to stats.dayHigh
        )

        for ((name, value) in prices) {
            if (!value.isFinite()) {
                errors += "$name is not finite"
            } else if (value <= 0.0) {
                errors += "$name must be greater than zero"
            }
        }

        if (!stats.dayChange.isFinite()) {
            errors += "dayChange is not finite"
        }

        if (
            stats.dayLow.isFinite() &&
            stats.dayHigh.isFinite() &&
            stats.dayLow > stats.dayHigh
        ) {
            errors += "dayLow cannot exceed dayHigh"
        }

        if (
            stats.latest.isFinite() &&
            stats.dayLow.isFinite() &&
            stats.dayHigh.isFinite() &&
            stats.dayLow > 0.0 &&
            stats.dayHigh > 0.0 &&
            (stats.latest < stats.dayLow || stats.latest > stats.dayHigh)
        ) {
            errors += "latest is outside the daily low/high range"
        }

        if (
            stats.bestBuy.isFinite() &&
            stats.bestSell.isFinite() &&
            stats.bestBuy > 0.0 &&
            stats.bestSell > 0.0 &&
            stats.bestBuy > stats.bestSell
        ) {
            errors += "bestBuy cannot exceed bestSell"
        }

        return errors
    }
}
