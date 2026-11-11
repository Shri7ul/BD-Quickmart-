/// A user-facing authentication error (wrong role, pending/disabled account,
/// or any message the sign-in/register screen should display verbatim).
class AuthException implements Exception {
  AuthException(this.message);

  final String message;

  @override
  String toString() => message;
}
