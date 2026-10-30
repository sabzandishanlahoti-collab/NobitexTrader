package ir.nobitextrader.app.strategy

import kotlin.math.max

/**
 * Deterministic, research-only OHLC backtest.
 * Signals use data through a completed candle and enter at the next candle open.
 * If stop and target are both touched in one candle, stop-loss wins.
 * No exchange orders are ever submitted.
 */
class BacktestEngine {
    fun run(
        candles: List<Candle>,
        config: BacktestConfig = BacktestConfig(),
        signalAtClose: (List<Candle>) -> BacktestSignal?
    ): BacktestResult {
        require(candles.size >= 2) { "At least two candles are required" }
        require(candles.all(Candle::isValid)) { "Invalid candle data" }
        require(candles.zipWithNext().all { (a, b) -> b.timestampMillis > a.timestampMillis }) {
            "Candles must be strictly chronological"
        }

        var equity = config.initialCapital
        var peakEquity = equity
        var maxDrawdown = 0.0
        var pending: BacktestSignal? = null
        var position: Position? = null
        val trades = mutableListOf<BacktestTrade>()

        fun close(index: Int, rawExit: Double, reason: ExitReason) {
            val p = position ?: return
            val exitPrice = rawExit * when (p.direction) {
                TradeDirection.LONG -> 1.0 - config.slippageRate
                TradeDirection.SHORT -> 1.0 + config.slippageRate
                TradeDirection.FLAT -> 1.0
            }
            val exitFee = p.quantity * exitPrice * config.feeRate
            val gross = when (p.direction) {
                TradeDirection.LONG -> (exitPrice - p.entryPrice) * p.quantity
                TradeDirection.SHORT -> (p.entryPrice - exitPrice) * p.quantity
                TradeDirection.FLAT -> 0.0
            }
            val net = gross - p.entryFee - exitFee
            equity += gross - exitFee
            trades += BacktestTrade(
                direction = p.direction,
                entryTimeMillis = candles[p.entryIndex].timestampMillis,
                exitTimeMillis = candles[index].timestampMillis,
                entryPrice = p.entryPrice,
                exitPrice = exitPrice,
                quantity = p.quantity,
                grossPnl = gross,
                fees = p.entryFee + exitFee,
                netPnl = net,
                exitReason = reason,
                equityAfter = equity
            )
            position = null
        }

        candles.forEachIndexed { index, candle ->
            // A signal from the prior close may enter only at this candle's open.
            pending?.let { signal ->
                if (position == null) {
                    val entryPrice = candle.open * when (signal.direction) {
                        TradeDirection.LONG -> 1.0 + config.slippageRate
                        TradeDirection.SHORT -> 1.0 - config.slippageRate
                        TradeDirection.FLAT -> 1.0
                    }
                    val quantity = config.positionNotional / entryPrice
                    val entryFee = config.positionNotional * config.feeRate
                    if (quantity.isFinite() && entryFee.isFinite() &&
                        config.positionNotional + entryFee <= equity
                    ) {
                        equity -= entryFee
                        position = Position(signal.direction, index, entryPrice, quantity,
                            signal.stopLossPrice, signal.takeProfitPrice, entryFee)
                    }
                }
                pending = null
            }

            position?.let { p ->
                // A gap through the stop is filled at the worse opening price.
                val stopGap = if (p.direction == TradeDirection.LONG)
                    candle.open <= p.stopLossPrice else candle.open >= p.stopLossPrice
                val targetGap = if (p.direction == TradeDirection.LONG)
                    candle.open >= p.takeProfitPrice else candle.open <= p.takeProfitPrice
                val stopTouched = if (p.direction == TradeDirection.LONG)
                    candle.low <= p.stopLossPrice else candle.high >= p.stopLossPrice
                val targetTouched = if (p.direction == TradeDirection.LONG)
                    candle.high >= p.takeProfitPrice else candle.low <= p.takeProfitPrice

                when {
                    stopGap -> close(index, candle.open, ExitReason.STOP_LOSS)
                    targetGap -> close(index, p.takeProfitPrice, ExitReason.TAKE_PROFIT)
                    stopTouched -> close(index, p.stopLossPrice, ExitReason.STOP_LOSS)
                    targetTouched -> close(index, p.takeProfitPrice, ExitReason.TAKE_PROFIT)
                }
            }

            peakEquity = max(peakEquity, equity)
            if (peakEquity > 0.0) {
                maxDrawdown = max(maxDrawdown, (peakEquity - equity) / peakEquity)
            }

            // Last candle has no next open, so it cannot create a new position.
            if (index < candles.lastIndex && position == null) {
                signalAtClose(candles.subList(0, index + 1))?.let { signal ->
                    validateSignal(signal, candle.close)
                    pending = signal
                }
            }
        }

        position?.let { close(candles.lastIndex, candles.last().close, ExitReason.END_OF_DATA) }
        // Include the final end-of-data liquidation in realized-equity drawdown.
        peakEquity = max(peakEquity, equity)
        if (peakEquity > 0.0) {
            maxDrawdown = max(maxDrawdown, (peakEquity - equity) / peakEquity)
        }

        val winners = trades.count { it.netPnl > 0.0 }
        val losers = trades.count { it.netPnl < 0.0 }
        val grossProfit = trades.filter { it.netPnl > 0.0 }.sumOf { it.netPnl }
        val grossLoss = -trades.filter { it.netPnl < 0.0 }.sumOf { it.netPnl }
        return BacktestResult(
            initialCapital = config.initialCapital,
            finalEquity = equity,
            netPnl = equity - config.initialCapital,
            totalTrades = trades.size,
            winningTrades = winners,
            losingTrades = losers,
            winRate = if (trades.isEmpty()) 0.0 else winners.toDouble() / trades.size,
            profitFactor = when {
                grossLoss > 0.0 -> grossProfit / grossLoss
                grossProfit > 0.0 -> Double.POSITIVE_INFINITY
                else -> 0.0
            },
            maxDrawdown = maxDrawdown,
            totalFees = trades.sumOf { it.fees },
            trades = trades
        )
    }

