package ir.nobitextrader.app.strategy

/**
 * Liquidity Sweep strategy.
 *
 * LONG:
 * Price sweeps a previous swing low and closes back above that low.
 *
 * SHORT:
 * Price sweeps a previous swing high and closes back below that high.
 *
 * Uses closed candles supplied by the caller.
 * This strategy produces signals only; it does not place orders.
 */
class LiquiditySweepStrategy(
    private val lookbackCandles: Int = 10,
    private val minimumSweepPercent: Double = 0.02,
    private val rewardRiskRatio: Double = 2.0
) : TradingStrategy {

    override val id: StrategyId = StrategyId.LIQUIDITY_SWEEP

    init {
        require(lookbackCandles >= 2) {
            "lookbackCandles must be at least 2"
        }
        require(
            minimumSweepPercent.isFinite() &&
                minimumSweepPercent >= 0.0
        ) {
            "minimumSweepPercent must be finite and non-negative"
        }
        require(
            rewardRiskRatio.isFinite() &&
                rewardRiskRatio > 0.0
        ) {
            "rewardRiskRatio must be finite and positive"
        }
    }

    override fun evaluate(context: StrategyContext): StrategySignal {
        require(context.symbol.isNotBlank()) {
            "Symbol cannot be blank"
        }

        if (!context.enabled) {
            return flat(context.symbol, "Strategy is disabled")
        }

        val candles = context.candles

        if (candles.size < lookbackCandles + 1) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INSUFFICIENT_DATA,
                details = "Need $lookbackCandles prior candles and one latest candle"
            )
        }

        if (candles.any { !it.isValid() }) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INVALID_MARKET_DATA,
                details = "Invalid OHLCV candle"
            )
        }

        if (candles.zipWithNext().any { (a, b) ->
                b.timestampMillis <= a.timestampMillis
            }
        ) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INVALID_MARKET_DATA,
                details = "Candles must be strictly chronological"
            )
        }

        val latest = candles.last()
        val previous = candles.dropLast(1).takeLast(lookbackCandles)

        val priorLow = previous.minOf { it.low }
        val priorHigh = previous.maxOf { it.high }

        val sweptLow = latest.low < priorLow &&
            latest.close > priorLow

        val sweptHigh = latest.high > priorHigh &&
            latest.close < priorHigh

        val lowSweepPercent =
            (priorLow - latest.low) / priorLow * 100.0

        val highSweepPercent =
            (latest.high - priorHigh) / priorHigh * 100.0

        val validBullishSweep =
            sweptLow && lowSweepPercent >= minimumSweepPercent

        val validBearishSweep =
            sweptHigh && highSweepPercent >= minimumSweepPercent

        // If both conditions occur on the same candle, direction is ambiguous.
        if (validBullishSweep && validBearishSweep) {
            return flat(
                context.symbol,
                "Both-side sweep detected; direction is ambiguous"
            )
        }

        if (validBullishSweep) {
            val entry = latest.close
            val stopLoss = latest.low
            val risk = entry - stopLoss

            if (risk <= 0.0 || !risk.isFinite()) {
                return flat(context.symbol, "Invalid bullish sweep risk")
            }

            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.LONG,
                confidence = confidenceFor(lowSweepPercent),
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = stopLoss,
                takeProfitPrice = entry + risk * rewardRiskRatio,
                details = "Bullish liquidity sweep; depth=${lowSweepPercent}%"
            )
        }

        if (validBearishSweep) {
            val entry = latest.close
            val stopLoss = latest.high
            val risk = stopLoss - entry

            if (risk <= 0.0 || !risk.isFinite()) {
                return flat(context.symbol, "Invalid bearish sweep risk")
            }

            val takeProfit = entry - risk * rewardRiskRatio

            if (takeProfit <= 0.0 || !takeProfit.isFinite()) {
                return flat(context.symbol, "Invalid bearish target price")
            }

            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.SHORT,
                confidence = confidenceFor(highSweepPercent),
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = stopLoss,
                takeProfitPrice = takeProfit,
                details = "Bearish liquidity sweep; depth=${highSweepPercent}%"
            )
        }

        return flat(
            context.symbol,
            "No qualifying liquidity sweep in the latest candle"
        )
    }

    private fun confidenceFor(sweepPercent: Double): Double {
        // Setup-strength score only; it is not a win probability.
        return (0.5 + sweepPercent / 5.0).coerceIn(0.5, 0.9)
    }

    private fun flat(symbol: String, details: String): StrategySignal {
        return StrategySignal(
            strategyId = id,
            symbol = symbol,
            direction = TradeDirection.FLAT,
            confidence = 0.0,
            reason = SignalReason.NO_SETUP,
            details = details
        )
    }
}
