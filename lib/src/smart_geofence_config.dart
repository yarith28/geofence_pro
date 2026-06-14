/// Android Fused Location priority used by smart_geofence location requests.
///
/// Prefer [balancedPowerAccuracy] for low-power proximity checks,
/// [passive] only for opportunistic no-power listening, and [highAccuracy]
/// only for short foreground confirms or foreground-service FencePulse ticks.
enum SmartGeofenceLocationPriority {
  highAccuracy,
  balancedPowerAccuracy,
  lowPower,
  passive,
}

/// Scheduler used by the reconcile self-heal watchdog.
enum SmartGeofenceReconcileScheduler {
  /// Uses WorkManager periodic work. This is the safest default.
  workManager,

  /// Uses AlarmManager alarms. They are exact when permitted and inexact when
  /// exact alarms are unavailable.
  exactAlarm,
}

/// Configuration for the Android-only smart layers.
///
/// iOS and other platforms ignore these settings and continue through
/// native_geofence directly.
class SmartGeofenceConfig {
  /// Location-driven recovery and confirmation layers.
  final SmartGeofenceEscalationConfig escalation;

  /// Optional bounded foreground-service near-boundary watch.
  final SmartGeofenceFencePulseConfig fencePulse;

  /// Periodic self-heal/re-arm behavior.
  final SmartGeofenceReconcileConfig reconcile;

  /// App-private native file logging.
  final SmartGeofenceLogConfig logging;

  /// How long Android may take to promote a requested foreground service before
  /// smart_geofence treats the launch as stuck and schedules a retry.
  final Duration foregroundServiceLaunchTimeout;

  /// Small delay before draining foreground-service launch requests so
  /// near-simultaneous background wakes start in one controlled batch.
  final Duration foregroundServiceStartDelay;

  /// Retry delays for foreground-service launches.
  final List<Duration> foregroundServiceRetryDelays;

  /// Maximum age for queued foreground confirm requests.
  final Duration confirmQueueMaxAge;

  const SmartGeofenceConfig({
    this.escalation = const SmartGeofenceEscalationConfig(),
    this.fencePulse = const SmartGeofenceFencePulseConfig(),
    this.reconcile = const SmartGeofenceReconcileConfig(),
    this.logging = const SmartGeofenceLogConfig(),
    this.foregroundServiceLaunchTimeout = const Duration(seconds: 10),
    this.foregroundServiceStartDelay = const Duration(seconds: 1),
    this.foregroundServiceRetryDelays = const <Duration>[
      Duration(milliseconds: 250),
      Duration(seconds: 2),
      Duration(seconds: 15),
      Duration(seconds: 45),
      Duration(minutes: 2),
    ],
    this.confirmQueueMaxAge = const Duration(minutes: 7),
  });

