package ir.nobitextrader.app.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.nobitextrader.app.TraderSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MarketScannerUiState(
    val isScanning: Boolean = false,
    val results: List<MarketScanResult> = emptyList(),
    val errorMessage: String? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val sortMode: MarketSortMode = MarketSortMode.BALANCED_PRIORITY,
    val filterMode: MarketResultFilterMode = MarketResultFilterMode.ALL,
    val autoRefreshEnabled: Boolean = false,
    val refreshIntervalMs: Long = MarketRefreshPolicy.DEFAULT_INTERVAL_MS
)

class MarketScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MarketScannerUiState())
    val uiState: StateFlow<MarketScannerUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var autoRefreshJob: Job? = null
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
                totalCount = 0
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
                                val current = _uiState.value

                                _uiState.value = current.copy(
                                    results = buildDisplayedResults(
                                        results = snapshot,
                                        sortMode = current.sortMode,
                                        filterMode = current.filterMode
                                    ),
                                    completedCount = snapshot.size
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
                    totalCount = results.size
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
                            ?: "اسکن بازار ناموفق بود."
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

    fun clearResults() {
        cancelScan()
        allResults = emptyList()

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            results = emptyList(),
            errorMessage = null,
            completedCount = 0,
            totalCount = 0
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
        super.onCleared()
    }
}
