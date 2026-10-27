
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

        setRefreshInterval(refreshMs)

        if (settings.autoScanEnabled) {
            if (!_uiState.value.autoRefreshEnabled) {
                _uiState.value =
                    _uiState.value.copy(autoRefreshEnabled = true)
                startAutoRefresh()
                scanMarkets()
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

        scanJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                errorMessage = null,
                completedCount = 0,
                totalCount = 0
            )

            try {
                val results = withContext(Dispatchers.IO) {
                    if (symbols == null) {
                        val all = MarketScanner.scanAll(policy)
                        all.take(maxMarketsPerBatch)
                    } else {
                        MarketScanner.scan(
                            symbols = symbols.take(maxMarketsPerBatch),
                            policy = policy
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
                    _uiState.value = _uiState.value.copy(
                        isScanning = false,
                        errorMessage = e.message
                            ?: "اسکن بازار ناموفق بود."
                    )
                }
            } finally {
                if (generation == scanGeneration) {
                    _uiState.value =
                        _uiState.value.copy(isScanning = false)
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

        _uiState.value =
            _uiState.value.copy(refreshIntervalMs = normalized)

        if (_uiState.value.autoRefreshEnabled) {
            startAutoRefresh()
        }
    }

    fun toggleAutoRefresh() {
        if (_uiState.value.autoRefreshEnabled) {
            stopAutoRefresh()
        } else {
            _uiState.value =
                _uiState.value.copy(autoRefreshEnabled = true)
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

        _uiState.value =
            _uiState.value.copy(autoRefreshEnabled = false)
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

        _uiState.value =
            _uiState.value.copy(isScanning = false)
    }

    override fun onCleared() {
        autoRefreshJob?.cancel()
        scanJob?.cancel()
        super.onCleared()
    }
}
