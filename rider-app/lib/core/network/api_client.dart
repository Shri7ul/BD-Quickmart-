import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../config/env.dart';
import '../config/server_config.dart';
import 'api_exception.dart';

export 'api_exception.dart' show ApiException;

/// Exchanges a stored refresh token for a fresh access token. Returns the new
/// access token, or null when the session cannot be restored.
typedef TokenRefresher = Future<String?> Function();

/// Centralized HTTP client for authenticated backend calls.
///
/// - Reads the current base URL from [ServerConfig] on every request, so a
///   runtime server override takes effect immediately.
/// - Injects the in-memory access token as a bearer header.
/// - On a 401, performs a single-flight silent refresh ([TokenRefresher]) and
///   retries the failed request exactly once. Concurrent 401s share the same
///   refresh future.
///
/// Public auth endpoints (login/register/refresh) intentionally use a separate
/// client (see `AuthApi`) so a 401 there can never trigger a refresh loop.
class ApiClient {
  ApiClient({required ServerConfig serverConfig, Dio? dio})
      : _serverConfig = serverConfig,
        _dio = dio ?? _buildDio() {
    // A bare Dio (no interceptors) is used for the single retry. Reusing the
    // main interceptor chain from inside an onError callback would re-enter the
    // queued interceptor; a separate client keeps the retry deadlock-free.
    _retryDio = Dio(_dio.options.copyWith(baseUrl: _serverConfig.baseUrl));
    // Reuse the adapter so an injected fake adapter (tests) or a configured
    // adapter (production) also serves the single retry request.
    _retryDio.httpClientAdapter = _dio.httpClientAdapter;
    _dio.interceptors.add(
      QueuedInterceptorsWrapper(
        onRequest: _onRequest,
        onResponse: _onResponse,
        onError: _onError,
      ),
    );
  }

  static const String _retriedKey = 'bdqm_retried';

  final ServerConfig _serverConfig;
  final Dio _dio;
  late final Dio _retryDio;

  String? _accessToken;
  TokenRefresher? _refresher;
  void Function()? _onAuthFailure;
  Future<String?>? _refreshFuture;

  static Dio _buildDio() => Dio(
        BaseOptions(
          connectTimeout: AppConfig.requestTimeout,
          receiveTimeout: AppConfig.requestTimeout,
          sendTimeout: AppConfig.requestTimeout,
          contentType: Headers.jsonContentType,
          responseType: ResponseType.json,
          validateStatus: (int? status) => status != null && status < 300,
        ),
      );

  /// Attaches the bearer token used by subsequent requests.
  void setAuthToken(String token) {
    _accessToken = token;
  }

  /// Clears any attached credentials.
  void clearAuthToken() {
    _accessToken = null;
  }

  /// Registers the silent-refresh handler.
  void setRefreshHandler(TokenRefresher? handler) {
    _refresher = handler;
  }

  /// Registers a callback invoked when the session cannot be restored.
  void setOnAuthFailure(void Function()? handler) {
    _onAuthFailure = handler;
  }

  void _onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    options.baseUrl = _serverConfig.baseUrl;
    final String? token = _accessToken;
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    AppLogger.v('→ ${options.method} ${options.uri}');
    handler.next(options);
  }

  void _onResponse(
    Response<dynamic> response,
    ResponseInterceptorHandler handler,
  ) {
    AppLogger.v('← ${response.statusCode} ${response.requestOptions.uri}');
    handler.next(response);
  }

  Future<void> _onError(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final RequestOptions options = error.requestOptions;
    final bool unauthorized = error.response?.statusCode == 401;

    // Never refresh for public auth routes, after a retry, or without a handler.
    if (!unauthorized ||
        _refresher == null ||
        options.extra[_retriedKey] == true ||
        options.path.startsWith('/auth/')) {
      handler.next(error);
      return;
    }

    final String? sentToken = options.headers['Authorization'] as String?;
    final String? currentToken =
        _accessToken == null ? null : 'Bearer $_accessToken';

    String? newToken;
    if (currentToken != null && currentToken != sentToken) {
      // A preceding queued request already refreshed the token while this
      // error waited in the queue; reuse it instead of refreshing again.
      newToken = _accessToken;
    } else {
      newToken = await _runSingleFlightRefresh();
    }

    if (newToken == null) {
      _onAuthFailure?.call();
      handler.next(error);
      return;
    }

    options.extra[_retriedKey] = true;
    options.headers['Authorization'] = 'Bearer $newToken';
    try {
      final Response<dynamic> response =
          await _retryDio.fetch<dynamic>(options);
      handler.resolve(response);
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  /// Only one refresh call runs at a time; every concurrent 401 awaits it.
  Future<String?> _runSingleFlightRefresh() {
    final Future<String?>? existing = _refreshFuture;
    if (existing != null) {
      return existing;
    }
    final Future<String?> future =
        _refresher!().whenComplete(() => _refreshFuture = null);
    _refreshFuture = future;
    return future;
  }

  Future<Map<String, dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    final Response<Map<String, dynamic>> response = await _request(
      () => _dio.get<Map<String, dynamic>>(
        path,
        queryParameters: queryParameters,
      ),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> post(String path, {Object? data}) async {
    final Response<Map<String, dynamic>> response = await _request(
      () => _dio.post<Map<String, dynamic>>(path, data: data),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> put(String path, {Object? data}) async {
    final Response<Map<String, dynamic>> response = await _request(
      () => _dio.put<Map<String, dynamic>>(path, data: data),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> patch(String path, {Object? data}) async {
    final Response<Map<String, dynamic>> response = await _request(
      () => _dio.patch<Map<String, dynamic>>(path, data: data),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> delete(String path) async {
    final Response<Map<String, dynamic>> response = await _request(
      () => _dio.delete<Map<String, dynamic>>(path),
    );
    return _unwrap(response);
  }

  Future<Response<Map<String, dynamic>>> _request(
    Future<Response<Map<String, dynamic>>> Function() request,
  ) async {
    try {
      return await request();
    } on DioException catch (error) {
      throw apiExceptionFromDio(error);
    }
  }

  /// Extracts the payload from the standard `ApiResponse` envelope.
  Map<String, dynamic> _unwrap(Response<Map<String, dynamic>> response) {
    final Map<String, dynamic>? body = response.data;
    if (body == null) {
      return const <String, dynamic>{};
    }
    final dynamic data = body['data'];
    if (data is Map<String, dynamic>) {
      return data;
    }
    return body;
  }
}
