
package ir.nobitextrader.app.risk

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * File-backed persistent risk event history.
 *
 * Records are stored chronologically and bounded by maxEvents.
 * A failed replacement attempts to restore the previous history.
 */
class PersistentRiskEventStore(
    private val file: File,
    private val maxEvents: Int = 1000
) {
    private val lock = ReentrantLock()

    init {
        require(maxEvents > 0) {
            "maxEvents must be greater than zero"
        }
    }

    fun load(): List<RiskEvent> = lock.withLock {
        loadUnlocked()
    }

    fun append(event: RiskEvent) = lock.withLock {
        val updated = (loadUnlocked() + event).takeLast(maxEvents)
        writeSafely(updated)
    }

    fun latest(limit: Int = 100): List<RiskEvent> {
        require(limit >= 0) { "limit cannot be negative" }

        return lock.withLock {
            loadUnlocked().takeLast(limit).asReversed()
        }
    }

    fun count(): Int = lock.withLock {
        loadUnlocked().size
    }

    fun clear() = lock.withLock {
        if (file.exists() && !file.delete()) {
            throw IOException("Could not delete risk event history")
        }
    }

    private fun loadUnlocked(): List<RiskEvent> {
        if (!file.exists()) return emptyList()

        if (!file.isFile) {
            throw IOException("Risk event path is not a file")
        }

        return try {
            file.readLines(StandardCharsets.UTF_8)
                .filter { it.isNotEmpty() }
                .mapIndexed { index, record ->
                    try {
                        RiskEventSerializer.deserialize(record)
                    } catch (exception: Exception) {
                        throw IOException(
                            "Invalid risk event record at line ${index + 1}",
                            exception
                        )
                    }
                }
                .takeLast(maxEvents)
        } catch (exception: IOException) {
            throw exception
        } catch (exception: Exception) {
            throw IOException("Could not read risk event history", exception)
        }
    }

    private fun writeSafely(events: List<RiskEvent>) {
        val parent = file.absoluteFile.parentFile
            ?: throw IOException("Invalid risk event history path")

        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Could not create risk event directory")
        }

        val temp = File(parent, "${file.name}.tmp")
        val backup = File(parent, "${file.name}.bak")

        if (temp == file || backup == file) {
            throw IOException("Invalid risk event history filename")
        }

        try {
            FileOutputStream(temp).use { output ->
                events.forEach { event ->
                    val record = RiskEventSerializer.serialize(event)
                    output.write(record.toByteArray(StandardCharsets.UTF_8))
                    output.write('\n'.code)
                }
                output.flush()
                output.fd.sync()
            }

            var backedUp = false

            if (file.exists()) {
                if (backup.exists() && !backup.delete()) {
                    throw IOException("Could not clear old backup file")
                }

                if (!file.renameTo(backup)) {
                    throw IOException("Could not preserve existing history")
                }
                backedUp = true
            }

            if (!temp.renameTo(file)) {
                if (backedUp && !backup.renameTo(file)) {
                    throw IOException(
                        "Replacement failed and previous history could not be restored; backup remains at ${backup.absolutePath}"
                    )
                }
                throw IOException("Could not install new risk event history")
            }

            if (backup.exists() && !backup.delete()) {
                // The new history is already installed. Retain the backup
                // rather than treating a backup-cleanup failure as data loss.
            }
        } catch (exception: Exception) {
            if (temp.exists()) temp.delete()

            if (!file.exists() && backup.exists()) {
                if (!backup.renameTo(file)) {
                    throw IOException(
                        "History update failed; previous history remains at ${backup.absolutePath}",
                        exception
                    )
                }
            }

            throw exception
        }
    }
}
