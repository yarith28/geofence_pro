import 'package:native_geofence/native_geofence.dart' as ng;

import 'smart_geofence_config.dart';

/// Combined diagnostic status for native_geofence plus the Android-only
/// smart_geofence recovery/battery layers.
class SmartGeofenceStatus {
  final ng.NativeGeofenceStatus nativeStatus;
  final bool smartLayerSupported;
  final SmartGeofenceConfig config;
  final List<String> mirroredFenceIds;
  final int? androidSdkInt;
  final String? deviceManufacturer;
  final String? deviceModel;
  final bool? locationPermissionGranted;
  final bool? backgroundLocationPermissionGranted;
  final bool? activityRecognitionPermissionGranted;
  final bool? foregroundServicePermissionGranted;
  final bool? foregroundServiceLocationPermissionGranted;
  final bool? exactAlarmPermissionGranted;
  final bool? locationServicesEnabled;
  final bool? proximityReceiverDeclared;
  final bool? motionReceiverDeclared;
  final bool? bootReceiverDeclared;
  final bool? reconcileAlarmReceiverDeclared;
  final bool? locationConfirmReceiverDeclared;
  final bool? locationConfirmServiceDeclared;
  final bool? locationConfirmServiceHasLocationType;
  final bool? locationConfirmServiceRunning;
  final bool? locationConfirmServiceForegroundReady;
  final int? locationConfirmLaunchToken;
  final int? locationConfirmLaunchRequestedAtMillis;
  final int? locationConfirmForegroundReadyAtMillis;
  final int? locationConfirmLastLaunchFailureAtMillis;
  final String? locationConfirmLastLaunchFailureReason;
  final bool? locationConfirmCanRun;
  final int? locationConfirmQueueSize;
  final bool? fencePulseReceiverDeclared;
  final int? fencePulseStartedAtMillis;
  final int? fencePulseIdleTicks;
  final bool? fencePulseCanRun;
  final bool? proximityEligible;
  final bool? passiveLocationEligible;
  final bool? motionGateEligible;
  final bool? reconcileEligible;
  final bool? proximityPendingIntentExists;
  final bool? passiveLocationPendingIntentExists;
  final bool? motionPendingIntentExists;
  final bool? reconcileAlarmPendingIntentExists;
  final bool? locationConfirmStartPendingIntentExists;
  final bool? locationConfirmWatchdogPendingIntentExists;
  final bool? fencePulseAlarmPendingIntentExists;

  const SmartGeofenceStatus({
    required this.nativeStatus,
    required this.smartLayerSupported,
    this.config = const SmartGeofenceConfig(),
    this.mirroredFenceIds = const <String>[],
    this.androidSdkInt,
    this.deviceManufacturer,
    this.deviceModel,
    this.locationPermissionGranted,
    this.backgroundLocationPermissionGranted,
    this.activityRecognitionPermissionGranted,
    this.foregroundServicePermissionGranted,
    this.foregroundServiceLocationPermissionGranted,
    this.exactAlarmPermissionGranted,
    this.locationServicesEnabled,
    this.proximityReceiverDeclared,
    this.motionReceiverDeclared,
    this.bootReceiverDeclared,
    this.reconcileAlarmReceiverDeclared,
    this.locationConfirmReceiverDeclared,
    this.locationConfirmServiceDeclared,
    this.locationConfirmServiceHasLocationType,
    this.locationConfirmServiceRunning,
    this.locationConfirmServiceForegroundReady,
    this.locationConfirmLaunchToken,
    this.locationConfirmLaunchRequestedAtMillis,
    this.locationConfirmForegroundReadyAtMillis,
    this.locationConfirmLastLaunchFailureAtMillis,
    this.locationConfirmLastLaunchFailureReason,
    this.locationConfirmCanRun,
    this.locationConfirmQueueSize,
    this.fencePulseReceiverDeclared,
    this.fencePulseStartedAtMillis,
    this.fencePulseIdleTicks,
    this.fencePulseCanRun,
    this.proximityEligible,
    this.passiveLocationEligible,
    this.motionGateEligible,
    this.reconcileEligible,
    this.proximityPendingIntentExists,
    this.passiveLocationPendingIntentExists,
    this.motionPendingIntentExists,
    this.reconcileAlarmPendingIntentExists,
    this.locationConfirmStartPendingIntentExists,
    this.locationConfirmWatchdogPendingIntentExists,
    this.fencePulseAlarmPendingIntentExists,
  });

