
package ir.nobitextrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
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
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NobitexTrader",
                fontSize = 28.sp
            )

            Text(
                text = "ربات هوشمند معاملات",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 20.sp
            )

            Text(
                text = "اتصال به صرافی هنوز راه‌اندازی نشده است",
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(
                onClick = {},
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("آماده‌سازی پروژه")
            }
        }
    }
}
