package ir.nobitextrader.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.nobitextrader.app.ui.components.CyberTokens
import ir.nobitextrader.app.ui.screens.dashboard.components.*

@Composable
fun DashboardScreen(
    isConnected: Boolean,
    batteryLevel: Int,
    onSettingsClick: () -> Unit,
    tickers: List<TickerData>,
    initialCapital: String,
    capitalInUse: String,
    netProfit: String,
    isProfitPositive: Boolean,
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
                batteryLevel = batteryLevel,
                onSettingsClick = onSettingsClick
            )
        }

        item(key = "market_ticker") {
            MarketTicker(tickers = tickers)
            Spacer(modifier = Modifier.height(8.dp))
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
                onAutoTradeToggle = onAutoTradeToggle
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
