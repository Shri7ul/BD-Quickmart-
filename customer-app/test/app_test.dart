import 'package:customer_app/app/app.dart';
import 'package:customer_app/core/localization/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('CustomerApp renders the login screen', (tester) async {
    await tester.pumpWidget(const CustomerApp());
    await tester.pumpAndSettle();

    expect(find.text('বিডি কুইকমার্ট'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
  });

  test('Localizations resolve Bangla and English strings', () {
    final bangla = AppLocalizations(const Locale('bn'));
    final english = AppLocalizations(const Locale('en'));

    expect(bangla.isBangla, isTrue);
    expect(bangla.navHome, 'হোম');
    expect(english.navHome, 'Home');
  });
}
