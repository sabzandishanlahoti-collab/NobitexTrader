package ir.nobitextrader.app.strategy

/**
 * Whale-pressure signal based on real aggressive buy/sell trade flow.
 *
 * IMPORTANT:
 * - Feed this strategy actual exchange trade data.
 * - Do not infer buyer/seller aggression from candle colour.
 * - A large trade alone does not prove that a whale is accumulating.
 * - This class emits signals only; it never places orders.
 */
class WhalePressureStrategy(
    private val minimumTrades: Int = 20,
    private val minimumImbalance: Double = 0.20,
    private val minimumLargeTradeCount: Int = 1,
    private val largeTradeMultiplier: Double = 5.0,
    private val rewardRiskRatio: Double = 2.0,
    private val stopLossPercent: Double = 0.50
) : TradingStrategy {

    override val id: StrategyId = StrategyId.WHALE

    enum class Aggressor {
        BUY,
        SELL,
        UNKNOWN
    }

    data class MarketTrade(
        val timestampMillis: Long,
        val price: Double,
        val quantity: Double,
        val aggressor: Aggressor
    ) {
        fun isValid(): Boolean =
            timestampMillis >= 0L &&
                price.isFinite() && price > 0.0 &&
                quantity.isFinite() && quantity > 0.0
    }

    data class TradeFlow(
        val trades: List<MarketTrade>,
        val referencePrice: Double
    )

    private var latestFlow: TradeFlow? = null

    init {
        require(minimumTrades >= 1) {
            "minimumTrades must be positive"
        }
        require(minimumImbalance.isFinite() && minimumImbalance in 0.0..1.0) {
            "minimumImbalance must be between 0 and 1"
        }
        require(minimumLargeTradeCount >= 0) {
            "minimumLargeTradeCount cannot be negative"
        }
        require(largeTradeMultiplier.isFinite() && largeTradeMultiplier > 0.0) {
            "largeTradeMultiplier must be finite and positive"
        }
        require(rewardRiskRatio.isFinite() && rewardRiskRatio > 0.0) {
            "rewardRiskRatio must be finite and positive"
        }
        require(stopLossPercent.isFinite() && stopLossPercent > 0.0) {
            "stopLossPercent must be finite and positive"
        }
    }

    /**
     * Supply a fresh snapshot from the exchange adapter.
     * The strategy does not fetch network data by itself.
     */
    fun updateTradeFlow(flow: TradeFlow) {
        require(flow.referencePrice.isFinite() && flow.referencePrice > 0.0) {
            "Reference price must be positive and finite"
        }
        require(flow.trades.all { it.isValid() }) {
            "Trade flow contains invalid trades"
        }
        require(flow.trades.zipWithNext().all { (a, b) ->
            b.timestampMillis >= a.timestampMillis
        }) {
            "Trades must be ordered by timestamp"
        }

        latestFlow = flow
    }

    override fun evaluate(context: StrategyContext): StrategySignal {
        require(context.symbol.isNotBlank()) {
            "Symbol cannot be blank"
        }

        if (!context.enabled) {
            return flat(context.symbol, "Strategy is disabled")
        }

        val flow = latestFlow
            ?: return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INSUFFICIENT_DATA,
                details = "No real exchange trade-flow snapshot supplied"
            )

        if (flow.trades.size < minimumTrades) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INSUFFICIENT_DATA,
                referencePrice = flow.referencePrice,
                details = "Need at least $minimumTrades trades"
            )
        }

        val knownTrades = flow.trades.filter {
            it.aggressor != Aggressor.UNKNOWN
        }

        if (knownTrades.size < minimumTrades) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INSUFFICIENT_DATA,
                referencePrice = flow.referencePrice,
                details = "Not enough trades have a known aggressor"
            )
        }

        val buyVolume = knownTrades
            .filter { it.aggressor == Aggressor.BUY }
            .sumOf { it.quantity }

        val sellVolume = knownTrades
            .filter { it.aggressor == Aggressor.SELL }
            .sumOf { it.quantity }

        val totalVolume = buyVolume + sellVolume

        if (totalVolume <= 0.0 || !totalVolume.isFinite()) {
            return flat(context.symbol, "Trade volume is invalid")
        }

        val imbalance = (buyVolume - sellVolume) / totalVolume

        val averageQuantity = knownTrades
            .map { it.quantity }
            .average()

        val largeBuyCount = knownTrades.count {
            it.aggressor == Aggressor.BUY &&
                it.quantity >= averageQuantity * largeTradeMultiplier
        }

        val largeSellCount = knownTrades.count {
            it.aggressor == Aggressor.SELL &&
                it.quantity >= averageQuantity * largeTradeMultiplier
        }

        val longSetup = imbalance >= minimumImbalance &&
            largeBuyCount >= minimumLargeTradeCount

        val shortSetup = imbalance <= -minimumImbalance &&
            largeSellCount >= minimumLargeTradeCount

        if (longSetup && shortSetup) {
            return flat(context.symbol, "Conflicting trade-flow conditions")
        }

        val entry = flow.referencePrice
        val stopDistance = entry * stopLossPercent / 100.0

        if (longSetup) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.LONG,
                confidence = score(imbalance),
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = entry - stopDistance,
                takeProfitPrice = entry + stopDistance * rewardRiskRatio,
                details = "Aggressive buy imbalance=$imbalance; large buys=$largeBuyCount"
            )
        }

        if (shortSetup) {
            val target = entry - stopDistance * rewardRiskRatio
            if (target <= 0.0 || !target.isFinite()) {
                return flat(context.symbol, "Invalid short target price")
            }

            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.SHORT,
                confidence = score(-imbalance),
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = entry + stopDistance,
                takeProfitPrice = target,
                details = "Aggressive sell imbalance=$imbalance; large sells=$largeSellCount"
            )
        }

        return flat(
            context.symbol,
            "Buy/sell pressure did not meet configured thresholds"
        )
    }

    private fun score(absoluteImbalance: Double): Double =
        (0.5 + absoluteImbalance / 2.0).coerceIn(0.5, 0.95)

    private fun flat(symbol: String, details: String) = StrategySignal(
        strategyId = id,
        symbol = symbol,
        direction = TradeDirection.FLAT,
        confidence = 0.0,
        reason = SignalReason.NO_SETUP,
        details = details
    )
}
