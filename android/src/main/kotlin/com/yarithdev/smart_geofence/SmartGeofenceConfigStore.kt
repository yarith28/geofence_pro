package com.yarithdev.smart_geofence

import android.content.Context

/**
 * Persisted smart-layer configuration. Survives process death so background components
 * (receivers, workers) can read the active config without a live Dart isolate.
 */
data class SmartGeofenceConfig(
    val proximityRadiusMeters: Double,
    val escalationEnabled: Boolean,
    val proximityLocationPriority: String,
    val proximityIntervalMillis: Long,
    val proximityFastestIntervalMillis: Long,
    val proximityMaxWaitMillis: Long,
    val proximityMinDisplacementMeters: Double,
    val passiveLocationPriority: String,
    val passiveLocationIntervalMillis: Long,
    val passiveLocationFastestIntervalMillis: Long,
    val passiveLocationMaxWaitMillis: Long,
    val passiveAmbiguousConfirmEnabled: Boolean,
    val gpsConfirmPriority: String,
    val gpsConfirmTimeoutMillis: Long,
    val boundaryMinMarginMeters: Double,
    val boundaryMarginRatio: Double,
    val boundaryMaxMarginRatio: Double,
    val teleportGuardEnabled: Boolean,
    val teleportMaxSpeedMetersPerSecond: Double,
    val fencePulseEnabled: Boolean,
    val fencePulseDurationMinutes: Long,
    val fencePulseIntervalSeconds: Long,
    val fencePulseMinIntervalMillis: Long,
    val fencePulseMaxIdleTicks: Int,
    val fencePulseExactAlarmStartDelayMillis: Long,
    val fencePulseNotificationTitle: String,
    val fencePulseNotificationChannelId: String,
    val fencePulseNotificationChannelName: String,
    val fencePulseNotificationId: Int,
    val fencePulseNotificationSmallIconResourceName: String?,
    val motionStationaryTtlMillis: Long,
    val reconcileIntervalMinutes: Long,
    val reconcileInitialDelayMillis: Long,
    val reconcileFlexIntervalMillis: Long,
    val reconcileRequiresBatteryNotLow: Boolean,
    val reconcileScheduler: String,
    val logFileEnabled: Boolean,
    val maxLogFileBytes: Int,
    val passiveLocationEnabled: Boolean,
    val foregroundServiceLaunchTimeoutMillis: Long,
    val foregroundServiceStartDelayMillis: Long,
    val foregroundServiceRetryDelaysMillis: List<Long>,
    val confirmQueueMaxAgeMillis: Long,
) {
    companion object {
        fun default(): SmartGeofenceConfig = SmartGeofenceConfig(
            proximityRadiusMeters = Constants.DEFAULT_PROXIMITY_RADIUS_METERS,
            escalationEnabled = true,
            proximityLocationPriority = Constants.DEFAULT_LOCATION_PRIORITY_BALANCED_POWER_ACCURACY,
            proximityIntervalMillis = Constants.DEFAULT_PROXIMITY_INTERVAL_MILLIS,
            proximityFastestIntervalMillis = Constants.DEFAULT_PROXIMITY_FASTEST_INTERVAL_MILLIS,
            proximityMaxWaitMillis = Constants.DEFAULT_PROXIMITY_MAX_WAIT_MILLIS,
            proximityMinDisplacementMeters = Constants.DEFAULT_PROXIMITY_MIN_DISPLACEMENT_METERS,
            passiveLocationPriority = Constants.DEFAULT_LOCATION_PRIORITY_PASSIVE,
            passiveLocationIntervalMillis = Constants.DEFAULT_PASSIVE_LOCATION_INTERVAL_MILLIS,
            passiveLocationFastestIntervalMillis =
                Constants.DEFAULT_PASSIVE_LOCATION_FASTEST_INTERVAL_MILLIS,
            passiveLocationMaxWaitMillis = Constants.DEFAULT_PASSIVE_LOCATION_MAX_WAIT_MILLIS,
            passiveAmbiguousConfirmEnabled =
                Constants.DEFAULT_PASSIVE_AMBIGUOUS_CONFIRM_ENABLED,
            gpsConfirmPriority = Constants.DEFAULT_LOCATION_PRIORITY_HIGH_ACCURACY,
            gpsConfirmTimeoutMillis = Constants.DEFAULT_GPS_CONFIRM_TIMEOUT_MILLIS,
            boundaryMinMarginMeters = Constants.DEFAULT_BOUNDARY_MIN_MARGIN_METERS,
            boundaryMarginRatio = Constants.DEFAULT_BOUNDARY_MARGIN_RATIO,
            boundaryMaxMarginRatio = Constants.DEFAULT_BOUNDARY_MAX_MARGIN_RATIO,
            teleportGuardEnabled = Constants.DEFAULT_TELEPORT_GUARD_ENABLED,
            teleportMaxSpeedMetersPerSecond = Constants.DEFAULT_TELEPORT_MAX_SPEED_MPS,
            fencePulseEnabled = false,
            fencePulseDurationMinutes = Constants.DEFAULT_FENCE_PULSE_DURATION_MINUTES,
            fencePulseIntervalSeconds = Constants.DEFAULT_FENCE_PULSE_INTERVAL_SECONDS,
            fencePulseMinIntervalMillis = Constants.DEFAULT_FENCE_PULSE_MIN_INTERVAL_MILLIS,
            fencePulseMaxIdleTicks = Constants.DEFAULT_FENCE_PULSE_MAX_IDLE_TICKS,
            fencePulseExactAlarmStartDelayMillis =
                Constants.DEFAULT_FENCE_PULSE_EXACT_ALARM_START_DELAY_MILLIS,
            fencePulseNotificationTitle = Constants.DEFAULT_FENCE_PULSE_NOTIFICATION_TITLE,
            fencePulseNotificationChannelId = Constants.DEFAULT_FENCE_PULSE_NOTIFICATION_CHANNEL_ID,
            fencePulseNotificationChannelName =
                Constants.DEFAULT_FENCE_PULSE_NOTIFICATION_CHANNEL_NAME,
            fencePulseNotificationId = Constants.DEFAULT_FENCE_PULSE_NOTIFICATION_ID,
            fencePulseNotificationSmallIconResourceName = null,
            motionStationaryTtlMillis = Constants.DEFAULT_MOTION_STATIONARY_TTL_MILLIS,
            reconcileIntervalMinutes = Constants.DEFAULT_RECONCILE_INTERVAL_MINUTES,
            reconcileInitialDelayMillis = Constants.DEFAULT_RECONCILE_INITIAL_DELAY_MILLIS,
            reconcileFlexIntervalMillis = Constants.DEFAULT_RECONCILE_FLEX_INTERVAL_MILLIS,
            reconcileRequiresBatteryNotLow = Constants.DEFAULT_RECONCILE_REQUIRES_BATTERY_NOT_LOW,
            reconcileScheduler = Constants.RECONCILE_SCHEDULER_WORK_MANAGER,
            logFileEnabled = false,
            maxLogFileBytes = Constants.DEFAULT_LOG_FILE_MAX_BYTES,
            passiveLocationEnabled = Constants.DEFAULT_PASSIVE_LOCATION_ENABLED,
            foregroundServiceLaunchTimeoutMillis =
                Constants.DEFAULT_FOREGROUND_SERVICE_LAUNCH_TIMEOUT_MILLIS,
            foregroundServiceStartDelayMillis =
                Constants.DEFAULT_FOREGROUND_SERVICE_START_DELAY_MILLIS,
            foregroundServiceRetryDelaysMillis =
                Constants.DEFAULT_FOREGROUND_SERVICE_RETRY_DELAYS_MILLIS,
            confirmQueueMaxAgeMillis = Constants.DEFAULT_CONFIRM_QUEUE_MAX_AGE_MILLIS,
        )
    }
}

