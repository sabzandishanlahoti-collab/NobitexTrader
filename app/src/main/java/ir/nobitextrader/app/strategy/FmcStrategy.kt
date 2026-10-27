package ir.nobitextrader.app.strategy

import kotlin.math.abs

/**
 * FMC: Momentum + Flow Confirmation.
 *
 * Operational definition:
 * - Compare the latest closed candle's body with the prior candle bodies.
 * - Require directional momentum and above-average volume.
 * - Emit a signal only when both conditions agree.
 *
 * Volume is not proof of buyer/seller aggression. Real trade-flow data
 * should be used separately when available.
 *
 * Signals only; no orders are placed here.
 */
class FmcStrategy(
    private val lookbackCandles: Int = 10,
    private val minimumBodyPercent: Double = 0.15,
    private val minimumVolumeRatio: Double = 1.5,
    private val rewardRiskRatio: Double = 2.0,
    private val stopLossPercent: Double = 0.50
) : TradingStrategy {

    override val id: StrategyId = StrategyId.FMC

    init {
        require(lookbackCandles >= 2) {
            "lookbackCandles must be at least 2"
        }
        require(minimumBodyPercent.isFinite() && minimumBodyPercent >= 0.0) {
            "minimumBodyPercent must be finite and non-negative"
        }
        require(minimumVolumeRatio.isFinite() && minimumVolumeRatio > 0.0) {
            "minimumVolumeRatio must be finite and positive"
        }
        require(rewardRiskRatio.isFinite() && rewardRiskRatio > 0.0) {
            "rewardRiskRatio must be finite and positive"
        }
        require(stopLossPercent.isFinite() && stopLossPercent > 0.0) {
            "stopLossPercent must be finite and positive"
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
                details = "Need ${lookbackCandles + 1} closed candles"
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
        val baseline = candles.dropLast(1).takeLast(lookbackCandles)

        val averageVolume = baseline.map { it.volume }.average()
        if (!averageVolume.isFinite() || averageVolume <= 0.0) {
            return flat(context.symbol, "Baseline volume is zero or invalid")
        }

        val volumeRatio = latest.volume / averageVolume
        val bodyPercent = abs(latest.close - latest.open) /
            latest.open * 100.0

        if (bodyPercent < minimumBodyPercent ||
            volumeRatio < minimumVolumeRatio
        ) {
            return flat(
                context.symbol,
                "Momentum or volume confirmation threshold not met"
            )
        }

        val entry = latest.close
        val stopDistance = entry * stopLossPercent / 100.0

        if (!stopDistance.isFinite() || stopDistance <= 0.0) {
            return flat(context.symbol, "Invalid stop distance")
        }

        val score = (
            0.5 +
                (bodyPercent / (minimumBodyPercent * 4.0).coerceAtLeast(0.01)) * 0.2 +
                (volumeRatio / minimumVolumeRatio).coerceAtMost(2.0) * 0.1
            ).coerceIn(0.5, 0.9)

        if (latest.close > latest.open) {
            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.LONG,
                confidence = score,
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = entry - stopDistance,
                takeProfitPrice = entry + stopDistance * rewardRiskRatio,
                details = "Bullish momentum; body=${bodyPercent}%; volumeRatio=$volumeRatio"
            )
        }

        if (latest.close < latest.open) {
            val target = entry - stopDistance * rewardRiskRatio

            if (target <= 0.0 || !target.isFinite()) {
                return flat(context.symbol, "Invalid short target price")
            }

            return StrategySignal(
                strategyId = id,
                symbol = context.symbol,
                direction = TradeDirection.SHORT,
                confidence = score,
                reason = SignalReason.SETUP_CONFIRMED,
                referencePrice = entry,
                stopLossPrice = entry + stopDistance,
                takeProfitPrice = target,
                details = "Bearish momentum; body=${bodyPercent}%; volumeRatio=$volumeRatio"
            )
        }

        return flat(context.symbol, "Latest candle has no directional body")
    }

    private fun flat(symbol: String, details: String) = StrategySignal(
        strategyId = id,
        symbol = symbol,
        direction = TradeDirection.FLAT,
        confidence = 0.0,
        reason = SignalReason.NO_SETUP,
        details = details
    )
}
