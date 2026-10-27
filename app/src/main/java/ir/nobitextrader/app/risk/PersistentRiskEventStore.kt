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
 * Stores serialized events in chronological order.
 * Writes use a temporary file before replacing the main file.
 *
 * This class does not execute orders or replace RiskLock.
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

    /**
     * Loads events from disk.
     *
     * Invalid records cause an exception instead of being silently
     * discarded, so history corruption is not hidden.
     */
    fun load(): List<RiskEvent> = lock.withLock {
        if (!file.exists()) {
            return emptyList()
        }

        if (!file.isFile) {
            throw IOException("Risk event path is not a file")
        }

        val records = file.readLines(StandardCharsets.UTF_8)
            .filter { it.isNotEmpty() }

        records.mapIndexed { index, record ->
            try {
                RiskEventSerializer.deserialize(record)
            } catch (exception: Exception) {
                throw IOException(
                    "Invalid risk event record at line ${index + 1}",
                    exception
                )
            }
        }.takeLast(maxEvents)
    }

    /**
     * Appends an event and persists the bounded history.
     *
     * The in-memory result is not reported as saved unless the disk
     * replacement succeeds.
     */
    fun append(event: RiskEvent) = lock.withLock {
        val current = loadUnlocked()
        val updated = (current + event).takeLast(maxEvents)
        writeAtomically(updated)
    }

    /**
     * Returns newest events first, matching RiskEventStore.latest().
     */
    fun latest(limit: Int = 100): List<RiskEvent> {
        require(limit >= 0) {
            "limit cannot be negative"
        }

        return lock.withLock {
            loadUnlocked().takeLast(limit).asReversed()
        }
    }

    fun count(): Int = lock.withLock {
        loadUnlocked().size
    }

    /**
     * Deletes persisted history.
     * Call only for an intentional user-requested history reset.
     */
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

        return file.readLines(StandardCharsets.UTF_8)
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
    }

    private fun writeAtomically(events: List<RiskEvent>) {
        val parent = file.absoluteFile.parentFile
            ?: throw IOException("Invalid risk event history path")

        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Could not create risk event directory")
        }

        val tempFile = File(parent, "${file.name}.tmp")

        try {
            FileOutputStream(tempFile).use { output ->
                events.forEach { event ->
                    val record = RiskEventSerializer.serialize(event)
                    output.write(record.toByteArray(StandardCharsets.UTF_8))
                    output.write('\n'.code)
                }

                output.flush()
                output.fd.sync()
            }

            if (file.exists()) {
                if (!file.delete()) {
                    throw IOException(
                        "Could not replace existing risk event history"
                    )
                }
            }

            if (!tempFile.renameTo(file)) {
                throw IOException(
                    "Could not move temporary risk history into place"
                )
            }
        } catch (exception: Exception) {
            tempFile.delete()
            throw exception
        }
    }
}
