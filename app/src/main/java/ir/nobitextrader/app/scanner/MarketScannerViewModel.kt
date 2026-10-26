
package ir.nobitextrader.app.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun scanMarkets(symbols: List<String>? = null) {
        if (scanJob?.isActive == true) return

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
                        MarketScanner.scanAll()
                    } else {
                        MarketScanner.scan(symbols)
                    }
                }

                val current = _uiState.value
                val sorted = MarketScanner.sortResults(
                    results,
                    current.sortMode
                )

                _uiState.value = current.copy(
                    isScanning = false,
                    results = sorted,
                    errorMessage = null,
                    completedCount = results.size,
                    totalCount = results.size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = e.message ?: "اسکن بازار ناموفق بود."
                )
            }
        }
    }

    fun setSortMode(mode: MarketSortMode) {
        val current = _uiState.value
        _uiState.value = current.copy(
            sortMode = mode,
            results = MarketScanner.sortResults(current.results, mode)
        )
    }

    fun setFilterMode(mode: MarketResultFilterMode) {
        _uiState.value = _uiState.value.copy(filterMode = mode)
    }

    fun setRefreshInterval(intervalMs: Long) {
        _uiState.value = _uiState.value.copy(
            refreshIntervalMs = MarketRefreshPolicy.normalizeInterval(intervalMs)
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
        scanJob?.cancel()
        scanJob = null

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            results = emptyList(),
            errorMessage = null,
            completedCount = 0,
            totalCount = 0
        )
    }

    fun cancelScan() {
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
