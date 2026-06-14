package com.yarithdev.smart_geofence

import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import com.yarithdev.smart_geofence.fgs.CallbackForegroundService
import com.yarithdev.smart_geofence.proximity.LocationConfirmQueue

/**
 * smart_geofence Android plugin.
 *
 * Hosts the method channel for the Android-only smart layers (proximity
 * escalation, motion gating, FencePulse, reconcile). Base geofence CRUD and the
 * geofence callback are owned by native_geofence and orchestrated from Dart;
 * this plugin only manages the smart layers that sit on top.
 */
class SmartGeofencePlugin :
    FlutterPlugin,
    MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var appContext: Context

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        SmartGeofenceLogger.initialize(appContext)
        channel = MethodChannel(binding.binaryMessenger, Constants.METHOD_CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> result.success("Android ${android.os.Build.VERSION.RELEASE}")

            "configure" -> {
                val config = parseConfig(call)
                SmartGeofenceConfigStore.save(appContext, config)
                SmartGeofenceLogger.configure(
                    appContext,
                    config.logFileEnabled,
                    config.maxLogFileBytes
                )
                // Apply immediately to any running layers (no-op if no fences).
                SmartGeofenceController.refresh(appContext)
                SmartGeofenceLogger.d(appContext, TAG, "Configured: $config")
                result.success(null)
            }

            "start" -> {
                val config = SmartGeofenceConfigStore.load(appContext)
                SmartGeofenceController.start(appContext, config)
                result.success(null)
            }

            "stop" -> {
                SmartGeofenceController.stop(appContext)
                result.success(null)
            }

            "registerFence" -> {
                val previous = FenceStore.upsert(appContext, parseFence(call))
                SmartGeofenceController.refresh(appContext)
                result.success(previous?.toMap())
            }

            "getCallbackHandles" -> {
                val ids = call.argument<List<String>>("ids") ?: emptyList()
                result.success(
                    ids.associateWith { id ->
                        FenceStore.get(appContext, id)?.callbackHandle ?: 0L
                    }
                )
            }

            "removeFence" -> {
                val id = call.argument<String>("id")
                if (id == null) {
                    result.error("invalid_argument", "Missing fence id.", null)
                } else {
                    FenceStore.remove(appContext, id)
                    LocationConfirmQueue.removeFence(appContext, id)
                    SmartGeofenceController.refresh(appContext)
                    result.success(null)
                }
            }

            "removeAllFences" -> {
                FenceStore.removeAll(appContext)
                SmartGeofenceController.stop(appContext)
                result.success(null)
            }

            "getDiagnosticStatus" -> result.success(SmartGeofenceDiagnostics.getStatus(appContext))

            "readLogFile" -> result.success(SmartGeofenceLogger.read(appContext))

            "clearLogFile" -> {
                SmartGeofenceLogger.clear(appContext)
                result.success(null)
            }

            "promoteCallbackToForeground" -> {
                CallbackForegroundService.promote(appContext)
                result.success(null)
            }

            "demoteCallbackToBackground" -> {
                CallbackForegroundService.demote(appContext)
                result.success(null)
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun parseConfig(call: MethodCall): SmartGeofenceConfig {
        val d = SmartGeofenceConfig.default()
        return SmartGeofenceConfig(
            proximityRadiusMeters = doubleArg(call, "proximityRadiusMeters", d.proximityRadiusMeters),
            escalationEnabled = boolArg(call, "escalationEnabled", d.escalationEnabled),
            proximityLocationPriority = stringArg(
                call,
                "proximityLocationPriority",
                d.proximityLocationPriority
            ),
            proximityIntervalMillis =
                longArg(call, "proximityIntervalMillis", d.proximityIntervalMillis),
            proximityFastestIntervalMillis = longArg(
                call,
                "proximityFastestIntervalMillis",
                d.proximityFastestIntervalMillis
            ),
            proximityMaxWaitMillis =
                longArg(call, "proximityMaxWaitMillis", d.proximityMaxWaitMillis),
            proximityMinDisplacementMeters = doubleArg(
                call,
                "proximityMinDisplacementMeters",
                d.proximityMinDisplacementMeters
            ),
            passiveLocationPriority = stringArg(
                call,
                "passiveLocationPriority",
                d.passiveLocationPriority
            ),
            passiveLocationIntervalMillis = longArg(
                call,
                "passiveLocationIntervalMillis",
                d.passiveLocationIntervalMillis
            ),
            passiveLocationFastestIntervalMillis = longArg(
                call,
                "passiveLocationFastestIntervalMillis",
                d.passiveLocationFastestIntervalMillis
            ),
            passiveLocationMaxWaitMillis =
                longArg(call, "passiveLocationMaxWaitMillis", d.passiveLocationMaxWaitMillis),
            passiveAmbiguousConfirmEnabled = boolArg(
                call,
                "passiveAmbiguousConfirmEnabled",
                d.passiveAmbiguousConfirmEnabled
            ),
            gpsConfirmPriority = stringArg(call, "gpsConfirmPriority", d.gpsConfirmPriority),
            gpsConfirmTimeoutMillis =
                longArg(call, "gpsConfirmTimeoutMillis", d.gpsConfirmTimeoutMillis),
            boundaryMinMarginMeters =
                doubleArg(call, "boundaryMinMarginMeters", d.boundaryMinMarginMeters),
            boundaryMarginRatio = doubleArg(call, "boundaryMarginRatio", d.boundaryMarginRatio),
            boundaryMaxMarginRatio =
                doubleArg(call, "boundaryMaxMarginRatio", d.boundaryMaxMarginRatio),
            teleportGuardEnabled = boolArg(call, "teleportGuardEnabled", d.teleportGuardEnabled),
            teleportMaxSpeedMetersPerSecond = doubleArg(
                call,
                "teleportMaxSpeedMetersPerSecond",
                d.teleportMaxSpeedMetersPerSecond
            ),
            fencePulseEnabled = boolArg(call, "fencePulseEnabled", d.fencePulseEnabled),
            fencePulseDurationMinutes =
                longArg(call, "fencePulseDurationMinutes", d.fencePulseDurationMinutes),
            fencePulseIntervalSeconds =
                longArg(call, "fencePulseIntervalSeconds", d.fencePulseIntervalSeconds),
            fencePulseMinIntervalMillis =
                longArg(call, "fencePulseMinIntervalMillis", d.fencePulseMinIntervalMillis),
            fencePulseMaxIdleTicks =
                intArg(call, "fencePulseMaxIdleTicks", d.fencePulseMaxIdleTicks),
            fencePulseExactAlarmStartDelayMillis = longArg(
                call,
                "fencePulseExactAlarmStartDelayMillis",
                d.fencePulseExactAlarmStartDelayMillis
            ),
            fencePulseNotificationTitle = stringArg(
                call,
                "fencePulseNotificationTitle",
                d.fencePulseNotificationTitle
            ),
            fencePulseNotificationChannelId = stringArg(
                call,
                "fencePulseNotificationChannelId",
                d.fencePulseNotificationChannelId
            ),
            fencePulseNotificationChannelName = stringArg(
                call,
                "fencePulseNotificationChannelName",
                d.fencePulseNotificationChannelName
            ),
            fencePulseNotificationId =
                intArg(call, "fencePulseNotificationId", d.fencePulseNotificationId),
            fencePulseNotificationSmallIconResourceName = nullableStringArg(
                call,
                "fencePulseNotificationSmallIconResourceName"
            ),
            motionStationaryTtlMillis =
                longArg(call, "motionStationaryTtlMillis", d.motionStationaryTtlMillis),
            reconcileIntervalMinutes =
                longArg(call, "reconcileIntervalMinutes", d.reconcileIntervalMinutes),
            reconcileInitialDelayMillis =
                longArg(call, "reconcileInitialDelayMillis", d.reconcileInitialDelayMillis),
            reconcileFlexIntervalMillis =
                longArg(call, "reconcileFlexIntervalMillis", d.reconcileFlexIntervalMillis),
            reconcileRequiresBatteryNotLow = boolArg(
                call,
                "reconcileRequiresBatteryNotLow",
                d.reconcileRequiresBatteryNotLow
            ),
            reconcileScheduler = stringArg(
                call,
                "reconcileScheduler",
                d.reconcileScheduler
            ),
            logFileEnabled = boolArg(call, "logFileEnabled", d.logFileEnabled),
            maxLogFileBytes = intArg(call, "maxLogFileBytes", d.maxLogFileBytes),
            passiveLocationEnabled =
                boolArg(call, "passiveLocationEnabled", d.passiveLocationEnabled),
            foregroundServiceLaunchTimeoutMillis = longArg(
                call,
                "foregroundServiceLaunchTimeoutMillis",
                d.foregroundServiceLaunchTimeoutMillis
            ),
            foregroundServiceStartDelayMillis = longArg(
                call,
                "foregroundServiceStartDelayMillis",
                d.foregroundServiceStartDelayMillis
            ),
            foregroundServiceRetryDelaysMillis = longListArg(
                call,
                "foregroundServiceRetryDelaysMillis",
                d.foregroundServiceRetryDelaysMillis
            ),
            confirmQueueMaxAgeMillis = longArg(
                call,
                "confirmQueueMaxAgeMillis",
                d.confirmQueueMaxAgeMillis
            ),
        )
    }

    private fun boolArg(call: MethodCall, key: String, defaultValue: Boolean): Boolean =
        call.argument<Boolean>(key) ?: defaultValue

    private fun stringArg(call: MethodCall, key: String, defaultValue: String): String =
        call.argument<String>(key) ?: defaultValue

    private fun nullableStringArg(call: MethodCall, key: String): String? =
        call.argument<String>(key)

    private fun intArg(call: MethodCall, key: String, defaultValue: Int): Int =
        call.argument<Int>(key)
            ?: call.argument<Long>(key)?.toInt()
            ?: call.argument<Double>(key)?.toInt()
            ?: defaultValue

    private fun longArg(call: MethodCall, key: String, defaultValue: Long): Long =
        call.argument<Long>(key)
            ?: call.argument<Int>(key)?.toLong()
            ?: call.argument<Double>(key)?.toLong()
            ?: defaultValue

    private fun longListArg(call: MethodCall, key: String, defaultValue: List<Long>): List<Long> {
        val value = call.argument<List<Any?>>(key) ?: return defaultValue
        val parsed = value.mapNotNull {
            when (it) {
                is Long -> it
                is Int -> it.toLong()
                is Double -> it.toLong()
                is Float -> it.toLong()
                else -> null
            }?.takeIf { item -> item >= 0L }
        }
        return parsed.takeIf { it.isNotEmpty() } ?: defaultValue
    }

    private fun doubleArg(call: MethodCall, key: String, defaultValue: Double): Double =
        call.argument<Double>(key)
            ?: call.argument<Int>(key)?.toDouble()
            ?: call.argument<Long>(key)?.toDouble()
            ?: defaultValue

    private fun parseFence(call: MethodCall): SmartGeofenceFence {
        val triggers = call.argument<List<String>>("triggers") ?: emptyList()
        return SmartGeofenceFence(
            id = call.argument<String>("id")!!,
            latitude = call.argument<Double>("latitude")!!,
            longitude = call.argument<Double>("longitude")!!,
            radiusMeters = call.argument<Double>("radiusMeters")!!,
            triggersEnter = triggers.contains("enter"),
            triggersExit = triggers.contains("exit"),
            triggersDwell = triggers.contains("dwell"),
            callbackHandle = (call.argument<Long>("callbackHandle"))
                ?: (call.argument<Int>("callbackHandle"))?.toLong() ?: 0L,
        )
    }

    companion object {
        const val TAG = "SmartGeofencePlugin"
    }
}
