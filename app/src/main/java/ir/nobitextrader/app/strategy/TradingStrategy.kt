
package ir.nobitextrader.app.strategy

enum class StrategyId {
    WHALE,
    FMC,
    FVG,
    LIQUIDITY_SWEEP,
    ORDER_BLOCK,
    APEX_SNIPER,
    KALMAN_TREND,
    MICRO_SCALPER
}

enum class TradeDirection {
    LONG,
    SHORT,
    FLAT
}

enum class SignalReason {
    SETUP_CONFIRMED,
    INSUFFICIENT_DATA,
    NO_SETUP,
    INVALID_MARKET_DATA
}

data class Candle(
    val timestampMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    fun isValid(): Boolean {
        return timestampMillis >= 0L &&
            open.isFinite() &&
            high.isFinite() &&
            low.isFinite() &&
            close.isFinite() &&
            volume.isFinite() &&
            open > 0.0 &&
            high > 0.0 &&
            low > 0.0 &&
            close > 0.0 &&
            volume >= 0.0 &&
            high >= low &&
            high >= open &&
            high >= close &&
            low <= open &&
            low <= close
    }
}

data class StrategyContext(
    val symbol: String,
    val candles: List<Candle>,
    val enabled: Boolean = true
)

data class StrategySignal(
    val strategyId: StrategyId,
    val symbol: String,
    val direction: TradeDirection,
    val confidence: Double,
    val reason: SignalReason,
    val referencePrice: Double? = null,
    val stopLossPrice: Double? = null,
    val takeProfitPrice: Double? = null,
    val details: String = ""
) {
    init {
        require(symbol.isNotBlank()) {
            "Symbol cannot be blank"
        }
        require(confidence.isFinite() && confidence in 0.0..1.0) {
            "Confidence must be between 0 and 1"
        }
        require(
            referencePrice == null ||
                (referencePrice.isFinite() && referencePrice > 0.0)
        ) {
            "Reference price must be positive and finite"
        }
        require(
            stopLossPrice == null ||
                (stopLossPrice.isFinite() && stopLossPrice > 0.0)
        ) {
            "Stop-loss price must be positive and finite"
        }
        require(
            takeProfitPrice == null ||
                (takeProfitPrice.isFinite() && takeProfitPrice > 0.0)
        ) {
            "Take-profit price must be positive and finite"
        }
    }
}

interface TradingStrategy {
    val id: StrategyId

    fun evaluate(context: StrategyContext): StrategySignal
}