  factory SmartGeofenceConfig.fromMap(Map<Object?, Object?> map) {
    const defaults = SmartGeofenceConfig();
    return SmartGeofenceConfig(
      escalation: SmartGeofenceEscalationConfig(
        enabled: _bool(map, 'escalationEnabled', defaults.escalation.enabled),
        proximity: SmartGeofenceProximityConfig(
          radiusMeters: _double(
            map,
            'proximityRadiusMeters',
            defaults.escalation.proximity.radiusMeters,
          ),
          priority: _priority(
            map,
            'proximityLocationPriority',
            defaults.escalation.proximity.priority,
          ),
          interval: Duration(
            milliseconds: _int(
              map,
              'proximityIntervalMillis',
              defaults.escalation.proximity.interval.inMilliseconds,
            ),
          ),
          fastestInterval: Duration(
            milliseconds: _int(
              map,
              'proximityFastestIntervalMillis',
              defaults.escalation.proximity.fastestInterval.inMilliseconds,
            ),
          ),
          maxWait: Duration(
            milliseconds: _int(
              map,
              'proximityMaxWaitMillis',
              defaults.escalation.proximity.maxWait.inMilliseconds,
            ),
          ),
          minDisplacementMeters: _double(
            map,
            'proximityMinDisplacementMeters',
            defaults.escalation.proximity.minDisplacementMeters,
          ),
        ),
        passive: SmartGeofencePassiveLocationConfig(
          enabled: _bool(
            map,
            'passiveLocationEnabled',
            defaults.escalation.passive.enabled,
          ),
          priority: _priority(
            map,
            'passiveLocationPriority',
            defaults.escalation.passive.priority,
          ),
          interval: Duration(
            milliseconds: _int(
              map,
              'passiveLocationIntervalMillis',
              defaults.escalation.passive.interval.inMilliseconds,
            ),
          ),
          fastestInterval: Duration(
            milliseconds: _int(
              map,
              'passiveLocationFastestIntervalMillis',
              defaults.escalation.passive.fastestInterval.inMilliseconds,
            ),
          ),
          maxWait: Duration(
            milliseconds: _int(
              map,
              'passiveLocationMaxWaitMillis',
              defaults.escalation.passive.maxWait.inMilliseconds,
            ),
          ),
          ambiguousConfirmEnabled: _bool(
            map,
            'passiveAmbiguousConfirmEnabled',
            defaults.escalation.passive.ambiguousConfirmEnabled,
          ),
        ),
        gpsConfirm: SmartGeofenceGpsConfirmConfig(
          priority: _priority(
            map,
            'gpsConfirmPriority',
            defaults.escalation.gpsConfirm.priority,
          ),
          timeout: Duration(
            milliseconds: _int(
              map,
              'gpsConfirmTimeoutMillis',
              defaults.escalation.gpsConfirm.timeout.inMilliseconds,
            ),
          ),
        ),
        boundary: SmartGeofenceBoundaryConfig(
          minMarginMeters: _double(
            map,
            'boundaryMinMarginMeters',
            defaults.escalation.boundary.minMarginMeters,
          ),
          marginRatio: _double(
            map,
            'boundaryMarginRatio',
            defaults.escalation.boundary.marginRatio,
          ),
          maxMarginRatio: _double(
            map,
            'boundaryMaxMarginRatio',
            defaults.escalation.boundary.maxMarginRatio,
          ),
        ),
        teleportGuard: SmartGeofenceTeleportGuardConfig(
          enabled: _bool(
            map,
            'teleportGuardEnabled',
            defaults.escalation.teleportGuard.enabled,
          ),
          maxSpeedMetersPerSecond: _double(
            map,
            'teleportMaxSpeedMetersPerSecond',
            defaults.escalation.teleportGuard.maxSpeedMetersPerSecond,
          ),
        ),
      ),
      fencePulse: SmartGeofenceFencePulseConfig(
        enabled: _bool(map, 'fencePulseEnabled', defaults.fencePulse.enabled),
        duration: Duration(
          minutes: _int(
            map,
            'fencePulseDurationMinutes',
            defaults.fencePulse.duration.inMinutes,
          ),
        ),
        interval: Duration(
          seconds: _int(
            map,
            'fencePulseIntervalSeconds',
            defaults.fencePulse.interval.inSeconds,
          ),
        ),
        minInterval: Duration(
          milliseconds: _int(
            map,
            'fencePulseMinIntervalMillis',
            defaults.fencePulse.minInterval.inMilliseconds,
          ),
        ),
        maxIdleTicks: _int(
          map,
          'fencePulseMaxIdleTicks',
          defaults.fencePulse.maxIdleTicks,
        ),
        exactAlarmStartDelay: Duration(
          milliseconds: _int(
            map,
            'fencePulseExactAlarmStartDelayMillis',
            defaults.fencePulse.exactAlarmStartDelay.inMilliseconds,
          ),
        ),
        notification: SmartGeofenceFencePulseNotificationConfig(
          title: _string(
            map,
            'fencePulseNotificationTitle',
            defaults.fencePulse.notification.title,
          ),
          channelId: _string(
            map,
            'fencePulseNotificationChannelId',
            defaults.fencePulse.notification.channelId,
          ),
          channelName: _string(
            map,
            'fencePulseNotificationChannelName',
            defaults.fencePulse.notification.channelName,
          ),
          notificationId: _int(
            map,
            'fencePulseNotificationId',
            defaults.fencePulse.notification.notificationId,
          ),
          smallIconResourceName: _stringOrNull(
            map,
            'fencePulseNotificationSmallIconResourceName',
          ),
        ),
        motionGate: SmartGeofenceMotionGateConfig(
          stationaryTtl: Duration(
            milliseconds: _int(
              map,
              'motionStationaryTtlMillis',
              defaults.fencePulse.motionGate.stationaryTtl.inMilliseconds,
            ),
          ),
        ),
      ),
      reconcile: SmartGeofenceReconcileConfig(
        scheduler: _reconcileScheduler(
          map,
          'reconcileScheduler',
          defaults.reconcile.scheduler,
        ),
        interval: Duration(
          minutes: _int(
            map,
            'reconcileIntervalMinutes',
            defaults.reconcile.interval.inMinutes,
          ),
        ),
        initialDelay: Duration(
          milliseconds: _int(
            map,
            'reconcileInitialDelayMillis',
            defaults.reconcile.initialDelay.inMilliseconds,
          ),
        ),
        flexInterval: Duration(
          milliseconds: _int(
            map,
            'reconcileFlexIntervalMillis',
            defaults.reconcile.flexInterval.inMilliseconds,
          ),
        ),
        requiresBatteryNotLow: _bool(
          map,
          'reconcileRequiresBatteryNotLow',
          defaults.reconcile.requiresBatteryNotLow,
        ),
      ),
      logging: SmartGeofenceLogConfig(
        fileEnabled: _bool(map, 'logFileEnabled', defaults.logging.fileEnabled),
        maxFileBytes: _int(
          map,
          'maxLogFileBytes',
          defaults.logging.maxFileBytes,
        ),
      ),
      foregroundServiceLaunchTimeout: Duration(
        milliseconds: _int(
          map,
          'foregroundServiceLaunchTimeoutMillis',
          defaults.foregroundServiceLaunchTimeout.inMilliseconds,
        ),
      ),
      foregroundServiceStartDelay: Duration(
        milliseconds: _int(
          map,
          'foregroundServiceStartDelayMillis',
          defaults.foregroundServiceStartDelay.inMilliseconds,
        ),
      ),
      foregroundServiceRetryDelays: _durationList(
        map,
        'foregroundServiceRetryDelaysMillis',
        defaults.foregroundServiceRetryDelays,
      ),
      confirmQueueMaxAge: Duration(
        milliseconds: _int(
          map,
          'confirmQueueMaxAgeMillis',
          defaults.confirmQueueMaxAge.inMilliseconds,
        ),
      ),
    );
  }