object SmartGeofenceConfigStore {
    fun save(context: Context, config: SmartGeofenceConfig) {
        context.applicationContext
            .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(Constants.CONFIG_PROXIMITY_RADIUS_METERS, config.proximityRadiusMeters.toFloat())
            .putBoolean(Constants.CONFIG_ESCALATION_ENABLED, config.escalationEnabled)
            .putString(Constants.CONFIG_PROXIMITY_LOCATION_PRIORITY, config.proximityLocationPriority)
            .putLong(Constants.CONFIG_PROXIMITY_INTERVAL_MILLIS, config.proximityIntervalMillis)
            .putLong(
                Constants.CONFIG_PROXIMITY_FASTEST_INTERVAL_MILLIS,
                config.proximityFastestIntervalMillis
            )
            .putLong(Constants.CONFIG_PROXIMITY_MAX_WAIT_MILLIS, config.proximityMaxWaitMillis)
            .putFloat(
                Constants.CONFIG_PROXIMITY_MIN_DISPLACEMENT_METERS,
                config.proximityMinDisplacementMeters.toFloat()
            )
            .putString(Constants.CONFIG_PASSIVE_LOCATION_PRIORITY, config.passiveLocationPriority)
            .putLong(
                Constants.CONFIG_PASSIVE_LOCATION_INTERVAL_MILLIS,
                config.passiveLocationIntervalMillis
            )
            .putLong(
                Constants.CONFIG_PASSIVE_LOCATION_FASTEST_INTERVAL_MILLIS,
                config.passiveLocationFastestIntervalMillis
            )
            .putLong(
                Constants.CONFIG_PASSIVE_LOCATION_MAX_WAIT_MILLIS,
                config.passiveLocationMaxWaitMillis
            )
            .putBoolean(
                Constants.CONFIG_PASSIVE_AMBIGUOUS_CONFIRM_ENABLED,
                config.passiveAmbiguousConfirmEnabled
            )
            .putString(Constants.CONFIG_GPS_CONFIRM_PRIORITY, config.gpsConfirmPriority)
            .putLong(Constants.CONFIG_GPS_CONFIRM_TIMEOUT_MILLIS, config.gpsConfirmTimeoutMillis)
            .putFloat(Constants.CONFIG_BOUNDARY_MIN_MARGIN_METERS, config.boundaryMinMarginMeters.toFloat())
            .putFloat(Constants.CONFIG_BOUNDARY_MARGIN_RATIO, config.boundaryMarginRatio.toFloat())
            .putFloat(
                Constants.CONFIG_BOUNDARY_MAX_MARGIN_RATIO,
                config.boundaryMaxMarginRatio.toFloat()
            )
            .putBoolean(Constants.CONFIG_TELEPORT_GUARD_ENABLED, config.teleportGuardEnabled)
            .putFloat(
                Constants.CONFIG_TELEPORT_MAX_SPEED_MPS,
                config.teleportMaxSpeedMetersPerSecond.toFloat()
            )
            .putBoolean(Constants.CONFIG_FENCE_PULSE_ENABLED, config.fencePulseEnabled)
            .putLong(Constants.CONFIG_FENCE_PULSE_DURATION_MINUTES, config.fencePulseDurationMinutes)
            .putLong(Constants.CONFIG_FENCE_PULSE_INTERVAL_SECONDS, config.fencePulseIntervalSeconds)
            .putLong(Constants.CONFIG_FENCE_PULSE_MIN_INTERVAL_MILLIS, config.fencePulseMinIntervalMillis)
            .putInt(Constants.CONFIG_FENCE_PULSE_MAX_IDLE_TICKS, config.fencePulseMaxIdleTicks)
            .putLong(
                Constants.CONFIG_FENCE_PULSE_EXACT_ALARM_START_DELAY_MILLIS,
                config.fencePulseExactAlarmStartDelayMillis
            )
            .putString(Constants.CONFIG_FENCE_PULSE_NOTIFICATION_TITLE, config.fencePulseNotificationTitle)
            .putString(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_CHANNEL_ID,
                config.fencePulseNotificationChannelId
            )
            .putString(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_CHANNEL_NAME,
                config.fencePulseNotificationChannelName
            )
            .putInt(Constants.CONFIG_FENCE_PULSE_NOTIFICATION_ID, config.fencePulseNotificationId)
            .putString(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_SMALL_ICON_RESOURCE_NAME,
                config.fencePulseNotificationSmallIconResourceName
            )
            .putLong(Constants.CONFIG_MOTION_STATIONARY_TTL_MILLIS, config.motionStationaryTtlMillis)
            .putLong(Constants.CONFIG_RECONCILE_INTERVAL_MINUTES, config.reconcileIntervalMinutes)
            .putLong(Constants.CONFIG_RECONCILE_INITIAL_DELAY_MILLIS, config.reconcileInitialDelayMillis)
            .putLong(Constants.CONFIG_RECONCILE_FLEX_INTERVAL_MILLIS, config.reconcileFlexIntervalMillis)
            .putBoolean(
                Constants.CONFIG_RECONCILE_REQUIRES_BATTERY_NOT_LOW,
                config.reconcileRequiresBatteryNotLow
            )
            .putString(Constants.CONFIG_RECONCILE_SCHEDULER, config.reconcileScheduler)
            .putBoolean(Constants.CONFIG_LOG_FILE_ENABLED, config.logFileEnabled)
            .putInt(Constants.CONFIG_MAX_LOG_FILE_BYTES, config.maxLogFileBytes)
            .putBoolean(Constants.CONFIG_PASSIVE_LOCATION_ENABLED, config.passiveLocationEnabled)
            .putLong(
                Constants.CONFIG_FOREGROUND_SERVICE_LAUNCH_TIMEOUT_MILLIS,
                config.foregroundServiceLaunchTimeoutMillis
            )
            .putLong(
                Constants.CONFIG_FOREGROUND_SERVICE_START_DELAY_MILLIS,
                config.foregroundServiceStartDelayMillis
            )
            .putString(
                Constants.CONFIG_FOREGROUND_SERVICE_RETRY_DELAYS_MILLIS,
                config.foregroundServiceRetryDelaysMillis.joinToString(",")
            )
            .putLong(Constants.CONFIG_CONFIRM_QUEUE_MAX_AGE_MILLIS, config.confirmQueueMaxAgeMillis)
            .apply()
    }

