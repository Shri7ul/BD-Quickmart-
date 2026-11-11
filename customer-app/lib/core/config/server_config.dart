import 'package:flutter/foundation.dart';

import '../storage/key_value_store.dart';
import 'app_config.dart';

/// Holds the runtime backend base URL.
///
/// The compile-time default comes from `--dart-define API_BASE_URL=...`
/// (see [AppConfig.apiBaseUrl]). A user can override it at runtime from the
/// sign-in screen so a physical device can reach a backend on the LAN. The
/// override is persisted in a [KeyValueStore] and takes effect on the next
/// request without rebuilding the HTTP client instances.
class ServerConfig extends ChangeNotifier {
  ServerConfig(this._store, {String? defaultBaseUrl})
      : _defaultBaseUrl = defaultBaseUrl ?? AppConfig.apiBaseUrl,
        _baseUrl = defaultBaseUrl ?? AppConfig.apiBaseUrl;

  static const String _overrideKey = 'bdqm.server.baseUrl';

  final KeyValueStore _store;
  final String _defaultBaseUrl;
  String _baseUrl;

  /// Base URL currently in effect, including the `/api/v1` prefix.
  String get baseUrl => _baseUrl;

  /// The compile-time default (used to decide whether an override is active).
  String get defaultBaseUrl => _defaultBaseUrl;

  bool get isOverridden => _baseUrl != _defaultBaseUrl;

  /// Loads any persisted override. Call once during app startup.
  Future<void> load() async {
    final String? stored = await _store.read(_overrideKey);
    if (stored != null && stored.trim().isNotEmpty) {
      _baseUrl = _normalize(stored);
      notifyListeners();
    }
  }

  /// Persists a new base URL. Passing an empty value or the default clears the
  /// override. Throws [ArgumentError] when the value is not an http(s) URL.
  Future<void> setBaseUrl(String value) async {
    final String trimmed = value.trim();
    if (trimmed.isEmpty || _normalize(trimmed) == _defaultBaseUrl) {
      await _store.remove(_overrideKey);
      _baseUrl = _defaultBaseUrl;
      notifyListeners();
      return;
    }

    final String normalized = _normalize(trimmed);
    final Uri? uri = Uri.tryParse(normalized);
    if (uri == null ||
        !uri.hasScheme ||
        (uri.scheme != 'http' && uri.scheme != 'https') ||
        uri.host.isEmpty) {
      throw ArgumentError('Enter a valid http(s) server URL.');
    }

    await _store.write(_overrideKey, normalized);
    _baseUrl = normalized;
    notifyListeners();
  }

  String _normalize(String raw) {
    String value = raw.trim();
    while (value.endsWith('/')) {
      value = value.substring(0, value.length - 1);
    }
    return value;
  }
}
