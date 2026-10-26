
package ir.nobitextrader.app.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MarketScannerUiState(
    val isScanning: Boolean = false,
    val results: List<MarketScanResult> = emptyList(),
    val errorMessage: String? = null,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val sortMode: MarketSortMode = MarketSortMode.SPREAD_LOW_TO_HIGH
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

                val currentMode = _uiState.value.sortMode
                val sortedResults = MarketScanner.sortResults(
                    results = results,
                    mode = currentMode
                )

                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    results = sortedResults,
                    errorMessage = null,
                    completedCount = results.size,
                    totalCount = results.size
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = exception.message
                        ?: "اسکن بازار ناموفق بود."
                )
            }
        }
    }

    fun setSortMode(mode: MarketSortMode) {
        val current = _uiState.value
        _uiState.value = current.copy(
            sortMode = mode,
            results = MarketScanner.sortResults(
                results = current.results,
                mode = mode
            )
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
        scanJob?.cancel()
        super.onCleared()
    }
}