    fun load(context: Context): SmartGeofenceConfig {
        val p = context.applicationContext
            .getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        val d = SmartGeofenceConfig.default()
        return SmartGeofenceConfig(
            proximityRadiusMeters = p.getFloat(
                Constants.CONFIG_PROXIMITY_RADIUS_METERS, d.proximityRadiusMeters.toFloat()
            ).toDouble(),
            escalationEnabled = p.getBoolean(Constants.CONFIG_ESCALATION_ENABLED, d.escalationEnabled),
            proximityLocationPriority = p.getString(
                Constants.CONFIG_PROXIMITY_LOCATION_PRIORITY,
                d.proximityLocationPriority
            ) ?: d.proximityLocationPriority,
            proximityIntervalMillis = p.getLong(
                Constants.CONFIG_PROXIMITY_INTERVAL_MILLIS,
                d.proximityIntervalMillis
            ),
            proximityFastestIntervalMillis = p.getLong(
                Constants.CONFIG_PROXIMITY_FASTEST_INTERVAL_MILLIS,
                d.proximityFastestIntervalMillis
            ),
            proximityMaxWaitMillis = p.getLong(
                Constants.CONFIG_PROXIMITY_MAX_WAIT_MILLIS,
                d.proximityMaxWaitMillis
            ),
            proximityMinDisplacementMeters = p.getFloat(
                Constants.CONFIG_PROXIMITY_MIN_DISPLACEMENT_METERS,
                d.proximityMinDisplacementMeters.toFloat()
            ).toDouble(),
            passiveLocationPriority = p.getString(
                Constants.CONFIG_PASSIVE_LOCATION_PRIORITY,
                d.passiveLocationPriority
            ) ?: d.passiveLocationPriority,
            passiveLocationIntervalMillis = p.getLong(
                Constants.CONFIG_PASSIVE_LOCATION_INTERVAL_MILLIS,
                d.passiveLocationIntervalMillis
            ),
            passiveLocationFastestIntervalMillis = p.getLong(
                Constants.CONFIG_PASSIVE_LOCATION_FASTEST_INTERVAL_MILLIS,
                d.passiveLocationFastestIntervalMillis
            ),
            passiveLocationMaxWaitMillis = p.getLong(
                Constants.CONFIG_PASSIVE_LOCATION_MAX_WAIT_MILLIS,
                d.passiveLocationMaxWaitMillis
            ),
            passiveAmbiguousConfirmEnabled = p.getBoolean(
                Constants.CONFIG_PASSIVE_AMBIGUOUS_CONFIRM_ENABLED,
                d.passiveAmbiguousConfirmEnabled
            ),
            gpsConfirmPriority = p.getString(
                Constants.CONFIG_GPS_CONFIRM_PRIORITY,
                d.gpsConfirmPriority
            ) ?: d.gpsConfirmPriority,
            gpsConfirmTimeoutMillis = p.getLong(
                Constants.CONFIG_GPS_CONFIRM_TIMEOUT_MILLIS,
                d.gpsConfirmTimeoutMillis
            ),
            boundaryMinMarginMeters = p.getFloat(
                Constants.CONFIG_BOUNDARY_MIN_MARGIN_METERS,
                d.boundaryMinMarginMeters.toFloat()
            ).toDouble(),
            boundaryMarginRatio = p.getFloat(
                Constants.CONFIG_BOUNDARY_MARGIN_RATIO,
                d.boundaryMarginRatio.toFloat()
            ).toDouble(),
            boundaryMaxMarginRatio = p.getFloat(
                Constants.CONFIG_BOUNDARY_MAX_MARGIN_RATIO,
                d.boundaryMaxMarginRatio.toFloat()
            ).toDouble(),
            teleportGuardEnabled = p.getBoolean(
                Constants.CONFIG_TELEPORT_GUARD_ENABLED,
                d.teleportGuardEnabled
            ),
            teleportMaxSpeedMetersPerSecond = p.getFloat(
                Constants.CONFIG_TELEPORT_MAX_SPEED_MPS,
                d.teleportMaxSpeedMetersPerSecond.toFloat()
            ).toDouble(),
            fencePulseEnabled = p.getBoolean(Constants.CONFIG_FENCE_PULSE_ENABLED, d.fencePulseEnabled),
            fencePulseDurationMinutes = p.getLong(
                Constants.CONFIG_FENCE_PULSE_DURATION_MINUTES, d.fencePulseDurationMinutes
            ),
            fencePulseIntervalSeconds = p.getLong(
                Constants.CONFIG_FENCE_PULSE_INTERVAL_SECONDS, d.fencePulseIntervalSeconds
            ),
            fencePulseMinIntervalMillis = p.getLong(
                Constants.CONFIG_FENCE_PULSE_MIN_INTERVAL_MILLIS,
                d.fencePulseMinIntervalMillis
            ),
            fencePulseMaxIdleTicks = p.getInt(
                Constants.CONFIG_FENCE_PULSE_MAX_IDLE_TICKS,
                d.fencePulseMaxIdleTicks
            ),
            fencePulseExactAlarmStartDelayMillis = p.getLong(
                Constants.CONFIG_FENCE_PULSE_EXACT_ALARM_START_DELAY_MILLIS,
                d.fencePulseExactAlarmStartDelayMillis
            ),
            fencePulseNotificationTitle = p.getString(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_TITLE,
                d.fencePulseNotificationTitle
            ) ?: d.fencePulseNotificationTitle,
            fencePulseNotificationChannelId = p.getString(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_CHANNEL_ID,
                d.fencePulseNotificationChannelId
            ) ?: d.fencePulseNotificationChannelId,
            fencePulseNotificationChannelName = p.getString(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_CHANNEL_NAME,
                d.fencePulseNotificationChannelName
            ) ?: d.fencePulseNotificationChannelName,
            fencePulseNotificationId = p.getInt(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_ID,
                d.fencePulseNotificationId
            ),
            fencePulseNotificationSmallIconResourceName = p.getString(
                Constants.CONFIG_FENCE_PULSE_NOTIFICATION_SMALL_ICON_RESOURCE_NAME,
                d.fencePulseNotificationSmallIconResourceName
            ),
            motionStationaryTtlMillis = p.getLong(
                Constants.CONFIG_MOTION_STATIONARY_TTL_MILLIS,
                d.motionStationaryTtlMillis
            ),
            reconcileIntervalMinutes = p.getLong(
                Constants.CONFIG_RECONCILE_INTERVAL_MINUTES, d.reconcileIntervalMinutes
            ),
            reconcileInitialDelayMillis = p.getLong(
                Constants.CONFIG_RECONCILE_INITIAL_DELAY_MILLIS,
                d.reconcileInitialDelayMillis
            ),
            reconcileFlexIntervalMillis = p.getLong(
                Constants.CONFIG_RECONCILE_FLEX_INTERVAL_MILLIS,
                d.reconcileFlexIntervalMillis
            ),
            reconcileRequiresBatteryNotLow = p.getBoolean(
                Constants.CONFIG_RECONCILE_REQUIRES_BATTERY_NOT_LOW,
                d.reconcileRequiresBatteryNotLow
            ),
            reconcileScheduler = p.getString(
                Constants.CONFIG_RECONCILE_SCHEDULER,
                d.reconcileScheduler
            ) ?: d.reconcileScheduler,
            logFileEnabled = p.getBoolean(Constants.CONFIG_LOG_FILE_ENABLED, d.logFileEnabled),
            maxLogFileBytes = p.getInt(Constants.CONFIG_MAX_LOG_FILE_BYTES, d.maxLogFileBytes),
            passiveLocationEnabled = p.getBoolean(
                Constants.CONFIG_PASSIVE_LOCATION_ENABLED,
                d.passiveLocationEnabled
            ),
            foregroundServiceLaunchTimeoutMillis = p.getLong(
                Constants.CONFIG_FOREGROUND_SERVICE_LAUNCH_TIMEOUT_MILLIS,
                d.foregroundServiceLaunchTimeoutMillis
            ),
            foregroundServiceStartDelayMillis = p.getLong(
                Constants.CONFIG_FOREGROUND_SERVICE_START_DELAY_MILLIS,
                d.foregroundServiceStartDelayMillis
            ),
            foregroundServiceRetryDelaysMillis = parseRetryDelays(
                p.getString(Constants.CONFIG_FOREGROUND_SERVICE_RETRY_DELAYS_MILLIS, null),
                d.foregroundServiceRetryDelaysMillis
            ),
            confirmQueueMaxAgeMillis = p.getLong(
                Constants.CONFIG_CONFIRM_QUEUE_MAX_AGE_MILLIS,
                d.confirmQueueMaxAgeMillis
            ),
        )
    }

    private fun parseRetryDelays(raw: String?, defaultValue: List<Long>): List<Long> {
        val parsed = raw
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull()?.takeIf { value -> value >= 0L } }
            ?.takeIf { it.isNotEmpty() }
        return parsed ?: defaultValue
    }
}
