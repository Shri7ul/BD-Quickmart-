import 'package:customer_app/app/app.dart';
import 'package:customer_app/core/config/server_config.dart';
import 'package:customer_app/core/localization/app_localizations.dart';
import 'package:customer_app/core/network/api_client.dart';
import 'package:customer_app/core/storage/key_value_store.dart';
import 'package:customer_app/core/storage/token_storage.dart';
import 'package:customer_app/features/auth/data/auth_api.dart';
import 'package:customer_app/features/auth/state/auth_controller.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// Builds a fully in-memory auth graph so widget tests never touch plugin
/// channels (secure storage / shared_preferences) or the network.
({AuthController auth, ServerConfig config}) buildTestGraph() {
  final ServerConfig config = ServerConfig(InMemoryKeyValueStore());
  final AuthController auth = AuthController(
    authApi: AuthApi(config),
    apiClient: ApiClient(serverConfig: config),
    tokenStorage: InMemoryTokenStorage(),
    allowedRole: 'CUSTOMER',
  );
  auth.attach();
  return (auth: auth, config: config);
}

void main() {
  testWidgets('CustomerApp shows the login screen when signed out',
      (WidgetTester tester) async {
    final graph = buildTestGraph();
    await graph.auth.bootstrap(); // empty storage -> unauthenticated

    await tester.pumpWidget(
      CustomerApp(auth: graph.auth, serverConfig: graph.config),
    );
    await tester.pumpAndSettle();

    expect(find.text('বিডি কুইকমার্ট'), findsOneWidget);
    expect(find.byType(TextFormField), findsNWidgets(2));
    expect(find.text('Change server URL'), findsOneWidget);
  });

  test('Localizations resolve Bangla and English strings', () {
    final AppLocalizations bangla = AppLocalizations(const Locale('bn'));
    final AppLocalizations english = AppLocalizations(const Locale('en'));

    expect(bangla.isBangla, isTrue);
    expect(bangla.navHome, 'হোম');
    expect(english.navHome, 'Home');
  });
}
