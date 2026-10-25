
package ir.nobitextrader.app.risk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskLockTest {

    private fun approvedDecision() = RiskDecision(
        approved = true,
        reason = "Approved by risk engine",
        positionSizeUsd = 100.0,
        riskBudgetUsd = 5.0
    )

    @Test
    fun newLockStartsUnlocked() {
        val lock = RiskLock()

        assertFalse(lock.isLocked())
        assertEquals(
            RiskLockReason.NONE,
            lock.currentState().reason
        )
    }

    @Test
    fun killSwitchLocksTrading() {
        val lock = RiskLock()

        val state = lock.activateKillSwitch()

        assertTrue(state.locked)
        assertTrue(lock.isLocked())
        assertEquals(
            RiskLockReason.MANUAL_KILL,
            state.reason
        )
    }

    @Test
    fun lockedStateRejectsApprovedDecision() {
        val lock = RiskLock()
        lock.activateKillSwitch()

        val result = lock.enforce(approvedDecision())

        assertFalse(result.approved)
        assertEquals(0.0, result.positionSizeUsd, 0.0)
    }

    @Test
    fun unlockedStatePreservesDecision() {
        val lock = RiskLock()
        val original = approvedDecision()

        val result = lock.enforce(original)

        assertTrue(result.approved)
        assertEquals(
            original.positionSizeUsd,
            result.positionSizeUsd,
            0.0
        )
    }

    @Test
    fun cannotUnlockWithoutConfirmation() {
        val lock = RiskLock()
        lock.activateKillSwitch()

        assertFalse(lock.unlockAfterManualReview(false))
        assertTrue(lock.isLocked())
    }

    @Test
    fun confirmedManualReviewCanUnlock() {
        val lock = RiskLock()
        lock.activateKillSwitch()

        assertTrue(lock.unlockAfterManualReview(true))
        assertFalse(lock.isLocked())
    }

    @Test
    fun firstLockReasonIsPreserved() {
        val lock = RiskLock()

        lock.lock(
            RiskLockReason.MAX_DRAWDOWN,
            "Maximum drawdown reached"
        )
        lock.activateKillSwitch()

        assertEquals(
            RiskLockReason.MAX_DRAWDOWN,
            lock.currentState().reason
        )
    }

    @Test
    fun emptyLockMessageUsesFallback() {
        val lock = RiskLock()

        val state = lock.lock(
            RiskLockReason.INVALID_STATE,
            " "
        )

        assertTrue(state.locked)
        assertTrue(state.message.isNotBlank())
    }
}
