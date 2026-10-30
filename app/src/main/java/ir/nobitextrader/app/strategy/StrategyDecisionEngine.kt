package ir.nobitextrader.app.strategy

/**
 * Converts strategy batch output into a conservative directional decision.
 *
 * This class does not fetch market data, manage account risk, access exchange APIs,
 * or place orders. Strategy evaluation remains independent from decision filtering.
 */
class StrategyDecisionEngine(
    private val minimumAgreement: Int = DEFAULT_MINIMUM_AGREEMENT
) {
    init {
        require(minimumAgreement >= 1) {
            "Minimum strategy agreement must be at least 1"
        }
    }

    fun decide(batch: StrategyBatchResult): StrategyDecision {
        if (!batch.isInputValid) {
            return noTrade(
                symbol = batch.symbol,
                reason = batch.inputError ?: "Invalid strategy input"
            )
        }

        val structuralError = validateBatch(batch)
        if (structuralError != null) {
            return noTrade(
                symbol = batch.symbol,
                reason = structuralError
            )
        }

        val actionable = batch.actionableSignals
        val longSignals = actionable.filter { it.direction == TradeDirection.LONG }
        val shortSignals = actionable.filter { it.direction == TradeDirection.SHORT }

        if (longSignals.isNotEmpty() && shortSignals.isNotEmpty()) {
            return noTrade(
                symbol = batch.symbol,
                reason = "Conflicting confirmed LONG and SHORT signals",
                longStrategyIds = longSignals.map { it.strategyId },
                shortStrategyIds = shortSignals.map { it.strategyId }
            )
        }

        val alignedSignals = when {
            longSignals.isNotEmpty() -> longSignals
            shortSignals.isNotEmpty() -> shortSignals
            else -> emptyList()
        }

        if (alignedSignals.size < minimumAgreement) {
            return noTrade(
                symbol = batch.symbol,
                reason = "Not enough agreeing confirmed strategies: " +
                    "${alignedSignals.size}/$minimumAgreement",
                candidateDirection = alignedSignals.firstOrNull()?.direction,
                candidateStrategyIds = alignedSignals.map { it.strategyId }
            )
        }

        val direction = alignedSignals.first().direction
        val averageConfidence = alignedSignals.map { it.confidence }.average()

        return StrategyDecision(
            symbol = batch.symbol,
            direction = direction,
            confidence = averageConfidence,
            agreeingStrategyIds = alignedSignals.map { it.strategyId },
            longStrategyIds = longSignals.map { it.strategyId },
            shortStrategyIds = shortSignals.map { it.strategyId },
            reason = "Confirmed by ${alignedSignals.size} agreeing strategies",
            isActionable = true
        )
    }

    /**
     * Defends the decision boundary even when a batch is constructed outside
     * StrategyOrchestrator (for example by tests, adapters, or future modules).
     */
    private fun validateBatch(batch: StrategyBatchResult): String? {
        if (batch.symbol.isBlank()) return "Market symbol cannot be blank"
        if (batch.expectedStrategyCount < 1 ||
            batch.attemptedStrategyCount < 0 ||
            batch.attemptedStrategyCount > batch.expectedStrategyCount
        ) {
            return "Invalid strategy batch counters"
        }

        if (batch.attemptedStrategyCount !=
            batch.signals.size + batch.failures.size
        ) {
            return "Strategy batch result count is inconsistent"
        }

        if (batch.signals.any { it.symbol != batch.symbol }) {
            return "Strategy signal symbol does not match the batch symbol"
        }

        if (batch.signals.any {
                !it.confidence.isFinite() || it.confidence !in 0.0..1.0
            }
        ) {
            return "Strategy signal contains invalid confidence"
        }

        val reportedIds = batch.signals.map { it.strategyId } +
            batch.failures.map { it.strategyId }
        if (reportedIds.size != reportedIds.toSet().size) {
            return "Duplicate strategy result IDs in batch"
        }

        return null
    }

    private fun noTrade(
        symbol: String,
        reason: String,
        candidateDirection: TradeDirection? = null,
        candidateStrategyIds: List<StrategyId> = emptyList(),
        longStrategyIds: List<StrategyId> = emptyList(),
        shortStrategyIds: List<StrategyId> = emptyList()
    ) = StrategyDecision(
        symbol = symbol,
        direction = TradeDirection.FLAT,
        confidence = 0.0,
        agreeingStrategyIds = candidateStrategyIds,
        longStrategyIds = longStrategyIds,
        shortStrategyIds = shortStrategyIds,
        reason = reason,
        isActionable = false,
        candidateDirection = candidateDirection
    )

    companion object {
        const val DEFAULT_MINIMUM_AGREEMENT = 2
    }
}

/**
 * A decision only; it is not an order and must not be treated as execution approval.
 */
data class StrategyDecision(
    val symbol: String,
    val direction: TradeDirection,
    val confidence: Double,
    val agreeingStrategyIds: List<StrategyId>,
    val longStrategyIds: List<StrategyId>,
    val shortStrategyIds: List<StrategyId>,
    val reason: String,
    val isActionable: Boolean,
    val candidateDirection: TradeDirection? = null
)
