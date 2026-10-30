package ir.nobitextrader.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CandleAnalysisWindowTest {

    @Test
    fun excludesTheCurrentlyFormingFifteenMinuteCandle() {
        // 2026-10-30 10:37:42 UTC
        val window = CandleAnalysisWindow.forNow(
            nowMillis = 1_793_356_662_000L,
            intervalSeconds = 900L,
            lookbackHours = 48L
        )

        // Last fully closed candle opened at 10:15 UTC.
        assertEquals(1_793_355_300L, window.toUnixSeconds)
        assertEquals(1_793_182_500L, window.fromUnixSeconds)
        assertEquals(172_800L, window.toUnixSeconds - window.fromUnixSeconds)
    }

    @Test
    fun exactCandleBoundaryStillExcludesTheCandleThatJustOpened() {
        // 2026-10-30 10:30:00 UTC
        val window = CandleAnalysisWindow.forNow(
            nowMillis = 1_793_356_200_000L,
            intervalSeconds = 900L,
            lookbackHours = 48L
        )

        // The 10:30 candle has just opened; 10:15 is the last closed candle.
        assertEquals(1_793_355_300L, window.toUnixSeconds)
        assertEquals(172_800L, window.toUnixSeconds - window.fromUnixSeconds)
    }

    @Test
    fun oneSecondBeforeBoundaryUsesThePreviousClosedCandle() {
        // 2026-10-30 10:29:59 UTC
        val window = CandleAnalysisWindow.forNow(
            nowMillis = 1_793_356_199_000L,
            intervalSeconds = 900L,
            lookbackHours = 48L
        )

        assertEquals(1_793_354_400L, window.toUnixSeconds)
        assertEquals(172_800L, window.toUnixSeconds - window.fromUnixSeconds)
    }

    @Test
    fun windowHasStrictlyIncreasingUnixBounds() {
        val window = CandleAnalysisWindow.forNow(
            nowMillis = 1_793_356_662_000L,
            intervalSeconds = 900L,
            lookbackHours = 48L
        )

        assertTrue(window.fromUnixSeconds >= 0L)
        assertTrue(window.fromUnixSeconds < window.toUnixSeconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroInterval() {
        CandleAnalysisWindow.forNow(
            nowMillis = 1_793_356_662_000L,
            intervalSeconds = 0L,
            lookbackHours = 48L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroLookback() {
        CandleAnalysisWindow.forNow(
            nowMillis = 1_793_356_662_000L,
            intervalSeconds = 900L,
            lookbackHours = 0L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWindowBeforeUnixEpoch() {
        CandleAnalysisWindow.forNow(
            nowMillis = 1_000L,
            intervalSeconds = 900L,
            lookbackHours = 48L
        )
    }
}
