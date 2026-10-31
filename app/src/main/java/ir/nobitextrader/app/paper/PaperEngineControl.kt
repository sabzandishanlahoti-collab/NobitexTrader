package ir.nobitextrader.app.paper

import android.content.Context

enum class PaperEngineMode {
    STOPPED,
    RUNNING,
    KILLED
}

/**
 * Lifecycle gate for paper-only automation.
 * This state machine does not submit exchange orders or modify the paper wallet.
 */
object PaperEngineControl {
    fun start(current: PaperEngineMode): PaperEngineMode = PaperEngineMode.RUNNING
    fun stop(): PaperEngineMode = PaperEngineMode.STOPPED
    fun kill(): PaperEngineMode = PaperEngineMode.KILLED
}

/** Persists the paper-engine control state. A running state is never resumed after process restart. */
object PaperEngineControlStore {
    private const val PREFS = "paper_engine_control"
    private const val KEY_MODE = "mode"

    fun load(context: Context): PaperEngineMode {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = runCatching {
            PaperEngineMode.valueOf(prefs.getString(KEY_MODE, PaperEngineMode.STOPPED.name)!!)
        }.getOrDefault(PaperEngineMode.STOPPED)

        // Fail safe: require a fresh START after app/process restart.
        if (stored == PaperEngineMode.RUNNING) {
            save(context, PaperEngineMode.STOPPED)
            return PaperEngineMode.STOPPED
        }
        return stored
    }

    fun save(context: Context, mode: PaperEngineMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .commit()
    }
}
