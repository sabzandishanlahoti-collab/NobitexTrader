
package ir.nobitextrader.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class MarketStats(
    val market: String,
    val latest: Double,
    val bestBuy: Double,
    val bestSell: Double,
    val dayLow: Double,
    val dayHigh: Double,
    val dayChange: Double,
    val isClosed: Boolean
)

object NobitexMarketApi {

    private const val STATS_URL =
        "https://apiv2.nobitex.ir/market/stats"

    suspend fun fetchMarketStats(): MarketStats =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null

            try {
                connection = (URL(STATS_URL).openConnection()
                    as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty(
                        "User-Agent",
                        "NobitexTrader-Android"
                    )
                }

                val status = connection.responseCode
                val stream = if (status in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = stream?.bufferedReader()?.use {
                    it.readText()
                } ?: throw IOException(
                    "پاسخ خالی از نوبیتکس؛ HTTP $status"
                )

                if (status !in 200..299) {
                    throw IOException(
                        "خطای HTTP $status هنگام اتصال به نوبیتکس"
                    )
                }

                val root = JSONObject(response)
                val stats = root.optJSONObject("stats")
                    ?: throw IOException(
                        "ساختار پاسخ بازار معتبر نیست"
                    )

                val marketData = stats.optJSONObject("btc-irt")
                    ?: throw IOException(
                        "بازار BTC/IRT در پاسخ پیدا نشد"
                    )

                fun number(key: String): Double {
                    val value = marketData.opt(key)
                    val parsed = when (value) {
                        is Number -> value.toDouble()
                        is String -> value.toDoubleOrNull()
                        else -> null
                    }

                    return parsed?.takeIf { it.isFinite() }
                        ?: throw IOException(
                            "مقدار $key معتبر نیست"
                        )
                }

                val marketStats = MarketStats(
                    market = "BTC/IRT",
                    latest = number("latest"),
                    bestBuy = number("bestBuy"),
                    bestSell = number("bestSell"),
                    dayLow = number("dayLow"),
                    dayHigh = number("dayHigh"),
                    dayChange = number("dayChange"),
                    isClosed = marketData.optBoolean(
                        "isClosed",
                        false
                    )
                )

                val validationErrors =
                    MarketStatsValidator.validate(marketStats)

                if (validationErrors.isNotEmpty()) {
                    throw IOException(
                        "دادهٔ بازار نامعتبر است: " +
                            validationErrors.joinToString("; ")
                    )
                }

                marketStats
            } catch (e: Exception) {
                if (e is IOException) throw e

                throw IOException(
                    "خطا در دریافت اطلاعات: " +
                        (e.message ?: "نامشخص"),
                    e
                )
            } finally {
                connection?.disconnect()
            }
        }
}
