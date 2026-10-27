package ir.nobitextrader.app.risk

import kotlin.math.max
import kotlin.math.min

/**
 * Independent risk gate for spot trading.
 *
 * All percentage fields use percentage points:
 * 0.5 means 0.5%, not 50%.
 *
 * This class does not submit exchange orders.
 * Every proposed order must pass through this gate.
 */
data class RiskConfig(
    val riskPerTradePct: Double = 0.5,
    val maxTotalOpenRiskPct: Double = 1.5,
    val dailyLossLimitPct: Double = 1.5,
    val protectiveDrawdownPct: Double = 7.0,
    val maxDrawdownPct: Double = 10.0,
    val maxPositionCapitalPct: Double = 25.0,
    val maxSpreadPct: Double = 0.5
) {
    fun validate(): Boolean {
        return riskPerTradePct.isFinite() &&
            riskPerTradePct > 0.0 &&
            maxTotalOpenRiskPct.isFinite() &&
            maxTotalOpenRiskPct > 0.0 &&
            dailyLossLimitPct.isFinite() &&
            dailyLossLimitPct > 0.0 &&
            protectiveDrawdownPct.isFinite() &&
            protectiveDrawdownPct >= 0.0 &&
            maxDrawdownPct.isFinite() &&
            maxDrawdownPct > protectiveDrawdownPct &&
            maxPositionCapitalPct.isFinite() &&
            maxPositionCapitalPct > 0.0 &&
            maxPositionCapitalPct <= 100.0 &&
            maxSpreadPct.isFinite() &&
            maxSpreadPct >= 0.0
    }
}

data class RiskRequest(
    val equityUsd: Double,
    val peakEquityUsd: Double,
    val dayStartEquityUsd: Double,
    val freeCapitalUsd: Double,
    val openRiskUsd: Double,
    val stopLossPct: Double,
    val estimatedRoundTripCostsPct: Double,
    val spreadPct: Double,
    val marketDataValid: Boolean,
    val liquiditySufficient: Boolean,
    val killSwitchActive: Boolean
)

data class RiskDecision(
    val approved: Boolean,
    val reason: String,
    val positionSizeUsd: Double = 0.0,
    val riskBudgetUsd: Double = 0.0,
    val drawdownPct: Double = 0.0,
    val dailyLossPct: Double = 0.0,
    val protectiveMode: Boolean = false
)

