package ir.nobitextrader.app.data

import ir.nobitextrader.app.strategy.Candle

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

    @Test
    fun blankResponseThrowsIOException() {
        assertIOException {
            NobitexCandlesApi.parseResponse("  \\n")
        }
    }

    @Test
    fun unexpectedResponseStatusThrowsIOException() {
        assertIOException {
            NobitexCandlesApi.parseResponse("""{"s":"unknown"}""")
        }
    }

    @Test
    fun missingVolumeArrayThrowsIOException() {
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
    fun negativeTimestampThrowsIOException() {
        val response = """
            {
              "s": "ok",
              "t": [-1],
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
    fun timestampThatOverflowsMillisecondsThrowsIOException() {
        val response = """
            {
              "s": "ok",
              "t": [9223372036854776],
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
    fun zeroVolumeIsAccepted() {
        val response = """
            {
              "s": "ok",
              "t": [1720000000],
              "o": [60000],
              "h": [61000],
              "l": [59000],
              "c": [60500],
              "v": [0.0]
            }
        """.trimIndent()

        val candles = NobitexCandlesApi.parseResponse(response)

        assertEquals(1, candles.size)
        assertEquals(0.0, candles.single().volume, 0.0)
    }


    @Test
    fun candleSeriesValidatorAcceptsFreshContinuousHistory() {
        val candles = validSeries()
        CandleSeriesValidator.validate(
            candles = candles,
            resolution = "5",
            nowUnixSeconds = candles.last().timestampMillis / 1_000L + 300L
        )
    }

    @Test
    fun candleSeriesValidatorRejectsDuplicateTimestamps() {
        val candles = validSeries().toMutableList()
        candles[10] = candles[10].copy(timestampMillis = candles[9].timestampMillis)
        assertIllegalArgument {
            CandleSeriesValidator.validate(
                candles, "5", candles.last().timestampMillis / 1_000L + 300L
            )
        }
    }

    @Test
    fun candleSeriesValidatorRejectsMissingIntervals() {
        val candles = validSeries().toMutableList()
        candles[10] = candles[10].copy(timestampMillis = candles[10].timestampMillis + 300_000L)
        assertIllegalArgument {
            CandleSeriesValidator.validate(
                candles, "5", candles.last().timestampMillis / 1_000L + 600L
            )
        }
    }

    @Test
    fun candleSeriesValidatorRejectsStaleLatestCandle() {
        val candles = validSeries()
        assertIllegalArgument {
            CandleSeriesValidator.validate(
                candles, "5", candles.last().timestampMillis / 1_000L + 901L
            )
        }
    }

    @Test
    fun candleSeriesValidatorRejectsStillOpenLatestCandle() {
        val candles = validSeries()
        assertIllegalArgument {
            CandleSeriesValidator.validate(
                candles, "5", candles.last().timestampMillis / 1_000L + 299L
            )
        }
    }

    @Test
    fun candleSeriesValidatorRejectsInsufficientHistory() {
        val candles = validSeries().take(19)
        assertIllegalArgument {
            CandleSeriesValidator.validate(
                candles, "5", candles.last().timestampMillis / 1_000L + 300L
            )
        }
    }

    @Test
    fun candleSeriesValidatorRejectsUnsupportedResolution() {
        val candles = validSeries()
        assertIllegalArgument {
            CandleSeriesValidator.validate(
                candles, "7", candles.last().timestampMillis / 1_000L + 300L
            )
        }
    }

    private fun validSeries(): List<Candle> {
        val firstTimestampSeconds = 1_700_000_000L
        return List(20) { index ->
            Candle(
                timestampMillis = (firstTimestampSeconds + index * 300L) * 1_000L,
                open = 100.0,
                high = 102.0,
                low = 99.0,
                close = 101.0,
                volume = 10.0
            )
        }
    }

    private fun assertIllegalArgument(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected validation failure.
        }
    }

}
