package ir.nobitextrader.app.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MarketRefreshPolicyTest {

    @Test
    fun defaultPolicyUsesExpectedSafeValues() {
        val policy = MarketRefreshPolicy.DEFAULT

        assertEquals(30_000L, policy.refreshIntervalMillis)
        assertEquals(10_000L, policy.requestTimeoutMillis)
        assertEquals(10, policy.maxMarketsPerBatch)
    }

    @Test
    fun acceptsMinimumAndMaximumSupportedValues() {
        val minimum = MarketRefreshPolicy(
            refreshIntervalMillis = MarketRefreshPolicy.MIN_REFRESH_INTERVAL_MS,
            requestTimeoutMillis = 1_000L,
            maxMarketsPerBatch = 1
        )
        val maximum = MarketRefreshPolicy(
            refreshIntervalMillis = MarketRefreshPolicy.MAX_REFRESH_INTERVAL_MS,
            requestTimeoutMillis = 60_000L,
            maxMarketsPerBatch = 50
        )

        assertEquals(5_000L, minimum.refreshIntervalMillis)
        assertEquals(3_600_000L, maximum.refreshIntervalMillis)
        assertEquals(1, minimum.maxMarketsPerBatch)
        assertEquals(50, maximum.maxMarketsPerBatch)
    }

    @Test
    fun rejectsRefreshIntervalsOutsideSupportedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy(refreshIntervalMillis = 4_999L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy(refreshIntervalMillis = 3_600_001L)
        }
    }

    @Test
    fun rejectsRequestTimeoutOutsideSupportedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy(requestTimeoutMillis = 999L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy(requestTimeoutMillis = 60_001L)
        }
    }

    @Test
    fun rejectsMarketBatchSizeOutsideSupportedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy(maxMarketsPerBatch = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy(maxMarketsPerBatch = 51)
        }
    }

    @Test
    fun normalizesRefreshIntervalAtBothBoundaries() {
        assertEquals(
            MarketRefreshPolicy.MIN_REFRESH_INTERVAL_MS,
            MarketRefreshPolicy.normalizeInterval(Long.MIN_VALUE)
        )
        assertEquals(
            MarketRefreshPolicy.MAX_REFRESH_INTERVAL_MS,
            MarketRefreshPolicy.normalizeInterval(Long.MAX_VALUE)
        )
        assertEquals(
            15_000L,
            MarketRefreshPolicy.normalizeInterval(15_000L)
        )
    }

    @Test
    fun createsPolicyFromRefreshSeconds() {
        val policy = MarketRefreshPolicy.fromRefreshSeconds(
            refreshSeconds = 45L,
            requestTimeoutMillis = 20_000L,
            maxMarketsPerBatch = 25
        )

        assertEquals(45_000L, policy.refreshIntervalMillis)
        assertEquals(20_000L, policy.requestTimeoutMillis)
        assertEquals(25, policy.maxMarketsPerBatch)
    }

    @Test
    fun rejectsRefreshSecondsThatOverflowMilliseconds() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy.fromRefreshSeconds(Long.MAX_VALUE)
        }
    }

    @Test
    fun rejectsNegativeRefreshSeconds() {
        assertThrows(IllegalArgumentException::class.java) {
            MarketRefreshPolicy.fromRefreshSeconds(-1L)
        }
    }
}
