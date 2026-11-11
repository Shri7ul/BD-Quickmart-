/// Safe public projection of a backend user (Goal 02 `UserResponse`).
///
/// Never contains the password hash or any server-side secret.
class AuthUser {
  const AuthUser({
    required this.publicId,
    required this.fullName,
    required this.email,
    required this.phone,
    required this.role,
    required this.status,
  });

  final String publicId;
  final String fullName;
  final String email;
  final String phone;
  final String role;
  final String status;

  bool get isActive => status == 'ACTIVE';
  bool get isPending => status == 'PENDING';

  factory AuthUser.fromJson(Map<String, dynamic> json) => AuthUser(
        publicId: json['publicId']?.toString() ?? '',
        fullName: json['fullName']?.toString() ?? '',
        email: json['email']?.toString() ?? '',
        phone: json['phone']?.toString() ?? '',
        role: json['role']?.toString() ?? '',
        status: json['status']?.toString() ?? '',
      );

  Map<String, dynamic> toJson() => <String, dynamic>{
        'publicId': publicId,
        'fullName': fullName,
        'email': email,
        'phone': phone,
        'role': role,
        'status': status,
      };
}
