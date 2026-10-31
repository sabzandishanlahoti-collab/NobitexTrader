package ir.nobitextrader.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.nobitextrader.app.data.NobitexCandlesApi
import ir.nobitextrader.app.strategy.Candle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ChartTimeframe(val label: String, val resolution: String, val seconds: Long)

@Composable
internal fun BtcChartScreen(english: Boolean) {
    val frames = remember {
        listOf(
            ChartTimeframe("15m", "15", 900L),
            ChartTimeframe("1H", "60", 3_600L),
            ChartTimeframe("4H", "240", 14_400L),
            ChartTimeframe("1D", "D", 86_400L)
        )
    }
    var selected by remember { mutableStateOf(frames[1]) }
    var candles by remember { mutableStateOf<List<Candle>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var updatedAt by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selected) {
        loading = true
        error = null
        try {
            val to = System.currentTimeMillis() / 1_000L
            candles = NobitexCandlesApi.fetchCandles(
                symbol = "BTCIRT",
                resolution = selected.resolution,
                fromUnixSeconds = to - selected.seconds * 120L,
                toUnixSeconds = to
            )
            updatedAt = System.currentTimeMillis()
            if (candles.isEmpty()) error = if (english) "No candle data returned." else "داده کندلی دریافت نشد."
        } catch (e: Exception) {
            error = e.message ?: "Candle request failed"
            candles = emptyList()
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BTC / IRT", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
        Text(
            if (english) "NOBITEX • OHLC CANDLES • READ ONLY" else "کندل واقعی نوبیتکس • فقط مشاهده",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            frames.forEach { frame ->
                if (selected == frame) {
                    Button(onClick = { selected = frame }, modifier = Modifier.weight(1f)) {
                        Text(frame.label, fontSize = 11.sp)
                    }
                } else {
                    OutlinedButton(onClick = { selected = frame }, modifier = Modifier.weight(1f)) {
                        Text(frame.label, fontSize = 11.sp)
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (english) "MARKET CHART" else "نمودار بازار",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (loading) CircularProgressIndicator(Modifier.width(18.dp).height(18.dp), strokeWidth = 2.dp)
                    else Text(if (english) "REST DATA" else "داده REST", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                }
                if (candles.isNotEmpty()) {
                    CandlestickPlot(candles = candles.takeLast(100), modifier = Modifier.fillMaxWidth().height(270.dp))
                    val last = candles.last()
                    Text(
                        (if (english) "Last close: " else "آخرین بسته‌شدن: ") + String.format(Locale.US, "%,.0f", last.close),
                        color = if (last.close >= last.open) AfaColorSchemes.profit else AfaColorSchemes.loss,
                        fontSize = 13.sp
                    )
                    Text(
                        (if (english) "Candles: " else "تعداد کندل: ") + candles.size,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                } else if (loading) {
                    Text(if (english) "Loading verified OHLC history…" else "در حال دریافت تاریخچه کندل…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(190.dp))
                } else {
                    Text(error ?: if (english) "No data." else "داده‌ای موجود نیست.", color = AfaColorSchemes.loss, fontSize = 12.sp)
                    Spacer(Modifier.height(150.dp))
                }
                updatedAt?.let {
                    Text(
                        (if (english) "Fetched: " else "زمان دریافت: ") + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
                Text(
                    if (english) "REST polling • Not a WebSocket stream • No trading orders" else "دریافت REST • بدون جریان WebSocket • بدون ثبت سفارش",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun CandlestickPlot(candles: List<Candle>, modifier: Modifier = Modifier) {
    val up = AfaColorSchemes.profit
    val down = AfaColorSchemes.loss
    Canvas(modifier = modifier) {
        if (candles.isEmpty()) return@Canvas
        val minPrice = candles.minOf { it.low }
        val maxPrice = candles.maxOf { it.high }
        val span = (maxPrice - minPrice).takeIf { it > 0.0 } ?: (maxPrice * 0.001)
        val top = size.height * 0.04f
        val bottom = size.height * 0.96f
        val usable = bottom - top
        val step = size.width / candles.size
        val bodyWidth = (step * 0.58f).coerceAtLeast(2f)
        fun y(price: Double): Float = bottom - (((price - minPrice) / span).toFloat() * usable)
        for ((index, candle) in candles.withIndex()) {
            val x = step * index + step / 2f
            val color = if (candle.close >= candle.open) up else down
            drawLine(color, Offset(x, y(candle.high)), Offset(x, y(candle.low)), strokeWidth = 1.5f)
            val bodyTop = minOf(y(candle.open), y(candle.close))
            val bodyBottom = maxOf(y(candle.open), y(candle.close))
            val bodyHeight = (bodyBottom - bodyTop).coerceAtLeast(2f)
            drawRect(
                color = color,
                topLeft = Offset(x - bodyWidth / 2f, bodyTop),
                size = Size(bodyWidth, bodyHeight)
            )
        }
    }
}
