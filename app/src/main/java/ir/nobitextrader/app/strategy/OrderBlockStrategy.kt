package ir.nobitextrader.app.strategy

/**
 * Basic Order Block strategy using OHLCV candles.
 *
 * A bullish block is the last bearish candle before a strong bullish
 * displacement candle.
 *
 * A bearish block is the last bullish candle before a strong bearish
 * displacement candle.
 *
 * A signal is generated only when a later candle enters the block zone
 * and closes back out in the expected direction.
 *
 * The caller must provide closed candles in chronological order.
 */
class OrderBlockStrategy(
    private val lookbackCandles: Int = 12,
    private val displacementPercent: Double = 0.20,
    private val minimumBodyToRangeRatio: Double = 0.60,
    private val rewardRiskRatio: Double = 2.0
) : TradingStrategy {

    override val id: StrategyId = StrategyId.ORDER_BLOCK

    init {
        require(lookbackCandles >= 3) {
            "lookbackCandles must be at least 3"
        }
        require(displacementPercent.isFinite() && displacementPercent >= 0.0) {
            "displacementPercent must be finite and non-negative"
        }
        require(
            minimumBodyToRangeRatio.isFinite() &&
                minimumBodyToRangeRatio in 0.0..1.0
        ) {
            "minimumBodyToRangeRatio must be between 0 and 1"
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
            return flat(context.symbol, "Strategy is disabled")
        }

        val candles = context.candles

        if (candles.size < lookbackCandles + 2) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.FLAT,
                confidence = 0.0,
                reason = SignalReason.INSUFFICIENT_DATA,
                details = "Not enough candles to identify and retest an order block"
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

        val latestIndex = candles.lastIndex
        val latest = candles[latestIndex]
        val searchStart = (latestIndex - lookbackCandles - 1).coerceAtLeast(0)

        // Search backward for the most recent retested bullish or bearish block.
        for (blockIndex in latestIndex - 2 downTo searchStart) {
            val block = candles[blockIndex]
            val impulse = candles[blockIndex + 1]

            val impulseRange = impulse.high - impulse.low
            if (impulseRange <= 0.0) continue

            val bodyRatio =
                kotlin.math.abs(impulse.close - impulse.open) / impulseRange

            val displacement =
                kotlin.math.abs(impulse.close - impulse.open) /
                    block.close * 100.0

            if (bodyRatio < minimumBodyToRangeRatio ||
                displacement < displacementPercent
            ) {
                continue
            }

            val bullishBlock =
                block.close < block.open &&
                    impulse.close > impulse.open &&
                    impulse.close > block.high

            val bearishBlock =
                block.close > block.open &&
                    impulse.close < impulse.open &&
                    impulse.close < block.low

            if (!bullishBlock && !bearishBlock) continue

            // Ignore a block if price crossed completely through it after formation.
            val laterCandles = candles.subList(blockIndex + 2, latestIndex)

            if (bullishBlock) {
                val invalidated = laterCandles.any { it.close < block.low }
                if (invalidated) continue

                val retested = latest.low <= block.high &&
                    latest.low >= block.low &&
                    latest.close > block.high

                if (!retested) continue

                val entry = latest.close
                val stopLoss = block.low
                val risk = entry - stopLoss

                if (risk <= 0.0 || !risk.isFinite()) continue

                return StrategySignal(
                    strategyId = id,
                    symbol = context.symbol,
                    direction = TradeDirection.LONG,
                    confidence = 0.65,
                    reason = SignalReason.SETUP_CONFIRMED,
                    referencePrice = entry,
                    stopLossPrice = stopLoss,
                    takeProfitPrice = entry + risk * rewardRiskRatio,
                    details = "Bullish order block retest at candle index $blockIndex"
                )
            }

            if (bearishBlock) {
                val invalidated = laterCandles.any { it.close > block.high }
                if (invalidated) continue

                val retested = latest.high >= block.low &&
                    latest.high <= block.high &&
                    latest.close < block.low

                if (!retested) continue

                val entry = latest.close
                val stopLoss = block.high
                val risk = stopLoss - entry

                if (risk <= 0.0 || !risk.isFinite()) continue

                val takeProfit = entry - risk * rewardRiskRatio
                if (takeProfit <= 0.0 || !takeProfit.isFinite()) continue

                return StrategySignal(
                    strategyId = id,
                    symbol = context.symbol,
                    direction = TradeDirection.SHORT,
                    confidence = 0.65,
                    reason = SignalReason.SETUP_CONFIRMED,
                    referencePrice = entry,
                    stopLossPrice = stopLoss,
                    takeProfitPrice = takeProfit,
                    details = "Bearish order block retest at candle index $blockIndex"
                )
            }
        }

        return flat(
            context.symbol,
            "No valid order block retest detected"
        )
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
