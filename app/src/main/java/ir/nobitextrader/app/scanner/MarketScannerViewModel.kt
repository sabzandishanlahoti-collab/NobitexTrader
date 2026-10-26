
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
    val totalCount: Int = 0
)

class MarketScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        MarketScannerUiState()
    )

    val uiState: StateFlow<MarketScannerUiState> =
        _uiState.asStateFlow()

    private var scanJob: Job? = null

    /**
     * Null means scan every market returned by Nobitex.
     * A supplied list scans only those requested markets.
     */
    fun scanMarkets(
        symbols: List<String>? = null
    ) {
        if (_uiState.value.isScanning) return

        if (symbols != null && symbols.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "فهرست بازارها خالی است."
            )
            return
        }

        val requestedCount = symbols?.distinct()?.size ?: 0

        _uiState.value = MarketScannerUiState(
            isScanning = true,
            totalCount = requestedCount
        )

        scanJob = viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    if (symbols == null) {
                        MarketScanner.scanAll()
                    } else {
                        MarketScanner.scan(symbols)
                    }
                }

                _uiState.value = MarketScannerUiState(
                    isScanning = false,
                    results = results,
                    errorMessage = if (results.isEmpty()) {
                        "هیچ نتیجه‌ای از اسکن دریافت نشد."
                    } else {
                        null
                    },
                    completedCount = results.size,
                    totalCount = results.size
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = e.message
                        ?: "خطا هنگام دریافت اطلاعات بازارها",
                    completedCount = 0
                )
            } finally {
                scanJob = null
            }
        }
    }

    fun clearResults() {
        if (_uiState.value.isScanning) return
        _uiState.value = MarketScannerUiState()
    }

    fun cancelScan() {
        val job = scanJob ?: return

        _uiState.value = _uiState.value.copy(
            isScanning = false,
            errorMessage = "درخواست توقف اسکن ارسال شد."
        )

        job.cancel()
        scanJob = null
    }

    override fun onCleared() {
        scanJob?.cancel()
        scanJob = null
        super.onCleared()
    }
}