  Map<String, dynamic> toMap() => {
    'proximityRadiusMeters': escalation.proximity.radiusMeters,
    'escalationEnabled': escalation.enabled,
    'proximityLocationPriority': escalation.proximity.priority.name,
    'proximityIntervalMillis': escalation.proximity.interval.inMilliseconds,
    'proximityFastestIntervalMillis':
        escalation.proximity.fastestInterval.inMilliseconds,
    'proximityMaxWaitMillis': escalation.proximity.maxWait.inMilliseconds,
    'proximityMinDisplacementMeters':
        escalation.proximity.minDisplacementMeters,
    'passiveLocationPriority': escalation.passive.priority.name,
    'passiveLocationIntervalMillis': escalation.passive.interval.inMilliseconds,
    'passiveLocationFastestIntervalMillis':
        escalation.passive.fastestInterval.inMilliseconds,
    'passiveLocationMaxWaitMillis': escalation.passive.maxWait.inMilliseconds,
    'passiveAmbiguousConfirmEnabled':
        escalation.passive.ambiguousConfirmEnabled,
    'gpsConfirmPriority': escalation.gpsConfirm.priority.name,
    'gpsConfirmTimeoutMillis': escalation.gpsConfirm.timeout.inMilliseconds,
    'boundaryMinMarginMeters': escalation.boundary.minMarginMeters,
    'boundaryMarginRatio': escalation.boundary.marginRatio,
    'boundaryMaxMarginRatio': escalation.boundary.maxMarginRatio,
    'teleportGuardEnabled': escalation.teleportGuard.enabled,
    'teleportMaxSpeedMetersPerSecond':
        escalation.teleportGuard.maxSpeedMetersPerSecond,
    'fencePulseEnabled': fencePulse.enabled,
    'fencePulseDurationMinutes': fencePulse.duration.inMinutes,
    'fencePulseIntervalSeconds': fencePulse.interval.inSeconds,
    'fencePulseMinIntervalMillis': fencePulse.minInterval.inMilliseconds,
    'fencePulseMaxIdleTicks': fencePulse.maxIdleTicks,
    'fencePulseExactAlarmStartDelayMillis':
        fencePulse.exactAlarmStartDelay.inMilliseconds,
    'fencePulseNotificationTitle': fencePulse.notification.title,
    'fencePulseNotificationChannelId': fencePulse.notification.channelId,
    'fencePulseNotificationChannelName': fencePulse.notification.channelName,
    'fencePulseNotificationId': fencePulse.notification.notificationId,
    'fencePulseNotificationSmallIconResourceName':
        fencePulse.notification.smallIconResourceName,
    'motionStationaryTtlMillis':
        fencePulse.motionGate.stationaryTtl.inMilliseconds,
    'reconcileScheduler': reconcile.scheduler.name,
    'reconcileIntervalMinutes': reconcile.interval.inMinutes,
    'reconcileInitialDelayMillis': reconcile.initialDelay.inMilliseconds,
    'reconcileFlexIntervalMillis': reconcile.flexInterval.inMilliseconds,
    'reconcileRequiresBatteryNotLow': reconcile.requiresBatteryNotLow,
    'passiveLocationEnabled': escalation.passive.enabled,
    'foregroundServiceLaunchTimeoutMillis':
        foregroundServiceLaunchTimeout.inMilliseconds,
    'foregroundServiceStartDelayMillis':
        foregroundServiceStartDelay.inMilliseconds,
    'foregroundServiceRetryDelaysMillis': foregroundServiceRetryDelays
        .map((delay) => delay.inMilliseconds)
        .toList(growable: false),
    'confirmQueueMaxAgeMillis': confirmQueueMaxAge.inMilliseconds,
    'logFileEnabled': logging.fileEnabled,
    'maxLogFileBytes': logging.maxFileBytes,
  };

