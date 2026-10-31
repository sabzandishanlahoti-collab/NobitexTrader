package ir.nobitextrader.app.ui.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.ui.components.CyberCard
import ir.nobitextrader.app.ui.components.CyberStartButton
import ir.nobitextrader.app.ui.components.CyberStopKillButton
import ir.nobitextrader.app.ui.components.CyberTokens

@Composable
fun ControlPanel(
    isBotRunning: Boolean,
    isAutoTradeEnabled: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onKillClick: () -> Unit,
    onAutoTradeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    controlsEnabled: Boolean = false
) {
    CyberCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        borderColor = CyberTokens.NeonTurquoise.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "تنظیمات ربات",
                        color = CyberTokens.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (controlsEnabled) "معامله خودکار" else "موتور معامله هنوز آماده اجرا نیست",
                        color = CyberTokens.TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isAutoTradeEnabled,
                    onCheckedChange = onAutoTradeToggle,
                    enabled = controlsEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberTokens.NeonTurquoise,
                        checkedTrackColor = CyberTokens.NeonTurquoise.copy(alpha = 0.3f),
                        uncheckedThumbColor = CyberTokens.TextSecondary,
                        uncheckedTrackColor = CyberTokens.Surface
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberStartButton(
                    isActive = isBotRunning,
                    onClick = onStartClick,
                    enabled = controlsEnabled
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberStopKillButton(
                        text = "STOP",
                        isKill = false,
                        onClick = onStopClick,
                        modifier = Modifier.width(84.dp),
                        enabled = controlsEnabled
                    )

                    CyberStopKillButton(
                        text = "KILL",
                        isKill = true,
                        onClick = onKillClick,
                        modifier = Modifier.width(84.dp),
                        enabled = controlsEnabled
                    )
                }
            }
        }
    }
}
