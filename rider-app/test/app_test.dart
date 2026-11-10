import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:rider_app/app/app.dart';
import 'package:rider_app/core/localization/app_localizations.dart';
import 'package:rider_app/features/assignments/domain/delivery_assignment.dart';
import 'package:rider_app/features/availability/domain/rider_status.dart';

void main() {
  testWidgets('RiderApp renders the dashboard', (tester) async {
    await tester.pumpWidget(const RiderApp());
    await tester.pumpAndSettle();

    expect(find.text('ড্যাশবোর্ড'), findsOneWidget);
  });

  test('Localizations resolve rider-specific vocabulary', () {
    final bangla = AppLocalizations(const Locale('bn'));
    final english = AppLocalizations(const Locale('en'));

    expect(bangla.navAssignments, 'অ্যাসাইনমেন্ট');
    expect(english.navAssignments, 'Assignments');
    expect(english.accept, 'Accept');
  });

  test('A stale location fix is not treated as live', () {
    final fresh = RiderLocation(
      latitude: 23.7,
      longitude: 90.4,
      recordedAt: DateTime.now(),
    );
    final stale = RiderLocation(
      latitude: 23.7,
      longitude: 90.4,
      recordedAt: DateTime.now().subtract(const Duration(minutes: 5)),
    );

    expect(fresh.isFresh(), isTrue);
    expect(stale.isFresh(), isFalse);
  });

  test('Assignment starts in the offered state', () {
    const assignment = DeliveryAssignment(
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
    expect(assignment.copyWith(status: AssignmentStatus.accepted).isActive, isTrue);
  });
}
