import 'package:flutter/material.dart';
import 'package:smart_geofence/smart_geofence.dart';

/// Geofence transition callback. MUST be a top-level or static function
/// annotated with `@pragma('vm:entry-point')` so it can be invoked from the
/// background isolate when the app is not in the foreground.
@pragma('vm:entry-point')
Future<void> geofenceCallback(GeofenceCallbackParams params) async {
  // ignore: avoid_print
  print(
    'smart_geofence callback: ${params.event} for '
    '${params.geofences.map((g) => g.id).toList()}',
  );
}

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final SmartGeofenceManager _manager = SmartGeofenceManager.instance;
  String _status = 'Not initialized';
  List<String> _ids = const [];
  String _logPreview = '';

  @override
  void initState() {
    super.initState();
    _init();
  }

  Future<void> _init() async {
    await _manager.initialize(
      config: const SmartGeofenceConfig(
        escalation: SmartGeofenceEscalationConfig(
          proximity: SmartGeofenceProximityConfig(radiusMeters: 1000),
        ),
        logging: SmartGeofenceLogConfig(fileEnabled: true),
      ),
    );
    await _refresh();
    setState(() => _status = 'Initialized');
  }

  Future<void> _addSampleGeofence() async {
    final geofence = Geofence(
      id: 'sample-${DateTime.now().millisecondsSinceEpoch}',
      location: Location(latitude: 37.4220, longitude: -122.0841),
      radiusMeters: 150,
      triggers: {GeofenceEvent.enter, GeofenceEvent.exit},
      iosSettings: const IosGeofenceSettings(initialTrigger: true),
      androidSettings: const AndroidGeofenceSettings(
        initialTriggers: {GeofenceEvent.enter},
        loiteringDelay: Duration(seconds: 30),
      ),
    );
    await _manager.createGeofence(geofence, geofenceCallback);
    await _refresh();
  }

  Future<void> _removeAll() async {
    await _manager.removeAllGeofences();
    await _refresh();
  }

  Future<void> _refresh() async {
    final ids = await _manager.getRegisteredGeofenceIds();
    setState(() => _ids = ids);
  }

  Future<void> _readLogs() async {
    final logs = await _manager.readLogFile();
    setState(() {
      _logPreview = logs.isEmpty ? 'No logs yet.' : logs;
    });
  }

  Future<void> _clearLogs() async {
    await _manager.clearLogFile();
    setState(() => _logPreview = 'Logs cleared.');
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('smart_geofence example')),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Status: $_status'),
              const SizedBox(height: 8),
              ElevatedButton(
                onPressed: _addSampleGeofence,
                child: const Text('Add sample geofence'),
              ),
              ElevatedButton(
                onPressed: _removeAll,
                child: const Text('Remove all'),
              ),
              ElevatedButton(
                onPressed: _readLogs,
                child: const Text('Read logs'),
              ),
              ElevatedButton(
                onPressed: _clearLogs,
                child: const Text('Clear logs'),
              ),
              const SizedBox(height: 16),
              Text('Registered (${_ids.length}):'),
              SizedBox(
                height: 120,
                child: ListView(children: _ids.map((id) => Text(id)).toList()),
              ),
              const SizedBox(height: 16),
              const Text('Log file preview:'),
              Expanded(child: SingleChildScrollView(child: Text(_logPreview))),
            ],
          ),
        ),
      ),
    );
  }
}
