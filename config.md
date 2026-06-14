# smart_geofence Configuration

`SmartGeofenceConfig` controls the Android-only smart layers that sit on top of
`native_geofence`. On iOS and other platforms, these settings are ignored and
`native_geofence` handles geofencing directly.

## Quick Start

The defaults are intentionally conservative: opportunistic no-power location listening is enabled,
proximity escalation is enabled, FencePulse is disabled, and reconcile runs every
30 minutes.

```dart
await SmartGeofenceManager.instance.initialize(
  config: const SmartGeofenceConfig(
    escalation: SmartGeofenceEscalationConfig(
      proximity: SmartGeofenceProximityConfig(radiusMeters: 1000),
      passive: SmartGeofencePassiveLocationConfig(enabled: true),
    ),
  ),
);
```

## Location Priorities

`SmartGeofenceLocationPriority` maps to Android Fused Location priorities:

| Value | Meaning |
| --- | --- |
| `highAccuracy` | Uses the most accurate available provider. Highest battery cost. |
| `balancedPowerAccuracy` | Uses lower-power network/cell/wifi style fixes when possible. |
| `lowPower` | Lower-power location request. |
| `passive` | No-power listener for fixes produced by other apps or services. |

## Proximity Monitor

The proximity monitor is the active low-power stream that decides when a precise
GPS confirm is worth spending.

| Field | Default | Effect |
| --- | --- | --- |
| `escalation.proximity.radiusMeters` | `1000.0` | Distance from the fence edge where smart_geofence may escalate. Larger catches more missed OS events but spends more confirms. |
| `escalation.enabled` | `true` | Enables proximity and one-shot GPS confirmation. |
| `escalation.proximity.priority` | `balancedPowerAccuracy` | Fused Location priority for the active proximity stream. |
| `escalation.proximity.interval` | `2 minutes` | Desired proximity update cadence. |
| `escalation.proximity.fastestInterval` | `1 minute` | Fastest accepted proximity update cadence. |
| `escalation.proximity.maxWait` | `5 minutes` | Maximum batching delay for proximity fixes. |
| `escalation.proximity.minDisplacementMeters` | `200.0` | Minimum movement before another proximity fix is requested. |

## Passive Monitor

The passive monitor listens for no-power fixes produced by other location
clients. A confident passive fix can queue an event immediately; an ambiguous
near-boundary passive fix can optionally trigger one active GPS confirm.

| Field | Default | Effect |
| --- | --- | --- |
| `escalation.passive.enabled` | `true` | Enables passive no-power location checks. |
| `escalation.passive.priority` | `passive` | Fused Location priority for passive updates. Keep this `passive` for strict no-power behavior. |
| `escalation.passive.interval` | `20 minutes` | Desired passive update cadence. |
| `escalation.passive.fastestInterval` | `10 minutes` | Fastest accepted passive update cadence. |
| `escalation.passive.maxWait` | `40 minutes` | Maximum batching delay for passive fixes. |
| `escalation.passive.ambiguousConfirmEnabled` | `true` | Allows an ambiguous passive near-fence fix to spend one active GPS confirm. |

## GPS Confirm

GPS confirm spends a single current-location request when a coarse/passive fix
shows the device near a fence boundary.

| Field | Default | Effect |
| --- | --- | --- |
| `escalation.gpsConfirm.priority` | `highAccuracy` | Fused Location priority for one-shot confirmation. |
| `escalation.gpsConfirm.timeout` | `8 seconds` | Maximum time to wait for a confirm fix. Keep this short enough for background broadcast work. |

## Boundary Classification

Boundary classification uses the reported fix accuracy plus a configurable
dead-band around the fence edge. If a fix cannot confidently classify as inside
or outside, no event is queued.

| Field | Default | Effect |
| --- | --- | --- |
| `escalation.boundary.minMarginMeters` | `20.0` | Minimum dead-band around the fence edge. |
| `escalation.boundary.marginRatio` | `0.10` | Radius-relative dead-band. For a 500 m fence, this contributes 50 m. |
| `escalation.boundary.maxMarginRatio` | `0.50` | Maximum radius-relative clamp for the dead-band. |

## Teleport Guard

The teleport guard rejects physically implausible non-mock fixes before they can
queue an event. Mock fixes are allowed so emulator and GPX testing still work.

| Field | Default | Effect |
| --- | --- | --- |
| `escalation.teleportGuard.enabled` | `true` | Enables rejection of implausible jumps between confirmed fixes. |
| `escalation.teleportGuard.maxSpeedMetersPerSecond` | `70.0` | Maximum plausible speed between fixes before a fix is rejected. About 252 km/h. |

## FencePulse

FencePulse is off by default. It is a bounded alarm-only near-boundary pulse.
It does not require foreground-service or exact-alarm manifest setup. Most apps
should leave it disabled unless they need repeated follow-up after an uncertain
near-boundary wake.

