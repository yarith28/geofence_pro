package com.yarithdev.smart_geofence.motion

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

/**
 * Optional gate/throttle backed by Activity Recognition. It does not detect
 * crossings (it has no position) — it only answers "is the device confidently
 * still?", which lets the sustained FencePulse skip GPS ticks when the user can't
 * be crossing anything. Pure battery optimization; safe to no-op when the
 * permission is missing.
 */
object MotionGate {
    private const val TAG = "MotionGate"
    private const val KEY_STATIONARY = "motion_stationary"
    private const val KEY_STATIONARY_AT = "motion_stationary_at"

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        val appContext = context.applicationContext
        if (!hasPermission(appContext)) {
            SmartGeofenceLogger.d(appContext, TAG, "Activity recognition permission missing; motion gate disabled.")
            return
        }
        val transitions = listOf(
            transition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transition(DetectedActivity.STILL, ActivityTransition.ACTIVITY_TRANSITION_EXIT),
            transition(DetectedActivity.WALKING, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transition(DetectedActivity.RUNNING, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transition(DetectedActivity.ON_BICYCLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
            transition(DetectedActivity.IN_VEHICLE, ActivityTransition.ACTIVITY_TRANSITION_ENTER),
        )
        try {
            ActivityRecognition.getClient(appContext)
                .requestActivityTransitionUpdates(
                    ActivityTransitionRequest(transitions), pendingIntent(appContext)
                )
            SmartGeofenceLogger.d(appContext, TAG, "Motion gate started.")
        } catch (e: SecurityException) {
            SmartGeofenceLogger.w(appContext, TAG, "Failed to start motion gate: ${e.message}", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stop(context: Context) {
        val appContext = context.applicationContext
        if (!hasPermission(appContext)) return
        try {
            ActivityRecognition.getClient(appContext)
                .removeActivityTransitionUpdates(pendingIntent(appContext))
        } catch (e: SecurityException) {
            SmartGeofenceLogger.w(appContext, TAG, "Failed to stop motion gate: ${e.message}", e)
        }
    }

    /**
     * Defaults to false (assume moving) so we never wrongly suppress a fix.
     * Also returns false if the last stationary reading is stale.
     */
    fun isLikelyStationary(context: Context): Boolean {
        val appContext = context.applicationContext
        val p = appContext
            .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!p.getBoolean(KEY_STATIONARY, false)) return false
        val at = p.getLong(KEY_STATIONARY_AT, 0L)
        val ttlMs = SmartGeofenceConfigStore.load(appContext).motionStationaryTtlMillis
        if (ttlMs <= 0L) return false
        return System.currentTimeMillis() - at < ttlMs
    }

    fun setStationary(context: Context, stationary: Boolean) {
        context.applicationContext
            .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_STATIONARY, stationary)
            .putLong(KEY_STATIONARY_AT, System.currentTimeMillis())
            .apply()
    }

    private fun transition(activity: Int, type: Int): ActivityTransition =
        ActivityTransition.Builder()
            .setActivityType(activity)
            .setActivityTransition(type)
            .build()

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MotionReceiver::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(
            context, Constants.PENDING_INTENT_REQUEST_BASE + 1, intent, flags
        )
    }

    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
