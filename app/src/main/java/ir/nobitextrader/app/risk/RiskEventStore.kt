
package ir.nobitextrader.app.risk

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe, bounded in-memory risk event history.
 *
 * This store is intentionally not persistent.
 * Durable storage must be implemented separately before live trading.
 */
class RiskEventStore(
    private val maxEvents: Int = 1000
) {
    private val lock = ReentrantLock()
    private val events = ArrayDeque<RiskEvent>()

    init {
        require(maxEvents > 0) {
            "maxEvents must be greater than zero"
        }
    }

    fun append(event: RiskEvent) {
        lock.withLock {
            events.addLast(event)

            while (events.size > maxEvents) {
                events.removeFirst()
            }
        }
    }

    fun latest(limit: Int = 100): List<RiskEvent> {
        require(limit >= 0) {
            "limit cannot be negative"
        }

        return lock.withLock {
            events.toList()
                .takeLast(limit)
                .asReversed()
        }
    }

    fun count(): Int = lock.withLock {
        events.size
    }

    fun clear() {
        lock.withLock {
            events.clear()
        }
    }
}
