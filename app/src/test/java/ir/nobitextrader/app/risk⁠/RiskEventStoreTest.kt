
package ir.nobitextrader.app.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEventStoreTest {

    private fun event(id: Int): RiskEvent {
        return RiskEvent(
            type = RiskEventType.TRADE_REJECTED,
            timestampMillis = id.toLong(),
            message = "event-$id"
        )
    }

    @Test
    fun keepsNewestEventsWithinCapacity() {
        val store = RiskEventStore(maxEvents = 2)

        store.append(event(1))
        store.append(event(2))
        store.append(event(3))

        assertEquals(2, store.count())
        assertEquals(
            listOf("event-3", "event-2"),
            store.latest().map { it.message }
        )
    }

    @Test
    fun latestReturnsNewestFirst() {
        val store = RiskEventStore()

        store.append(event(1))
        store.append(event(2))
        store.append(event(3))

        assertEquals(
            listOf("event-3", "event-2", "event-1"),
            store.latest(3).map { it.message }
        )
    }

    @Test
    fun zeroLimitReturnsEmptyList() {
        val store = RiskEventStore()
        store.append(event(1))

        assertTrue(store.latest(0).isEmpty())
    }

    @Test
    fun clearRemovesAllEvents() {
        val store = RiskEventStore()

        store.append(event(1))
        store.append(event(2))
        store.clear()

        assertEquals(0, store.count())
        assertTrue(store.latest().isEmpty())
    }

    @Test
    fun rejectsZeroCapacity() {
        var thrown = false

        try {
            RiskEventStore(maxEvents = 0)
        } catch (exception: IllegalArgumentException) {
            thrown = true
        }

        assertTrue(thrown)
    }

    @Test
    fun rejectsNegativeCapacity() {
        var thrown = false

        try {
            RiskEventStore(maxEvents = -1)
        } catch (exception: IllegalArgumentException) {
            thrown = true
        }

        assertTrue(thrown)
    }

    @Test
    fun rejectsNegativeLimit() {
        val store = RiskEventStore()
        var thrown = false

        try {
            store.latest(-1)
        } catch (exception: IllegalArgumentException) {
            thrown = true
        }

        assertTrue(thrown)
    }
}