  factory SmartGeofenceStatus.fromMap({
    required ng.NativeGeofenceStatus nativeStatus,
    required bool smartLayerSupported,
    required Map<Object?, Object?> map,
  }) {
    final configMap = _map(map, 'config');
    return SmartGeofenceStatus(
      nativeStatus: nativeStatus,
      smartLayerSupported: smartLayerSupported,
      config: _configFromMap(configMap),
      mirroredFenceIds: _stringList(map, 'mirroredFenceIds'),
      androidSdkInt: _intOrNull(map, 'androidSdkInt'),
      deviceManufacturer: _stringOrNull(map, 'deviceManufacturer'),
      deviceModel: _stringOrNull(map, 'deviceModel'),
      locationPermissionGranted: _boolOrNull(map, 'locationPermissionGranted'),
      backgroundLocationPermissionGranted: _boolOrNull(
        map,
        'backgroundLocationPermissionGranted',
      ),
      activityRecognitionPermissionGranted: _boolOrNull(
        map,
        'activityRecognitionPermissionGranted',
      ),
      foregroundServicePermissionGranted: _boolOrNull(
        map,
        'foregroundServicePermissionGranted',
      ),
      foregroundServiceLocationPermissionGranted: _boolOrNull(
        map,
        'foregroundServiceLocationPermissionGranted',
      ),
      exactAlarmPermissionGranted: _boolOrNull(
        map,
        'exactAlarmPermissionGranted',
      ),
      locationServicesEnabled: _boolOrNull(map, 'locationServicesEnabled'),
      proximityReceiverDeclared: _boolOrNull(map, 'proximityReceiverDeclared'),
      motionReceiverDeclared: _boolOrNull(map, 'motionReceiverDeclared'),
      bootReceiverDeclared: _boolOrNull(map, 'bootReceiverDeclared'),
      reconcileAlarmReceiverDeclared: _boolOrNull(
        map,
        'reconcileAlarmReceiverDeclared',
      ),
      locationConfirmReceiverDeclared: _boolOrNull(
        map,
        'locationConfirmReceiverDeclared',
      ),
      locationConfirmServiceDeclared: _boolOrNull(
        map,
        'locationConfirmServiceDeclared',
      ),
      locationConfirmServiceHasLocationType: _boolOrNull(
        map,
        'locationConfirmServiceHasLocationType',
      ),
      locationConfirmServiceRunning: _boolOrNull(
        map,
        'locationConfirmServiceRunning',
      ),
      locationConfirmServiceForegroundReady: _boolOrNull(
        map,
        'locationConfirmServiceForegroundReady',
      ),
      locationConfirmLaunchToken: _intOrNull(map, 'locationConfirmLaunchToken'),
      locationConfirmLaunchRequestedAtMillis: _intOrNull(
        map,
        'locationConfirmLaunchRequestedAtMillis',
      ),
      locationConfirmForegroundReadyAtMillis: _intOrNull(
        map,
        'locationConfirmForegroundReadyAtMillis',
      ),
      locationConfirmLastLaunchFailureAtMillis: _intOrNull(
        map,
        'locationConfirmLastLaunchFailureAtMillis',
      ),
      locationConfirmLastLaunchFailureReason: _stringOrNull(
        map,
        'locationConfirmLastLaunchFailureReason',
      ),
      locationConfirmCanRun: _boolOrNull(map, 'locationConfirmCanRun'),
      locationConfirmQueueSize: _intOrNull(map, 'locationConfirmQueueSize'),
      fencePulseReceiverDeclared: _boolOrNull(
        map,
        'fencePulseReceiverDeclared',
      ),
      fencePulseStartedAtMillis: _intOrNull(map, 'fencePulseStartedAtMillis'),
      fencePulseIdleTicks: _intOrNull(map, 'fencePulseIdleTicks'),
      fencePulseCanRun: _boolOrNull(map, 'fencePulseCanRun'),
      proximityEligible: _boolOrNull(map, 'proximityEligible'),
      passiveLocationEligible: _boolOrNull(map, 'passiveLocationEligible'),
      motionGateEligible: _boolOrNull(map, 'motionGateEligible'),
      reconcileEligible: _boolOrNull(map, 'reconcileEligible'),
      proximityPendingIntentExists: _boolOrNull(
        map,
        'proximityPendingIntentExists',
      ),
      passiveLocationPendingIntentExists: _boolOrNull(
        map,
        'passiveLocationPendingIntentExists',
      ),
      motionPendingIntentExists: _boolOrNull(map, 'motionPendingIntentExists'),
      reconcileAlarmPendingIntentExists: _boolOrNull(
        map,
        'reconcileAlarmPendingIntentExists',
      ),
      locationConfirmStartPendingIntentExists: _boolOrNull(
        map,
        'locationConfirmStartPendingIntentExists',
      ),
      locationConfirmWatchdogPendingIntentExists: _boolOrNull(
        map,
        'locationConfirmWatchdogPendingIntentExists',
      ),
      fencePulseAlarmPendingIntentExists:
          _boolOrNull(map, 'fencePulseAlarmPendingIntentExists') ??
          _boolOrNull(map, 'fencePulseStartPendingIntentExists'),
    );
  }

