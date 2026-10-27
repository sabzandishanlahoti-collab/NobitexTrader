package ir.nobitextrader.app.strategy

/**
 * Fair Value Gap (FVG) strategy.
 *
 * Assumption: context.candles contains closed candles in chronological order.
 * This class only generates a signal; it does not place orders.
 *
 * Bullish FVG:
 *     latest.low > first.high
 *
 * Bearish FVG:
 *     latest.high < first.low
 */
class FvgStrategy(
    private val minGapPercent: Double = 0.05,
    private val rewardRiskRatio: Double = 2.0
) : TradingStrategy {

    override val id: StrategyId = StrategyId.FVG

    init {
        require(minGapPercent.isFinite() && minGapPercent >= 0.0) {
            "minGapPercent must be finite and non-negative"
        }
        require(rewardRiskRatio.isFinite() && rewardRiskRatio > 0.0) {
            "rewardRiskRatio must be finite and positive"
        }
    }

    override fun evaluate(context: StrategyContext): StrategySignal {
        require(context.symbol.isNotBlank()) {
            "Symbol cannot be blank"
        }

        if (!context.enabled) {
            return noSetup(context.symbol, "Strategy is disabled")
        }

        val candles = context.candles

        if (candles.size < 3) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INSUFFICIENT_DATA,
                details = "At least three closed candles are required"
            )
        }

        if (candles.any { !it.isValid() }) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INVALID_MARKET_DATA,
                details = "One or more candles contain invalid OHLCV data"
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

        val first = candles[candles.lastIndex - 2]
        val latest = candles.last()

        val bullishGap = latest.low - first.high
        val bearishGap = first.low - latest.high

        val bullishGapPercent =
            if (bullishGap > 0.0) bullishGap / first.high * 100.0 else 0.0

        val bearishGapPercent =
            if (bearishGap > 0.0) bearishGap / first.low * 100.0 else 0.0

        if (bullishGap > 0.0 && bullishGapPercent >= minGapPercent) {
            val entry = latest.close
            val stopLoss = first.high
            val risk = entry - stopLoss

            if (risk <= 0.0 || !risk.isFinite()) {
                return noSetup(
                    context.symbol,
                    "Bullish gap found, but risk calculation is invalid"
                )
            }

            val takeProfit = entry + risk * rewardRiskRatio

            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.LONG,
                confidence = confidenceFor(bullishGapPercent),
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = stopLoss,
                takeProfitPrice = takeProfit,
                details = "Bullish FVG; gap=${bullishGapPercent}%"
            )
        }

        if (bearishGap > 0.0 && bearishGapPercent >= minGapPercent) {
            val entry = latest.close
            val stopLoss = first.low
            val risk = stopLoss - entry

            if (risk <= 0.0 || !risk.isFinite()) {
                return noSetup(
                    context.symbol,
                    "Bearish gap found, but risk calculation is invalid"
                )
            }

            val takeProfit = entry - risk * rewardRiskRatio

            if (takeProfit <= 0.0 || !takeProfit.isFinite()) {
                return noSetup(
                    context.symbol,
                    "Bearish gap found, but target price is invalid"
                )
            }

            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.SHORT,
                confidence = confidenceFor(bearishGapPercent),
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = stopLoss,
                takeProfitPrice = takeProfit,
                details = "Bearish FVG; gap=${bearishGapPercent}%"
            )
        }

        return noSetup(context.symbol, "No qualifying FVG in the latest three candles")
    }

    private fun confidenceFor(gapPercent: Double): Double {
        // A bounded setup-strength score, not a probability of profit.
        return (0.5 + gapPercent / 10.0).coerceIn(0.5, 0.9)
    }

    private fun noSetup(symbol: String, details: String): StrategySignal {
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
