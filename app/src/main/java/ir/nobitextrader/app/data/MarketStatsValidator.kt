
package ir.nobitextrader.app.data

object MarketStatsValidator {

    fun isValid(stats: MarketStats): Boolean {
        return validate(stats).isEmpty()
    }

    fun validate(stats: MarketStats): List<String> {
        val errors = mutableListOf<String>()

        if (stats.market.isBlank()) {
            errors += "Market symbol is empty"
        }

        if (!stats.latest.isFinite()) {
            errors += "Latest price is not finite"
        } else if (stats.latest <= 0.0) {
            errors += "Latest price must be greater than zero"
        }

        if (!stats.bestBuy.isFinite()) {
            errors += "Best buy price is not finite"
        } else if (stats.bestBuy <= 0.0) {
            errors += "Best buy price must be greater than zero"
        }

        if (!stats.bestSell.isFinite()) {
            errors += "Best sell price is not finite"
        } else if (stats.bestSell <= 0.0) {
            errors += "Best sell price must be greater than zero"
        }

        if (!stats.dayLow.isFinite()) {
            errors += "Daily low is not finite"
        } else if (stats.dayLow <= 0.0) {
            errors += "Daily low must be greater than zero"
        }

        if (!stats.dayHigh.isFinite()) {
            errors += "Daily high is not finite"
        } else if (stats.dayHigh <= 0.0) {
            errors += "Daily high must be greater than zero"
        }

        if (!stats.dayChange.isFinite()) {
            errors += "Day change is not finite"
        }

        if (
            stats.dayLow.isFinite() &&
            stats.dayHigh.isFinite() &&
            stats.dayLow > stats.dayHigh
        ) {
            errors += "Daily low exceeds daily high"
        }

        if (
            stats.bestBuy.isFinite() &&
            stats.bestSell.isFinite() &&
            stats.bestBuy > stats.bestSell
        ) {
            errors += "Best buy price exceeds best sell price"
        }

        return errors
    }
}
