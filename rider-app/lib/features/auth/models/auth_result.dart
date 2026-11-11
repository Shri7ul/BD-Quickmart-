import 'auth_user.dart';

/// Token pair returned by POST /auth/login and POST /auth/refresh, including
/// the authenticated user so a single round-trip can restore a session.
class AuthResult {
  const AuthResult({
    required this.accessToken,
    required this.refreshToken,
    required this.tokenType,
    required this.expiresInSeconds,
    required this.user,
  });

  final String accessToken;
  final String refreshToken;
  final String tokenType;
  final int expiresInSeconds;
  final AuthUser user;

  factory AuthResult.fromJson(Map<String, dynamic> json) {
    final dynamic rawUser = json['user'];
    return AuthResult(
      accessToken: json['accessToken']?.toString() ?? '',
      refreshToken: json['refreshToken']?.toString() ?? '',
      tokenType: json['tokenType']?.toString() ?? 'Bearer',
      expiresInSeconds: (json['expiresInSeconds'] as num?)?.toInt() ?? 0,
      user: AuthUser.fromJson(
        rawUser is Map<String, dynamic>
            ? rawUser
            : const <String, dynamic>{},
      ),
    );
  }
}
