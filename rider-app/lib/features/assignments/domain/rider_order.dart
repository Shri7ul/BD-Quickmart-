/// Rider-facing order model mirroring the backend OrderResponse.
class RiderOrderModel {
  RiderOrderModel({
    required this.publicId,
    required this.status,
    required this.totalAmount,
    required this.deliveryAddress,
    required this.contactPhone,
    required this.customerName,
    required this.notes,
    required this.items,
    required this.events,
    required this.createdAt,
    this.riderName,
  });

  final String publicId;
  final String status;
  final double totalAmount;
  final String deliveryAddress;
  final String contactPhone;
  final String customerName;
  final String? notes;
  final List<RiderOrderItem> items;
  final List<RiderOrderEvent> events;
  final DateTime createdAt;
  final String? riderName;

  bool get isActive => const {'ASSIGNED', 'PICKED_UP', 'OUT_FOR_DELIVERY'}.contains(status);

  factory RiderOrderModel.fromJson(Map<String, dynamic> json) => RiderOrderModel(
        publicId: json['publicId'] as String,
        status: json['status'] as String,
        totalAmount: (json['totalAmount'] as num).toDouble(),
        deliveryAddress: json['deliveryAddress'] as String,
        contactPhone: json['contactPhone'] as String,
        customerName: json['customerName'] as String? ?? 'Customer',
        notes: json['notes'] as String?,
        riderName: json['riderName'] as String?,
        items: (json['items'] as List<dynamic>? ?? [])
            .map((e) => RiderOrderItem.fromJson(e as Map<String, dynamic>))
            .toList(),
        events: (json['events'] as List<dynamic>? ?? [])
            .map((e) => RiderOrderEvent.fromJson(e as Map<String, dynamic>))
            .toList(),
        createdAt: DateTime.parse(json['createdAt'] as String),
      );
}

class RiderOrderItem {
  RiderOrderItem({required this.productName, required this.quantity, required this.lineTotal});

  final String productName;
  final int quantity;
  final double lineTotal;

  factory RiderOrderItem.fromJson(Map<String, dynamic> json) => RiderOrderItem(
        productName: json['productName'] as String,
        quantity: json['quantity'] as int,
        lineTotal: (json['lineTotal'] as num).toDouble(),
      );
}

class RiderOrderEvent {
  RiderOrderEvent({required this.status, required this.at, this.note});

  final String status;
  final DateTime at;
  final String? note;

  factory RiderOrderEvent.fromJson(Map<String, dynamic> json) => RiderOrderEvent(
        status: json['status'] as String,
        at: DateTime.parse(json['at'] as String),
        note: json['note'] as String?,
      );
}