  @Deprecated('Use escalation.proximity.radiusMeters instead.')
  double get proximityRadiusMeters => escalation.proximity.radiusMeters;

  @Deprecated('Use escalation.enabled instead.')
  bool get escalationEnabled => escalation.enabled;

  @Deprecated('Use escalation.proximity.priority instead.')
  SmartGeofenceLocationPriority get proximityLocationPriority =>
      escalation.proximity.priority;

  @Deprecated('Use escalation.proximity.interval instead.')
  Duration get proximityInterval => escalation.proximity.interval;

  @Deprecated('Use escalation.proximity.fastestInterval instead.')
  Duration get proximityFastestInterval => escalation.proximity.fastestInterval;

  @Deprecated('Use escalation.proximity.maxWait instead.')
  Duration get proximityMaxWait => escalation.proximity.maxWait;

  @Deprecated('Use escalation.proximity.minDisplacementMeters instead.')
  double get proximityMinDisplacementMeters =>
      escalation.proximity.minDisplacementMeters;

  @Deprecated('Use escalation.passive.priority instead.')
  SmartGeofenceLocationPriority get passiveLocationPriority =>
      escalation.passive.priority;

  @Deprecated('Use escalation.passive.interval instead.')
  Duration get passiveLocationInterval => escalation.passive.interval;

  @Deprecated('Use escalation.passive.fastestInterval instead.')
  Duration get passiveLocationFastestInterval =>
      escalation.passive.fastestInterval;

  @Deprecated('Use escalation.passive.maxWait instead.')
  Duration get passiveLocationMaxWait => escalation.passive.maxWait;

  @Deprecated('Use escalation.passive.ambiguousConfirmEnabled instead.')
  bool get passiveAmbiguousConfirmEnabled =>
      escalation.passive.ambiguousConfirmEnabled;

  @Deprecated('Use escalation.gpsConfirm.priority instead.')
  SmartGeofenceLocationPriority get gpsConfirmPriority =>
      escalation.gpsConfirm.priority;

  @Deprecated('Use escalation.gpsConfirm.timeout instead.')
  Duration get gpsConfirmTimeout => escalation.gpsConfirm.timeout;

  @Deprecated('Use escalation.boundary.minMarginMeters instead.')
  double get boundaryMinMarginMeters => escalation.boundary.minMarginMeters;

  @Deprecated('Use escalation.boundary.marginRatio instead.')
  double get boundaryMarginRatio => escalation.boundary.marginRatio;

  @Deprecated('Use escalation.boundary.maxMarginRatio instead.')
  double get boundaryMaxMarginRatio => escalation.boundary.maxMarginRatio;

  @Deprecated('Use escalation.teleportGuard.enabled instead.')
  bool get teleportGuardEnabled => escalation.teleportGuard.enabled;

  @Deprecated('Use escalation.teleportGuard.maxSpeedMetersPerSecond instead.')
  double get teleportMaxSpeedMetersPerSecond =>
      escalation.teleportGuard.maxSpeedMetersPerSecond;

  @Deprecated('Use fencePulse.enabled instead.')
  bool get fencePulseEnabled => fencePulse.enabled;

  @Deprecated('Use fencePulse.duration instead.')
  Duration get fencePulseDuration => fencePulse.duration;

  @Deprecated('Use fencePulse.interval instead.')
  Duration get fencePulseInterval => fencePulse.interval;

  @Deprecated('Use fencePulse.minInterval instead.')
  Duration get fencePulseMinInterval => fencePulse.minInterval;

  @Deprecated('Use fencePulse.maxIdleTicks instead.')
  int get fencePulseMaxIdleTicks => fencePulse.maxIdleTicks;

