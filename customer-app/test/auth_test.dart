import 'dart:convert';
import 'dart:typed_data';

import 'package:customer_app/core/config/server_config.dart';
import 'package:customer_app/core/network/api_client.dart';
import 'package:customer_app/core/storage/key_value_store.dart';
import 'package:customer_app/core/storage/token_storage.dart';
import 'package:customer_app/features/auth/auth_exception.dart';
import 'package:customer_app/features/auth/data/auth_api.dart';
import 'package:customer_app/features/auth/models/auth_result.dart';
import 'package:customer_app/features/auth/models/auth_user.dart';
import 'package:customer_app/features/auth/state/auth_controller.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

// ---------------------------------------------------------------------------
// Fake HTTP layer
// ---------------------------------------------------------------------------

ResponseBody _json(Map<String, dynamic> body, int status) =>
    ResponseBody.fromString(
      jsonEncode(body),
      status,
      headers: <String, List<String>>{
        'content-type': <String>['application/json'],
      },
    );

class _FakeAdapter implements HttpClientAdapter {
  _FakeAdapter(this._handle);

  final ResponseBody Function(RequestOptions options) _handle;
  final List<RequestOptions> requests = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    return _handle(options);
  }

  @override
  void close({bool force = false}) {}
}

Dio _dioWith(_FakeAdapter adapter) => Dio(
      BaseOptions(
        baseUrl: 'http://test/api/v1',
        contentType: Headers.jsonContentType,
        responseType: ResponseType.json,
        validateStatus: (int? status) => status != null && status < 300,
      ),
    )..httpClientAdapter = adapter;

Map<String, dynamic> _user({
  String role = 'CUSTOMER',
  String status = 'ACTIVE',
}) =>
    <String, dynamic>{
      'publicId': 'pub-1',
      'fullName': 'Test User',
      'email': 'test@example.com',
      'phone': '01700000000',
      'role': role,
      'status': status,
    };

Map<String, dynamic> _tokens({
  String role = 'CUSTOMER',
  String status = 'ACTIVE',
  String access = 'access-1',
  String refresh = 'refresh-1',
}) =>
    <String, dynamic>{
      'accessToken': access,
      'refreshToken': refresh,
      'tokenType': 'Bearer',
      'expiresInSeconds': 900,
      'user': _user(role: role, status: status),
    };

Map<String, dynamic> _envelope(Map<String, dynamic>? data) =>
    <String, dynamic>{
      'success': true,
      'code': 'BDQM-0200',
      'message': 'OK',
      'data': data,
    };

/// Canned auth backend whose role/status and counters are configurable.
class _FakeBackend {
  _FakeBackend({
    this.role = 'CUSTOMER',
    this.status = 'ACTIVE',
    this.restoredStatus = 'ACTIVE',
  });

  String role;
  String status;
  String restoredStatus;
  int registerCalls = 0;
  int loginCalls = 0;
  int refreshCalls = 0;
  int logoutCalls = 0;

  ResponseBody handle(RequestOptions options) {
    switch (options.path) {
      case '/auth/register':
        registerCalls++;
        return _json(_envelope(_user(role: role, status: status)), 201);
      case '/auth/login':
        loginCalls++;
        return _json(_envelope(_tokens(role: role, status: status)), 200);
      case '/auth/refresh':
        refreshCalls++;
        return _json(
          _envelope(_tokens(
            role: role,
            status: restoredStatus,
            access: 'access-new',
            refresh: 'refresh-new',
          )),
          200,
        );
      case '/auth/logout':
        logoutCalls++;
        return _json(_envelope(null), 200);
      case '/auth/me':
        return _json(_envelope(_user(role: role, status: restoredStatus)), 200);
      default:
        return _json(_envelope(<String, dynamic>{'ok': true}), 200);
    }
  }
}

class _AuthGraph {
  _AuthGraph(this.auth, this.storage, this.backend);

  final AuthController auth;
  final InMemoryTokenStorage storage;
  final _FakeBackend backend;
}

