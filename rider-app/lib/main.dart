import 'package:flutter/material.dart';

import 'app/app.dart';
import 'core/config/server_config.dart';
import 'core/network/api_client.dart';
import 'core/storage/prefs_store.dart';
import 'core/storage/token_storage.dart';
import 'features/auth/data/auth_api.dart';
import 'features/auth/state/auth_controller.dart';

/// Wires the Goal 02 auth graph before the first frame:
/// settings -> server config -> HTTP clients -> controller -> bootstrap.
///
/// New riders self-register as PENDING; [pendingAllowed] keeps them signed in
/// on the /pending screen until an administrator approves the account.
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final PrefsKeyValueStore prefs = await PrefsKeyValueStore.create();
  final ServerConfig serverConfig = ServerConfig(prefs);
  await serverConfig.load();

  final ApiClient apiClient = ApiClient(serverConfig: serverConfig);
  final AuthApi authApi = AuthApi(serverConfig);
  final AuthController auth = AuthController(
    authApi: authApi,
    apiClient: apiClient,
    tokenStorage: SecureTokenStorage(),
    allowedRole: 'RIDER',
    pendingAllowed: true,
  );

  auth.attach();
  await auth.bootstrap();

  runApp(RiderApp(auth: auth, serverConfig: serverConfig));
}
