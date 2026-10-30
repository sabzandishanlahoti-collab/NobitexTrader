package ir.nobitextrader.app.strategy

/**
 * Runs registered strategies against one shared market context.
 * This class does not aggregate signals, manage risk, access APIs, or place orders.
 */
class StrategyOrchestrator(strategies: List<TradingStrategy>) {
    private val strategies = strategies.toList()

    init {
        require(this.strategies.isNotEmpty()) { "At least one trading strategy is required" }
        val duplicates = this.strategies.groupingBy { it.id }
            .eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "Duplicate strategy IDs: ${duplicates.joinToString()}" }
    }

    fun evaluate(context: StrategyContext): StrategyBatchResult {
        val inputError = validate(context)
        if (inputError != null) {
            return StrategyBatchResult(
                symbol = context.symbol,
                expectedStrategyCount = strategies.size,
                attemptedStrategyCount = 0,
                signals = emptyList(),
                failures = emptyList(),
                inputError = inputError
            )
        }

        val signals = mutableListOf<StrategySignal>()
        val failures = mutableListOf<StrategyExecutionFailure>()
        var attempted = 0

        strategies.forEach { strategy ->
            attempted++
            try {
                val signal = strategy.evaluate(context)
                when {
                    signal.strategyId != strategy.id ->
                        failures += StrategyExecutionFailure(
                            strategy.id,
                            "Returned signal strategy ID does not match the registered strategy"
                        )
                    signal.symbol != context.symbol ->
                        failures += StrategyExecutionFailure(
                            strategy.id,
                            "Returned signal symbol does not match the requested symbol"
                        )
                    else -> signals += signal
                }
            } catch (exception: Exception) {
                failures += StrategyExecutionFailure(
                    strategy.id,
                    exception.message?.takeIf { it.isNotBlank() }
                        ?: exception.javaClass.simpleName
                )
            }
        }

        return StrategyBatchResult(
            symbol = context.symbol,
            expectedStrategyCount = strategies.size,
            attemptedStrategyCount = attempted,
            signals = signals.toList(),
            failures = failures.toList(),
            inputError = null
        )
    }

    private fun validate(context: StrategyContext): String? {
        if (context.symbol.isBlank()) return "Market symbol cannot be blank"
        if (context.candles.any { !it.isValid() }) {
            return "Market data contains an invalid candle"
        }
        if (context.candles.zipWithNext().any { (a, b) ->
                b.timestampMillis <= a.timestampMillis
            }
        ) {
            return "Candles must be strictly chronological"
        }
        return null
    }
}

data class StrategyExecutionFailure(
    val strategyId: StrategyId,
    val message: String
)

data class StrategyBatchResult(
    val symbol: String,
    val expectedStrategyCount: Int,
    val attemptedStrategyCount: Int,
    val signals: List<StrategySignal>,
    val failures: List<StrategyExecutionFailure>,
    val inputError: String?
) {
    val isInputValid: Boolean
        get() = inputError == null

    val isComplete: Boolean
        get() = isInputValid &&
            attemptedStrategyCount == expectedStrategyCount &&
            failures.isEmpty()

    val actionableSignals: List<StrategySignal>
        get() = if (!isInputValid) emptyList() else signals.filter {
            it.reason == SignalReason.SETUP_CONFIRMED &&
                it.direction != TradeDirection.FLAT
        }
}