  @Deprecated('Use fencePulse.exactAlarmStartDelay instead.')
  Duration get fencePulseExactAlarmStartDelay =>
      fencePulse.exactAlarmStartDelay;

  @Deprecated('Use fencePulse.notification.title instead.')
  String get fencePulseNotificationTitle => fencePulse.notification.title;

  @Deprecated('Use fencePulse.notification.channelId instead.')
  String get fencePulseNotificationChannelId =>
      fencePulse.notification.channelId;

  @Deprecated('Use fencePulse.notification.channelName instead.')
  String get fencePulseNotificationChannelName =>
      fencePulse.notification.channelName;

  @Deprecated('Use fencePulse.notification.notificationId instead.')
  int get fencePulseNotificationId => fencePulse.notification.notificationId;

  @Deprecated('Use fencePulse.notification.smallIconResourceName instead.')
  String? get fencePulseNotificationSmallIconResourceName =>
      fencePulse.notification.smallIconResourceName;

  @Deprecated('Use fencePulse.motionGate.stationaryTtl instead.')
  Duration get motionStationaryTtl => fencePulse.motionGate.stationaryTtl;

  @Deprecated('Use reconcile.interval instead.')
  Duration get reconcileInterval => reconcile.interval;

  @Deprecated('Use reconcile.scheduler instead.')
  SmartGeofenceReconcileScheduler get reconcileScheduler => reconcile.scheduler;

  @Deprecated('Use reconcile.initialDelay instead.')
  Duration get reconcileInitialDelay => reconcile.initialDelay;

  @Deprecated('Use reconcile.flexInterval instead.')
  Duration get reconcileFlexInterval => reconcile.flexInterval;

  @Deprecated('Use reconcile.requiresBatteryNotLow instead.')
  bool get reconcileRequiresBatteryNotLow => reconcile.requiresBatteryNotLow;

  @Deprecated('Use escalation.passive.enabled instead.')
  bool get passiveLocationEnabled => escalation.passive.enabled;

  @Deprecated('Use logging.fileEnabled instead.')
  bool get logFileEnabled => logging.fileEnabled;

  @Deprecated('Use logging.maxFileBytes instead.')
  int get maxLogFileBytes => logging.maxFileBytes;

  @override
  String toString() =>
      'SmartGeofenceConfig(escalation: $escalation, fencePulse: $fencePulse, '
      'reconcile: $reconcile, logging: $logging, '
      'foregroundServiceLaunchTimeout: $foregroundServiceLaunchTimeout, '
      'foregroundServiceStartDelay: $foregroundServiceStartDelay, '
      'foregroundServiceRetryDelays: $foregroundServiceRetryDelays, '
      'confirmQueueMaxAge: $confirmQueueMaxAge)';
}

class SmartGeofenceEscalationConfig {
  /// Whether to run the proximity -> foreground GPS confirm layer.
  ///
  /// Default: `true`. Disable only when you want native_geofence behavior with
  /// passive/reconcile layers off the active confirm path.
  final bool enabled;

  /// Low-power active stream used to decide when GPS is worth spending.
  final SmartGeofenceProximityConfig proximity;

  /// Opportunistic no-power fixes produced by other location clients.
  final SmartGeofencePassiveLocationConfig passive;

  /// One-shot precise confirmation near a boundary.
  final SmartGeofenceGpsConfirmConfig gpsConfirm;

  /// Accuracy-aware inside/outside classification.
  final SmartGeofenceBoundaryConfig boundary;

  /// Rejects physically implausible non-mock fixes before callback queueing.
  final SmartGeofenceTeleportGuardConfig teleportGuard;

  const SmartGeofenceEscalationConfig({
    this.enabled = true,
    this.proximity = const SmartGeofenceProximityConfig(),
    this.passive = const SmartGeofencePassiveLocationConfig(),
    this.gpsConfirm = const SmartGeofenceGpsConfirmConfig(),
    this.boundary = const SmartGeofenceBoundaryConfig(),
    this.teleportGuard = const SmartGeofenceTeleportGuardConfig(),
  });

  @override
  String toString() =>
      'SmartGeofenceEscalationConfig(enabled: $enabled, '
      'proximity: $proximity, passive: $passive, '
      'gpsConfirm: $gpsConfirm, boundary: $boundary, '
      'teleportGuard: $teleportGuard)';
}

class SmartGeofenceProximityConfig {
  /// How close (meters, measured to the fence edge) the device must be before
  /// smart_geofence spends a precise GPS confirm.
  ///
  /// Default: `1000`. Typical range: `500..2000`.
  final double radiusMeters;

