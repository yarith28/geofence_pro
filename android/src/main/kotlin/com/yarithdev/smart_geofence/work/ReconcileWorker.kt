package com.yarithdev.smart_geofence.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.yarithdev.smart_geofence.SmartGeofenceController
import com.yarithdev.smart_geofence.SmartGeofenceLogger

/**
 * Periodic self-heal. Re-arms the full pro layer set (proximity + motion, gated
 * by config) if anything was dropped by a low-memory kill or process death, and
 * stops everything if no fences remain. Delegates to [SmartGeofenceController] so
 * it stays consistent with start/refresh elsewhere.
 *
 * NOTE: re-registering dropped OS geofences is native_geofence's responsibility
 * (its reboot receiver, declared via smart_geofence's manifest). This worker covers
 * the always-on Android pieces that have no Dart isolate to lean on.
 */
class ReconcileWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result {
        SmartGeofenceController.refresh(applicationContext, scheduleReconcile = false)
        SmartGeofenceLogger.d(applicationContext, TAG, "Reconcile: re-armed smart layers.")
        return Result.success()
    }

    companion object {
        private const val TAG = "ReconcileWorker"
    }
}
