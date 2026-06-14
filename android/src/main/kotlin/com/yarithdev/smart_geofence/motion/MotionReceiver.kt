package com.yarithdev.smart_geofence.motion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * Updates [MotionGate]'s stationary flag from Activity Recognition transitions.
 * Entering STILL marks stationary; leaving STILL or entering any moving activity
 * marks moving.
 */
class MotionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val appContext = context.applicationContext

        // Events are time-ordered; the last relevant one wins.
        for (event in result.transitionEvents) {
            val entering = event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER
            when (event.activityType) {
                DetectedActivity.STILL -> MotionGate.setStationary(appContext, entering)
                DetectedActivity.WALKING,
                DetectedActivity.RUNNING,
                DetectedActivity.ON_BICYCLE,
                DetectedActivity.IN_VEHICLE,
                DetectedActivity.ON_FOOT ->
                    if (entering) MotionGate.setStationary(appContext, false)
            }
        }
        SmartGeofenceLogger.d(appContext, TAG, "Motion update: stationary=${MotionGate.isLikelyStationary(appContext)}")
    }

    companion object {
        private const val TAG = "MotionReceiver"
    }
}