  /// Human-readable problems or partial-state mismatches worth surfacing in an
  /// app support screen.
  List<String> get warnings {
    final items = <String>[];
    if (!smartLayerSupported) return items;

    final nativeIds = nativeStatus.persistedGeofenceIds.toSet();
    final smartIds = mirroredFenceIds.toSet();
    final missingMirrors = nativeIds.difference(smartIds).toList()..sort();
    final orphanMirrors = smartIds.difference(nativeIds).toList()..sort();

    if (missingMirrors.isNotEmpty) {
      items.add(
        'native_geofence has fences not mirrored by smart_geofence: '
        '${missingMirrors.join(', ')}.',
      );
    }
    if (orphanMirrors.isNotEmpty) {
      items.add(
        'smart_geofence has mirrored fences missing from native_geofence: '
        '${orphanMirrors.join(', ')}.',
      );
    }
    if (locationServicesEnabled == false) {
      items.add('Device location services are disabled.');
    }
    if (locationPermissionGranted == false) {
      items.add('Location permission is missing.');
    }
    if (backgroundLocationPermissionGranted == false) {
      items.add('Background location permission is missing.');
    }
    if (proximityReceiverDeclared == false) {
      items.add('ProximityReceiver is not declared in the merged manifest.');
    }
    if (motionReceiverDeclared == false) {
      items.add('MotionReceiver is not declared in the merged manifest.');
    }
    if (bootReceiverDeclared == false) {
      items.add(
        'BootReceiver is not declared; smart layers will not re-arm after reboot.',
      );
    }
    if (config.escalation.enabled) {
      if (foregroundServicePermissionGranted == false) {
        items.add(
          'One-shot GPS confirm requires FOREGROUND_SERVICE in the merged manifest.',
        );
      }
      if (foregroundServiceLocationPermissionGranted == false) {
        items.add(
          'One-shot GPS confirm requires FOREGROUND_SERVICE_LOCATION on Android 14+.',
        );
      }
      if (locationConfirmReceiverDeclared == false) {
        items.add('LocationConfirmLaunchReceiver is not declared.');
      }
      if (locationConfirmServiceDeclared == false) {
        items.add('LocationConfirmService is not declared.');
      }
      if (locationConfirmServiceHasLocationType == false) {
        items.add(
          'LocationConfirmService is missing foregroundServiceType="location".',
        );
      }
      if ((locationConfirmQueueSize ?? 0) > 0 &&
          locationConfirmCanRun == false) {
        items.add(
          'Foreground GPS confirm requests are queued but the foreground confirm service cannot run.',
        );
      }
      if (locationConfirmServiceRunning == true &&
          locationConfirmServiceForegroundReady == false) {
        items.add(
          'LocationConfirmService is running but has not reached foreground-ready state.',
        );
      }
      if (_hasFailure(locationConfirmLastLaunchFailureReason)) {
        items.add(
          'Last LocationConfirmService launch failure: '
          '$locationConfirmLastLaunchFailureReason.',
        );
      }
    }
    if (config.reconcile.scheduler ==
        SmartGeofenceReconcileScheduler.exactAlarm) {
      if (reconcileAlarmReceiverDeclared == false) {
        items.add('ReconcileAlarmReceiver is not declared.');
      }
      if (exactAlarmPermissionGranted == false) {
        items.add(
          'AlarmManager reconcile is configured but exact alarms are not permitted; inexact AlarmManager timing will be used.',
        );
      }
    }
    if (config.escalation.passive.enabled && passiveLocationEligible == false) {
      items.add('Passive location is enabled but is not currently eligible.');
    }
    if (config.fencePulse.enabled) {
      if (fencePulseReceiverDeclared == false) {
        items.add('FencePulseAlarmReceiver is not declared.');
      }
      if (activityRecognitionPermissionGranted == false) {
        items.add(
          'Activity recognition permission is missing; motion gate is disabled.',
        );
      }
    }
    return items;
  }

