package com.yarithdev.smart_geofence.fencepulse

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.SmartGeofenceLogger

object FencePulseScheduler {
    private const val TAG = "FencePulseScheduler"

    fun schedule(context: Context, delayMillis: Long): Boolean {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager == null) {
            SmartGeofenceLogger.w(appContext, TAG, "AlarmManager unavailable; FencePulse tick not scheduled.")
            return false
        }
        val normalizedDelay = delayMillis.coerceAtLeast(0L)
        val triggerAt = System.currentTimeMillis() + normalizedDelay
        var pending: PendingIntent? = null
        return try {
            pending = pendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "FencePulse alarm scheduled in ${normalizedDelay}ms triggerAt=$triggerAt."
            )
            true
        } catch (e: SecurityException) {
            pending?.cancel()
            SmartGeofenceLogger.w(appContext, TAG, "Failed to schedule FencePulse alarm: ${e.message}", e)
            false
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        existingPendingIntent(appContext)?.let {
            alarmManager?.cancel(it)
            it.cancel()
            SmartGeofenceLogger.d(appContext, TAG, "FencePulse alarm cancelled.")
        }
    }

    fun pendingIntentExists(context: Context): Boolean =
        existingPendingIntent(context.applicationContext) != null

    private fun existingPendingIntent(context: Context): PendingIntent? =
        pendingIntent(context, PendingIntent.FLAG_NO_CREATE)

    private fun pendingIntent(context: Context, baseFlags: Int): PendingIntent {
        var flags = baseFlags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_FENCE_PULSE_LAUNCH,
            Intent(context, FencePulseAlarmReceiver::class.java),
            flags,
        )
    }
}
