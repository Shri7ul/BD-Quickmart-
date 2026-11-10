import 'package:dio/dio.dart';

import '../config/app_config.dart';
import '../config/env.dart';

/// Thrown when the backend returns a non-success status.
class ApiException implements Exception {
  ApiException(this.message, {this.statusCode, this.code});

  final String message;
  final int? statusCode;
  final String? code;

  @override
  String toString() => message;
}

/// Centralized HTTP client for the rider application.
///
/// Shares the same envelope contract as the customer client, but exposes
/// rider-scoped endpoints and keeps the routing/assignment vocabulary in the
/// call sites rather than in the transport layer.
class ApiClient {
  ApiClient({Dio? dio}) : _dio = dio ?? _buildDio();

  final Dio _dio;

  static Dio _buildDio() {
    final dio = Dio(
      BaseOptions(
        baseUrl: AppConfig.apiBaseUrl,
        connectTimeout: AppConfig.requestTimeout,
        receiveTimeout: AppConfig.requestTimeout,
        sendTimeout: AppConfig.requestTimeout,
        contentType: Headers.jsonContentType,
        responseType: ResponseType.json,
        validateStatus: (status) => status != null && status < 300,
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          AppLogger.v('→ ${options.method} ${options.path}');
          handler.next(options);
        },
        onResponse: (response, handler) {
          AppLogger.v('← ${response.statusCode} ${response.requestOptions.path}');
          handler.next(response);
        },
        onError: (error, handler) {
          AppLogger.e('Network error on ${error.requestOptions.path}', error);
          handler.next(error);
        },
      ),
    );

    return dio;
  }

  /// Attaches the bearer token to subsequent requests.
  void setAuthToken(String token) {
    _dio.options.headers['Authorization'] = 'Bearer $token';
  }

  /// Clears any attached credentials.
  void clearAuthToken() {
    _dio.options.headers.remove('Authorization');
  }

  Future<Map<String, dynamic>> get(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    final response = await _guard(
      () => _dio.get<Map<String, dynamic>>(
        path,
        queryParameters: queryParameters,
      ),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> post(String path, {Object? data}) async {
    final response = await _guard(
      () => _dio.post<Map<String, dynamic>>(path, data: data),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> put(String path, {Object? data}) async {
    final response = await _guard(
      () => _dio.put<Map<String, dynamic>>(path, data: data),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> patch(String path, {Object? data}) async {
    final response = await _guard(
      () => _dio.patch<Map<String, dynamic>>(path, data: data),
    );
    return _unwrap(response);
  }

  Future<Map<String, dynamic>> delete(String path) async {
    final response = await _guard(() => _dio.delete<Map<String, dynamic>>(path));
    return _unwrap(response);
  }

  /// Runs a request and converts transport failures into [ApiException].
  Future<Response<Map<String, dynamic>>> _guard(
    Future<Response<Map<String, dynamic>>> Function() request,
  ) async {
    try {
      return await request();
    } on DioException catch (error) {
      final response = error.response;
      if (response != null) {
        final body = response.data;
        final message = body is Map<String, dynamic>
            ? (body['message']?.toString() ?? 'Request failed')
            : 'Request failed';
        throw ApiException(
          message,
          statusCode: response.statusCode,
          code: body is Map<String, dynamic> ? body['code']?.toString() : null,
        );
      }
      throw ApiException(
        error.type == DioExceptionType.connectionTimeout ||
                error.type == DioExceptionType.receiveTimeout ||
                error.type == DioExceptionType.sendTimeout
            ? 'Request timed out'
            : 'Unable to reach the server',
        code: error.type.name,
      );
    }
  }

  /// Extracts the payload from the standard `ApiResponse` envelope.
  Map<String, dynamic> _unwrap(Response<Map<String, dynamic>> response) {
    final body = response.data;
    if (body == null) return const {};
    final data = body['data'];
    if (data is Map<String, dynamic>) return data;
    return body;
  }
}