  @override
  String toString() {
    return 'SmartGeofenceStatus('
        'smartLayerSupported: $smartLayerSupported, '
        'config: $config, '
        'mirroredFenceIds: [${mirroredFenceIds.join(',')}], '
        'locationPermissionGranted: $locationPermissionGranted, '
        'backgroundLocationPermissionGranted: $backgroundLocationPermissionGranted, '
        'locationServicesEnabled: $locationServicesEnabled, '
        'proximityEligible: $proximityEligible, '
        'passiveLocationEligible: $passiveLocationEligible, '
        'locationConfirmCanRun: $locationConfirmCanRun, '
        'locationConfirmServiceForegroundReady: $locationConfirmServiceForegroundReady, '
        'locationConfirmLastLaunchFailureReason: $locationConfirmLastLaunchFailureReason, '
        'locationConfirmQueueSize: $locationConfirmQueueSize, '
        'fencePulseCanRun: $fencePulseCanRun, '
        'fencePulseAlarmPendingIntentExists: $fencePulseAlarmPendingIntentExists, '
        'fencePulseIdleTicks: $fencePulseIdleTicks, '
        'warnings: $warnings, '
        'nativeStatus: $nativeStatus)';
  }

  static SmartGeofenceConfig _configFromMap(Map<Object?, Object?> map) {
    return SmartGeofenceConfig.fromMap(map);
  }

  static Map<Object?, Object?> _map(Map<Object?, Object?> map, String key) {
    final value = map[key];
    if (value is Map<Object?, Object?>) return value;
    if (value is Map) return value.cast<Object?, Object?>();
    return const <Object?, Object?>{};
  }

  static List<String> _stringList(Map<Object?, Object?> map, String key) {
    final value = map[key];
    if (value is List) {
      return value.whereType<String>().toList(growable: false);
    }
    return const <String>[];
  }

  static bool? _boolOrNull(Map<Object?, Object?> map, String key) {
    final value = map[key];
    return value is bool ? value : null;
  }

  static int? _intOrNull(Map<Object?, Object?> map, String key) {
    final value = map[key];
    return value is num ? value.toInt() : null;
  }

  static String? _stringOrNull(Map<Object?, Object?> map, String key) {
    final value = map[key];
    return value is String ? value : null;
  }

  static bool _hasFailure(String? reason) =>
      reason != null && reason.trim().isNotEmpty;
}
