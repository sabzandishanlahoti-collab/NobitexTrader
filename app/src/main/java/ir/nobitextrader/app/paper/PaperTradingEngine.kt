package ir.nobitextrader.app.paper

/**
 * Local-only simulated spot broker. It never sends orders to Nobitex.
 * Buy fills use best ask; sell fills use best bid. Fees are charged on both sides.
 */
data class PaperPosition(
    val symbol: String,
    val quantity: Double,
    val averageEntryPrice: Double,
    /** Remaining quote-currency cost basis, including allocated entry fees. */
    val costBasis: Double
)

data class PaperTrade(
    val id: Long,
    val symbol: String,
    val side: PaperSide,
    val quantity: Double,
    val price: Double,
    val fee: Double,
    /** Zero for buys; net realized P/L for sells. */
    val realizedPnl: Double,
    val timestampMillis: Long
)

enum class PaperSide { BUY, SELL }

data class PaperAccount(
    val initialCash: Double,
    val cash: Double,
    val realizedPnl: Double = 0.0,
    val positions: Map<String, PaperPosition> = emptyMap(),
    val trades: List<PaperTrade> = emptyList()
) {
    init {
        require(initialCash.isFinite() && initialCash > 0.0)
        require(cash.isFinite() && cash >= -1e-8)
        require(realizedPnl.isFinite())
    }

    companion object {
        const val DEFAULT_INITIAL_CASH = 1_000_000_000.0
        fun create(initialCash: Double = DEFAULT_INITIAL_CASH) =
            PaperAccount(initialCash = initialCash, cash = initialCash)
    }
}

data class PaperPortfolioValue(
    val cash: Double,
    val positionsValue: Double,
    val unrealizedPnl: Double,
    val realizedPnl: Double,
    val totalEquity: Double,
    val unpricedPositionCount: Int
)

object PaperTradingEngine {
    const val DEFAULT_FEE_RATE = 0.001
    const val DEFAULT_MAX_POSITION_CAPITAL_PCT = 25.0
    const val DEFAULT_MAX_TOTAL_INVESTED_PCT = 80.0

    fun buy(
        account: PaperAccount,
        symbol: String,
        bestAsk: Double,
        quoteAmount: Double,
        feeRate: Double = DEFAULT_FEE_RATE,
        timestampMillis: Long = System.currentTimeMillis()
    ): PaperAccount {
        val market = normalizeSymbol(symbol)
        validateMarketPrice(bestAsk, "bestAsk")
        validateFee(feeRate)
        require(quoteAmount.isFinite() && quoteAmount > 0.0) { "Buy amount must be positive" }
        val fee = quoteAmount * feeRate
        val totalDebit = quoteAmount + fee
        require(totalDebit.isFinite()) { "Buy amount is too large" }
        require(totalDebit <= account.cash + 1e-8) { "Insufficient virtual cash" }

        val quantity = quoteAmount / bestAsk
        require(quantity.isFinite() && quantity > 0.0) { "Invalid order quantity" }
        val old = account.positions[market]
        val oldQuantity = old?.quantity ?: 0.0
        val newQuantity = oldQuantity + quantity
        val newBasis = (old?.costBasis ?: 0.0) + totalDebit
        val position = PaperPosition(
            symbol = market,
            quantity = newQuantity,
            averageEntryPrice = (newBasis / newQuantity),
            costBasis = newBasis
        )
        val trade = PaperTrade(
            id = timestampMillis,
            symbol = market,
            side = PaperSide.BUY,
            quantity = quantity,
            price = bestAsk,
            fee = fee,
            realizedPnl = 0.0,
            timestampMillis = timestampMillis
        )
        return account.copy(
            cash = (account.cash - totalDebit).coerceAtLeast(0.0),
            positions = account.positions + (market to position),
            trades = account.trades + trade
        )
    }