  /// Fused Location priority for the active proximity monitor.
  ///
  /// Default/recommended: [SmartGeofenceLocationPriority.balancedPowerAccuracy].
  final SmartGeofenceLocationPriority priority;

  /// Desired cadence for the active low-power proximity monitor.
  ///
  /// Default: `2 minutes`. Typical range: `1..5 minutes`.
  final Duration interval;

  /// Fastest accepted cadence for proximity monitor updates.
  ///
  /// Default: `1 minute`. Keep this at or below [interval].
  final Duration fastestInterval;

  /// Maximum batching delay for proximity monitor updates.
  ///
  /// Default: `5 minutes`.
  final Duration maxWait;

  /// Minimum movement before another proximity fix is requested.
  ///
  /// Default: `200`. Typical range: `100..300`.
  final double minDisplacementMeters;

  const SmartGeofenceProximityConfig({
    this.radiusMeters = 1000.0,
    this.priority = SmartGeofenceLocationPriority.balancedPowerAccuracy,
    this.interval = const Duration(minutes: 2),
    this.fastestInterval = const Duration(minutes: 1),
    this.maxWait = const Duration(minutes: 5),
    this.minDisplacementMeters = 200.0,
  });

  @override
  String toString() =>
      'SmartGeofenceProximityConfig(radiusMeters: $radiusMeters, '
      'priority: $priority, interval: $interval, '
      'fastestInterval: $fastestInterval, maxWait: $maxWait, '
      'minDisplacementMeters: $minDisplacementMeters)';
}

class SmartGeofencePassiveLocationConfig {
  /// Whether Android should listen for opportunistic no-power location fixes.
  ///
  /// Default/recommended: `true`.
  final bool enabled;

  /// Fused Location priority for the opportunistic passive monitor.
  ///
  /// Default/recommended: [SmartGeofenceLocationPriority.passive].
  final SmartGeofenceLocationPriority priority;

  /// Desired cadence for opportunistic location updates.
  ///
  /// Default: `20 minutes`. Passive fixes are opportunistic.
  final Duration interval;

  /// Fastest accepted cadence for opportunistic location updates.
  ///
  /// Default: `10 minutes`.
  final Duration fastestInterval;

  /// Maximum batching delay for opportunistic location updates.
  ///
  /// Default: `40 minutes`.
  final Duration maxWait;

  /// Whether an ambiguous passive near-fence fix may spend one active GPS
  /// confirm.
  ///
  /// Default: `true`.
  final bool ambiguousConfirmEnabled;

  const SmartGeofencePassiveLocationConfig({
    this.enabled = true,
    this.priority = SmartGeofenceLocationPriority.passive,
    this.interval = const Duration(minutes: 20),
    this.fastestInterval = const Duration(minutes: 10),
    this.maxWait = const Duration(minutes: 40),
    this.ambiguousConfirmEnabled = true,
  });

  @override
  String toString() =>
      'SmartGeofencePassiveLocationConfig(enabled: $enabled, '
      'priority: $priority, interval: $interval, '
      'fastestInterval: $fastestInterval, maxWait: $maxWait, '
      'ambiguousConfirmEnabled: $ambiguousConfirmEnabled)';
}

class SmartGeofenceGpsConfirmConfig {
  /// Fused Location priority for foreground precise confirm requests.
  ///
  /// Default/recommended: [SmartGeofenceLocationPriority.highAccuracy].
  final SmartGeofenceLocationPriority priority;

  /// Timeout for a single foreground confirm.
  ///
  /// Default: `8 seconds`. Recommended range: `5..9 seconds`.
  final Duration timeout;

  const SmartGeofenceGpsConfirmConfig({
    this.priority = SmartGeofenceLocationPriority.highAccuracy,
    this.timeout = const Duration(seconds: 8),
  });

  @override
  String toString() =>
      'SmartGeofenceGpsConfirmConfig(priority: $priority, timeout: $timeout)';
}

class SmartGeofenceBoundaryConfig {
  /// Minimum dead-band around a fence edge before a fix is classified.
  ///
  /// Default: `20`. Typical range: `10..50`.
  final double minMarginMeters;

  /// Radius-relative dead-band around a fence edge.
  ///
  /// Default: `0.10` (10% of radius).
  final double marginRatio;

  /// Maximum radius-relative dead-band clamp.
  ///
  /// Default: `0.50`.
  final double maxMarginRatio;

  const SmartGeofenceBoundaryConfig({
    this.minMarginMeters = 20.0,
    this.marginRatio = 0.10,
    this.maxMarginRatio = 0.50,
  });

