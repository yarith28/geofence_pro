import 'dart:ui';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:native_geofence/native_geofence.dart' as ng;

import 'smart_geofence_config.dart';
import 'smart_geofence_status.dart';

/// Callback invoked for a geofence transition. Structurally identical to
/// native_geofence's callback, re-declared so consumers only import
/// `smart_geofence`.
typedef SmartGeofenceCallback =
    Future<void> Function(ng.GeofenceCallbackParams params);

@pragma('vm:entry-point')
Future<void> smartGeofenceCallbackDispatcher(
  ng.GeofenceCallbackParams params,
) async {
  await _SmartGeofenceCallbackDispatcher.dispatch(params);
}

class _SmartGeofenceCallbackDispatcher {
  static Future<void> dispatch(ng.GeofenceCallbackParams params) async {
    if (params.geofences.isEmpty) return;
    final handles = await _callbackHandlesFor(
      params.geofences.map((geofence) => geofence.id).toSet().toList(),
    );
    final missing = <String>[];
    final byHandle = <int, List<ng.ActiveGeofence>>{};
    for (final geofence in params.geofences) {
      final handle = handles[geofence.id];
      if (handle == null || handle <= 0) {
        missing.add(geofence.id);
        continue;
      }
      byHandle.putIfAbsent(handle, () => <ng.ActiveGeofence>[]).add(geofence);
    }
    if (missing.isNotEmpty) {
      throw ng.NativeGeofenceException(
        code: ng.NativeGeofenceErrorCode.callbackNotFound,
        message: 'smart_geofence callback handle not found.',
        details:
            'Missing callback handle for geofence id(s): '
            '${missing.join(', ')}.',
      );
    }

    for (final entry in byHandle.entries) {
      final callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(entry.key),
      );
      if (callback == null) {
        throw ng.NativeGeofenceException(
          code: ng.NativeGeofenceErrorCode.callbackNotFound,
          message: 'Original smart_geofence callback was not found.',
          details: 'callbackHandle=${entry.key}',
        );
      }
      if (callback is! SmartGeofenceCallback) {
        throw ng.NativeGeofenceException(
          code: ng.NativeGeofenceErrorCode.callbackInvalid,
          message: 'Original smart_geofence callback signature is invalid.',
          details: 'callbackHandle=${entry.key}, type=${callback.runtimeType}',
        );
      }
      await callback(
        ng.GeofenceCallbackParams(
          geofences: List<ng.ActiveGeofence>.unmodifiable(entry.value),
          event: params.event,
          location: params.location,
        ),
      );
    }
  }

  static Future<Map<String, int>> _callbackHandlesFor(List<String> ids) async {
    final raw = await SmartGeofenceManager._channel.invokeMethod<Object?>(
      'getCallbackHandles',
      <String, Object?>{'ids': ids},
    );
    if (raw is! Map) return const <String, int>{};
    final result = <String, int>{};
    raw.forEach((key, value) {
      final id = key?.toString();
      final handle = switch (value) {
        int() => value,
        double() => value.toInt(),
        _ => null,
      };
      if (id != null && id.isNotEmpty && handle != null) {
        result[id] = handle;
      }
    });
    return result;
  }
}

/// Single entry point for smart_geofence.
///
/// Wraps native_geofence (which owns the native geofence API and is used
/// directly on iOS) and layers Android-only smart behavior on top: no-power
/// passive checks, proximity-aware escalation, motion gating, optional
/// FencePulse, and a reconcile watchdog. Apps talk to this class only.
class SmartGeofenceManager {
  SmartGeofenceManager._();

  static final SmartGeofenceManager instance = SmartGeofenceManager._();

  static const MethodChannel _channel = MethodChannel('smart_geofence');

  /// The smart native layers exist only on Android. Elsewhere this wrapper is a
  /// straight pass-through to native_geofence.
  bool get _smartLayerSupported =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  /// Initialize the underlying geofencing engine and apply [config].
  Future<void> initialize({
    SmartGeofenceConfig config = const SmartGeofenceConfig(),
  }) async {
    await ng.NativeGeofenceManager.instance.initialize();
    await configure(config);
  }

  /// Update the smart-layer configuration. No-op on non-Android platforms.
  Future<void> configure(SmartGeofenceConfig config) async {
    await ng.NativeGeofenceManager.instance.configureLogFile(
      config: ng.NativeGeofenceLogFileConfig(
        enabled: config.logging.fileEnabled,
        maxBytes: config.logging.maxFileBytes,
      ),
    );
    if (!_smartLayerSupported) return;
    await _channel.invokeMethod('configure', config.toMap());
  }

  /// Read app-private Android log files for both smart_geofence and native_geofence.
  ///
  /// Returns an empty string when file logging has not been enabled and no prior
  /// logs remain. On non-Android platforms this returns the native_geofence
  /// platform result, which is currently empty.
  Future<String> readLogFile() async {
    final nativeLogs = await ng.NativeGeofenceManager.instance.readLogFile();
    if (!_smartLayerSupported) return nativeLogs;
    final smartLogs = await _channel.invokeMethod<String>('readLogFile') ?? '';
    final sections = <String>[
      if (nativeLogs.isNotEmpty) '--- native_geofence ---\n$nativeLogs',
      if (smartLogs.isNotEmpty) '--- smart_geofence ---\n$smartLogs',
    ];
    return sections.join('\n');
  }

