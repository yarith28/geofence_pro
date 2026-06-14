package com.yarithdev.smart_geofence

/** Shared constants for the smart_geofence Android layer. */
object Constants {
    const val METHOD_CHANNEL_NAME = "smart_geofence"

    // SharedPreferences store for smart-layer configuration + runtime state.
    const val PREFS_NAME = "smart_geofence"

    // Config keys (mirrors the Dart SmartGeofenceConfig).
    const val CONFIG_PROXIMITY_RADIUS_METERS = "config_proximity_radius_meters"
    const val CONFIG_ESCALATION_ENABLED = "config_escalation_enabled"
    const val CONFIG_PROXIMITY_LOCATION_PRIORITY = "config_proximity_location_priority"
    const val CONFIG_PROXIMITY_INTERVAL_MILLIS = "config_proximity_interval_millis"
    const val CONFIG_PROXIMITY_FASTEST_INTERVAL_MILLIS =
        "config_proximity_fastest_interval_millis"
    const val CONFIG_PROXIMITY_MAX_WAIT_MILLIS = "config_proximity_max_wait_millis"
    const val CONFIG_PROXIMITY_MIN_DISPLACEMENT_METERS =
        "config_proximity_min_displacement_meters"
    const val CONFIG_PASSIVE_LOCATION_PRIORITY = "config_passive_location_priority"
    const val CONFIG_PASSIVE_LOCATION_INTERVAL_MILLIS =
        "config_passive_location_interval_millis"
    const val CONFIG_PASSIVE_LOCATION_FASTEST_INTERVAL_MILLIS =
        "config_passive_location_fastest_interval_millis"
    const val CONFIG_PASSIVE_LOCATION_MAX_WAIT_MILLIS =
        "config_passive_location_max_wait_millis"
    const val CONFIG_PASSIVE_AMBIGUOUS_CONFIRM_ENABLED =
        "config_passive_ambiguous_confirm_enabled"
    const val CONFIG_GPS_CONFIRM_PRIORITY = "config_gps_confirm_priority"
    const val CONFIG_GPS_CONFIRM_TIMEOUT_MILLIS = "config_gps_confirm_timeout_millis"
    const val CONFIG_BOUNDARY_MIN_MARGIN_METERS = "config_boundary_min_margin_meters"
    const val CONFIG_BOUNDARY_MARGIN_RATIO = "config_boundary_margin_ratio"
    const val CONFIG_BOUNDARY_MAX_MARGIN_RATIO = "config_boundary_max_margin_ratio"
    const val CONFIG_TELEPORT_GUARD_ENABLED = "config_teleport_guard_enabled"
    const val CONFIG_TELEPORT_MAX_SPEED_MPS = "config_teleport_max_speed_mps"
    const val CONFIG_FENCE_PULSE_ENABLED = "config_fence_pulse_enabled"
    const val CONFIG_FENCE_PULSE_DURATION_MINUTES = "config_fence_pulse_duration_minutes"
    const val CONFIG_FENCE_PULSE_INTERVAL_SECONDS = "config_fence_pulse_interval_seconds"
    const val CONFIG_FENCE_PULSE_MIN_INTERVAL_MILLIS =
        "config_fence_pulse_min_interval_millis"
    const val CONFIG_FENCE_PULSE_MAX_IDLE_TICKS = "config_fence_pulse_max_idle_ticks"
    const val CONFIG_FENCE_PULSE_EXACT_ALARM_START_DELAY_MILLIS =
        "config_fence_pulse_exact_alarm_start_delay_millis"
    const val CONFIG_FENCE_PULSE_NOTIFICATION_TITLE =
        "config_fence_pulse_notification_title"
    const val CONFIG_FENCE_PULSE_NOTIFICATION_CHANNEL_ID =
        "config_fence_pulse_notification_channel_id"
    const val CONFIG_FENCE_PULSE_NOTIFICATION_CHANNEL_NAME =
        "config_fence_pulse_notification_channel_name"
    const val CONFIG_FENCE_PULSE_NOTIFICATION_ID = "config_fence_pulse_notification_id"
    const val CONFIG_FENCE_PULSE_NOTIFICATION_SMALL_ICON_RESOURCE_NAME =
        "config_fence_pulse_notification_small_icon_resource_name"
    const val CONFIG_MOTION_STATIONARY_TTL_MILLIS = "config_motion_stationary_ttl_millis"
    const val CONFIG_RECONCILE_INTERVAL_MINUTES = "config_reconcile_interval_minutes"
    const val CONFIG_RECONCILE_INITIAL_DELAY_MILLIS =
        "config_reconcile_initial_delay_millis"
    const val CONFIG_RECONCILE_FLEX_INTERVAL_MILLIS =
        "config_reconcile_flex_interval_millis"
    const val CONFIG_RECONCILE_REQUIRES_BATTERY_NOT_LOW =
        "config_reconcile_requires_battery_not_low"
    const val CONFIG_RECONCILE_SCHEDULER = "config_reconcile_scheduler"
    const val CONFIG_LOG_FILE_ENABLED = "config_log_file_enabled"
    const val CONFIG_MAX_LOG_FILE_BYTES = "config_max_log_file_bytes"
    const val CONFIG_PASSIVE_LOCATION_ENABLED = "config_passive_location_enabled"
    const val CONFIG_FOREGROUND_SERVICE_LAUNCH_TIMEOUT_MILLIS =
        "config_foreground_service_launch_timeout_millis"
    const val CONFIG_FOREGROUND_SERVICE_START_DELAY_MILLIS =
        "config_foreground_service_start_delay_millis"
    const val CONFIG_FOREGROUND_SERVICE_RETRY_DELAYS_MILLIS =
        "config_foreground_service_retry_delays_millis"
    const val CONFIG_CONFIRM_QUEUE_MAX_AGE_MILLIS =
        "config_confirm_queue_max_age_millis"

