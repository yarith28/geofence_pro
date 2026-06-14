package com.yarithdev.smart_geofence.proximity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.yarithdev.smart_geofence.Constants
import com.yarithdev.smart_geofence.SmartGeofenceConfigStore
import com.yarithdev.smart_geofence.SmartGeofenceLogger
import com.yarithdev.smart_geofence.LocationPriorityMapper
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

/**
 * Coarse, low-power "are we near a fence?" stream.
 *
 * Uses a BALANCED-power, displacement-gated FusedLocation request delivered via
 * a PendingIntent broadcast, so it wakes [ProximityReceiver] even when the app
 * process is dead. It can actively produce coarse (cell/wifi) fixes on
 * movement, which we use only to decide when to spend a precise GPS confirm.
 */
object ProximityMonitor {
    private const val TAG = "ProximityMonitor"

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext) || !hasBackgroundLocationPermission(appContext)) {
            SmartGeofenceLogger.w(appContext, TAG, "Skipping proximity monitor: location permission missing.")
            return
        }
        val config = SmartGeofenceConfigStore.load(appContext)
        val request = LocationRequest.Builder(
            LocationPriorityMapper.toAndroidPriority(config.proximityLocationPriority),
            config.proximityIntervalMillis.coerceAtLeast(1L)
        )
            .setMinUpdateIntervalMillis(config.proximityFastestIntervalMillis.coerceAtLeast(0L))
            .setMaxUpdateDelayMillis(config.proximityMaxWaitMillis.coerceAtLeast(0L))
            .setMinUpdateDistanceMeters(config.proximityMinDisplacementMeters.coerceAtLeast(0.0).toFloat())
            .build()
        try {
            LocationServices.getFusedLocationProviderClient(appContext)
                .requestLocationUpdates(request, pendingIntent(appContext))
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Proximity monitor started " +
                    "(priority=${config.proximityLocationPriority}, " +
                    "interval=${config.proximityIntervalMillis}ms, " +
                    "min=${config.proximityFastestIntervalMillis}ms, " +
                    "maxWait=${config.proximityMaxWaitMillis}ms, " +
                    "displacement=${config.proximityMinDisplacementMeters}m)."
            )
        } catch (e: SecurityException) {
            SmartGeofenceLogger.w(appContext, TAG, "Failed to start proximity monitor: ${e.message}", e)
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        LocationServices.getFusedLocationProviderClient(appContext)
            .removeLocationUpdates(pendingIntent(appContext))
        SmartGeofenceLogger.d(appContext, TAG, "Proximity monitor stopped.")
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ProximityReceiver::class.java).apply {
            data = Uri.parse(Constants.PENDING_INTENT_DATA_PROXIMITY)
            putExtra(
                Constants.EXTRA_LOCATION_WAKE_SOURCE,
                Constants.LOCATION_WAKE_SOURCE_PROXIMITY
            )
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(
            context, Constants.PENDING_INTENT_REQUEST_BASE, intent, flags
        )
    }

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
