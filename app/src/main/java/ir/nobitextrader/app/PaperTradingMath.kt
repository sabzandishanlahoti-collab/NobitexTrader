package ir.nobitextrader.app

internal data class PaperTradeCalculation(
    val cash: Double,
    val quantity: Double,
    val averageEntry: Double,
    val realizedPnl: Double,
    val fee: Double
)

internal object PaperTradingMath {
    fun calculate(
        side: String,
        oldCash: Double,
        oldQuantity: Double,
        oldAverageEntry: Double,
        quantity: Double,
        price: Double,
        feeRate: Double
    ): PaperTradeCalculation {
        require(side == "BUY" || side == "SELL") { "Invalid side" }
        require(oldCash.isFinite() && oldCash >= 0.0)
        require(oldQuantity.isFinite() && oldQuantity >= 0.0)
        require(oldAverageEntry.isFinite() && oldAverageEntry >= 0.0)
        require(quantity.isFinite() && quantity > 0.0) { "Enter a valid quantity" }
        require(price.isFinite() && price > 0.0) { "No valid market price is available" }
        require(feeRate.isFinite() && feeRate in 0.0..0.02) {
            "Fee rate must be between 0% and 2%"
        }

        val value = quantity * price
        require(value.isFinite() && value > 0.0) { "Invalid order value" }
        val fee = value * feeRate
        require(fee.isFinite()) { "Invalid fee amount" }

        if (side == "BUY") {
            val debit = value + fee
            require(debit.isFinite() && debit <= oldCash) {
                "Insufficient virtual cash including fee"
            }
            val newQuantity = oldQuantity + quantity
            require(newQuantity.isFinite() && newQuantity > 0.0)
            val newAverage = ((oldQuantity * oldAverageEntry) + value + fee) / newQuantity
            return PaperTradeCalculation(
                cash = oldCash - debit,
                quantity = newQuantity,
                averageEntry = newAverage,
                realizedPnl = 0.0,
                fee = fee
            )
        }

        require(quantity <= oldQuantity + 1e-12) { "Insufficient virtual position" }
        val newQuantity = (oldQuantity - quantity).coerceAtLeast(0.0)
        val newAverage = if (newQuantity <= 1e-12) 0.0 else oldAverageEntry
        val realized = (price - oldAverageEntry) * quantity - fee
        val newCash = oldCash + value - fee
        require(newCash.isFinite() && newCash >= -1e-8) { "Invalid virtual cash balance" }
        return PaperTradeCalculation(
            cash = newCash,
            quantity = newQuantity,
            averageEntry = newAverage,
            realizedPnl = realized,
            fee = fee
        )
    }
}
