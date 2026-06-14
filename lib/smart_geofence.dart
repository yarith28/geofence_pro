/// smart_geofence - a smart geofencing layer over native_geofence.
///
/// Apps use [SmartGeofenceManager] as the single entry point. Geofence
/// registration, the native geofence APIs, and all iOS behavior come from
/// native_geofence; smart_geofence adds Android-only no-power passive checks,
/// proximity-aware escalation, motion gating, an optional FencePulse, and a
/// reconcile watchdog.
library;

// Re-export the native_geofence model types so consumers only import
// `smart_geofence`.
export 'package:native_geofence/native_geofence.dart'
    show
        Geofence,
        Location,
        GeofenceEvent,
        ActiveGeofence,
        GeofenceCallbackParams,
        IosGeofenceSettings,
        AndroidGeofenceSettings,
        NativeGeofenceStatus,
        NativeGeofenceException,
        // Usable inside a geofence callback to promote/demote the Android
        // native_geofence foreground service (drop-in for migrating apps).
        NativeGeofenceBackgroundManager;

export 'src/smart_geofence_background_manager.dart';
export 'src/smart_geofence_config.dart';
export 'src/smart_geofence_manager.dart'
    show SmartGeofenceManager, SmartGeofenceCallback;
export 'src/smart_geofence_status.dart';
