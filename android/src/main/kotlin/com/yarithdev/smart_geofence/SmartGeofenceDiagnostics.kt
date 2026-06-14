package com.yarithdev.smart_geofence

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.yarithdev.smart_geofence.fencepulse.FencePulseAlarmReceiver
import com.yarithdev.smart_geofence.fencepulse.FencePulseScheduler
import com.yarithdev.smart_geofence.fencepulse.FencePulseStateStore
import com.yarithdev.smart_geofence.fgs.ForegroundStartCoordinator
import com.yarithdev.smart_geofence.fgs.ForegroundServiceLaunchReceiver
import com.yarithdev.smart_geofence.motion.MotionReceiver
import com.yarithdev.smart_geofence.proximity.LocationConfirmLaunchGate
import com.yarithdev.smart_geofence.proximity.LocationConfirmLaunchReceiver
import com.yarithdev.smart_geofence.proximity.LocationConfirmQueue
import com.yarithdev.smart_geofence.proximity.LocationConfirmService
import com.yarithdev.smart_geofence.proximity.ProximityReceiver
import com.yarithdev.smart_geofence.work.ReconcileAlarmReceiver

/** Read-only diagnostics for the Android-only smart_geofence layers. */
object SmartGeofenceDiagnostics {
    fun getStatus(context: Context): Map<String, Any?> {
        val appContext = context.applicationContext
        val config = SmartGeofenceConfigStore.load(appContext)
        val fences = FenceStore.getAll(appContext)
        val mirroredFenceIds = fences.map { it.id }.sorted()
        val locationServicesEnabled = locationServicesEnabled(appContext)
        val locationPermissionGranted = hasLocationPermission(appContext)
        val backgroundLocationPermissionGranted = hasBackgroundLocationPermission(appContext)
        val activityRecognitionPermissionGranted = hasActivityRecognitionPermission(appContext)
        val foregroundServicePermissionGranted =
            granted(appContext, Manifest.permission.FOREGROUND_SERVICE)
        val foregroundServiceLocationPermissionGranted =
            hasForegroundServiceLocationPermission(appContext)
        val exactAlarmPermissionGranted = exactAlarmPermissionGranted(appContext)
        val proximityReceiverDeclared = hasReceiver(appContext, ProximityReceiver::class.java)
        val motionReceiverDeclared = hasReceiver(appContext, MotionReceiver::class.java)
        val bootReceiverDeclared = hasReceiver(appContext, BootReceiver::class.java)
        val reconcileAlarmReceiverDeclared =
            hasReceiver(appContext, ReconcileAlarmReceiver::class.java)
        val locationConfirmReceiverDeclared =
            hasReceiver(appContext, LocationConfirmLaunchReceiver::class.java)
        val foregroundServiceLaunchReceiverDeclared =
            hasReceiver(appContext, ForegroundServiceLaunchReceiver::class.java)
        val locationConfirmServiceInfo = serviceInfo(appContext, LocationConfirmService::class.java)
        val locationConfirmServiceDeclared = locationConfirmServiceInfo != null
        val locationConfirmServiceHasLocationType =
            serviceHasLocationType(locationConfirmServiceInfo)
        val fencePulseReceiverDeclared =
            hasReceiver(appContext, FencePulseAlarmReceiver::class.java)
        val locationConfirmCanRun = foregroundServicePermissionGranted &&
            foregroundServiceLocationPermissionGranted &&
            locationConfirmReceiverDeclared &&
            foregroundServiceLaunchReceiverDeclared &&
            locationConfirmServiceDeclared &&
            locationConfirmServiceHasLocationType
        val fencePulseCanRun = config.fencePulseEnabled &&
            fencePulseReceiverDeclared
        val locationConfirmLaunch = ForegroundLaunchState.snapshot(
            appContext,
            ForegroundLaunchState.SERVICE_LOCATION_CONFIRM
        )
        val fencePulseState = FencePulseStateStore.load(appContext)
        val hasFences = mirroredFenceIds.isNotEmpty()
        val commonLocationEligible = hasFences &&
            config.escalationEnabled &&
            locationServicesEnabled != false &&
            locationPermissionGranted &&
            backgroundLocationPermissionGranted &&
            proximityReceiverDeclared

        return linkedMapOf(
            "androidSdkInt" to Build.VERSION.SDK_INT,
            "deviceManufacturer" to Build.MANUFACTURER,
            "deviceModel" to Build.MODEL,
            "config" to config.toMap(),
            "mirroredFenceIds" to mirroredFenceIds,
            "mirroredFenceCount" to mirroredFenceIds.size,
            "locationPermissionGranted" to locationPermissionGranted,
            "backgroundLocationPermissionGranted" to backgroundLocationPermissionGranted,
            "activityRecognitionPermissionGranted" to activityRecognitionPermissionGranted,
            "foregroundServicePermissionGranted" to foregroundServicePermissionGranted,
            "foregroundServiceLocationPermissionGranted" to foregroundServiceLocationPermissionGranted,
            "exactAlarmPermissionGranted" to exactAlarmPermissionGranted,
            "locationServicesEnabled" to locationServicesEnabled,
            "proximityReceiverDeclared" to proximityReceiverDeclared,
            "motionReceiverDeclared" to motionReceiverDeclared,
            "bootReceiverDeclared" to bootReceiverDeclared,
            "reconcileAlarmReceiverDeclared" to reconcileAlarmReceiverDeclared,
            "locationConfirmReceiverDeclared" to locationConfirmReceiverDeclared,
            "foregroundServiceLaunchReceiverDeclared" to foregroundServiceLaunchReceiverDeclared,
            "foregroundStartCoordinatorWindowClosed" to
                ForegroundStartCoordinator.batchWindowClosed(appContext),
            "foregroundStartQueuedServices" to
                ForegroundStartCoordinator.queuedServiceKeys(appContext),
            "foregroundStartBatchPendingIntentExists" to
                ForegroundStartCoordinator.batchStartPendingIntentExists(appContext),
            "locationConfirmServiceDeclared" to locationConfirmServiceDeclared,
            "locationConfirmServiceHasLocationType" to locationConfirmServiceHasLocationType,
            "locationConfirmServiceRunning" to LocationConfirmService.isRunning,
            "locationConfirmServiceForegroundReady" to LocationConfirmService.isForegroundReady,
            "locationConfirmLaunchToken" to locationConfirmLaunch.activeToken,
            "locationConfirmLaunchRequestedAtMillis" to locationConfirmLaunch.launchRequestedAtMillis,
            "locationConfirmForegroundReadyAtMillis" to locationConfirmLaunch.foregroundReadyAtMillis,
            "locationConfirmLastLaunchFailureAtMillis" to locationConfirmLaunch.lastFailureAtMillis,
            "locationConfirmLastLaunchFailureReason" to locationConfirmLaunch.lastFailureReason,
            "locationConfirmCanRun" to locationConfirmCanRun,
            "locationConfirmQueueSize" to LocationConfirmQueue.count(appContext),
            "fencePulseReceiverDeclared" to fencePulseReceiverDeclared,
            "fencePulseStartedAtMillis" to fencePulseState?.startedAtMillis,
            "fencePulseIdleTicks" to fencePulseState?.idleTicks,
            "fencePulseCanRun" to fencePulseCanRun,
            "proximityEligible" to commonLocationEligible,
            "passiveLocationEligible" to (commonLocationEligible && config.passiveLocationEnabled),
            "motionGateEligible" to (
                config.fencePulseEnabled &&
                    fencePulseCanRun &&
                    activityRecognitionPermissionGranted &&
                    motionReceiverDeclared
                ),
            "reconcileEligible" to hasFences,
            "proximityPendingIntentExists" to proximityPendingIntentExists(appContext),
            "passiveLocationPendingIntentExists" to passiveLocationPendingIntentExists(appContext),
            "motionPendingIntentExists" to motionPendingIntentExists(appContext),
            "reconcileAlarmPendingIntentExists" to reconcileAlarmPendingIntentExists(appContext),
            "locationConfirmStartPendingIntentExists" to
                LocationConfirmLaunchGate.pendingIntentExists(appContext),
            "locationConfirmWatchdogPendingIntentExists" to
                LocationConfirmLaunchGate.watchdogPendingIntentExists(appContext),
            "fencePulseAlarmPendingIntentExists" to
                FencePulseScheduler.pendingIntentExists(appContext),
        )
    }

