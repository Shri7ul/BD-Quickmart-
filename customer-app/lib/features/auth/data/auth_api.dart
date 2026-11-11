import 'package:dio/dio.dart';

import '../../../core/config/app_config.dart';
import '../../../core/config/server_config.dart';
import '../../../core/network/api_exception.dart';
import '../models/auth_result.dart';
import '../models/auth_user.dart';

/// Unauthenticated auth calls.
///
/// Uses a bare [Dio] with NO auth/refresh interceptors, so a 401 from
/// /auth/login, /auth/register or /auth/refresh can never trigger a token
/// refresh loop. The base URL is taken from [ServerConfig] on each request.
class AuthApi {
  AuthApi(this._serverConfig, {Dio? dio})
      : _dio = dio ??
            Dio(
              BaseOptions(
                connectTimeout: AppConfig.requestTimeout,
                receiveTimeout: AppConfig.requestTimeout,
                sendTimeout: AppConfig.requestTimeout,
                contentType: Headers.jsonContentType,
                responseType: ResponseType.json,
                validateStatus: (int? status) => status != null && status < 300,
              ),
            ) {
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (RequestOptions options, RequestInterceptorHandler handler) {
          options.baseUrl = _serverConfig.baseUrl;
          handler.next(options);
        },
      ),
    );
  }

  final ServerConfig _serverConfig;
  final Dio _dio;

  /// Public self-registration. Returns the created user (no tokens yet).
  Future<AuthUser> register({
    required String fullName,
    required String email,
    required String phone,
    required String password,
    required String role,
  }) async {
    final Map<String, dynamic> data = await _post(
      '/auth/register',
      <String, dynamic>{
        'fullName': fullName,
        'email': email,
        'phone': phone,
        'password': password,
        'role': role,
      },
    );
    return AuthUser.fromJson(data);
  }

  /// Login with an email address or phone number as [identifier].
  Future<AuthResult> login(String identifier, String password) async {
    final Map<String, dynamic> data = await _post(
      '/auth/login',
      <String, dynamic>{'identifier': identifier, 'password': password},
    );
    return AuthResult.fromJson(data);
  }

  /// Rotates a refresh token. Returns the new token pair and the user.
  Future<AuthResult> refresh(String refreshToken) async {
    final Map<String, dynamic> data = await _post(
      '/auth/refresh',
      <String, dynamic>{'refreshToken': refreshToken},
    );
    return AuthResult.fromJson(data);
  }

  /// Fetches the current user with an explicit access token.
  Future<AuthUser> me(String accessToken) async {
    try {
      final Response<Map<String, dynamic>> response =
          await _dio.get<Map<String, dynamic>>(
        '/auth/me',
        options: Options(
          headers: <String, dynamic>{'Authorization': 'Bearer $accessToken'},
        ),
      );
      return AuthUser.fromJson(_unwrap(response.data));
    } on DioException catch (error) {
      throw apiExceptionFromDio(error);
    }
  }

  Future<Map<String, dynamic>> _post(
    String path,
    Map<String, dynamic> body,
  ) async {
    try {
      final Response<Map<String, dynamic>> response =
          await _dio.post<Map<String, dynamic>>(path, data: body);
      return _unwrap(response.data);
    } on DioException catch (error) {
      throw apiExceptionFromDio(error);
    }
  }

  Map<String, dynamic> _unwrap(Map<String, dynamic>? body) {
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