  @override
  String toString() =>
      'SmartGeofenceBoundaryConfig(minMarginMeters: $minMarginMeters, '
      'marginRatio: $marginRatio, maxMarginRatio: $maxMarginRatio)';
}

class SmartGeofenceTeleportGuardConfig {
  /// Whether to reject physically implausible non-mock fixes before callback queueing.
  ///
  /// Default/recommended: `true`.
  final bool enabled;

  /// Maximum plausible speed between confirmed fixes before rejection.
  ///
  /// Default: `70` m/s (about 250 km/h).
  final double maxSpeedMetersPerSecond;

  const SmartGeofenceTeleportGuardConfig({
    this.enabled = true,
    this.maxSpeedMetersPerSecond = 70.0,
  });

  @override
  String toString() =>
      'SmartGeofenceTeleportGuardConfig(enabled: $enabled, '
      'maxSpeedMetersPerSecond: $maxSpeedMetersPerSecond)';
}

class SmartGeofenceFencePulseConfig {
  /// Opt-in bounded alarm pulse while near a fence.
  ///
  /// FencePulse itself does not start a foreground service or fetch GPS. When a
  /// tick decides a fresh fix is useful, it queues the regular foreground GPS
  /// confirm service.
  ///
  /// Default: `false`.
  final bool enabled;

  /// How long a FencePulse window may remain active per activation.
  ///
  /// Default: `2 hours`.
  final Duration duration;

  /// Cadence for the opt-in FencePulse alarm pulse.
  ///
  /// Default: `75 seconds`. Typical range: `60..180 seconds`.
  final Duration interval;

  /// Lower bound applied to [interval].
  ///
  /// Default: `30 seconds`.
  final Duration minInterval;

  /// Stop FencePulse after this many consecutive stationary ticks.
  ///
  /// Default: `5`.
  final int maxIdleTicks;

  /// Delay before the first FencePulse tick after a near-boundary wake.
  ///
  /// Default: `250 milliseconds`.
  final Duration exactAlarmStartDelay;

  /// Compatibility-backed notification configuration used by foreground confirm.
  final SmartGeofenceFencePulseNotificationConfig notification;

  /// Motion-gate configuration used to skip FencePulse GPS ticks while still.
  final SmartGeofenceMotionGateConfig motionGate;

  const SmartGeofenceFencePulseConfig({
    this.enabled = false,
    this.duration = const Duration(hours: 2),
    this.interval = const Duration(seconds: 75),
    this.minInterval = const Duration(seconds: 30),
    this.maxIdleTicks = 5,
    this.exactAlarmStartDelay = const Duration(milliseconds: 250),
    this.notification = const SmartGeofenceFencePulseNotificationConfig(),
    this.motionGate = const SmartGeofenceMotionGateConfig(),
  });

  const SmartGeofenceFencePulseConfig.disabled() : this(enabled: false);

  @override
  String toString() =>
      'SmartGeofenceFencePulseConfig(enabled: $enabled, duration: $duration, '
      'interval: $interval, minInterval: $minInterval, '
      'maxIdleTicks: $maxIdleTicks, '
      'exactAlarmStartDelay: $exactAlarmStartDelay, '
      'notification: $notification, motionGate: $motionGate)';
}

class SmartGeofenceFencePulseNotificationConfig {
  /// Foreground confirm notification title.
  ///
  /// Default: `"Checking nearby geofence"`.
  final String title;

  /// Foreground confirm notification channel id.
  ///
  /// Default: `"smart_geofence_fence_pulse"`.
  final String channelId;

  /// Foreground confirm notification channel name.
  ///
  /// Default: `"Geofence monitoring"`.
  final String channelName;

  /// Foreground confirm notification id.
  ///
  /// Default: `9102`.
  final int notificationId;

  /// Optional drawable or mipmap resource name for the notification small icon.
  final String? smallIconResourceName;

  const SmartGeofenceFencePulseNotificationConfig({
    this.title = 'Checking nearby geofence',
    this.channelId = 'smart_geofence_fence_pulse',
    this.channelName = 'Geofence monitoring',
    this.notificationId = 9102,
    this.smallIconResourceName,
  });

  @override
  String toString() =>
      'SmartGeofenceFencePulseNotificationConfig(title: $title, '
      'channelId: $channelId, channelName: $channelName, '
      'notificationId: $notificationId, '
      'smallIconResourceName: $smallIconResourceName)';
}

class SmartGeofenceMotionGateConfig {
  /// How long a stationary Activity Recognition reading remains trusted.
  ///
  /// Default: `15 minutes`.
  final Duration stationaryTtl;