    // Config defaults.
    const val DEFAULT_PROXIMITY_RADIUS_METERS = 1000.0
    const val DEFAULT_LOCATION_PRIORITY_HIGH_ACCURACY = "highAccuracy"
    const val DEFAULT_LOCATION_PRIORITY_BALANCED_POWER_ACCURACY = "balancedPowerAccuracy"
    const val DEFAULT_LOCATION_PRIORITY_LOW_POWER = "lowPower"
    const val DEFAULT_LOCATION_PRIORITY_PASSIVE = "passive"
    const val DEFAULT_PROXIMITY_INTERVAL_MILLIS = 2 * 60 * 1000L
    const val DEFAULT_PROXIMITY_FASTEST_INTERVAL_MILLIS = 1 * 60 * 1000L
    const val DEFAULT_PROXIMITY_MAX_WAIT_MILLIS = 5 * 60 * 1000L
    const val DEFAULT_PROXIMITY_MIN_DISPLACEMENT_METERS = 200.0
    const val DEFAULT_PASSIVE_LOCATION_INTERVAL_MILLIS = 20 * 60 * 1000L
    const val DEFAULT_PASSIVE_LOCATION_FASTEST_INTERVAL_MILLIS = 10 * 60 * 1000L
    const val DEFAULT_PASSIVE_LOCATION_MAX_WAIT_MILLIS = 40 * 60 * 1000L
    const val DEFAULT_PASSIVE_AMBIGUOUS_CONFIRM_ENABLED = true
    const val DEFAULT_GPS_CONFIRM_TIMEOUT_MILLIS = 8 * 1000L
    const val DEFAULT_BOUNDARY_MIN_MARGIN_METERS = 20.0
    const val DEFAULT_BOUNDARY_MARGIN_RATIO = 0.10
    const val DEFAULT_BOUNDARY_MAX_MARGIN_RATIO = 0.50
    const val DEFAULT_TELEPORT_GUARD_ENABLED = true
    const val DEFAULT_TELEPORT_MAX_SPEED_MPS = 70.0
    const val DEFAULT_FENCE_PULSE_DURATION_MINUTES = 120L
    const val DEFAULT_FENCE_PULSE_INTERVAL_SECONDS = 75L
    const val DEFAULT_FENCE_PULSE_MIN_INTERVAL_MILLIS = 30 * 1000L
    const val DEFAULT_FENCE_PULSE_MAX_IDLE_TICKS = 5
    const val DEFAULT_FENCE_PULSE_EXACT_ALARM_START_DELAY_MILLIS = 250L
    const val DEFAULT_FENCE_PULSE_NOTIFICATION_TITLE = "Checking nearby geofence"
    const val DEFAULT_FENCE_PULSE_NOTIFICATION_CHANNEL_ID = "smart_geofence_fence_pulse"
    const val DEFAULT_FENCE_PULSE_NOTIFICATION_CHANNEL_NAME = "Geofence monitoring"
    const val DEFAULT_FENCE_PULSE_NOTIFICATION_ID = 9102
    const val DEFAULT_CONFIRM_NOTIFICATION_TITLE = "Checking nearby geofence"
    const val DEFAULT_CONFIRM_NOTIFICATION_ID = 9102
    const val DEFAULT_MOTION_STATIONARY_TTL_MILLIS = 15 * 60 * 1000L
    const val DEFAULT_RECONCILE_INTERVAL_MINUTES = 30L
    const val DEFAULT_RECONCILE_INITIAL_DELAY_MILLIS = 0L
    const val DEFAULT_RECONCILE_FLEX_INTERVAL_MILLIS = 0L
    const val DEFAULT_RECONCILE_REQUIRES_BATTERY_NOT_LOW = false
    const val RECONCILE_SCHEDULER_WORK_MANAGER = "workManager"
    const val RECONCILE_SCHEDULER_EXACT_ALARM = "exactAlarm"
    const val DEFAULT_PASSIVE_LOCATION_ENABLED = true
    const val DEFAULT_FOREGROUND_SERVICE_LAUNCH_TIMEOUT_MILLIS = 10 * 1000L
    const val DEFAULT_FOREGROUND_SERVICE_START_DELAY_MILLIS = 1_000L
    const val DEFAULT_CONFIRM_QUEUE_MAX_AGE_MILLIS = 7 * 60 * 1000L
    const val DEFAULT_LOG_FILE_MAX_BYTES = 256 * 1024
    const val MIN_LOG_FILE_MAX_BYTES = 16 * 1024
    const val MAX_LOG_FILE_MAX_BYTES = 5 * 1024 * 1024

