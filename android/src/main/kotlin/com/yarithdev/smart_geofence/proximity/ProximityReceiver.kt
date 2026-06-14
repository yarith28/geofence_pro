package com.yarithdev.smart_geofence.proximity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.FenceStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceFence
import com.yarithdev.smart_geofence.fencepulse.FencePulseController
import com.google.android.gms.location.LocationResult

/**
 * Wakes on a coarse proximity fix (even from a dead process). Computes the
 * nearest registered fence and, when the device is within the configured
 * proximity radius and escalation is enabled, hands off to the precise GPS
 * confirm step. This is the cheap gate that decides when a precise fix is worth
 * spending.
 */
class ProximityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = LocationResult.extractResult(intent) ?: return
        val location = result.lastLocation ?: return
        val appContext = context.applicationContext
        val source = wakeSource(intent)

        try {
            evaluate(appContext, location, source)
        } catch (e: Throwable) {
            SmartGeofenceLogger.w(appContext, TAG, "Proximity evaluation failed: ${e.message}", e)
        }
    }

    private fun evaluate(
        context: Context,
        location: Location,
        source: String,
    ) {
        val fences = FenceStore.getAll(context)
        if (fences.isEmpty()) {
            ProximityMonitor.stop(context)
            OpportunisticLocationMonitor.stop(context)
            return
        }
        val config = SmartGeofenceConfigStore.load(context)
        if (!config.escalationEnabled) {
            ProximityMonitor.stop(context)
            OpportunisticLocationMonitor.stop(context)
            return
        }

        var nearest: SmartGeofenceFence? = null
        var nearestEdgeDistance = Double.MAX_VALUE
        for (fence in fences) {
            val center = Location("smart_geofence").apply {
                latitude = fence.latitude
                longitude = fence.longitude
            }
            val edgeDistance = location.distanceTo(center).toDouble() - fence.radiusMeters
            if (edgeDistance < nearestEdgeDistance) {
                nearestEdgeDistance = edgeDistance
                nearest = fence
            }
        }

        val accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else 0.0
        val withinProximity = nearestEdgeDistance - accuracy <= config.proximityRadiusMeters
        SmartGeofenceLogger.d(
            context,
            TAG,
            "Location wake source=$source nearest fence=${nearest?.id} " +
                "edgeDist=${nearestEdgeDistance.toInt()}m " +
                "acc=${accuracy.toInt()}m withinProximity=$withinProximity"
        )

        if (withinProximity && nearest != null) {
            val knownSource = if (source == Constants.LOCATION_WAKE_SOURCE_PASSIVE) {
                Constants.EVENT_SOURCE_SMART_GEOFENCE_PASSIVE
            } else {
                Constants.EVENT_SOURCE_SMART_GEOFENCE_PROXIMITY
            }
            val decided = GpsConfirm.evaluateKnownLocation(
                context,
                location,
                nearest,
                knownSource
            )
            if (decided) {
                SmartGeofenceLogger.d(
                    context,
                    TAG,
                    "Known fix gave a confident decision for ${nearest.id}; no foreground confirm needed."
                )
                return
            }
            if (source == Constants.LOCATION_WAKE_SOURCE_PASSIVE) {
                if (!config.passiveAmbiguousConfirmEnabled) {
                    SmartGeofenceLogger.d(
                        context,
                        TAG,
                        "Passive fix near ${nearest.id} was ambiguous; active confirm disabled."
                    )
                    return
                }
                SmartGeofenceLogger.d(
                    context,
                    TAG,
                    "Passive fix near ${nearest.id} was ambiguous; escalating with precise confirm."
                )
            }
            // Always resolve an unresolved proximity wake with a foreground
            // confirm. FencePulse follow-up must not replace the immediate
            // confirm because it can skip ticks while stationary.
            val confirmSource = if (source == Constants.LOCATION_WAKE_SOURCE_PASSIVE) {
                Constants.EVENT_SOURCE_SMART_GEOFENCE_PASSIVE_CONFIRM
            } else {
                Constants.EVENT_SOURCE_SMART_GEOFENCE_CONFIRM_PROXIMITY
            }
            SmartGeofenceLogger.d(
                context,
                TAG,
                "Within proximity of ${nearest.id} - queueing foreground precise confirm."
            )
            if (!LocationConfirmLaunchGate.enqueue(context, nearest, confirmSource)) {
                SmartGeofenceLogger.w(
                    context,
                    TAG,
                    "Foreground confirm launch was not started for ${nearest.id}; request remains queued."
                )
            }
            if (FencePulseController.maybeStart(context)) {
                SmartGeofenceLogger.d(
                    context,
                    TAG,
                    "FencePulse scheduled for follow-up watch near ${nearest.id}."
                )
            }
            return
        }
    }

    private fun wakeSource(intent: Intent): String =
        intent.getStringExtra(Constants.EXTRA_LOCATION_WAKE_SOURCE)
            ?: Constants.LOCATION_WAKE_SOURCE_PROXIMITY

    companion object {
        private const val TAG = "ProximityReceiver"
    }
}
