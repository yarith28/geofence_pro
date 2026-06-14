package com.yarithdev.smart_geofence.fencepulse

import android.content.Context
import com.yarithdev.smart_geofence.Constants

data class FencePulseState(
    val startedAtMillis: Long,
    val idleTicks: Int,
)

object FencePulseStateStore {
    private const val KEY_STARTED_AT = "fence_pulse_started_at_millis"
    private const val KEY_IDLE_TICKS = "fence_pulse_idle_ticks"

    fun load(context: Context): FencePulseState? {
        val prefs = prefs(context)
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        if (startedAt <= 0L) return null
        return FencePulseState(
            startedAtMillis = startedAt,
            idleTicks = prefs.getInt(KEY_IDLE_TICKS, 0),
        )
    }

    fun ensureStarted(context: Context, nowMillis: Long): FencePulseState {
        val existing = load(context)
        if (existing != null) return existing
        return FencePulseState(startedAtMillis = nowMillis, idleTicks = 0).also {
            save(context, it)
        }
    }

    fun save(context: Context, state: FencePulseState) {
        prefs(context).edit()
            .putLong(KEY_STARTED_AT, state.startedAtMillis)
            .putInt(KEY_IDLE_TICKS, state.idleTicks)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_STARTED_AT)
            .remove(KEY_IDLE_TICKS)
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
}
