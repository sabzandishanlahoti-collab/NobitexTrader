package ir.nobitextrader.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.ui.components.CyberCard
import ir.nobitextrader.app.ui.components.CyberTokens
import ir.nobitextrader.app.ui.screens.dashboard.components.*

@Composable
fun DashboardScreen(
    isConnected: Boolean,
    batteryLevel: Int?,
    onSettingsClick: () -> Unit,
    tickers: List<TickerData>,
    initialCapital: String,
    capitalInUse: String,
    netProfit: String,
    isProfitPositive: Boolean?,
    depositWithdraw: String,
    totalTrades: String,
    isBotRunning: Boolean,
    isAutoTradeEnabled: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onKillClick: () -> Unit,
    onAutoTradeToggle: (Boolean) -> Unit,
    longPositionsCount: String,
    longPositionsVolume: String,
    shortPositionsCount: String,
    shortPositionsVolume: String,
    todayBuysCount: String,
    todayBuysVolume: String,
    todaySellsCount: String,
    todaySellsVolume: String,
    onScannerClick: () -> Unit = {},
    onScanMarketsClick: () -> Unit = {},
    onPaperTradingClick: () -> Unit = {},
    isScanning: Boolean = false,
    scanProgressText: String? = null,
    engineControlsEnabled: Boolean = false,
    engineStatusText: String = "موتور معاملات مجازی متوقف است",
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberTokens.Background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(key = "dashboard_header") {
            DashboardHeader(
                isConnected = isConnected,
                isApiHealthy = false,
                batteryLevel = batteryLevel,
                onSettingsClick = onSettingsClick
            )
        }

        item(key = "market_ticker") {
            MarketTicker(tickers = tickers)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item(key = "market_scanner_access") {
            CyberCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                borderColor = CyberTokens.NeonTurquoise.copy(alpha = 0.25f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "رادار بازار نوبیتکس",
                        color = CyberTokens.TextPrimary,
                        fontSize = 14.sp
                    )
                    scanProgressText?.let {
                        Text(
                            text = it,
                            color = CyberTokens.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onScanMarketsClick,
                            enabled = !isScanning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isScanning) "در حال اسکن…" else "اسکن همه نمادها")
                        }
                        OutlinedButton(
                            onClick = onScannerClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("صفحه اسکنر")
                        }
                    }
                    OutlinedButton(onClick = onPaperTradingClick, modifier = Modifier.fillMaxWidth()) {
                        Text("معاملات مجازی با پول کاغذی")
                    }
                }
            }
        }

        item(key = "top_kpis") {
            TopKpiRow(
                initialCapital = initialCapital,
                capitalInUse = capitalInUse,
                netProfit = netProfit,
                isProfitPositive = isProfitPositive,
                depositWithdraw = depositWithdraw,
                totalTrades = totalTrades
            )
        }

        item(key = "control_panel") {
            ControlPanel(
                isBotRunning = isBotRunning,
                isAutoTradeEnabled = isAutoTradeEnabled,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onKillClick = onKillClick,
                onAutoTradeToggle = onAutoTradeToggle,
                controlsEnabled = engineControlsEnabled,
                engineStatusText = engineStatusText
            )
        }

        item(key = "trade_stats") {
            TradeStatsRow(
                longPositionsCount = longPositionsCount,
                longPositionsVolume = longPositionsVolume,
                shortPositionsCount = shortPositionsCount,
                shortPositionsVolume = shortPositionsVolume,
                todayBuysCount = todayBuysCount,
                todayBuysVolume = todayBuysVolume,
                todaySellsCount = todaySellsCount,
                todaySellsVolume = todaySellsVolume
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
