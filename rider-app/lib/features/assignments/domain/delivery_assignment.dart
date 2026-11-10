/// Status of a delivery assignment offered to a rider.
///
/// This mirrors the backend order lifecycle but is scoped to what a rider
/// actually observes: an offer is accepted or rejected before it becomes an
/// active delivery.
enum AssignmentStatus {
  /// Offered to the rider, awaiting a response.
  offered,

  /// Rider accepted; delivery is now in progress.
  accepted,

  /// Rider declined the offer.
  rejected,

  /// Offer expired without a response.
  expired,

  /// Delivery completed and handed over.
  completed,
}

/// A delivery job offered to a rider.
class DeliveryAssignment {
  const DeliveryAssignment({
    required this.id,
    required this.orderId,
    required this.storeName,
    required this.pickupAddress,
    required this.dropOffAddress,
    required this.estimatedDistanceKm,
    required this.estimatedEtaMinutes,
    required this.estimatedPayAmount,
    required this.pickupLatitude,
    required this.pickupLongitude,
    required this.dropOffLatitude,
    required this.dropOffLongitude,
    this.status = AssignmentStatus.offered,
  });

  final String id;
  final String orderId;
  final String storeName;
  final String pickupAddress;
  final String dropOffAddress;
  final double estimatedDistanceKm;
  final int estimatedEtaMinutes;
  final double estimatedPayAmount;
  final double pickupLatitude;
  final double pickupLongitude;
  final double dropOffLatitude;
  final double dropOffLongitude;
  final AssignmentStatus status;

  bool get isActive => status == AssignmentStatus.accepted;

  DeliveryAssignment copyWith({AssignmentStatus? status}) {
    return DeliveryAssignment(
      id: id,
      orderId: orderId,
      storeName: storeName,
      pickupAddress: pickupAddress,
      dropOffAddress: dropOffAddress,
      estimatedDistanceKm: estimatedDistanceKm,
      estimatedEtaMinutes: estimatedEtaMinutes,
      estimatedPayAmount: estimatedPayAmount,
      pickupLatitude: pickupLatitude,
      pickupLongitude: pickupLongitude,
      dropOffLatitude: dropOffLatitude,
      dropOffLongitude: dropOffLongitude,
      status: status ?? this.status,
    );
  }
}
