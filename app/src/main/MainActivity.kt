
package ir.nobitextrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import ir.nobitextrader.app.scanner.MarketScannerScreen
import ir.nobitextrader.app.scanner.MarketScannerViewModel
import ir.nobitextrader.app.scanner.MarketScannerViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scannerViewModel = ViewModelProvider(
            this,
            MarketScannerViewModelFactory()
        )[MarketScannerViewModel::class.java]

        setContent {
            MaterialTheme {
                Surface {
                    MarketScannerScreen(
                        viewModel = scannerViewModel,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
