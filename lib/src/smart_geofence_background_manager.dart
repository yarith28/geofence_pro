import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Android-only helper for promoting geofence callback work to smart_geofence's
/// shared foreground-service notification.
///
/// This is intended to be called from a [SmartGeofenceCallback] when the
/// callback needs extra time or needs to touch APIs that are safer from a
/// foreground service. It is a no-op on non-Android platforms.
class SmartGeofenceBackgroundManager {
  SmartGeofenceBackgroundManager._();

  static final SmartGeofenceBackgroundManager instance =
      SmartGeofenceBackgroundManager._();

  static const MethodChannel _channel = MethodChannel('smart_geofence');

  bool get _supported =>
      !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  /// Promote callback work to a foreground service using smart_geofence's
  /// configured foreground notification.
  Future<void> promoteToForeground() async {
    if (!_supported) return;
    await _channel.invokeMethod<void>('promoteCallbackToForeground');
  }

  /// Alias for [promoteToForeground].
  Future<void> startForeground() => promoteToForeground();

  /// Stop the callback foreground service.
  Future<void> demoteToBackground() async {
    if (!_supported) return;
    await _channel.invokeMethod<void>('demoteCallbackToBackground');
  }

  /// Alias for [demoteToBackground].
  Future<void> stopForeground() => demoteToBackground();
}
