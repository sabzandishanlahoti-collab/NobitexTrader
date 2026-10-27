
package ir.nobitextrader.app.risk

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PersistentRiskEventStoreTest {

    private lateinit var directory: File
    private lateinit var historyFile: File
    private lateinit var store: PersistentRiskEventStore

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("risk-history-test").toFile()
        historyFile = File(directory, "risk-events.log")
        store = PersistentRiskEventStore(historyFile, maxEvents = 3)
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun event(
        timestamp: Long,
        message: String = "event-$timestamp"
    ) = RiskEvent(
        type = RiskEventType.TRADE_REJECTED,
        timestampMillis = timestamp,
        message = message,
        equityUsd = 1000.0,
        drawdownPct = 2.0,
        dailyLossPct = 1.0,
        symbol = "BTCUSDT"
    )

    @Test
    fun appendPersistsEventAcrossStoreInstances() {
        store.append(event(1))
        store.append(event(2))

        val reopened = PersistentRiskEventStore(historyFile, maxEvents = 3)

        assertEquals(2, reopened.count())
        assertEquals(listOf(2L, 1L), reopened.latest().map { it.timestampMillis })
    }

    @Test
    fun appendKeepsOnlyNewestMaxEvents() {
        store.append(event(1))
        store.append(event(2))
        store.append(event(3))
        store.append(event(4))

        assertEquals(3, store.count())
        assertEquals(
            listOf(4L, 3L, 2L),
            store.latest().map { it.timestampMillis }
        )
    }

    @Test
    fun latestReturnsNewestFirstAndHonorsLimit() {
        store.append(event(10))
        store.append(event(20))
        store.append(event(30))

        assertEquals(
            listOf(30L, 20L),
            store.latest(2).map { it.timestampMillis }
        )
    }

    @Test
    fun clearRemovesPersistedHistory() {
        store.append(event(1))
        store.clear()

        assertEquals(0, store.count())
        assertTrue(store.load().isEmpty())
        assertFalse(historyFile.exists())
    }

    @Test
    fun corruptRecordFailsExplicitly() {
        historyFile.writeText("not-a-valid-risk-event-record\n")

        var thrown = false
        try {
            store.load()
        } catch (exception: IOException) {
            thrown = true
        }

        assertTrue("Corrupt history must not be silently accepted", thrown)
    }

    @Test
    fun serializerPreservesTabsAndNewlinesInMessage() {
        val original = event(123, "first\tsecond\nthird")

        store.append(original)

        assertEquals(original, store.load().single())
    }
}
