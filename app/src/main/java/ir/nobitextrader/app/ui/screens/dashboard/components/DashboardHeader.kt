package ir.nobitextrader.app.ui.screens.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.ui.components.CyberTokens
import ir.nobitextrader.app.ui.components.LiveIndicator

@Composable
fun DashboardHeader(
    isConnected: Boolean = false,
    isApiHealthy: Boolean = false,
    batteryLevel: Int? = null,
    onSettingsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CyberTokens.Background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = "NobitexTrader logo",
                    tint = CyberTokens.NeonTurquoise,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "NobitexTrader",
                        color = CyberTokens.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Auto Trading Bot | Crypto",
                        color = CyberTokens.TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CyberTokens.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiveIndicator(text = if (isConnected) "LIVE" else "OFFLINE")

            StatusBadge(
                text = if (isConnected) "Connected" else "Disconnected",
                icon = Icons.Default.Wifi,
                color = if (isConnected) CyberTokens.NeonGreen else CyberTokens.NeonRed
            )

            StatusBadge(
                text = if (isApiHealthy) "API OK" else "API UNKNOWN",
                icon = null,
                color = if (isApiHealthy) CyberTokens.NeonTurquoise else CyberTokens.TextSecondary
            )

            Spacer(modifier = Modifier.weight(1f))

            batteryLevel
                ?.coerceIn(0, 100)
                ?.let { level ->
                    StatusBadge(
                        text = "$level%",
                        icon = Icons.Default.BatteryFull,
                        color = CyberTokens.TextSecondary
                    )
                }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberTokens.Surface)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
