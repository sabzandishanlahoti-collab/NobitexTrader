
package ir.nobitextrader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NobitexCandlesApiTest {

    @Test
    fun parsesValidCandlesAndSortsByTimestamp() {
        val response = """
            {
              "s": "ok",
              "t": [120, 60],
              "o": [102, 100],
              "h": [105, 103],
              "l": [101, 99],
              "c": [104, 102],
              "v": [12, 10]
            }
        """.trimIndent()

        val candles = NobitexCandlesApi.parseResponse(response)

        assertEquals(2, candles.size)
        assertEquals(60_000L, candles[0].timestampMillis)
        assertEquals(120_000L, candles[1].timestampMillis)
        assertEquals(102.0, candles[0].close, 0.000001)
    }

    @Test
    fun noDataReturnsEmptyList() {
        val candles = NobitexCandlesApi.parseResponse(
            """{"s":"no_data"}"""
        )

        assertTrue(candles.isEmpty())
    }

    @Test(expected = IOException::class)
    fun invalidJsonThrowsIOException() {
        NobitexCandlesApi.parseResponse(
            "<html>Access denied</html>"
        )
    }

    @Test(expected = IOException::class)
    fun missingCandleArrayThrowsIOException() {
        NobitexCandlesApi.parseResponse(
            """{"s":"ok","t":[],"o":[],"h":[],"l":[],"c":[]}"""
        )
    }

    @Test(expected = IOException::class)
    fun mismatchedArrayLengthsThrowIOException() {
        NobitexCandlesApi.parseResponse(
            """
            {
              "s":"ok",
              "t":[60,120],
              "o":[100],
              "h":[101,102],
              "l":[99,100],
              "c":[100,101],
              "v":[1,2]
            }
            """.trimIndent()
        )
    }
}
