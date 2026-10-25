
package ir.nobitextrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                NobitexTraderApp()
            }
        }
    }
}

@Composable
fun NobitexTraderApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "NobitexTrader",
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ربات معاملات نوبیتکس",
                fontSize = 21.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "نسخه اولیه برنامه",
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = {}) {
                Text("آماده‌سازی")
            }
        }
    }
}
