/// Environment-driven configuration for the customer application.
///
/// Values are read from `.env` (see `.env.example`) with sane development
/// defaults so the app can boot without a populated environment file.
class AppConfig {
  const AppConfig._();

  /// Base URL of the BD QuickMart backend, including the `/api/v1` prefix.
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api/v1',
  );

  /// WebSocket endpoint used for live order tracking.
  static const String wsBaseUrl = String.fromEnvironment(
    'WS_BASE_URL',
    defaultValue: 'ws://10.0.2.2:8080/ws/tracking',
  );

  /// Request timeout for API calls.
  static const Duration requestTimeout = Duration(seconds: 30);

  /// Default supported locales. Bangla is listed first as the primary market
  /// language, but English remains fully supported.
  static const List<String> supportedLocales = ['bn', 'en'];

  /// Default locale applied on first launch.
  static const String defaultLocale = 'bn';

  /// Whether the app runs against a non-production backend.
  static const bool isDevelopment = bool.fromEnvironment(
    'IS_DEVELOPMENT',
    defaultValue: true,
  );
}
