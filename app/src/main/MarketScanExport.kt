
package ir.nobitextrader.app.scanner

object MarketScanExport {

    fun toCsv(
        results: List<MarketScanResult>
    ): String {
        val output = StringBuilder()

        output.append(
            "symbol,lastPrice,bestBid,bestAsk,spreadPercent," +
                "bidLiquidityTop5,askLiquidityTop5,success,error\n"
        )

        results.forEach { item ->
            output.append(
                listOf(
                    item.symbol,
                    item.lastPrice.toString(),
                    item.bestBid?.toString().orEmpty(),
                    item.bestAsk?.toString().orEmpty(),
                    item.spreadPercent?.toString().orEmpty(),
                    item.bidLiquidityTop5.toString(),
                    item.askLiquidityTop5.toString(),
                    item.success.toString(),
                    csvEscape(item.error.orEmpty())
                ).joinToString(",")
            )
            output.append('\n')
        }

        return output.toString()
    }

    private fun csvEscape(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
