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
 * Opportunistic no-power location feed.
 *
 * PRIORITY_PASSIVE does not turn location hardware on. It only receives fixes
 * produced for other clients, then lets smart_geofence reuse those fixes as a
 * cheap geofence-state check before deciding whether an active confirm is worth
 * spending.
 */
object OpportunisticLocationMonitor {
    private const val TAG = "OpportunisticLocationMonitor"

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        val appContext = context.applicationContext
        if (!hasLocationPermission(appContext) || !hasBackgroundLocationPermission(appContext)) {
            SmartGeofenceLogger.w(appContext, TAG, "Skipping opportunistic location monitor: location permission missing.")
            return
        }

        val config = SmartGeofenceConfigStore.load(appContext)
        val request = LocationRequest.Builder(
            LocationPriorityMapper.toAndroidPriority(config.passiveLocationPriority),
            config.passiveLocationIntervalMillis.coerceAtLeast(1L)
        )
            .setMinUpdateIntervalMillis(config.passiveLocationFastestIntervalMillis.coerceAtLeast(0L))
            .setMaxUpdateDelayMillis(config.passiveLocationMaxWaitMillis.coerceAtLeast(0L))
            .build()
        try {
            LocationServices.getFusedLocationProviderClient(appContext)
                .requestLocationUpdates(request, pendingIntent(appContext))
            SmartGeofenceLogger.d(
                appContext,
                TAG,
                "Opportunistic location monitor started " +
                    "(priority=${config.passiveLocationPriority}, " +
                    "interval=${config.passiveLocationIntervalMillis}ms, " +
                    "min=${config.passiveLocationFastestIntervalMillis}ms, " +
                    "maxWait=${config.passiveLocationMaxWaitMillis}ms)."
            )
        } catch (e: SecurityException) {
            SmartGeofenceLogger.w(appContext, TAG, "Failed to start opportunistic location monitor: ${e.message}", e)
        }
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        LocationServices.getFusedLocationProviderClient(appContext)
            .removeLocationUpdates(pendingIntent(appContext))
        SmartGeofenceLogger.d(appContext, TAG, "Opportunistic location monitor stopped.")
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ProximityReceiver::class.java).apply {
            data = Uri.parse(Constants.PENDING_INTENT_DATA_PASSIVE_LOCATION)
            putExtra(
                Constants.EXTRA_LOCATION_WAKE_SOURCE,
                Constants.LOCATION_WAKE_SOURCE_PASSIVE
            )
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_PASSIVE_LOCATION,
            intent,
            flags
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
