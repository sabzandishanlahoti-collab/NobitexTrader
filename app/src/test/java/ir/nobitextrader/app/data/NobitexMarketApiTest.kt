package ir.nobitextrader.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class NobitexMarketApiTest {

    @Test
    fun parsesAllMarketLatestPricesAndNormalizesRialSymbols() {
        val response = """
            {
              "status": "ok",
              "stats": {
                "btc-rls": {"latest": "100.5"},
                "eth-usdt": {"latest": 2500},
                "usdt-rls": {"latest": "610000"},
                "global": {"latest": "999999"}
              }
            }
        """.trimIndent()

        assertEquals(
            mapOf(
                "BTCIRT" to 100.5,
                "ETHUSDT" to 2500.0,
                "USDTIRT" to 610000.0
            ),
            NobitexMarketApi.parseAllLatestPrices(response)
        )
    }

    @Test
    fun skipsMalformedMarketsWithoutFabricatingPrices() {
        val response = """
            {
              "status": "ok",
              "stats": {
                "btc-rls": {"latest": "100.5"},
                "eth-rls": {"latest": "not-a-price"},
                "ltc-rls": {"latest": 0},
                "xrp-usdt": {"dayChange": "2.1"}
              }
            }
        """.trimIndent()

        assertEquals(
            mapOf("BTCIRT" to 100.5),
            NobitexMarketApi.parseAllLatestPrices(response)
        )
    }

    @Test
    fun rejectsUnsuccessfulOrEmptyAllMarketStatsResponses() {
        assertIOException {
            NobitexMarketApi.parseAllLatestPrices(
                """{"status":"failed","stats":{}}"""
            )
        }
        assertIOException {
            NobitexMarketApi.parseAllLatestPrices(
                """{"status":"ok","stats":{"global":{"latest":"1"}}}"""
            )
        }
        assertIOException {
            NobitexMarketApi.parseAllLatestPrices("not-json")
        }
    }

    private fun assertIOException(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IOException")
        } catch (_: IOException) {
            // Expected invalid or unusable public API response.
        }
    }
}
