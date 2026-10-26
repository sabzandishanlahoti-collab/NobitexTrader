
package ir.nobitextrader.app.risk

enum class RiskEventType {
    TRADE_APPROVED,
    TRADE_REJECTED,
    KILL_SWITCH_ACTIVATED,
    RISK_LOCKED,
    RISK_UNLOCKED,
    DAILY_LOSS_LIMIT_REACHED,
    MAX_DRAWDOWN_REACHED,
    INVALID_MARKET_DATA
}

data class RiskEvent(
    val type: RiskEventType,
    val timestampMillis: Long,
    val message: String,
    val equityUsd: Double? = null,
    val drawdownPct: Double? = null,
    val dailyLossPct: Double? = null,
    val symbol: String? = null
) {
    init {
        require(timestampMillis >= 0L) {
            "Timestamp cannot be negative"
        }
        require(message.isNotBlank()) {
            "Risk event message cannot be blank"
        }
        require(equityUsd == null ||
            (equityUsd.isFinite() && equityUsd >= 0.0)
        ) {
            "Equity must be finite and non-negative"
        }
        require(drawdownPct == null ||
            (drawdownPct.isFinite() && drawdownPct >= 0.0)
        ) {
            "Drawdown must be finite and non-negative"
        }
        require(dailyLossPct == null ||
            (dailyLossPct.isFinite() && dailyLossPct >= 0.0)
        ) {
            "Daily loss must be finite and non-negative"
        }
    }
}
