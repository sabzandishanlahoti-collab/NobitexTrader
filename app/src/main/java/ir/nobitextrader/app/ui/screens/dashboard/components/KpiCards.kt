package ir.nobitextrader.app.ui.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.ui.components.CyberCard
import ir.nobitextrader.app.ui.components.CyberTokens

@Composable
fun TopKpiRow(
    initialCapital: String,
    capitalInUse: String,
    netProfit: String,
    isProfitPositive: Boolean?,
    depositWithdraw: String,
    totalTrades: String,
    modifier: Modifier = Modifier
) {
    val profitColor = when (isProfitPositive) {
        true -> CyberTokens.NeonGreen
        false -> CyberTokens.NeonRed
        null -> CyberTokens.TextSecondary
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        item {
            KpiItemCard(
                title = "سرمایه اولیه",
                value = initialCapital,
                icon = Icons.Default.AccountBalanceWallet,
                iconColor = CyberTokens.NeonTurquoise
            )
        }
        item {
            KpiItemCard(
                title = "سرمایه در گردش",
                value = capitalInUse,
                icon = Icons.Default.PieChart,
                iconColor = CyberTokens.NeonTurquoise
            )
        }
        item {
            KpiItemCard(
                title = "سود خالص",
                value = netProfit,
                icon = Icons.Default.TrendingUp,
                iconColor = profitColor,
                valueColor = profitColor
            )
        }
        item {
            KpiItemCard(
                title = "واریز / برداشت",
                value = depositWithdraw,
                icon = Icons.Default.SyncAlt,
                iconColor = CyberTokens.TextSecondary
            )
        }
        item {
            KpiItemCard(
                title = "مجموع معاملات",
                value = totalTrades,
                icon = Icons.Default.Receipt,
                iconColor = CyberTokens.NeonTurquoise
            )
        }
    }
}

@Composable
private fun KpiItemCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    valueColor: Color = CyberTokens.TextPrimary
) {
    CyberCard(
        modifier = Modifier.width(130.dp),
        borderColor = CyberTokens.NeonTurquoise.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = CyberTokens.TextSecondary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TradeStatsRow(
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
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            TradeStatItem(
                title = "پوزیشن‌های لانگ",
                count = longPositionsCount,
                volume = longPositionsVolume,
                color = CyberTokens.NeonGreen
            )
        }
        item {
            TradeStatItem(
                title = "پوزیشن‌های شورت",
                count = shortPositionsCount,
                volume = shortPositionsVolume,
                color = CyberTokens.NeonRed
            )
        }
        item {
            TradeStatItem(
                title = "خریدهای امروز",
                count = todayBuysCount,
                volume = todayBuysVolume,
                color = CyberTokens.NeonGreen
            )
        }
        item {
            TradeStatItem(
                title = "فروش‌های امروز",
                count = todaySellsCount,
                volume = todaySellsVolume,
                color = CyberTokens.NeonRed
            )
        }
    }
}

@Composable
private fun TradeStatItem(
    title: String,
    count: String,
    volume: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    CyberCard(
        modifier = modifier.width(142.dp),
        borderColor = color.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = CyberTokens.TextSecondary,
                fontSize = 10.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = volume,
                color = CyberTokens.TextSecondary,
                fontSize = 9.sp
            )
        }
    }
}
