import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../core/config/server_config.dart';
import '../core/localization/app_localizations.dart';
import '../core/router/app_router.dart';
import '../core/theme/app_theme.dart';
import '../features/auth/state/auth_controller.dart';

/// Root widget of the BD QuickMart customer application (Goal 02).
///
/// The [AuthController] and [ServerConfig] are provided to the whole tree so
/// any screen can read session/server state. The [GoRouter] is created once
/// and bound to the controller as its `refreshListenable`.
class CustomerApp extends StatefulWidget {
  const CustomerApp({
    super.key,
    required this.auth,
    required this.serverConfig,
  });

  final AuthController auth;
  final ServerConfig serverConfig;

  @override
  State<CustomerApp> createState() => _CustomerAppState();
}

class _CustomerAppState extends State<CustomerApp> {
  late final GoRouter _router = createAppRouter(widget.auth);

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider<AuthController>.value(value: widget.auth),
        ChangeNotifierProvider<ServerConfig>.value(value: widget.serverConfig),
      ],
      child: MaterialApp.router(
        title: 'BD QuickMart',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        routerConfig: _router,
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        supportedLocales: AppLocalizations.supportedLocales,
        locale: const Locale('bn'),
      ),
    );
  }
}