    private fun SmartGeofenceConfig.toMap(): Map<String, Any?> = linkedMapOf(
        "proximityRadiusMeters" to proximityRadiusMeters,
        "escalationEnabled" to escalationEnabled,
        "proximityLocationPriority" to proximityLocationPriority,
        "proximityIntervalMillis" to proximityIntervalMillis,
        "proximityFastestIntervalMillis" to proximityFastestIntervalMillis,
        "proximityMaxWaitMillis" to proximityMaxWaitMillis,
        "proximityMinDisplacementMeters" to proximityMinDisplacementMeters,
        "passiveLocationPriority" to passiveLocationPriority,
        "passiveLocationIntervalMillis" to passiveLocationIntervalMillis,
        "passiveLocationFastestIntervalMillis" to passiveLocationFastestIntervalMillis,
        "passiveLocationMaxWaitMillis" to passiveLocationMaxWaitMillis,
        "passiveAmbiguousConfirmEnabled" to passiveAmbiguousConfirmEnabled,
        "gpsConfirmPriority" to gpsConfirmPriority,
        "gpsConfirmTimeoutMillis" to gpsConfirmTimeoutMillis,
        "boundaryMinMarginMeters" to boundaryMinMarginMeters,
        "boundaryMarginRatio" to boundaryMarginRatio,
        "boundaryMaxMarginRatio" to boundaryMaxMarginRatio,
        "teleportGuardEnabled" to teleportGuardEnabled,
        "teleportMaxSpeedMetersPerSecond" to teleportMaxSpeedMetersPerSecond,
        "fencePulseEnabled" to fencePulseEnabled,
        "fencePulseDurationMinutes" to fencePulseDurationMinutes,
        "fencePulseIntervalSeconds" to fencePulseIntervalSeconds,
        "fencePulseMinIntervalMillis" to fencePulseMinIntervalMillis,
        "fencePulseMaxIdleTicks" to fencePulseMaxIdleTicks,
        "fencePulseExactAlarmStartDelayMillis" to fencePulseExactAlarmStartDelayMillis,
        "fencePulseNotificationTitle" to fencePulseNotificationTitle,
        "fencePulseNotificationChannelId" to fencePulseNotificationChannelId,
        "fencePulseNotificationChannelName" to fencePulseNotificationChannelName,
        "fencePulseNotificationId" to fencePulseNotificationId,
        "fencePulseNotificationSmallIconResourceName" to fencePulseNotificationSmallIconResourceName,
        "motionStationaryTtlMillis" to motionStationaryTtlMillis,
        "reconcileIntervalMinutes" to reconcileIntervalMinutes,
        "reconcileInitialDelayMillis" to reconcileInitialDelayMillis,
        "reconcileFlexIntervalMillis" to reconcileFlexIntervalMillis,
        "reconcileRequiresBatteryNotLow" to reconcileRequiresBatteryNotLow,
        "reconcileScheduler" to reconcileScheduler,
        "passiveLocationEnabled" to passiveLocationEnabled,
        "foregroundServiceLaunchTimeoutMillis" to foregroundServiceLaunchTimeoutMillis,
        "foregroundServiceStartDelayMillis" to foregroundServiceStartDelayMillis,
        "foregroundServiceRetryDelaysMillis" to foregroundServiceRetryDelaysMillis,
        "confirmQueueMaxAgeMillis" to confirmQueueMaxAgeMillis,
        "logFileEnabled" to logFileEnabled,
        "maxLogFileBytes" to maxLogFileBytes,
    )

