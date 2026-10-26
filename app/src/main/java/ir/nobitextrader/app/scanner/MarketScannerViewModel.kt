
package ir.nobitextrader.app.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val sortMode: MarketSortMode = MarketSortMode.BALANCED_PRIORITY
)

class MarketScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MarketScannerUiState())
    val uiState: StateFlow<MarketScannerUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

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

                val sorted = MarketScanner.sortResults(
                    results,
                    _uiState.value.sortMode
                )

                _uiState.value = _uiState.value.copy(
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
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    override fun onCleared() {
        scanJob?.cancel()
        super.onCleared()
    }
}
