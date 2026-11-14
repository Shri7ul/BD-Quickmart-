/// Environment-driven configuration for the customer application.
///
/// Values are supplied at COMPILE TIME through `--dart-define` or
/// `--dart-define-from-file` (see `.env.example` and
/// `docs/development/DEPLOYMENT.md`). The `.env.example` file is
/// documentation only - the shipped app does not read a `.env` file.
///
/// Release builds MUST set `IS_DEVELOPMENT=false` and supply an HTTPS
/// `API_BASE_URL` / WSS `WS_BASE_URL`. When `IS_DEVELOPMENT=false` the
/// defaults below are deliberately non-routable `.invalid` hosts, so a
/// misconfigured release build fails closed instead of silently contacting
/// an emulator-only (10.0.2.2) or LAN development server.
class AppConfig {
  const AppConfig._();

  /// Whether the app targets a local/dev backend. Compile-time constant.
  /// A `--dart-define IS_DEVELOPMENT=false` release build evaluates to false.
  static const bool isDevelopment = bool.fromEnvironment(
    'IS_DEVELOPMENT',
    defaultValue: true,
  );

  /// Base URL of the BD QuickMart backend, including the `/api/v1` prefix.
  /// Debug default is the Android emulator alias; release default is https
  /// and non-routable. Always override with `--dart-define` for real builds.
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: isDevelopment
        ? 'http://10.0.2.2:8080/api/v1'
        : 'https://bd-quickmart-backend.example.invalid/api/v1',
  );

  /// WebSocket endpoint used for live order tracking.
  /// Debug default is cleartext ws; release default is secure wss.
  static const String wsBaseUrl = String.fromEnvironment(
    'WS_BASE_URL',
    defaultValue: isDevelopment
        ? 'ws://10.0.2.2:8080/ws/tracking'
        : 'wss://bd-quickmart-backend.example.invalid/ws/tracking',
  );

  /// Request timeout for API calls.
  static const Duration requestTimeout = Duration(seconds: 30);

  /// Default supported locales. Bangla is listed first as the primary market
  /// language, but English remains fully supported.
  static const List<String> supportedLocales = ['bn', 'en'];

  /// Default locale applied on first launch.
  static const String defaultLocale = 'bn';
}