    /**
     * Uses an all-in budget: the fee is deducted from this amount, never added on top.
     * One symbol is capped at 25% of initial capital; total investment is capped at 80%.
     */
    fun buyWithinBudget(
        account: PaperAccount,
        symbol: String,
        bestAsk: Double,
        totalBudget: Double,
        feeRate: Double = DEFAULT_FEE_RATE,
        maxPositionCapitalPct: Double = DEFAULT_MAX_POSITION_CAPITAL_PCT,
        maxTotalInvestedPct: Double = DEFAULT_MAX_TOTAL_INVESTED_PCT,
        timestampMillis: Long = System.currentTimeMillis()
    ): PaperAccount {
        validateFee(feeRate)
        require(totalBudget.isFinite() && totalBudget > 0.0) { "Buy budget must be positive" }
        require(maxPositionCapitalPct.isFinite() && maxPositionCapitalPct in 1.0..100.0) {
            "Maximum position percentage must be between 1 and 100"
        }
        require(maxTotalInvestedPct.isFinite() && maxTotalInvestedPct in 1.0..100.0) {
            "Maximum total investment percentage must be between 1 and 100"
        }
        val market = normalizeSymbol(symbol)
        val maxPositionCost = account.initialCash * maxPositionCapitalPct / 100.0
        val existingPositionCost = account.positions[market]?.costBasis ?: 0.0
        require(existingPositionCost + totalBudget <= maxPositionCost + 1e-8) {
            "Per-symbol investment limit exceeded"
        }
        val totalInvested = account.positions.values.sumOf { it.costBasis }
        val maxTotalInvested = account.initialCash * maxTotalInvestedPct / 100.0
        require(totalInvested + totalBudget <= maxTotalInvested + 1e-8) {
            "Portfolio investment limit exceeded; cash reserve must be preserved"
        }
        require(totalBudget <= account.cash + 1e-8) { "Insufficient virtual cash including fee" }
        val quoteAmount = totalBudget / (1.0 + feeRate)
        return buy(account, market, bestAsk, quoteAmount, feeRate, timestampMillis)
    }

    fun sell(
        account: PaperAccount,
        symbol: String,
        bestBid: Double,
        quantity: Double,
        feeRate: Double = DEFAULT_FEE_RATE,
        timestampMillis: Long = System.currentTimeMillis()
    ): PaperAccount {
        val market = normalizeSymbol(symbol)
        validateMarketPrice(bestBid, "bestBid")
        validateFee(feeRate)
        require(quantity.isFinite() && quantity > 0.0) { "Sell quantity must be positive" }
        val position = account.positions[market]
            ?: throw IllegalArgumentException("No virtual position for $market")
        require(quantity <= position.quantity + 1e-12) { "Sell quantity exceeds available position" }

        val actualQuantity = quantity.coerceAtMost(position.quantity)
        val gross = actualQuantity * bestBid
        val fee = gross * feeRate
        val allocatedBasis = position.costBasis * (actualQuantity / position.quantity)
        val pnl = gross - fee - allocatedBasis
        require(gross.isFinite() && fee.isFinite() && pnl.isFinite()) { "Invalid sell calculation" }

        val remainingQuantity = position.quantity - actualQuantity
        val remainingBasis = (position.costBasis - allocatedBasis).coerceAtLeast(0.0)
        val nextPositions = if (remainingQuantity <= 1e-12) {
            account.positions - market
        } else {
            account.positions + (market to position.copy(
                quantity = remainingQuantity,
                costBasis = remainingBasis,
                averageEntryPrice = remainingBasis / remainingQuantity
            ))
        }
        val trade = PaperTrade(
            id = timestampMillis,
            symbol = market,
            side = PaperSide.SELL,
            quantity = actualQuantity,
            price = bestBid,
            fee = fee,
            realizedPnl = pnl,
            timestampMillis = timestampMillis
        )
        return account.copy(
            cash = account.cash + gross - fee,
            realizedPnl = account.realizedPnl + pnl,
            positions = nextPositions,
            trades = account.trades + trade
        )
    }

    fun value(
        account: PaperAccount,
        bestBids: Map<String, Double>,
        feeRate: Double = DEFAULT_FEE_RATE
    ): PaperPortfolioValue {
        validateFee(feeRate)
        var marketValue = 0.0
        var unrealized = 0.0
        var unpriced = 0
        account.positions.values.forEach { position ->
            val bid = bestBids[position.symbol]
            if (bid == null) {
                unpriced++
                return@forEach
            }
            validateMarketPrice(bid, "bestBid")
            val proceeds = position.quantity * bid
            marketValue += proceeds
            unrealized += proceeds - proceeds * feeRate - position.costBasis
        }
        return PaperPortfolioValue(
            cash = account.cash,
            positionsValue = marketValue,
            unrealizedPnl = unrealized,
            realizedPnl = account.realizedPnl,
            totalEquity = account.cash + marketValue - (marketValue * feeRate),
            unpricedPositionCount = unpriced
        )
    }

    private fun normalizeSymbol(symbol: String): String {
        val normalized = symbol.trim().uppercase(java.util.Locale.ROOT)
        require(normalized.matches(Regex("[A-Z0-9]{3,20}"))) { "Invalid market symbol" }
        return normalized
    }

    private fun validateMarketPrice(price: Double, field: String) {
        require(price.isFinite() && price > 0.0) { "$field must be positive and finite" }
    }

    private fun validateFee(rate: Double) {
        require(rate.isFinite() && rate in 0.0..0.05) { "Fee rate must be between 0 and 5%" }
    }
}