_AuthGraph _graph({
  String allowedRole = 'CUSTOMER',
  bool pendingAllowed = false,
  String role = 'CUSTOMER',
  String status = 'ACTIVE',
  String restoredStatus = 'ACTIVE',
}) {
  final _FakeBackend backend = _FakeBackend(
    role: role,
    status: status,
    restoredStatus: restoredStatus,
  );
  final _FakeAdapter adapter = _FakeAdapter(backend.handle);
  final ServerConfig config = ServerConfig(InMemoryKeyValueStore());
  final AuthApi authApi = AuthApi(config, dio: _dioWith(adapter));
  final ApiClient apiClient = ApiClient(serverConfig: config, dio: _dioWith(adapter));
  final InMemoryTokenStorage storage = InMemoryTokenStorage();
  final AuthController auth = AuthController(
    authApi: authApi,
    apiClient: apiClient,
    tokenStorage: storage,
    allowedRole: allowedRole,
    pendingAllowed: pendingAllowed,
  );
  auth.attach();
  return _AuthGraph(auth, storage, backend);
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

void main() {
  group('Auth models', () {
    test('AuthUser.fromJson maps the safe public projection', () {
      final AuthUser user = AuthUser.fromJson(_user());
      expect(user.publicId, 'pub-1');
      expect(user.fullName, 'Test User');
      expect(user.role, 'CUSTOMER');
      expect(user.isActive, isTrue);
      expect(user.isPending, isFalse);
    });

    test('AuthResult.fromJson maps tokens and nested user', () {
      final AuthResult result = AuthResult.fromJson(_tokens());
      expect(result.accessToken, 'access-1');
      expect(result.refreshToken, 'refresh-1');
      expect(result.expiresInSeconds, 900);
      expect(result.user.email, 'test@example.com');
    });
  });

  group('ServerConfig', () {
    test('overrides, validates, and clears the base URL', () async {
      final InMemoryKeyValueStore store = InMemoryKeyValueStore();
      final ServerConfig config = ServerConfig(store);

      expect(config.baseUrl, config.defaultBaseUrl);
      await config.setBaseUrl('http://192.168.1.20:8080/api/v1/');
      expect(config.baseUrl, 'http://192.168.1.20:8080/api/v1');
      expect(config.isOverridden, isTrue);

      await expectLater(
        config.setBaseUrl('ftp://example.com'),
        throwsA(isA<ArgumentError>()),
      );

      await config.setBaseUrl('');
      expect(config.baseUrl, config.defaultBaseUrl);
      expect(config.isOverridden, isFalse);
    });

    test('load() restores a persisted override', () async {
      final InMemoryKeyValueStore store = InMemoryKeyValueStore();
      await store.write('bdqm.server.baseUrl', 'http://10.0.2.2:9090/api/v1');
      final ServerConfig config = ServerConfig(store);
      await config.load();
      expect(config.baseUrl, 'http://10.0.2.2:9090/api/v1');
    });
  });

  group('AuthController', () {
    test('bootstrap with no stored tokens is unauthenticated', () async {
      final _AuthGraph g = _graph();
      await g.auth.bootstrap();
      expect(g.auth.status, AuthStatus.unauthenticated);
      expect(g.auth.user, isNull);
      expect(g.backend.refreshCalls, 0);
    });

    test('login stores tokens and authenticates an active CUSTOMER', () async {
      final _AuthGraph g = _graph();
      await g.auth.login('test@example.com', 'password123');

      expect(g.auth.status, AuthStatus.authenticated);
      expect(g.auth.user?.email, 'test@example.com');
      final StoredTokens? stored = await g.storage.read();
      expect(stored, isNotNull);
      expect(stored!.accessToken, 'access-1');
      expect(g.backend.loginCalls, 1);
    });

    test('login rejects a non-CUSTOMER account and stores nothing', () async {
      final _AuthGraph g = _graph(role: 'RIDER');
      await expectLater(
        g.auth.login('rider@example.com', 'password123'),
        throwsA(isA<AuthException>()),
      );
      expect(g.auth.status, AuthStatus.unknown);
      expect(await g.storage.read(), isNull);
    });

    test('register then auto-login authenticates', () async {
      final _AuthGraph g = _graph();
      await g.auth.register(
        fullName: 'Test User',
        email: 'test@example.com',
        phone: '01700000000',
        password: 'password123',
      );
      expect(g.auth.status, AuthStatus.authenticated);
      expect(g.backend.registerCalls, 1);
      expect(g.backend.loginCalls, 1);
    });

    test('bootstrap restores a session via refresh rotation', () async {
      final _AuthGraph g = _graph();
      await g.storage.write(const StoredTokens(
        accessToken: 'old-access',
        refreshToken: 'old-refresh',
      ));
      await g.auth.bootstrap();

      expect(g.auth.status, AuthStatus.authenticated);
      expect(g.backend.refreshCalls, 1);
      final StoredTokens? stored = await g.storage.read();
      expect(stored!.accessToken, 'access-new');
      expect(stored.refreshToken, 'refresh-new');
    });

    test('logout revokes server tokens and clears local state', () async {
      final _AuthGraph g = _graph();
      await g.auth.login('test@example.com', 'password123');
      await g.auth.logout();

      expect(g.auth.status, AuthStatus.unauthenticated);
      expect(g.auth.user, isNull);
      expect(g.backend.logoutCalls, 1);
      expect(await g.storage.read(), isNull);
    });

    test('pending rider gets a pending session; approval promotes it', () async {
      final _AuthGraph g = _graph(
        allowedRole: 'RIDER',
        pendingAllowed: true,
        role: 'RIDER',
        status: 'PENDING',
        restoredStatus: 'PENDING',
      );
      await g.auth.login('rider@example.com', 'password123');
      expect(g.auth.status, AuthStatus.pending);

      g.backend.restoredStatus = 'ACTIVE';
      await g.auth.refreshSession();
      expect(g.auth.status, AuthStatus.authenticated);
      expect(g.backend.refreshCalls, 1);
    });
  });

  group('ApiClient silent refresh', () {
    test('concurrent 401s share one refresh and each retry once', () async {
      int refreshCount = 0;
      final _FakeAdapter adapter = _FakeAdapter((RequestOptions options) {
        if (options.path == '/protected') {
          if (options.extra['bdqm_retried'] == true) {
            return _json(_envelope(<String, dynamic>{'ok': true}), 200);
          }
          return _json(
            <String, dynamic>{'success': false, 'message': 'expired'},
            401,
          );
        }
        return _json(_envelope(null), 200);
      });

      final ServerConfig config = ServerConfig(InMemoryKeyValueStore());
      final ApiClient client =
          ApiClient(serverConfig: config, dio: _dioWith(adapter));
      client.setAuthToken('old-token');
      client.setRefreshHandler(() async {
        refreshCount++;
        client.setAuthToken('fresh-token');
        return 'fresh-token';
      });

      final List<Map<String, dynamic>> results = await Future.wait([
        client.get('/protected'),
        client.get('/protected'),
        client.get('/protected'),
      ]);

      expect(refreshCount, 1, reason: 'refresh must be single-flight');
      for (final Map<String, dynamic> result in results) {
        expect(result['ok'], isTrue);
      }
    });
  });
}
