package ir.nobitextrader.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NobitexOrderBookApiTest {

    private fun snapshot(json: String): OrderBookSnapshot =
        NobitexOrderBookApi.parseSnapshot("BTCIRT", JSONObject(json))

    private fun assertIOException(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected IOException")
        } catch (_: IOException) {
            // Expected malformed order-book response.
        }
    }

    @Test
    fun parsesAndSortsBidAndAskLevels() {
        val result = snapshot(
            """{"lastTradePrice":"100.5","lastUpdate":1234,"bids":[[99,2],[101,1],[100,3]],"asks":[[103,1],[102,2],[104,1]]}"""
        )

        assertEquals(101.0, result.bestBid!!, 0.0)
        assertEquals(102.0, result.bestAsk!!, 0.0)
        assertEquals(listOf(101.0, 100.0, 99.0), result.bids.map { it.price })
        assertEquals(listOf(102.0, 103.0, 104.0), result.asks.map { it.price })
        assertEquals(100.5, result.lastTradePrice, 0.0)
        assertEquals(1234L, result.lastUpdate)
    }

    @Test
    fun ignoresMalformedAndNonPositiveLevelsButKeepsValidLevels() {
        val result = snapshot(
            """{"lastTradePrice":100,"bids":[[99,2],["bad",3],[98,0],[97,-1],["NaN",2],[96,1]],"asks":[[101,2],["bad",1],[102,0],[103,1]]}"""
        )

        assertEquals(listOf(99.0, 96.0), result.bids.map { it.price })
        assertEquals(listOf(101.0, 103.0), result.asks.map { it.price })
    }

    @Test
    fun emptyBidSideIsRejected() {
        assertIOException {
            snapshot("""{"lastTradePrice":100,"bids":[],"asks":[[101,1]]}""")
        }
    }

    @Test
    fun emptyAskSideIsRejected() {
        assertIOException {
            snapshot("""{"lastTradePrice":100,"bids":[[99,1]],"asks":[]}""")
        }
    }

    @Test
    fun crossedOrderBookIsRejected() {
        assertIOException {
            snapshot("""{"lastTradePrice":100,"bids":[[102,1]],"asks":[[101,1]]}""")
        }
    }

    @Test
    fun missingOrderBookSideIsRejected() {
        assertIOException {
            snapshot("""{"lastTradePrice":100,"bids":[[99,1]]}""")
        }
    }

    @Test
    fun missingOrInvalidLastTradePriceIsRejected() {
        assertIOException {
            snapshot("""{"bids":[[99,1]],"asks":[[101,1]]}""")
        }
        assertIOException {
            snapshot("""{"lastTradePrice":"not-a-price","bids":[[99,1]],"asks":[[101,1]]}""")
        }
        assertIOException {
            snapshot("""{"lastTradePrice":0,"bids":[[99,1]],"asks":[[101,1]]}""")
        }
    }

    @Test
    fun equalBestBidAndAskProducesZeroSpread() {
        val result = snapshot(
            """{"lastTradePrice":100,"bids":[[100,1]],"asks":[[100,2]]}"""
        )

        assertEquals(0.0, result.spread!!, 0.0)
        assertEquals(0.0, result.spreadPercent!!, 0.0)
    }

    @Test
    fun spreadIsUnavailableWhenSnapshotHasNoLevels() {
        val snapshot = OrderBookSnapshot(
            symbol = "BTCIRT",
            lastTradePrice = 100.0,
            lastUpdate = 0L,
            bids = emptyList(),
            asks = listOf(OrderBookLevel(101.0, 1.0))
        )

        assertNull(snapshot.bestBid)
        assertNull(snapshot.spread)
        assertNull(snapshot.spreadPercent)
        assertNotNull(snapshot.bestAsk)
        assertFalse(snapshot.bids.isNotEmpty())
        assertTrue(snapshot.asks.isNotEmpty())
    }
}
