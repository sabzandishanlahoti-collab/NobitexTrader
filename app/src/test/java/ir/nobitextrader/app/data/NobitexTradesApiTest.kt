package ir.nobitextrader.app.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NobitexTradesApiTest {

    @Test
    fun parsesPublicBuyAndSellTrades() {
        val result = NobitexTradesApi.parseResponse(
            """{"status":"ok","trades":[
                {"time":1588689375067,"price":"1470000110","volume":"0.002","type":"sell"},
                {"time":1588689360464,"price":"1470000100","volume":"0.5","type":"buy"}
            ]}"""
        )

        assertEquals(2, result.size)
        assertEquals(1588689375067000L, result[0].timeMillis)
        assertEquals(1_470_000_110.0, result[0].price, 0.0)
        assertEquals(0.002, result[0].volume, 0.0)
        assertEquals(TradeSide.SELL, result[0].type)
        assertEquals(TradeSide.BUY, result[1].type)
        assertEquals(735_000_050.0, result[1].quoteVolume, 0.0)
    }

    @Test
    fun acceptsEmptyTradesArray() {
        assertTrue(
            NobitexTradesApi.parseResponse(
                """{"status":"ok","trades":[]}"""
            ).isEmpty()
        )
    }

    @Test
    fun rejectsUnknownSideAndInvalidPrice() {
        assertIOException {
            NobitexTradesApi.parseResponse(
                """{"status":"ok","trades":[{"time":1,"price":2,"volume":1,"type":"hold"}]}"""
            )
        }
        assertIOException {
            NobitexTradesApi.parseResponse(
                """{"status":"ok","trades":[{"time":1,"price":0,"volume":1,"type":"buy"}]}"""
            )
        }
    }

    @Test
    fun rejectsMissingTradesAndFailedStatus() {
        assertIOException {
            NobitexTradesApi.parseResponse("""{"status":"ok"}""")
        }
        assertIOException {
            NobitexTradesApi.parseResponse("""{"status":"failed","trades":[]}""")
        }
    }

    @Test
    fun rejectsNegativeVolumeAndInvalidTimestamp() {
        assertIOException {
            NobitexTradesApi.parseResponse(
                """{"status":"ok","trades":[{"time":1,"price":2,"volume":-1,"type":"buy"}]}"""
            )
        }
        assertIOException {
            NobitexTradesApi.parseResponse(
                """{"status":"ok","trades":[{"time":0,"price":2,"volume":1,"type":"buy"}]}"""
            )
        }
    }

    private fun assertIOException(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IOException")
        } catch (_: IOException) {
            // Expected malformed public trade response.
        }
    }
}
