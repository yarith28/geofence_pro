// Basic Flutter integration test for smart_geofence.
//
// Since integration tests run in a full Flutter application, they exercise the
// host (native) side of the plugin, unlike Dart unit tests.
//
// https://flutter.dev/to/integration-testing

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:smart_geofence/smart_geofence.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('initialize and query registered geofences', (
    WidgetTester tester,
  ) async {
    await SmartGeofenceManager.instance.initialize();
    // Round-trips through the platform channel into native_geofence. With no
    // fences registered this is an empty list, but it proves the plumbing works.
    final List<String> ids = await SmartGeofenceManager.instance
        .getRegisteredGeofenceIds();
    expect(ids, isA<List<String>>());
  });
}
