package ir.nobitextrader.app.strategy

import ir.nobitextrader.app.data.NobitexCandlesApi
import kotlinx.coroutines.CancellationException

/**
 * Loads real Nobitex candles, evaluates the registered candle strategies,
 * and produces a conservative decision.
 *
 * This coordinator is read-only: it does not access account endpoints,
 * manage balances, submit orders, or execute trades.
 *
 * WhalePressureStrategy is intentionally excluded until a real trade-flow
 * adapter supplies trades with a known aggressor.
 */
class StrategyAnalysisCoordinator(
    private val candleLoader: suspend (
        symbol: String,
        resolution: String,
        fromUnixSeconds: Long,
        toUnixSeconds: Long
    ) -> List<Candle> = { symbol, resolution, from, to ->
        NobitexCandlesApi.fetchCandles(
            symbol = symbol,
            resolution = resolution,
            fromUnixSeconds = from,
            toUnixSeconds = to
        )
    },
    strategies: List<TradingStrategy> = defaultStrategies(),
    private val decisionEngine: StrategyDecisionEngine =
        StrategyDecisionEngine()
) {
    private val orchestrator = StrategyOrchestrator(strategies)

    suspend fun analyze(
        symbol: String,
        resolution: String,
        fromUnixSeconds: Long,
        toUnixSeconds: Long,
        enabled: Boolean = true
    ): StrategyAnalysisResult {
        val candles = try {
            candleLoader(
                symbol,
                resolution,
                fromUnixSeconds,
                toUnixSeconds
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return StrategyAnalysisResult(
                symbol = symbol,
                candles = emptyList(),
                batch = null,
                decision = null,
                fetchError = exception.message?.takeIf { it.isNotBlank() }
                    ?: exception.javaClass.simpleName
            )
        }

        val batch = orchestrator.evaluate(
            StrategyContext(
                symbol = symbol,
                candles = candles,
                enabled = enabled
            )
        )

        return StrategyAnalysisResult(
            symbol = symbol,
            candles = candles,
            batch = batch,
            decision = decisionEngine.decide(batch),
            fetchError = null
        )
    }

    companion object {
        /**
         * Candle-only strategies with implemented evaluators.
         * Strategies requiring unavailable data are not registered here.
         */
        fun defaultStrategies(): List<TradingStrategy> = listOf(
            FmcStrategy(),
            FvgStrategy(),
            LiquiditySweepStrategy(),
            OrderBlockStrategy(),
            ScaliperStrategy()
        )
    }
}

/**
 * Analysis output only. A decision marked actionable is a strategy
 * consensus, not permission to place an exchange order.
 */
data class StrategyAnalysisResult(
    val symbol: String,
    val candles: List<Candle>,
    val batch: StrategyBatchResult?,
    val decision: StrategyDecision?,
    val fetchError: String?
) {
    val isFetchSuccessful: Boolean
        get() = fetchError == null

    val isAnalysisAvailable: Boolean
        get() = batch != null && decision != null

    val isActionable: Boolean
        get() = fetchError == null && decision?.isActionable == true
}