    val DEFAULT_FOREGROUND_SERVICE_RETRY_DELAYS_MILLIS =
        listOf(250L, 2_000L, 15_000L, 45_000L, 120_000L)

    const val LOG_FILE_NAME = "smart_geofence.log"

    // Location wake source carried by fused-location PendingIntent broadcasts.
    const val EXTRA_LOCATION_WAKE_SOURCE = "smart_geofence.location_wake_source"
    const val PENDING_INTENT_DATA_PROXIMITY = "smart-geofence://proximity"
    const val PENDING_INTENT_DATA_PASSIVE_LOCATION = "smart-geofence://passive-location"
    const val LOCATION_WAKE_SOURCE_PROXIMITY = "proximity"
    const val LOCATION_WAKE_SOURCE_PASSIVE = "passive"

    // Source labels forwarded to callback-worker diagnostics.
    const val EVENT_SOURCE_SMART_GEOFENCE_PROXIMITY = "smart_geofence_proximity"
    const val EVENT_SOURCE_SMART_GEOFENCE_PASSIVE = "smart_geofence_passive"
    const val EVENT_SOURCE_SMART_GEOFENCE_PASSIVE_CONFIRM = "smart_geofence_passive_confirm"
    const val EVENT_SOURCE_SMART_GEOFENCE_CONFIRM_PROXIMITY = "smart_geofence_confirm_proximity"
    const val EVENT_SOURCE_SMART_GEOFENCE_FENCE_PULSE_CONFIRM = "smart_geofence_fence_pulse_confirm"

    // Unique WorkManager name for the reconcile watchdog.
    const val RECONCILE_WORK_NAME = "smart_geofence.reconcile"

    // PendingIntent / request-code namespace base to avoid clashing with native_geofence.
    const val PENDING_INTENT_REQUEST_BASE = 9100
    const val PENDING_INTENT_REQUEST_PASSIVE_LOCATION = 9101
    const val PENDING_INTENT_REQUEST_FENCE_PULSE_LAUNCH = 9102
    const val PENDING_INTENT_REQUEST_RECONCILE_ALARM = 9103
    const val PENDING_INTENT_REQUEST_CONFIRM_LAUNCH = 9104
    const val PENDING_INTENT_REQUEST_CONFIRM_WATCHDOG = 9105
    const val PENDING_INTENT_REQUEST_FGS_BATCH_LAUNCH = 9106
}
