package com.yarithdev.smart_geofence.fgs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yarithdev.smart_geofence.ForegroundLaunchState
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.proximity.LocationConfirmLaunchGate

/**
 * Drains the shared foreground-service start batch.
 *
 * Foreground service requests are collected for a short cold-start window, then
 * started in one controlled batch. Individual services still own their
 * prerequisites, watchdogs, retries, and foreground promotion.
 */
class ForegroundServiceLaunchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        drain(context.applicationContext)
    }

    companion object {
        private const val TAG = "ForegroundServiceLaunchReceiver"

        fun drain(context: Context) {
            val appContext = context.applicationContext
            val queued = ForegroundStartCoordinator.drainQueuedStarts(appContext)
            if (queued.isEmpty()) return

            for (request in queued) {
                val accepted = when (request.serviceKey) {
                    ForegroundLaunchState.SERVICE_LOCATION_CONFIRM ->
                        LocationConfirmLaunchGate.requestStart(
                            appContext,
                            attempt = request.attempt,
                            reason = "batch ${request.reason}",
                            launchToken = request.launchToken,
                        )
                    else -> {
                        SmartGeofenceLogger.w(
                            appContext,
                            TAG,
                            "Unknown foreground-service launch key=${request.serviceKey}; dropping."
                        )
                        false
                    }
                }
                if (!accepted) {
                    ForegroundStartCoordinator.markBatchServiceFinished(
                        appContext,
                        request.serviceKey,
                        "start_not_accepted",
                        launchToken = request.launchToken,
                        attempt = request.attempt,
                    )
                }
            }
        }
    }
}
