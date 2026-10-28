
package ir.nobitextrader.app.strategy

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Short-term scalping strategy.
 *
 * Uses candle momentum, short-term trend, and volume confirmation.
 * This strategy only produces signals; it does not place orders.
 */
class ScaliperStrategy(
    private val fastEmaPeriod: Int = 5,
    private val slowEmaPeriod: Int = 13,
    private val minimumMomentumPercent: Double = 0.05,
    private val minimumVolumeRatio: Double = 1.20,
    private val rewardRiskRatio: Double = 1.50,
    private val stopLossPercent: Double = 0.30
) : TradingStrategy {

    override val id: StrategyId = StrategyId.MICRO_SCALPER

    init {
        require(fastEmaPeriod >= 2)
        require(slowEmaPeriod > fastEmaPeriod)
        require(minimumMomentumPercent >= 0.0)
        require(minimumVolumeRatio > 0.0)
        require(rewardRiskRatio > 0.0)
        require(stopLossPercent > 0.0)
    }

    override fun evaluate(context: StrategyContext): StrategySignal {
        fun flat(reason: SignalReason, details: String) = StrategySignal(
            strategyId = id,
            symbol = context.symbol,
            direction = TradeDirection.FLAT,
            confidence = 0.0,
            reason = reason,
            details = details
        )

        if (!context.enabled) {
            return flat(SignalReason.NO_SETUP, "Scaliper is disabled.")
        }

        val candles = context.candles

        if (candles.size < slowEmaPeriod + 1) {
            return flat(
                SignalReason.INSUFFICIENT_DATA,
                "Not enough candles for scalping analysis."
            )
        }

        if (context.symbol.isBlank() ||
            candles.any { !it.isValid() } ||
            candles.zipWithNext().any { (a, b) ->
                b.timestampMillis <= a.timestampMillis
            }
        ) {
            return flat(
                SignalReason.INVALID_MARKET_DATA,
                "Invalid or non-chronological market data."
            )
        }

        val closes = candles.map { it.close }
        val latest = candles.last()
        val previous = candles[candles.lastIndex - 1]

        val fastEma = ema(closes.takeLast(fastEmaPeriod), fastEmaPeriod)
        val slowEma = ema(closes.takeLast(slowEmaPeriod), slowEmaPeriod)

        val momentumPercent =
            (latest.close - previous.close) / previous.close * 100.0

        val averageVolume = candles
            .takeLast(slowEmaPeriod)
            .dropLast(1)
            .map { it.volume }
            .average()

        if (!fastEma.isFinite() ||
            !slowEma.isFinite() ||
            !averageVolume.isFinite() ||
            averageVolume <= 0.0
        ) {
            return flat(
                SignalReason.INVALID_MARKET_DATA,
                "Invalid EMA or volume values."
            )
        }

        val volumeRatio = latest.volume / averageVolume
        val bullishTrend = fastEma > slowEma
        val bearishTrend = fastEma < slowEma
        val bullishMomentum =
            momentumPercent >= minimumMomentumPercent
        val bearishMomentum =
            momentumPercent <= -minimumMomentumPercent
        val volumeConfirmed = volumeRatio >= minimumVolumeRatio

        val direction = when {
            bullishTrend && bullishMomentum && volumeConfirmed ->
                TradeDirection.LONG

            bearishTrend && bearishMomentum && volumeConfirmed ->
                TradeDirection.SHORT

            else -> TradeDirection.FLAT
        }

        if (direction == TradeDirection.FLAT) {
            return flat(
                SignalReason.NO_SETUP,
                "No confirmed scalping setup. " +
                    "Momentum=${"%.3f".format(java.util.Locale.US, momentumPercent)}%, " +
                    "VolumeRatio=${"%.2f".format(java.util.Locale.US, volumeRatio)}."
            )
        }

        val entry = latest.close
        val stopDistance = entry * stopLossPercent / 100.0

        val stopLoss = when (direction) {
            TradeDirection.LONG -> entry - stopDistance
            TradeDirection.SHORT -> entry + stopDistance
            TradeDirection.FLAT -> entry
        }

        val takeProfit = when (direction) {
            TradeDirection.LONG -> entry + stopDistance * rewardRiskRatio
            TradeDirection.SHORT -> entry - stopDistance * rewardRiskRatio
            TradeDirection.FLAT -> entry
        }

        val momentumStrength = min(
            1.0,
            abs(momentumPercent) / max(minimumMomentumPercent, 0.01) / 3.0
        )
        val volumeStrength = min(
            1.0,
            volumeRatio / minimumVolumeRatio / 2.0
        )
        val trendStrength = min(
            1.0,
            abs(fastEma - slowEma) / entry * 100.0
        )

        val confidence = (
            0.40 * momentumStrength +
                0.35 * volumeStrength +
                0.25 * trendStrength
            ).coerceIn(0.0, 1.0)

        return StrategySignal(
            strategyId = id,
            symbol = context.symbol,
            direction = direction,
            confidence = confidence,
            reason = SignalReason.SETUP_CONFIRMED,
            referencePrice = entry,
            stopLossPrice = stopLoss,
            takeProfitPrice = takeProfit,
            details = "Scaliper confirmed. " +
                "Momentum=${"%.3f".format(java.util.Locale.US, momentumPercent)}%, " +
                "VolumeRatio=${"%.2f".format(java.util.Locale.US, volumeRatio)}, " +
                "FastEMA=${"%.4f".format(java.util.Locale.US, fastEma)}, " +
                "SlowEMA=${"%.4f".format(java.util.Locale.US, slowEma)}."
        )
    }

    private fun ema(values: List<Double>, period: Int): Double {
        if (values.isEmpty()) return Double.NaN

        val multiplier = 2.0 / (period + 1.0)
        var result = values.first()

        for (i in 1 until values.size) {
            result = (values[i] - result) * multiplier + result
        }

        return result
    }
}
