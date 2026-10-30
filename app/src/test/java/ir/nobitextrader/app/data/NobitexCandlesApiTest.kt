package ir.nobitextrader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class NobitexCandlesApiTest {

    @Test
    fun parsesValidCandlesAndSortsByTimestamp() {
        val response = """
            {
              "s": "ok",
              "t": [1720003600, 1720000000],
              "o": [61000, 60000],
              "h": [62000, 61000],
              "l": [60500, 59500],
              "c": [61500, 60500],
              "v": [12.5, 10.0]
            }
        """.trimIndent()

        val candles = NobitexCandlesApi.parseResponse(response)

        assertEquals(2, candles.size)
        assertTrue(
            "Candles must be ordered by timestamp",
            candles[0].timestampMillis < candles[1].timestampMillis
        )

        assertEquals(1720000000000L, candles[0].timestampMillis)
        assertEquals(60000.0, candles[0].open, 0.0)
        assertEquals(61000.0, candles[0].high, 0.0)
        assertEquals(59500.0, candles[0].low, 0.0)
        assertEquals(60500.0, candles[0].close, 0.0)
        assertEquals(10.0, candles[0].volume, 0.0)
    }

    @Test
    fun noDataReturnsEmptyList() {
        val response = """{"s":"no_data"}"""

        val candles = NobitexCandlesApi.parseResponse(response)

        assertTrue(candles.isEmpty())
    }

    @Test
    fun invalidJsonThrowsIOException() {
        assertIOException {
            NobitexCandlesApi.parseResponse("not valid json")
        }
    }

    @Test
    fun missingCandleArrayThrowsIOException() {
        val response = """
            {
              "s": "ok",
              "t": [1720000000],
              "o": [60000],
              "h": [61000],
              "l": [59000],
              "c": [60500]
            }
        """.trimIndent()

        assertIOException {
            NobitexCandlesApi.parseResponse(response)
        }
    }

    @Test
    fun mismatchedArrayLengthsThrowIOException() {
        val response = """
            {
              "s": "ok",
              "t": [1720000000, 1720003600],
              "o": [60000],
              "h": [61000, 62000],
              "l": [59000, 60000],
              "c": [60500, 61500],
              "v": [10.0, 12.0]
            }
        """.trimIndent()

        assertIOException {
            NobitexCandlesApi.parseResponse(response)
        }
    }

    private fun assertIOException(block: () -> Unit) {
        try {
            block()
            fail("Expected IOException, but no exception was thrown")
        } catch (_: IOException) {
            // Expected result.
        }
    }
}

    @Test
    fun apiErrorStatusThrowsIOException() {
        val response = """{"s":"error","errmsg":"Invalid symbol"}"""

        try {
            NobitexCandlesApi.parseResponse(response)
            fail("Expected IOException for API error status")
        } catch (exception: IOException) {
            assertTrue(exception.message.orEmpty().contains("Invalid symbol"))
        }
    }

    @Test
    fun nonPositiveTimestampThrowsIOException() {
        val response = """
            {
              "s": "ok",
              "t": [0],
              "o": [60000],
              "h": [61000],
              "l": [59000],
              "c": [60500],
              "v": [10.0]
            }
        """.trimIndent()

        assertIOException {
            NobitexCandlesApi.parseResponse(response)
        }
    }

    @Test
    fun invalidOhlcRelationshipThrowsIOException() {
        val response = """
            {
              "s": "ok",
              "t": [1720000000],
              "o": [60000],
              "h": [59000],
              "l": [59500],
              "c": [60500],
              "v": [10.0]
            }
        """.trimIndent()

        assertIOException {
            NobitexCandlesApi.parseResponse(response)
        }
    }

    @Test
    fun negativeVolumeThrowsIOException() {
        val response = """
            {
              "s": "ok",
              "t": [1720000000],
              "o": [60000],
              "h": [61000],
              "l": [59000],
              "c": [60500],
              "v": [-1.0]
            }
        """.trimIndent()

        assertIOException {
            NobitexCandlesApi.parseResponse(response)
        }
    }
}
