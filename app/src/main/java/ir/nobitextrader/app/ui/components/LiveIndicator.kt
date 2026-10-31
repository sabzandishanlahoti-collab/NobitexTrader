package ir.nobitextrader.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveIndicator(
    text: String,
    active: Boolean = text.equals("LIVE", ignoreCase = true)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (active) CyberTokens.NeonGreen else Color(0xFF71818C))
        )
        Text(
            text = text,
            color = if (active) CyberTokens.NeonGreen else CyberTokens.TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
