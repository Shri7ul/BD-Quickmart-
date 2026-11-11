import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/storage/token_storage.dart';
import '../auth_exception.dart';
import '../data/auth_api.dart';
import '../models/auth_result.dart';
import '../models/auth_user.dart';

/// Session lifecycle state.
enum AuthStatus {
  /// Startup state before secure storage has been read.
  unknown,

  /// Signed in with an ACTIVE account of the app's role.
  authenticated,

  /// Signed in but awaiting administrator approval (rider app only).
  pending,

  /// No valid session.
  unauthenticated,
}

/// Owns the authentication session for a single mobile app.
///
/// The access token lives only in memory ([ApiClient]); both tokens are also
/// persisted in secure storage so a cold start can silently restore a session.
/// A [ChangeNotifier] doubles as go_router's `refreshListenable`.
class AuthController extends ChangeNotifier {
  AuthController({
    required AuthApi authApi,
    required ApiClient apiClient,
    required TokenStorage tokenStorage,
    required this.allowedRole,
    this.pendingAllowed = false,
  })  : _authApi = authApi,
        _apiClient = apiClient,
        _tokenStorage = tokenStorage;

  final AuthApi _authApi;
  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  /// Role this app accepts (CUSTOMER for the customer app, RIDER for rider).
  final String allowedRole;

  /// Whether a PENDING account of [allowedRole] still gets a session.
  /// The rider app enables this so new riders can wait for approval in-app.
  final bool pendingAllowed;

  AuthStatus _status = AuthStatus.unknown;
  AuthUser? _user;
  String? _message;

  AuthStatus get status => _status;
  AuthUser? get user => _user;
  String? get message => _message;
  bool get isAuthenticated => _status == AuthStatus.authenticated;
  bool get isPending => _status == AuthStatus.pending;

  /// Registers the silent-refresh and auth-failure callbacks on [ApiClient].
  /// Call once during app startup, before any authenticated request.
  void attach() {
    _apiClient.setRefreshHandler(_refresh);
    _apiClient.setOnAuthFailure(_setUnauthenticated);
  }

  /// Restores a session from secure storage by rotating the refresh token.
  Future<void> bootstrap() async {
    final StoredTokens? stored = await _tokenStorage.read();
    if (stored == null) {
      _setUnauthenticated();
      return;
    }
    _apiClient.setAuthToken(stored.accessToken);
    try {
      final AuthResult result = await _authApi.refresh(stored.refreshToken);
      await _commit(result);
    } catch (_) {
      await _clearLocalSession();
      _setUnauthenticated();
    }
  }

  /// Signs in with an email address or phone number.
  Future<void> login(String identifier, String password) async {
    _message = null;
    final AuthResult result = await _authApi.login(identifier, password);
    await _commit(result, throwOnReject: true);
  }

  /// Registers a public account, then signs in with the same credentials.
  Future<void> register({
    required String fullName,
    required String email,
    required String phone,
    required String password,
  }) async {
    _message = null;
    await _authApi.register(
      fullName: fullName,
      email: email,
      phone: phone,
      password: password,
      role: allowedRole,
    );
    await login(email, password);
  }

  /// Revokes the refresh-token family on the server, then clears local state.
  Future<void> logout() async {
    try {
      await _apiClient.post('/auth/logout');
    } catch (_) {
      // Local cleanup must happen even when the server call fails.
    }
    await _clearLocalSession();
    _setUnauthenticated();
  }

  /// Re-checks the account status, e.g. from the pending-approval screen.
  /// A rotation also surfaces a status change made by an administrator.
  Future<void> refreshSession() async {
    await _refresh();
  }

  /// Token refresher used by [ApiClient] on a 401.
  Future<String?> _refresh() async {
    final StoredTokens? stored = await _tokenStorage.read();
    if (stored == null) {
      return null;
    }
    try {
      final AuthResult result = await _authApi.refresh(stored.refreshToken);
      if (_rejectionFor(result.user) != null) {
        await _clearLocalSession();
        return null;
      }
      await _commit(result);
      return result.accessToken;
    } catch (_) {
      await _clearLocalSession();
      return null;
    }
  }

  /// Validates [result] against this app's policy and persists the session.
  Future<void> _commit(AuthResult result, {bool throwOnReject = false}) async {
    final String? rejection = _rejectionFor(result.user);
    if (rejection != null) {
      if (throwOnReject) {
        throw AuthException(rejection);
      }
      await _clearLocalSession();
      _setUnauthenticated(message: rejection);
      return;
    }

    await _applyTokens(result);
    _user = result.user;
    _status = _resolveStatus(result.user);
    _message = null;
    notifyListeners();
  }

  Future<void> _applyTokens(AuthResult result) async {
    _apiClient.setAuthToken(result.accessToken);
    await _tokenStorage.write(
      StoredTokens(
        accessToken: result.accessToken,
        refreshToken: result.refreshToken,
      ),
    );
  }

  Future<void> _clearLocalSession() async {
    _apiClient.clearAuthToken();
    await _tokenStorage.clear();
  }

  void _setUnauthenticated({String? message}) {
    _user = null;
    _status = AuthStatus.unauthenticated;
    if (message != null) {
      _message = message;
    }
    notifyListeners();
  }

  AuthStatus _resolveStatus(AuthUser user) {
    if (user.status == 'ACTIVE') {
      return AuthStatus.authenticated;
    }
    if (user.status == 'PENDING' && pendingAllowed) {
      return AuthStatus.pending;
    }
    return AuthStatus.unauthenticated;
  }

  /// Returns a user-facing reason when this app must reject the account.
  String? _rejectionFor(AuthUser user) {
    if (user.role != allowedRole) {
      return 'This is not a $allowedRole account. Please use the correct app.';
    }
    if (user.status == 'REJECTED') {
      return 'This account was rejected. Please contact support.';
    }
    if (user.status == 'DISABLED') {
      return 'This account has been disabled.';
    }
    if (user.status == 'PENDING' && !pendingAllowed) {
      return 'This account is pending approval.';
    }
    if (user.status != 'ACTIVE' && user.status != 'PENDING') {
      return 'This account is not active.';
    }
    return null;
  }
}
