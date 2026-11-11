import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:rider_app/app/app.dart';
import 'package:rider_app/core/config/server_config.dart';
import 'package:rider_app/core/localization/app_localizations.dart';
import 'package:rider_app/core/network/api_client.dart';
import 'package:rider_app/core/storage/key_value_store.dart';
import 'package:rider_app/core/storage/token_storage.dart';
import 'package:rider_app/features/assignments/domain/delivery_assignment.dart';
import 'package:rider_app/features/auth/data/auth_api.dart';
import 'package:rider_app/features/auth/state/auth_controller.dart';
import 'package:rider_app/features/availability/domain/rider_status.dart';

/// Builds a fully in-memory auth graph so widget tests never touch plugin
/// channels (secure storage / shared_preferences) or the network.
({AuthController auth, ServerConfig config}) buildTestGraph() {
  final ServerConfig config = ServerConfig(InMemoryKeyValueStore());
  final AuthController auth = AuthController(
    authApi: AuthApi(config),
    apiClient: ApiClient(serverConfig: config),
    tokenStorage: InMemoryTokenStorage(),
    allowedRole: 'RIDER',
    pendingAllowed: true,
  );
  auth.attach();
  return (auth: auth, config: config);
}

void main() {
  testWidgets('RiderApp shows the login screen when signed out',
      (WidgetTester tester) async {
    final graph = buildTestGraph();
    await graph.auth.bootstrap(); // empty storage -> unauthenticated

    await tester.pumpWidget(
      RiderApp(auth: graph.auth, serverConfig: graph.config),
    );
    await tester.pumpAndSettle();

    expect(find.text('বিডি কুইকমার্ট রাইডার'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
    expect(find.text('Change server URL'), findsOneWidget);
  });

  test('Localizations resolve rider-specific vocabulary', () {
    final AppLocalizations bangla = AppLocalizations(const Locale('bn'));
    final AppLocalizations english = AppLocalizations(const Locale('en'));

    expect(bangla.isBangla, isTrue);
    expect(bangla.navAssignments, 'অ্যাসাইনমেন্ট');
    expect(english.navAssignments, 'Assignments');
    expect(english.accept, 'Accept');
  });

  test('A stale location fix is not treated as live', () {
    final RiderLocation fresh = RiderLocation(
      latitude: 23.7,
      longitude: 90.4,
      recordedAt: DateTime.now(),
    );
    final RiderLocation stale = RiderLocation(
      latitude: 23.7,
      longitude: 90.4,
      recordedAt: DateTime.now().subtract(const Duration(minutes: 5)),
    );

    expect(fresh.isFresh(), isTrue);
    expect(stale.isFresh(), isFalse);
  });

  test('Assignment starts in the offered state', () {
    const DeliveryAssignment assignment = DeliveryAssignment(
      id: 'a1',
      orderId: 'o1',
      storeName: 'Mirpur Dark Store',
      pickupAddress: 'Mirpur 10',
      dropOffAddress: 'Kazipara',
      estimatedDistanceKm: 3.4,
      estimatedEtaMinutes: 18,
      estimatedPayAmount: 45,
      pickupLatitude: 23.78,
      pickupLongitude: 90.36,
      dropOffLatitude: 23.79,
      dropOffLongitude: 90.38,
    );

    expect(assignment.status, AssignmentStatus.offered);
    expect(assignment.isActive, isFalse);
    expect(assignment.copyWith(status: AssignmentStatus.accepted).isActive,
        isTrue);
  });
}
