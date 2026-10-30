package ir.nobitextrader.app.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.nobitextrader.app.TraderSettings
import ir.nobitextrader.app.strategy.StrategyAnalysisCoordinator
import ir.nobitextrader.app.strategy.StrategyAnalysisResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class StrategyAnalysisUiState(
    val isLoading: Boolean = false,
    val result: StrategyAnalysisResult? = null,
    val error: String? = null
)

data class MarketScannerUiState(
    val isScanning: Boolean = false,
    val results: List<MarketScanResult> = emptyList(),
    val errorMessage: String? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val successfulCount: Int = 0,
    val failedCount: Int = 0,
    val sortMode: MarketSortMode = MarketSortMode.BALANCED_PRIORITY,
    val filterMode: MarketResultFilterMode = MarketResultFilterMode.ALL,
    val autoRefreshEnabled: Boolean = false,
    val refreshIntervalMs: Long = MarketRefreshPolicy.DEFAULT_INTERVAL_MS,
    val analysisBySymbol: Map<String, StrategyAnalysisUiState> = emptyMap()
)

class MarketScannerViewModel(
    private val analysisCoordinator: StrategyAnalysisCoordinator =
        StrategyAnalysisCoordinator()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketScannerUiState())
    val uiState: StateFlow<MarketScannerUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var autoRefreshJob: Job? = null
    private val analysisJobs = mutableMapOf<String, Job>()
    private var scanGeneration = 0L
    private var allResults: List<MarketScanResult> = emptyList()

    private var requestTimeoutMs =
        MarketRefreshPolicy.DEFAULT.requestTimeoutMillis

    private var maxMarketsPerBatch =
        MarketRefreshPolicy.DEFAULT.maxMarketsPerBatch

    private var maxSpreadPercent = 1f

    fun applySettings(settings: TraderSettings) {
        requestTimeoutMs =
            settings.requestTimeoutSeconds.coerceIn(1, 60) * 1_000L

        maxMarketsPerBatch =
            settings.maxMarketsPerBatch.coerceIn(1, 50)

        maxSpreadPercent =
            settings.maxSpreadPercent.coerceIn(0.1f, 10f)

        val refreshMs =
            settings.refreshSeconds.coerceIn(5, 3600) * 1_000L

        val intervalChanged =
            _uiState.value.refreshIntervalMs != refreshMs

        _uiState.value = _uiState.value.copy(
            refreshIntervalMs = refreshMs
        )

        if (settings.autoScanEnabled) {
            if (!_uiState.value.autoRefreshEnabled) {
                _uiState.value = _uiState.value.copy(
                    autoRefreshEnabled = true
                )
                startAutoRefresh()
                scanMarkets()
            } else if (intervalChanged) {
                startAutoRefresh()
            }
        } else if (_uiState.value.autoRefreshEnabled) {
            stopAutoRefresh()
        }

        refreshDisplayedResults()
    }

    fun scanMarkets(symbols: List<String>? = null) {
        if (scanJob?.isActive == true) return

        val generation = ++scanGeneration

        val policy = MarketRefreshPolicy(
            refreshIntervalMillis = _uiState.value.refreshIntervalMs,
            requestTimeoutMillis = requestTimeoutMs,
            maxMarketsPerBatch = maxMarketsPerBatch
        )

        allResults = emptyList()

        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                results = emptyList(),
                errorMessage = null,
                completedCount = 0,
                totalCount = 0,
                successfulCount = 0,
                failedCount = 0
            )

            // This list is written by the scanner's single sequential
            // processing coroutine and copied before publishing to StateFlow.
            val streamedResults = mutableListOf<MarketScanResult>()

            try {
                val results = withContext(Dispatchers.IO) {
                    val progressCallback: (Int, Int) -> Unit =
                        { completed, total ->
                            if (generation == scanGeneration) {
                                _uiState.value = _uiState.value.copy(
                                    completedCount = completed,
                                    totalCount = total
                                )
                            }
                        }

                    val resultCallback: (MarketScanResult) -> Unit =
                        { result ->
                            if (generation == scanGeneration) {
                                streamedResults.add(result)
                                val snapshot = streamedResults.toList()
                                allResults = snapshot
                                val current = _uiState.value

                                _uiState.value = current.copy(
                                    results = buildDisplayedResults(
                                        results = snapshot,
                                        sortMode = current.sortMode,
                                        filterMode = current.filterMode
                                    ),
                                    completedCount = snapshot.size,
                                    successfulCount = snapshot.count { it.success },
                                    failedCount = snapshot.count { !it.success }
                                )
                            }
                        }

                    if (symbols == null) {
                        MarketScanner.scanAll(
                            policy = policy,
                            onProgress = progressCallback,
                            onResult = resultCallback
                        )
                    } else {
                        MarketScanner.scan(
                            symbols = symbols,
                            policy = policy,
                            onProgress = progressCallback,
                            onResult = resultCallback
                        )
                    }
                }

                if (generation != scanGeneration) return@launch

                allResults = results
                val current = _uiState.value

                _uiState.value = current.copy(
                    isScanning = false,
                    results = buildDisplayedResults(
                        results = allResults,
                        sortMode = current.sortMode,
                        filterMode = current.filterMode
                    ),
                    errorMessage = null,
                    completedCount = results.size,
                    totalCount = results.size,
                    successfulCount = results.count { it.success },
                    failedCount = results.count { !it.success }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (generation == scanGeneration) {
                    allResults = streamedResults.toList()
                    val current = _uiState.value

                    _uiState.value = current.copy(
                        isScanning = false,
                        results = buildDisplayedResults(
                            results = allResults,
                            sortMode = current.sortMode,
                            filterMode = current.filterMode
                        ),
                        errorMessage = e.message
                            ?: "اسکن بازار ناموفق بود.",
                        successfulCount = allResults.count { it.success },
                        failedCount = allResults.count { !it.success }
                    )
                }
            } finally {
                if (generation == scanGeneration) {
                    _uiState.value = _uiState.value.copy(
                        isScanning = false
                    )
                    scanJob = null
                }
            }
        }
    }

    private fun buildDisplayedResults(
        results: List<MarketScanResult>,
        sortMode: MarketSortMode,
        filterMode: MarketResultFilterMode
    ): List<MarketScanResult> {
        val spreadFiltered = results.filter {
            !it.success || it.spreadPercent <= maxSpreadPercent
        }

        return MarketResultFilter.apply(
            MarketScanner.sortResults(spreadFiltered, sortMode),
            filterMode
        )
    }

    private fun refreshDisplayedResults() {
        val current = _uiState.value
        _uiState.value = current.copy(
            results = buildDisplayedResults(
                results = allResults,
                sortMode = current.sortMode,
                filterMode = current.filterMode
            )
        )
    }

    fun setSortMode(mode: MarketSortMode) {
        val current = _uiState.value
        _uiState.value = current.copy(
            sortMode = mode,
            results = buildDisplayedResults(
                allResults,
                mode,
                current.filterMode
            )
        )
    }

    fun setFilterMode(mode: MarketResultFilterMode) {
        val current = _uiState.value
        _uiState.value = current.copy(
            filterMode = mode,
            results = buildDisplayedResults(
                allResults,
                current.sortMode,
                mode
            )
        )
    }

    fun setRefreshInterval(intervalMs: Long) {
        val normalized =
            MarketRefreshPolicy.normalizeInterval(intervalMs)

        _uiState.value = _uiState.value.copy(
            refreshIntervalMs = normalized
        )

        if (_uiState.value.autoRefreshEnabled) {
            startAutoRefresh()
        }
    }

    fun toggleAutoRefresh() {
        if (_uiState.value.autoRefreshEnabled) {
            stopAutoRefresh()
        } else {
            _uiState.value = _uiState.value.copy(
                autoRefreshEnabled = true
            )
            startAutoRefresh()
            scanMarkets()
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()

        autoRefreshJob = viewModelScope.launch {
            while (_uiState.value.autoRefreshEnabled) {
                delay(_uiState.value.refreshIntervalMs)

                if (_uiState.value.autoRefreshEnabled) {
                    scanMarkets()
                }
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null

        _uiState.value = _uiState.value.copy(
            autoRefreshEnabled = false
        )
    }

    fun analyzeMarket(symbol: String) {
        val normalizedSymbol = symbol.trim().uppercase(java.util.Locale.ROOT)
        if (normalizedSymbol.isBlank()) return
        if (analysisJobs[normalizedSymbol]?.isActive == true) return

        analysisJobs[normalizedSymbol] = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                analysisBySymbol = _uiState.value.analysisBySymbol +
                    (normalizedSymbol to StrategyAnalysisUiState(isLoading = true))
            )
            try {
                val window = CandleAnalysisWindow.forNow(
                    nowMillis = System.currentTimeMillis(),
                    intervalSeconds = DEFAULT_CANDLE_INTERVAL_SECONDS,
                    lookbackHours = DEFAULT_CANDLE_LOOKBACK_HOURS
                )
                val result = analysisCoordinator.analyze(
                    symbol = normalizedSymbol,
                    resolution = DEFAULT_CANDLE_RESOLUTION,
                    fromUnixSeconds = window.fromUnixSeconds,
                    toUnixSeconds = window.toUnixSeconds
                )
                _uiState.value = _uiState.value.copy(
                    analysisBySymbol = _uiState.value.analysisBySymbol +
                        (normalizedSymbol to StrategyAnalysisUiState(result = result))
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    analysisBySymbol = _uiState.value.analysisBySymbol +
                        (normalizedSymbol to StrategyAnalysisUiState(
                            error = e.message ?: "تحلیل ناموفق بود"
                        ))
                )
            } finally {
                // A cancelled older request must not remove a newer request
                // registered for the same symbol after clearResults().
                if (analysisJobs[normalizedSymbol] === coroutineContext[Job]) {
                    analysisJobs.remove(normalizedSymbol)
                }
            }
        }
    }

    fun clearResults() {
        cancelScan()
        analysisJobs.values.forEach { it.cancel() }
        analysisJobs.clear()
        allResults = emptyList()

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            results = emptyList(),
            errorMessage = null,
            completedCount = 0,
            totalCount = 0,
            successfulCount = 0,
            failedCount = 0,
            analysisBySymbol = emptyMap()
        )
    }

    fun cancelScan() {
        scanGeneration++
        scanJob?.cancel()
        scanJob = null

        _uiState.value = _uiState.value.copy(
            isScanning = false
        )
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        scanJob?.cancel()
        analysisJobs.values.forEach { it.cancel() }
        analysisJobs.clear()
        super.onCleared()
    }
}


