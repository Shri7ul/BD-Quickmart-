/// Availability state of a rider, used by the backend's assignment engine.
///
/// Only riders in the [online] state are eligible for new assignments.
enum RiderStatus {
  /// Signed in but not taking jobs.
  offline,

  /// Available for assignment.
  online,

  /// Currently handling a delivery.
  busy,

  /// Shift ended.
  offShift,
}

/// A geographic position reported by the rider device.
class RiderLocation {
  const RiderLocation({
    required this.latitude,
    required this.longitude,
    required this.recordedAt,
    this.accuracyMeters = 0,
    this.speedMetersPerSecond = 0,
  });

  final double latitude;
  final double longitude;
  final DateTime recordedAt;
  final double accuracyMeters;
  final double speedMetersPerSecond;

  /// Whether this fix is recent enough to be treated as live.
  ///
  /// The backend must show a stale marker rather than a fake live position
  /// when the rider's uplink drops.
  bool isFresh({Duration maxAge = const Duration(seconds: 30)}) {
    return DateTime.now().difference(recordedAt) <= maxAge;
  }
}
