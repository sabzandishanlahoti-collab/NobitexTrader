package ir.nobitextrader.app

import ir.nobitextrader.app.scanner.MarketScanResult
import ir.nobitextrader.app.strategy.StrategyDecision
import ir.nobitextrader.app.strategy.TradeDirection

/**
 * Pure safety gate for automatic paper orders.
 *
 * It never talks to an exchange or persists a trade. It only converts a
 * confirmed strategy decision and a fresh successful order-book snapshot
 * into a bounded virtual order plan.
 */
internal object PaperAutoTradingPolicy {
    fun plan(
        decision: StrategyDecision,
        market: MarketScanResult,
        portfolio: PaperPortfolio,
        feeRate: Double,
        orderBudgetIrr: Double,
        marketObservedAtMillis: Long,
        nowMillis: Long,
        maxMarketAgeMillis: Long = DEFAULT_MAX_MARKET_AGE_MS,
        maxSpreadPercent: Double = DEFAULT_MAX_SPREAD_PERCENT
    ): PaperAutoTradePlan {
        fun reject(reason: String) = PaperAutoTradePlan.Rejected(reason)

        if (!decision.isActionable || decision.direction == TradeDirection.FLAT) {
            return reject("No actionable strategy consensus: ${decision.reason}")
        }
        if (decision.symbol != market.symbol || market.symbol.isBlank()) {
            return reject("Strategy and market symbols do not match")
        }
        if (!market.success) return reject(market.error ?: "Market data request failed")
        if (marketObservedAtMillis <= 0L || nowMillis < marketObservedAtMillis) {
            return reject("Invalid market observation timestamp")
        }
        if (nowMillis - marketObservedAtMillis > maxMarketAgeMillis) {
            return reject("Market order book is stale")
        }
        if (!market.bestBid.isFinite() || !market.bestAsk.isFinite() ||
            market.bestBid <= 0.0 || market.bestAsk < market.bestBid
        ) return reject("Invalid best bid/ask")
        if (!market.spreadPercent.isFinite() || market.spreadPercent < 0.0 ||
            market.spreadPercent > maxSpreadPercent
        ) return reject("Spread exceeds the configured safety limit")
        if (!feeRate.isFinite() || feeRate !in 0.0000000001..0.02) {
            return reject("Paper trading fee is not configured or invalid")
        }
        if (!orderBudgetIrr.isFinite() || orderBudgetIrr <= 0.0) {
            return reject("Automatic order budget must be positive")
        }

        val positionQty = portfolio.quantity(market.symbol)
        val (side, price, quantity) = when (decision.direction) {
            TradeDirection.LONG -> {
                val affordableBudget = minOf(orderBudgetIrr, portfolio.cash / (1.0 + feeRate))
                val qty = affordableBudget / market.bestAsk
                Triple("BUY", market.bestAsk, qty)
            }
            TradeDirection.SHORT -> {
                if (!positionQty.isFinite() || positionQty <= 0.0) {
                    return reject("No virtual position is available to sell")
                }
                val qty = minOf(positionQty, orderBudgetIrr / market.bestBid)
                Triple("SELL", market.bestBid, qty)
            }
            TradeDirection.FLAT -> return reject("Flat decision cannot create an order")
        }

        val value = quantity * price
        if (!quantity.isFinite() || quantity <= 0.0 ||
            !value.isFinite() || value <= 0.0
        ) return reject("Calculated virtual order quantity is invalid")
        if (side == "BUY" && value * (1.0 + feeRate) > portfolio.cash + 1e-8) {
            return reject("Insufficient virtual cash including fee")
        }
        return PaperAutoTradePlan.Approved(
            side = side,
            symbol = market.symbol,
            quantity = quantity,
            limitPrice = price,
            estimatedValueIrr = value,
            reason = "Confirmed ${decision.direction} consensus: ${decision.agreeingStrategyIds.joinToString()}"
        )
    }

    const val DEFAULT_MAX_MARKET_AGE_MS = 15_000L
    const val DEFAULT_MAX_SPREAD_PERCENT = 1.0
}

internal sealed interface PaperAutoTradePlan {
    data class Approved(
        val side: String,
        val symbol: String,
        val quantity: Double,
        val limitPrice: Double,
        val estimatedValueIrr: Double,
        val reason: String
    ) : PaperAutoTradePlan

    data class Rejected(val reason: String) : PaperAutoTradePlan
}
