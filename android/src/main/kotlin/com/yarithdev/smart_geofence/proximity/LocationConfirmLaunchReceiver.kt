package com.yarithdev.smart_geofence.proximity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yarithdev.smart_geofence.SmartGeofenceLogger

class LocationConfirmLaunchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val attempt = intent.getIntExtra(LocationConfirmLaunchGate.EXTRA_START_ATTEMPT, 0)
        val token = intent.getLongExtra(LocationConfirmLaunchGate.EXTRA_LAUNCH_TOKEN, 0L)
        val purpose = intent.getStringExtra(LocationConfirmLaunchGate.EXTRA_ALARM_PURPOSE)
            ?: LocationConfirmLaunchGate.PURPOSE_START
        SmartGeofenceLogger.d(
            appContext,
            TAG,
            "Confirm launch alarm fired purpose=$purpose token=$token attempt=$attempt " +
                "pending=${LocationConfirmQueue.count(appContext)}."
        )
        if (purpose == LocationConfirmLaunchGate.PURPOSE_WATCHDOG) {
            LocationConfirmLaunchGate.handleWatchdog(appContext, attempt, token)
        } else {
            LocationConfirmLaunchGate.requestStart(
                appContext,
                attempt,
                reason = if (attempt > 0) "alarm retry" else "alarm start",
                launchToken = token
            )
        }
    }

    companion object {
        private const val TAG = "LocationConfirmLaunchReceiver"
    }
}
