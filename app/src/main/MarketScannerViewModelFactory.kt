
package ir.nobitextrader.app.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MarketScannerViewModelFactory :
    ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                MarketScannerViewModel::class.java
            )
        ) {
            return MarketScannerViewModel() as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