  /// Clear app-private Android log files for both smart_geofence and native_geofence.
  Future<void> clearLogFile() async {
    await ng.NativeGeofenceManager.instance.clearLogFile();
    if (!_smartLayerSupported) return;
    await _channel.invokeMethod('clearLogFile');
  }

  /// Return native_geofence diagnostics plus Android-only smart_geofence layer
  /// readiness: permissions, mirrored fences, declared recovery components, and
  /// PendingIntent handles for the proximity/passive/motion/FencePulse layers.
  Future<SmartGeofenceStatus> getDiagnosticStatus() async {
    final nativeStatus = await ng.NativeGeofenceManager.instance
        .getDiagnosticStatus();
    if (!_smartLayerSupported) {
      return SmartGeofenceStatus(
        nativeStatus: nativeStatus,
        smartLayerSupported: false,
      );
    }
    final raw = await _channel.invokeMethod<Object?>('getDiagnosticStatus');
    final map = raw is Map
        ? raw.cast<Object?, Object?>()
        : const <Object?, Object?>{};
    return SmartGeofenceStatus.fromMap(
      nativeStatus: nativeStatus,
      smartLayerSupported: true,
      map: map,
    );
  }

  /// Register a geofence. Delegates native geofence registration to
  /// native_geofence, then mirrors the geometry so the Android smart layers can
  /// compute proximity/passive checks in the background without a live Dart
  /// isolate.
  Future<void> createGeofence(
    ng.Geofence geofence,
    SmartGeofenceCallback callback,
  ) async {
    if (!_smartLayerSupported) {
      await ng.NativeGeofenceManager.instance.createGeofence(
        geofence,
        callback,
      );
      return;
    }
    final userCallbackHandle = PluginUtilities.getCallbackHandle(callback);
    if (userCallbackHandle == null) {
      throw ng.NativeGeofenceException.invalidArgument(
        message: 'Callback is invalid.',
      );
    }
    final dispatcherHandle = PluginUtilities.getCallbackHandle(
      smartGeofenceCallbackDispatcher,
    );
    if (dispatcherHandle == null) {
      throw ng.NativeGeofenceException.internal(
        message: 'smart_geofence callback dispatcher is invalid.',
      );
    }

    final previousMirror = await _registerFenceMirror(
      geofence,
      userCallbackHandle.toRawHandle(),
    );
    try {
      await ng.NativeGeofenceManager.instance.createGeofence(
        geofence,
        smartGeofenceCallbackDispatcher,
      );
    } catch (_) {
      await _restoreFenceMirror(geofence.id, previousMirror);
      rethrow;
    }
  }

  Future<Map<Object?, Object?>?> _registerFenceMirror(
    ng.Geofence geofence,
    int callbackHandle,
  ) async {
    final raw = await _channel.invokeMethod<Object?>('registerFence', {
      'id': geofence.id,
      'latitude': geofence.location.latitude,
      'longitude': geofence.location.longitude,
      'radiusMeters': geofence.radiusMeters,
      'triggers': geofence.triggers.map((e) => e.name).toList(),
      'callbackHandle': callbackHandle,
    });
    return raw is Map ? raw.cast<Object?, Object?>() : null;
  }

  Future<void> _restoreFenceMirror(
    String id,
    Map<Object?, Object?>? previousMirror,
  ) async {
    if (previousMirror == null) {
      await _channel.invokeMethod('removeFence', {'id': id});
      return;
    }
    await _channel.invokeMethod(
      'registerFence',
      previousMirror.map((key, value) => MapEntry(key.toString(), value)),
    );
  }

  Future<void> removeGeofence(ng.Geofence geofence) =>
      removeGeofenceById(geofence.id);

  Future<void> removeGeofenceById(String id) async {
    try {
      await ng.NativeGeofenceManager.instance.removeGeofenceById(id);
    } on ng.NativeGeofenceException catch (e) {
      if (_smartLayerSupported &&
          e.code == ng.NativeGeofenceErrorCode.geofenceNotFound) {
        await _channel.invokeMethod('removeFence', {'id': id});
      }
      rethrow;
    }
    if (!_smartLayerSupported) return;
    await _channel.invokeMethod('removeFence', {'id': id});
  }

  Future<void> removeAllGeofences() async {
    await ng.NativeGeofenceManager.instance.removeAllGeofences();
    if (!_smartLayerSupported) return;
    await _channel.invokeMethod('removeAllFences');
  }

  Future<List<String>> getRegisteredGeofenceIds() =>
      ng.NativeGeofenceManager.instance.getRegisteredGeofenceIds();

  Future<List<ng.ActiveGeofence>> getRegisteredGeofences() =>
      ng.NativeGeofenceManager.instance.getRegisteredGeofences();

  /// Re-register geofences after a reboot. Call from your boot handler /
  /// app resume; native_geofence re-creates the OS fences and smart_geofence
  /// re-arms its layers via the reconcile watchdog.
  Future<void> reCreateAfterReboot() =>
      ng.NativeGeofenceManager.instance.reCreateAfterReboot();

  /// Reconcile everything on app resume: re-register any dropped OS geofences
  /// and re-arm the Android smart layers. Safe to call often (idempotent).
  Future<void> reconcile() async {
    await ng.NativeGeofenceManager.instance.reCreateAfterReboot();
    if (!_smartLayerSupported) return;
    await _channel.invokeMethod('start');
  }
}
