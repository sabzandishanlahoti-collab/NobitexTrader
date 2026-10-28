
package ir.nobitextrader.app.data

import ir.nobitextrader.app.strategy.Candle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Live integration test for Nobitex candle history.
 *
 * This test uses the real public API.
 * It does not submit orders or place trades.
 *
 * Network access is required. A passing compile does not
 * guarantee that the live API endpoint or response format is correct.
 */
class NobitexCandlesApiTest {

    @Test
    fun fetchCandles_returnsValidOrderedCandlesFromLiveApi() =
        runBlocking {
            val toSeconds = System.currentTimeMillis() / 1000L
            val fromSeconds = toSeconds - 24L * 60L * 60L

            val candles = NobitexCandlesApi.fetchCandles(
                symbol = "BTCIRT",
                resolution = "60",
                fromUnixSeconds = fromSeconds,
                toUnixSeconds = toSeconds
            )

            assertTrue(
                "The live API returned no candles",
                candles.isNotEmpty()
            )

            assertTrue(
                "Candles must be ordered by timestamp",
                candles.zipWithNext().all { (first, second) ->
                    first.timestampMillis <= second.timestampMillis
                }
            )

            candles.forEachIndexed { index, candle ->
                assertTrue(
                    "Invalid candle at index $index",
                    candle.isValid()
                )
                assertTrue(
                    "Candle timestamp must be positive",
                    candle.timestampMillis > 0L
                )
                assertTrue(
                    "Candle close must be positive",
                    candle.close > 0.0
                )
                assertTrue(
                    "Candle volume must not be negative",
                    candle.volume >= 0.0
                )
            }

            assertEquals(
                candles.size,
                candles.distinctBy { it.timestampMillis }.size
            )
        }
}
