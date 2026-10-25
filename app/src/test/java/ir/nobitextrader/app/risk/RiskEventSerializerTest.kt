
package ir.nobitextrader.app.risk

import org.junit.Assert.assertEquals
import org.junit.Test

class RiskEventSerializerTest {

    @Test
    fun roundTripPreservesAllFields() {
        val original = RiskEvent(
            type = RiskEventType.TRADE_REJECTED,
            timestampMillis = 1_782_000_000_000L,
            message = "Spread too wide",
            equityUsd = 950.5,
            drawdownPct = 4.25,
            dailyLossPct = 1.1,
            symbol = "BTC/USDT"
        )

        val restored = RiskEventSerializer.deserialize(
            RiskEventSerializer.serialize(original)
        )

        assertEquals(original, restored)
    }

    @Test
    fun roundTripPreservesTabsNewlinesAndPercentSigns() {
        val original = RiskEvent(
            type = RiskEventType.RISK_LOCKED,
            timestampMillis = 123L,
            message = "Line 1\t100%\nLine 2\rLine 3",
            symbol = "TEST\tPAIR"
        )

        val restored = RiskEventSerializer.deserialize(
            RiskEventSerializer.serialize(original)
        )

        assertEquals(original, restored)
    }

    @Test
    fun roundTripPreservesNullFields() {
        val original = RiskEvent(
            type = RiskEventType.TRADE_APPROVED,
            timestampMillis = 456L,
            message = "Approved"
        )

        val restored = RiskEventSerializer.deserialize(
            RiskEventSerializer.serialize(original)
        )

        assertEquals(original, restored)
    }

    @Test
    fun rejectsRecordWithWrongFieldCount() {
        var thrown = false

        try {
            RiskEventSerializer.deserialize("TRADE_REJECTED\t123")
        } catch (exception: IllegalArgumentException) {
            thrown = true
        }

        assertEquals(true, thrown)
    }

    @Test
    fun rejectsUnknownEventType() {
        val record = "UNKNOWN\t123\tmessage\t\\N\t\\N\t\\N\t\\N"
        var thrown = false

        try {
            RiskEventSerializer.deserialize(record)
        } catch (exception: IllegalArgumentException) {
            thrown = true
        }

        assertEquals(true, thrown)
    }

    @Test
    fun rejectsInvalidTimestamp() {
        val record = "TRADE_REJECTED\tnot-a-time\tmessage\t\\N\t\\N\t\\N\t\\N"
        var thrown = false

        try {
            RiskEventSerializer.deserialize(record)
        } catch (exception: IllegalArgumentException) {
            thrown = true
        }

        assertEquals(true, thrown)
    }

    @Test
    fun rejectsInvalidNumericValue() {
        val record = "TRADE_REJECTED\t123\tmessage\tNaN\t\\N\t\\N\t\\N"
        var thrown = false

        try {
            RiskEventSerializer.deserialize(record)
        } catch (exception: IllegalArgumentException) {
            thrown = true
        }

        assertEquals(true, thrown)
    }
}
