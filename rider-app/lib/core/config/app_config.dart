/// Environment-driven configuration for the rider application.
///
/// The rider app talks to the same backend as the customer app but exposes a
/// different surface: assignments, navigation and earnings.
class AppConfig {
  AppConfig._();

  /// Base URL of the BD QuickMart backend, including the `/api/v1` prefix.
  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080/api/v1',
  );

  /// WebSocket endpoint used for assignment pushes and location relay.
  static const String wsBaseUrl = String.fromEnvironment(
    'WS_BASE_URL',
    defaultValue: 'ws://10.0.2.2:8080/ws/tracking',
  );

  /// How often the rider's GPS position is reported while on an active
  /// delivery.
  static const Duration locationUpdateInterval = Duration(seconds: 10);

  /// A fix older than this is reported as stale rather than live.
  static const Duration locationFreshnessWindow = Duration(seconds: 30);

  /// Request timeout for API calls.
  static const Duration requestTimeout = Duration(seconds: 30);

  /// Supported locales. Bangla is the primary market language.
  static const List<String> supportedLocales = ['bn', 'en'];

  /// Default locale applied on first launch.
  static const String defaultLocale = 'bn';

  /// Whether the app runs against a non-production backend.
  static const bool isDevelopment = bool.fromEnvironment(
    'IS_DEVELOPMENT',
    defaultValue: true,
  );
}