/**
 * Request bounds end at the opening timestamp of the last fully closed candle.
 * This helper does not alter any indicator, price, volume, or risk calculation.
 */
internal data class CandleAnalysisWindow(
    val fromUnixSeconds: Long,
    val toUnixSeconds: Long
) {
    companion object {
        fun forNow(
            nowMillis: Long,
            intervalSeconds: Long,
            lookbackHours: Long
        ): CandleAnalysisWindow {
            require(nowMillis > 0L) { "nowMillis must be positive" }
            require(intervalSeconds > 0L) { "intervalSeconds must be positive" }
            require(lookbackHours > 0L) { "lookbackHours must be positive" }

            val nowSeconds = nowMillis / 1_000L
            val currentCandleOpen = nowSeconds - (nowSeconds % intervalSeconds)
            val lastClosedCandleOpen = currentCandleOpen - intervalSeconds
            val lookbackSeconds = Math.multiplyExact(lookbackHours, 3_600L)
            val from = Math.subtractExact(lastClosedCandleOpen, lookbackSeconds)

            require(from >= 0L) { "Analysis window starts before Unix epoch" }
            return CandleAnalysisWindow(from, lastClosedCandleOpen)
        }
    }
}

private const val DEFAULT_CANDLE_RESOLUTION = "15"
private const val DEFAULT_CANDLE_INTERVAL_SECONDS = 900L
private const val DEFAULT_CANDLE_LOOKBACK_HOURS = 48L