| Field | Default | Effect |
| --- | --- | --- |
| `fencePulse.enabled` | `false` | Enables the opt-in alarm-only FencePulse follow-up. |
| `fencePulse.duration` | `2 hours` | Maximum runtime per FencePulse activation. |
| `fencePulse.interval` | `75 seconds` | Desired FencePulse tick cadence. |
| `fencePulse.minInterval` | `30 seconds` | Lower bound applied to `fencePulse.interval`. |
| `fencePulse.maxIdleTicks` | `5` | Stops FencePulse after this many consecutive stationary ticks. |
| `fencePulse.exactAlarmStartDelay` | `250 milliseconds` | Delay before the first FencePulse alarm tick after a near-boundary wake. |
| `fencePulse.notification.title` | `Checking nearby geofence` | Shared foreground-service notification title for GPS confirms and callback promotion. |
| `fencePulse.notification.channelId` | `smart_geofence_fence_pulse` | Notification channel id. Changing it creates/uses a different Android channel. |
| `fencePulse.notification.channelName` | `Geofence monitoring` | Notification channel name shown in Android settings. |
| `fencePulse.notification.notificationId` | `9102` | Shared foreground-service notification id. Values less than 1 fall back to the default. |
| `fencePulse.notification.smallIconResourceName` | `null` | Optional drawable or mipmap resource name. Falls back to Android's built-in location icon when null or not found. |

Example:

```dart
const SmartGeofenceConfig(
  fencePulse: SmartGeofenceFencePulseConfig(
    enabled: true,
    interval: Duration(seconds: 90),
    maxIdleTicks: 4,
    notification: SmartGeofenceFencePulseNotificationConfig(
      title: 'Checking nearby attendance zones',
      smallIconResourceName: 'ic_stat_location',
    ),
  ),
);
```

## Motion Gate

The motion gate uses Activity Recognition only to avoid FencePulse GPS ticks when
the device is confidently still. If permission is missing, it safely no-ops.

| Field | Default | Effect |
| --- | --- | --- |
| `fencePulse.motionGate.stationaryTtl` | `15 minutes` | How long a stationary Activity Recognition reading remains trusted. A stale reading is treated as unknown/moving. |

## Reconcile Watchdog

Reconcile is a periodic self-heal. It re-arms the Android smart layers after
process death or scheduler drops. It is not a detection path, so Doze deferral is
acceptable for the default WorkManager scheduler. Apps that already accept the
exact-alarm permission/policy surface can opt into exact-alarm reconcile.

| Field | Default | Effect |
| --- | --- | --- |
| `reconcile.scheduler` | `workManager` | Scheduler for the watchdog. `exactAlarm` uses AlarmManager; exact when permitted and inexact otherwise. |
| `reconcile.interval` | `30 minutes` | WorkManager repeat interval, or next AlarmManager delay after each run. |
| `reconcile.initialDelay` | `Duration.zero` | Delay before the first WorkManager run or first AlarmManager alarm. |
| `reconcile.flexInterval` | `Duration.zero` | WorkManager-only flex interval. Zero leaves default flex behavior untouched. |
| `reconcile.requiresBatteryNotLow` | `false` | WorkManager constraint. In exact-alarm mode, smart_geofence checks battery manually when the alarm fires and skips that run if low. |

Example exact-alarm reconcile:

```dart
const SmartGeofenceConfig(
  reconcile: SmartGeofenceReconcileConfig(
    scheduler: SmartGeofenceReconcileScheduler.exactAlarm,
    interval: Duration(minutes: 30),
  ),
);
```

Exact-alarm reconcile does not force exact-alarm permission onto every app. If
exact alarms are unavailable, smart_geofence logs the degraded mode and uses
inexact AlarmManager timing instead. WorkManager is only used when AlarmManager
scheduling itself is unavailable.

## Logging

File logging is app-private and disabled by default. When enabled,
`SmartGeofenceManager.readLogFile()` returns both `smart_geofence` and
`native_geofence` logs.

| Field | Default | Effect |
| --- | --- | --- |
| `logging.fileEnabled` | `false` | Enables bounded native file logging. |
| `logging.maxFileBytes` | `256 * 1024` | Maximum size of each native log file. Android clamps this internally between 16 KiB and 5 MiB. |

## Suggested Profiles

### Conservative

For maximum battery caution:

```dart
const SmartGeofenceConfig(
  escalation: SmartGeofenceEscalationConfig(
    proximity: SmartGeofenceProximityConfig(
      radiusMeters: 750,
      interval: Duration(minutes: 4),
      fastestInterval: Duration(minutes: 2),
      maxWait: Duration(minutes: 8),
      minDisplacementMeters: 300,
    ),
    passive: SmartGeofencePassiveLocationConfig(
      ambiguousConfirmEnabled: false,
    ),
  ),
  fencePulse: SmartGeofenceFencePulseConfig.disabled(),
);
```

### Balanced

Close to defaults, suitable for most apps:

```dart
const SmartGeofenceConfig(
  escalation: SmartGeofenceEscalationConfig(
    proximity: SmartGeofenceProximityConfig(radiusMeters: 1000),
    passive: SmartGeofencePassiveLocationConfig(enabled: true),
  ),
  fencePulse: SmartGeofenceFencePulseConfig.disabled(),
);
```

### More Responsive

For tighter Android recovery when battery tradeoffs are acceptable:

```dart
const SmartGeofenceConfig(
  escalation: SmartGeofenceEscalationConfig(
    proximity: SmartGeofenceProximityConfig(
      radiusMeters: 1500,
      interval: Duration(minutes: 1),
      fastestInterval: Duration(seconds: 30),
      maxWait: Duration(minutes: 2),
      minDisplacementMeters: 100,
    ),
    gpsConfirm: SmartGeofenceGpsConfirmConfig(
      timeout: Duration(seconds: 10),
    ),
  ),
);
```