class RiskEngine(
    private val config: RiskConfig = RiskConfig()
) {
    init {
        require(config.validate()) {
            "Invalid risk configuration"
        }
    }

    fun evaluate(request: RiskRequest): RiskDecision {
        if (request.killSwitchActive) {
            return reject("KILL switch is active")
        }

        if (!request.marketDataValid) {
            return reject("Market data is invalid or stale")
        }

        if (!request.liquiditySufficient) {
            return reject("Market liquidity is insufficient")
        }

        val positiveValues = listOf(
            request.equityUsd,
            request.peakEquityUsd,
            request.dayStartEquityUsd
        )

        if (positiveValues.any { !it.isFinite() || it <= 0.0 }) {
            return reject("Invalid account equity data")
        }

        val nonNegativeValues = listOf(
            request.freeCapitalUsd,
            request.openRiskUsd,
            request.estimatedRoundTripCostsPct,
            request.spreadPct
        )

        if (nonNegativeValues.any { !it.isFinite() || it < 0.0 }) {
            return reject("Invalid capital, cost, or risk data")
        }

        if (request.freeCapitalUsd > request.equityUsd) {
            return reject("Free capital exceeds account equity")
        }

        if (request.openRiskUsd > request.equityUsd) {
            return reject("Open risk exceeds account equity")
        }

        if (!request.stopLossPct.isFinite() ||
            request.stopLossPct <= 0.0
        ) {
            return reject("A valid stop loss is required")
        }

        if (request.spreadPct > config.maxSpreadPct) {
            return reject("Spread exceeds the configured limit")
        }

        val drawdownPct = max(
            0.0,
            (request.peakEquityUsd - request.equityUsd) /
                request.peakEquityUsd * 100.0
        )

        val dailyLossPct = max(
            0.0,
            (request.dayStartEquityUsd - request.equityUsd) /
                request.dayStartEquityUsd * 100.0
        )

        if (drawdownPct >= config.maxDrawdownPct) {
            return reject(
                reason = "Maximum drawdown reached; manual review required",
                drawdownPct = drawdownPct,
                dailyLossPct = dailyLossPct
            )
        }

        if (dailyLossPct >= config.dailyLossLimitPct) {
            return reject(
                reason = "Daily loss limit reached",
                drawdownPct = drawdownPct,
                dailyLossPct = dailyLossPct
            )
        }

        if (request.freeCapitalUsd <= 0.0) {
            return reject(
                reason = "No free capital available",
                drawdownPct = drawdownPct,
                dailyLossPct = dailyLossPct
            )
        }

        val protectiveMode =
            drawdownPct >= config.protectiveDrawdownPct

        val effectiveRiskPct =
            config.riskPerTradePct *
                if (protectiveMode) 0.5 else 1.0

        val requestedRiskBudgetUsd =
            request.equityUsd * effectiveRiskPct / 100.0

        val maxTotalRiskUsd =
            request.equityUsd *
                config.maxTotalOpenRiskPct / 100.0

        val remainingRiskUsd =
            max(0.0, maxTotalRiskUsd - request.openRiskUsd)

        if (remainingRiskUsd <= 0.0) {
            return reject(
                reason = "Total open-risk limit reached",
                drawdownPct = drawdownPct,
                dailyLossPct = dailyLossPct,
                protectiveMode = protectiveMode
            )
        }

        // Reduce the trade's risk budget when only part of the
        // total open-risk allowance remains.
        val effectiveRiskBudgetUsd =
            min(requestedRiskBudgetUsd, remainingRiskUsd)

        val totalRiskPct =
            request.stopLossPct +
                request.estimatedRoundTripCostsPct

        if (!totalRiskPct.isFinite() || totalRiskPct <= 0.0) {
            return reject(
                reason = "Invalid total trade risk",
                drawdownPct = drawdownPct,
                dailyLossPct = dailyLossPct,
                protectiveMode = protectiveMode
            )
        }

        val sizeAllowedByRisk =
            effectiveRiskBudgetUsd / (totalRiskPct / 100.0)

        val sizeAllowedByCapital =
            request.equityUsd *
                config.maxPositionCapitalPct / 100.0

        val positionSizeUsd = min(
            sizeAllowedByRisk,
            min(sizeAllowedByCapital, request.freeCapitalUsd)
        )

        if (!positionSizeUsd.isFinite() ||
            positionSizeUsd <= 0.0
        ) {
            return reject(
                reason = "Calculated position size is invalid",
                drawdownPct = drawdownPct,
                dailyLossPct = dailyLossPct,
                protectiveMode = protectiveMode
            )
        }

        return RiskDecision(
            approved = true,
            reason = if (protectiveMode) {
                "Approved in protective mode with reduced risk"
            } else if (effectiveRiskBudgetUsd < requestedRiskBudgetUsd) {
                "Approved with reduced size due to remaining open-risk allowance"
            } else {
                "Approved by risk engine"
            },
            positionSizeUsd = positionSizeUsd,
            riskBudgetUsd = effectiveRiskBudgetUsd,
            drawdownPct = drawdownPct,
            dailyLossPct = dailyLossPct,
            protectiveMode = protectiveMode
        )
    }

    private fun reject(
        reason: String,
        drawdownPct: Double = 0.0,
        dailyLossPct: Double = 0.0,
        protectiveMode: Boolean = false
    ) = RiskDecision(
        approved = false,
        reason = reason,
        drawdownPct = drawdownPct,
        dailyLossPct = dailyLossPct,
        protectiveMode = protectiveMode
    )
}