    private fun proximityPendingIntentExists(context: Context): Boolean {
        val intent = Intent(context, ProximityReceiver::class.java).apply {
            data = Uri.parse(Constants.PENDING_INTENT_DATA_PROXIMITY)
            putExtra(
                Constants.EXTRA_LOCATION_WAKE_SOURCE,
                Constants.LOCATION_WAKE_SOURCE_PROXIMITY
            )
        }
        return pendingIntentExists(context, Constants.PENDING_INTENT_REQUEST_BASE, intent)
    }

    private fun passiveLocationPendingIntentExists(context: Context): Boolean {
        val intent = Intent(context, ProximityReceiver::class.java).apply {
            data = Uri.parse(Constants.PENDING_INTENT_DATA_PASSIVE_LOCATION)
            putExtra(
                Constants.EXTRA_LOCATION_WAKE_SOURCE,
                Constants.LOCATION_WAKE_SOURCE_PASSIVE
            )
        }
        return pendingIntentExists(context, Constants.PENDING_INTENT_REQUEST_PASSIVE_LOCATION, intent)
    }

    private fun motionPendingIntentExists(context: Context): Boolean {
        val intent = Intent(context, MotionReceiver::class.java)
        return pendingIntentExists(context, Constants.PENDING_INTENT_REQUEST_BASE + 1, intent)
    }

    private fun reconcileAlarmPendingIntentExists(context: Context): Boolean {
        val intent = Intent(context, ReconcileAlarmReceiver::class.java)
        var flags = PendingIntent.FLAG_NO_CREATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(
            context,
            Constants.PENDING_INTENT_REQUEST_RECONCILE_ALARM,
            intent,
            flags
        ) != null
    }

    private fun pendingIntentExists(
        context: Context,
        requestCode: Int,
        intent: Intent,
    ): Boolean {
        var flags = PendingIntent.FLAG_NO_CREATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags) != null
    }

    private fun hasLocationPermission(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
            granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun hasBackgroundLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun hasActivityRecognitionPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return granted(context, Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun hasForegroundServiceLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        return granted(context, "android.permission.FOREGROUND_SERVICE_LOCATION")
    }

    private fun exactAlarmPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        return alarmManager?.canScheduleExactAlarms() == true
    }

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED

    private fun locationServicesEnabled(context: Context): Boolean? {
        return try {
            val locationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    ?: return null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }
        } catch (ignored: Throwable) {
            null
        }
    }

    private fun hasReceiver(context: Context, receiverClass: Class<*>): Boolean {
        return try {
            val component = ComponentName(context, receiverClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getReceiverInfo(
                    component,
                    PackageManager.ComponentInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getReceiverInfo(component, 0)
            }
            true
        } catch (ignored: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun serviceInfo(context: Context, serviceClass: Class<*>): ServiceInfo? {
        return try {
            val component = ComponentName(context, serviceClass)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getServiceInfo(
                    component,
                    PackageManager.ComponentInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getServiceInfo(component, 0)
            }
        } catch (ignored: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun serviceHasLocationType(serviceInfo: ServiceInfo?): Boolean {
        if (serviceInfo == null) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return (serviceInfo.foregroundServiceType and
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) != 0
    }
}
