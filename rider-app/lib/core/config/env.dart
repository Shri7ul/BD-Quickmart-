import 'package:logger/logger.dart';

/// Application-wide logger for the rider application.
class AppLogger {
  AppLogger._();

  static final Logger _logger = Logger(
    printer: PrettyPrinter(
      methodCount: 0,
      errorMethodCount: 5,
    ),
  );

  /// Verbose diagnostics - development only.
  static void v(dynamic message) => _logger.d(message.toString());

  /// Routine lifecycle information.
  static void i(dynamic message) => _logger.i(message.toString());

  /// Recoverable problems.
  static void w(dynamic message, [Object? error, StackTrace? stackTrace]) =>
      _logger.w(message.toString(), error: error, stackTrace: stackTrace);

  /// Failures that need attention.
  static void e(dynamic message, [Object? error, StackTrace? stackTrace]) =>
      _logger.e(message.toString(), error: error, stackTrace: stackTrace);
}
