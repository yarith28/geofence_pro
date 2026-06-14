package com.yarithdev.smart_geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the smart layers immediately after a reboot or app update.
 *
 * The periodic reconcile worker (WorkManager) survives reboot and would
 * eventually re-arm proximity, but this makes it immediate. Re-registering the
 * OS geofences themselves is native_geofence's responsibility (its own boot
 * receiver) — this only restores smart_geofence's always-on Android pieces.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        SmartGeofenceLogger.d(context, TAG, "Boot/update ($action): re-arming smart layers.")
        SmartGeofenceController.refresh(context)
    }

    companion object {
        private const val TAG = "SmartGeofenceBootReceiver"
    }
}
