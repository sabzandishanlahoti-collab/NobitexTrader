
package ir.nobitextrader.app.risk

import java.util.concurrent.atomic.AtomicReference

enum class RiskLockReason {
    NONE,
    MANUAL_KILL,
    MAX_DRAWDOWN,
    DAILY_LOSS_LIMIT,
    INVALID_STATE
}

data class RiskLockState(
    val locked: Boolean = false,
    val reason: RiskLockReason = RiskLockReason.NONE,
    val message: String = ""
)

/**
 * In-memory fail-closed lock for blocking new trade approvals.
 *
 * This class does not cancel exchange orders or close positions.
 * The trading engine must check isLocked before every new order.
 * Persistence and exchange cancellation are separate responsibilities.
 */
class RiskLock {

    private val state = AtomicReference(RiskLockState())

    fun currentState(): RiskLockState = state.get()

    fun isLocked(): Boolean = state.get().locked

    fun lock(
        reason: RiskLockReason,
        message: String
    ): RiskLockState {
        require(reason != RiskLockReason.NONE) {
            "A lock reason is required"
        }

        val safeMessage = message.trim().ifEmpty {
            "Trading locked: $reason"
        }

        while (true) {
            val current = state.get()

            // Never replace an existing lock with an unlocked state.
            val next = RiskLockState(
                locked = true,
                reason = if (current.locked) current.reason else reason,
                message = if (current.locked) {
                    current.message
                } else {
                    safeMessage
                }
            )

            if (state.compareAndSet(current, next)) {
                return next
            }
        }
    }

    fun activateKillSwitch(): RiskLockState =
        lock(
            reason = RiskLockReason.MANUAL_KILL,
            message = "Emergency stop activated; new trades must be blocked"
        )

    /**
     * Explicit manual acknowledgement is required to unlock.
     * Call only after verifying account risk and exchange state.
     */
    @Synchronized
    fun unlockAfterManualReview(
        confirmed: Boolean
    ): Boolean {
        if (!confirmed) return false

        state.set(RiskLockState())
        return true
    }

    /**
     * Convert a locked state into a RiskDecision rejection.
     */
    fun enforce(decision: RiskDecision): RiskDecision {
        val current = state.get()

        if (!current.locked) return decision

        return RiskDecision(
            approved = false,
            reason = current.message,
            drawdownPct = decision.drawdownPct,
            dailyLossPct = decision.dailyLossPct,
            protectiveMode = decision.protectiveMode
        )
    }
}
