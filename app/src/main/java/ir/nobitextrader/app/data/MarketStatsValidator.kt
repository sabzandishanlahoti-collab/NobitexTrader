
package ir.nobitextrader.app.data

object MarketStatsValidator {

    fun validate(stats: MarketStats): List<String> {
        val errors = mutableListOf<String>()

        if (stats.market.isBlank()) {
            errors += "Market symbol is empty"
        }

        if (!stats.latest.isFinite() || stats.latest <= 0.0) {
            errors += "Latest price must be finite and positive"
        }

        if (!stats.bestBuy.isFinite() || stats.bestBuy <= 0.0) {
            errors += "Best buy price must be finite and positive"
        }

        if (!stats.bestSell.isFinite() || stats.bestSell <= 0.0) {
            errors += "Best sell price must be finite and positive"
        }

        if (stats.bestBuy.isFinite() &&
            stats.bestSell.isFinite() &&
            stats.bestBuy > 0.0 &&
            stats.bestSell > 0.0 &&
            stats.bestBuy > stats.bestSell
        ) {
            errors += "Best buy price exceeds best sell price"
        }

        if (!stats.dayLow.isFinite() || stats.dayLow <= 0.0) {
            errors += "Daily low must be finite and positive"
        }

        if (!stats.dayHigh.isFinite() || stats.dayHigh <= 0.0) {
            errors += "Daily high must be finite and positive"
        }

        val validDailyRange =
            stats.dayLow.isFinite() &&
            stats.dayHigh.isFinite() &&
            stats.dayLow > 0.0 &&
            stats.dayHigh >= stats.dayLow

        if (stats.dayLow.isFinite() &&
            stats.dayHigh.isFinite() &&
            stats.dayLow > 0.0 &&
            stats.dayHigh > 0.0 &&
            stats.dayLow > stats.dayHigh
        ) {
            errors += "Daily low exceeds daily high"
        }

        if (validDailyRange &&
            stats.latest.isFinite() &&
            stats.latest > 0.0 &&
            (stats.latest < stats.dayLow ||
             stats.latest > stats.dayHigh)
        ) {
            errors += "Latest price is outside the daily range"
        }

        if (!stats.dayChange.isFinite()) {
            errors += "Daily change must be finite"
        }

        return errors
    }

    fun isValid(stats: MarketStats): Boolean =
        validate(stats).isEmpty()
}