  const SmartGeofenceMotionGateConfig({
    this.stationaryTtl = const Duration(minutes: 15),
  });

  @override
  String toString() =>
      'SmartGeofenceMotionGateConfig(stationaryTtl: $stationaryTtl)';
}

class SmartGeofenceReconcileConfig {
  /// Scheduler used for the reconcile watchdog.
  ///
  /// Default: [SmartGeofenceReconcileScheduler.workManager].
  /// [SmartGeofenceReconcileScheduler.exactAlarm] uses AlarmManager: exact when
  /// permitted and inexact when exact alarms are unavailable.
  final SmartGeofenceReconcileScheduler scheduler;

  /// Period of the reconcile watchdog. WorkManager's floor is 15 minutes.
  ///
  /// Default: `30 minutes`. For AlarmManager reconcile this is the delay used
  /// when scheduling the next alarm after each run.
  final Duration interval;

  /// Initial delay before the first reconcile watchdog run.
  ///
  /// Default: [Duration.zero].
  final Duration initialDelay;

  /// Optional flex interval for the periodic reconcile watchdog.
  ///
  /// Default: [Duration.zero]. Applies only to WorkManager.
  final Duration flexInterval;

  /// Whether reconcile work should wait until the battery is not low.
  ///
  /// Default: `false`. WorkManager enforces this as a scheduler constraint;
  /// AlarmManager reconcile checks it manually when the alarm fires.
  final bool requiresBatteryNotLow;

  const SmartGeofenceReconcileConfig({
    this.scheduler = SmartGeofenceReconcileScheduler.workManager,
    this.interval = const Duration(minutes: 30),
    this.initialDelay = Duration.zero,
    this.flexInterval = Duration.zero,
    this.requiresBatteryNotLow = false,
  });

  @override
  String toString() =>
      'SmartGeofenceReconcileConfig(scheduler: $scheduler, '
      'interval: $interval, '
      'initialDelay: $initialDelay, flexInterval: $flexInterval, '
      'requiresBatteryNotLow: $requiresBatteryNotLow)';
}

class SmartGeofenceLogConfig {
  /// Whether smart_geofence and native_geofence write app-private log files.
  ///
  /// Default: `false`.
  final bool fileEnabled;

  /// Maximum size of each native log file in bytes.
  ///
  /// Default: `256 KiB`.
  final int maxFileBytes;

  const SmartGeofenceLogConfig({
    this.fileEnabled = false,
    this.maxFileBytes = 256 * 1024,
  });

  @override
  String toString() =>
      'SmartGeofenceLogConfig(fileEnabled: $fileEnabled, '
      'maxFileBytes: $maxFileBytes)';
}

bool _bool(Map<Object?, Object?> map, String key, bool fallback) {
  final value = map[key];
  return value is bool ? value : fallback;
}

int _int(Map<Object?, Object?> map, String key, int fallback) {
  final value = map[key];
  return value is num ? value.toInt() : fallback;
}

double _double(Map<Object?, Object?> map, String key, double fallback) {
  final value = map[key];
  return value is num ? value.toDouble() : fallback;
}

List<Duration> _durationList(
  Map<Object?, Object?> map,
  String key,
  List<Duration> fallback,
) {
  final value = map[key];
  if (value is! List) return fallback;
  final parsed = <Duration>[];
  for (final item in value) {
    if (item is num && item >= 0) {
      parsed.add(Duration(milliseconds: item.toInt()));
    }
  }
  return parsed.isEmpty ? fallback : List<Duration>.unmodifiable(parsed);
}

String? _stringOrNull(Map<Object?, Object?> map, String key) {
  final value = map[key];
  return value is String ? value : null;
}

String _string(Map<Object?, Object?> map, String key, String fallback) =>
    _stringOrNull(map, key) ?? fallback;

SmartGeofenceLocationPriority _priority(
  Map<Object?, Object?> map,
  String key,
  SmartGeofenceLocationPriority fallback,
) {
  final name = _stringOrNull(map, key);
  if (name == null) return fallback;
  for (final value in SmartGeofenceLocationPriority.values) {
    if (value.name == name) return value;
  }
  return fallback;
}

SmartGeofenceReconcileScheduler _reconcileScheduler(
  Map<Object?, Object?> map,
  String key,
  SmartGeofenceReconcileScheduler fallback,
) {
  final name = _stringOrNull(map, key);
  if (name == null) return fallback;
  for (final value in SmartGeofenceReconcileScheduler.values) {
    if (value.name == name) return value;
  }
  return fallback;
}
