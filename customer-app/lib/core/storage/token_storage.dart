import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// A persisted token pair.
class StoredTokens {
  const StoredTokens({required this.accessToken, required this.refreshToken});

  final String accessToken;
  final String refreshToken;
}

/// Abstraction over secure token persistence.
///
/// The backend stores only SHA-256 hashes of refresh tokens. On device the raw
/// tokens are kept in the Android Keystore / iOS Keychain. Tests swap in an
/// in-memory implementation so no plugin channel is required.
abstract class TokenStorage {
  Future<StoredTokens?> read();
  Future<void> write(StoredTokens tokens);
  Future<void> clear();
}

/// Platform secure storage backed by `flutter_secure_storage`.
class SecureTokenStorage implements TokenStorage {
  static const String _accessKey = 'bdqm.accessToken';
  static const String _refreshKey = 'bdqm.refreshToken';

  SecureTokenStorage({FlutterSecureStorage? storage})
      : _storage = storage ?? const FlutterSecureStorage();

  final FlutterSecureStorage _storage;

  @override
  Future<StoredTokens?> read() async {
    final String? access = await _storage.read(key: _accessKey);
    final String? refresh = await _storage.read(key: _refreshKey);
    if (access == null || refresh == null) {
      return null;
    }
    return StoredTokens(accessToken: access, refreshToken: refresh);
  }

  @override
  Future<void> write(StoredTokens tokens) async {
    await _storage.write(key: _accessKey, value: tokens.accessToken);
    await _storage.write(key: _refreshKey, value: tokens.refreshToken);
  }

  @override
  Future<void> clear() async {
    await _storage.delete(key: _accessKey);
    await _storage.delete(key: _refreshKey);
  }
}

/// In-memory [TokenStorage] used by tests.
class InMemoryTokenStorage implements TokenStorage {
  StoredTokens? _tokens;

  @override
  Future<StoredTokens?> read() async => _tokens;

  @override
  Future<void> write(StoredTokens tokens) async {
    _tokens = tokens;
  }

  @override
  Future<void> clear() async {
    _tokens = null;
  }
}
