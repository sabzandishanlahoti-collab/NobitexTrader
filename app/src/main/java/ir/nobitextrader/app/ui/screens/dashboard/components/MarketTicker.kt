package ir.nobitextrader.app.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.ui.components.CyberTokens

data class TickerData(
    val symbol: String,
    val price: String,
    val changePercent: String,
    val isPositive: Boolean
)

@Composable
fun MarketTicker(
    tickers: List<TickerData>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberTokens.Background)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(
            items = tickers,
            key = { ticker -> ticker.symbol }
        ) { ticker ->
            TickerItem(ticker = ticker)
        }
    }
}

@Composable
private fun TickerItem(ticker: TickerData) {
    val changeColor = if (ticker.isPositive) {
        CyberTokens.NeonGreen
    } else {
        CyberTokens.NeonRed
    }
    val changeIcon = if (ticker.isPositive) "▲" else "▼"
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .background(CyberTokens.Surface)
            .border(
                width = 1.dp,
                color = CyberTokens.NeonTurquoise.copy(alpha = 0.1f),
                shape = shape
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = ticker.symbol,
                color = CyberTokens.TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = ticker.price,
                color = CyberTokens.TextPrimary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$changeIcon ${ticker.changePercent}",
            color = changeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