    private fun validateSignal(signal: BacktestSignal, signalClose: Double) {
        require(signal.direction == TradeDirection.LONG || signal.direction == TradeDirection.SHORT) {
            "Only LONG and SHORT signals can be backtested"
        }
        require(signal.stopLossPrice.isFinite() && signal.takeProfitPrice.isFinite() &&
            signal.stopLossPrice > 0.0 && signal.takeProfitPrice > 0.0
        ) { "Stop-loss and take-profit must be finite positive prices" }
        when (signal.direction) {
            TradeDirection.LONG -> require(
                signal.stopLossPrice < signalClose && signal.takeProfitPrice > signalClose
            ) { "LONG stop must be below and target above signal close" }
            TradeDirection.SHORT -> require(
                signal.stopLossPrice > signalClose && signal.takeProfitPrice < signalClose
            ) { "SHORT stop must be above and target below signal close" }
            TradeDirection.FLAT -> error("FLAT is not tradable")
        }
    }
}

data class BacktestConfig(
    val initialCapital: Double = 10_000.0,
    val positionNotional: Double = 1_000.0,
    val feeRate: Double = 0.001,
    val slippageRate: Double = 0.0005
) {
    init {
        require(initialCapital.isFinite() && initialCapital > 0.0)
        require(positionNotional.isFinite() && positionNotional > 0.0 && positionNotional <= initialCapital)
        require(feeRate.isFinite() && feeRate in 0.0..0.05)
        require(slippageRate.isFinite() && slippageRate in 0.0..0.05)
    }
}

data class BacktestSignal(
    val direction: TradeDirection,
    val stopLossPrice: Double,
    val takeProfitPrice: Double
)

data class BacktestResult(
    val initialCapital: Double,
    val finalEquity: Double,
    val netPnl: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val maxDrawdown: Double,
    val totalFees: Double,
    val trades: List<BacktestTrade>
)

data class BacktestTrade(
    val direction: TradeDirection,
    val entryTimeMillis: Long,
    val exitTimeMillis: Long,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val grossPnl: Double,
    val fees: Double,
    val netPnl: Double,
    val exitReason: ExitReason,
    val equityAfter: Double
)

enum class ExitReason { STOP_LOSS, TAKE_PROFIT, END_OF_DATA }

private data class Position(
    val direction: TradeDirection,
    val entryIndex: Int,
    val entryPrice: Double,
    val quantity: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val entryFee: Double
)
