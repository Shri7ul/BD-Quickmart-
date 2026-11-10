import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import '../core/localization/app_localizations.dart';
import '../core/network/api_client.dart';
import '../core/router/app_router.dart';
import '../core/theme/app_theme.dart';

/// Root widget of the BD QuickMart rider application.
class RiderApp extends StatelessWidget {
  const RiderApp({super.key, this.apiClient});

  /// Overridable for tests; the app uses a single shared instance in
  /// production.
  final ApiClient? apiClient;

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'BD QuickMart Rider',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      routerConfig: AppRouter.router,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      locale: const Locale('bn'),
    );
  }
}
