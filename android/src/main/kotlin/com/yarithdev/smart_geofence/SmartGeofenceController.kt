package com.yarithdev.smart_geofence

import android.content.Context
import com.yarithdev.smart_geofence.fencepulse.FencePulseController
import com.yarithdev.smart_geofence.motion.MotionGate
import com.yarithdev.smart_geofence.proximity.LocationConfirmLaunchGate
import com.yarithdev.smart_geofence.proximity.OpportunisticLocationMonitor
import com.yarithdev.smart_geofence.proximity.ProximityMonitor
import com.yarithdev.smart_geofence.work.ReconcileScheduler

/**
 * Orchestrates the Android smart layers. Stateless entry point that reads the
 * persisted [SmartGeofenceConfig] / fence store, so it works both from a live Dart isolate
 * and from background components (receivers, workers, boot).
 */
object SmartGeofenceController {
    private const val TAG = "SmartGeofenceController"

    /** Start (or refresh) the smart layers based on the current fence set + config. */
    fun start(
        context: Context,
        config: SmartGeofenceConfig,
        scheduleReconcile: Boolean = true,
    ) {
        val appContext = context.applicationContext
        SmartGeofenceConfigStore.save(appContext, config)

        val hasFences = FenceStore.getAll(appContext).isNotEmpty()
        if (!hasFences) {
            SmartGeofenceLogger.d(appContext, TAG, "No fences registered; stopping smart layers.")
            stop(appContext)
            return
        }

        // Proximity feeds foreground confirm escalation. Motion only feeds the optional
        // FencePulse, so keep Activity Recognition off unless FencePulse can
        // actually run. The reconcile watchdog always runs to keep the OS
        // fences healthy.
        if (config.escalationEnabled) {
            ProximityMonitor.start(appContext)
            if (config.passiveLocationEnabled) {
                OpportunisticLocationMonitor.start(appContext)
            } else {
                OpportunisticLocationMonitor.stop(appContext)
            }
            if (config.fencePulseEnabled && FencePulseController.canRun(appContext)) {
                MotionGate.start(appContext)
            } else {
                MotionGate.stop(appContext)
                FencePulseController.stop(appContext)
            }
        } else {
            LocationConfirmLaunchGate.stop(appContext)
            ProximityMonitor.stop(appContext)
            OpportunisticLocationMonitor.stop(appContext)
            MotionGate.stop(appContext)
            FencePulseController.stop(appContext)
        }
        if (scheduleReconcile) {
            ReconcileScheduler.schedule(appContext, config)
        }
        SmartGeofenceLogger.d(appContext, TAG, "Smart layers started ($config).")
    }

    /** Re-evaluate after the fence set changed (add/remove). */
    fun refresh(context: Context, scheduleReconcile: Boolean = true) {
        start(
            context,
            SmartGeofenceConfigStore.load(context.applicationContext),
            scheduleReconcile,
        )
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        ProximityMonitor.stop(appContext)
        OpportunisticLocationMonitor.stop(appContext)
        LocationConfirmLaunchGate.stop(appContext)
        MotionGate.stop(appContext)
        FencePulseController.stop(appContext)
        ReconcileScheduler.cancel(appContext)
        SmartGeofenceLogger.d(appContext, TAG, "Smart layers stopped.")
    }
}
