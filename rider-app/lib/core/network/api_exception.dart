import 'package:dio/dio.dart';

/// A backend/transport failure surfaced to the UI layer.
class ApiException implements Exception {
  ApiException(this.message, {this.statusCode, this.code});

  final String message;
  final int? statusCode;
  final String? code;

  @override
  String toString() => message;
}

/// Translates a [DioException] into an [ApiException], preferring the backend's
/// human-readable `message`/`code` from the standard error envelope.
ApiException apiExceptionFromDio(DioException error) {
  final Response<dynamic>? response = error.response;
  if (response != null) {
    final dynamic body = response.data;
    final String message = body is Map
        ? (body['message']?.toString() ?? 'Request failed')
        : 'Request failed';
    final String? code = body is Map ? body['code']?.toString() : null;
    return ApiException(message, statusCode: response.statusCode, code: code);
  }

  final bool timedOut = error.type == DioExceptionType.connectionTimeout ||
      error.type == DioExceptionType.receiveTimeout ||
      error.type == DioExceptionType.sendTimeout;
  return ApiException(
    timedOut ? 'Request timed out' : 'Unable to reach the server',
    code: error.type.name,
  );
}
