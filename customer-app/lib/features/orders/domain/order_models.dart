/// Order models mirroring the backend OrderResponse.
class OrderItemModel {
  OrderItemModel({
    required this.productPublicId,
    required this.productName,
    required this.unitPrice,
    required this.quantity,
    required this.lineTotal,
  });

  final String productPublicId;
  final String productName;
  final double unitPrice;
  final int quantity;
  final double lineTotal;

  factory OrderItemModel.fromJson(Map<String, dynamic> json) => OrderItemModel(
        productPublicId: json['productPublicId'] as String,
        productName: json['productName'] as String,
        unitPrice: (json['unitPrice'] as num).toDouble(),
        quantity: json['quantity'] as int,
        lineTotal: (json['lineTotal'] as num).toDouble(),
      );
}

class OrderEventModel {
  OrderEventModel({required this.status, required this.at, this.note});

  final String status;
  final DateTime at;
  final String? note;

  factory OrderEventModel.fromJson(Map<String, dynamic> json) => OrderEventModel(
        status: json['status'] as String,
        at: DateTime.parse(json['at'] as String),
        note: json['note'] as String?,
      );
}

class OrderModel {
  OrderModel({
    required this.publicId,
    required this.status,
    required this.subtotalAmount,
    required this.discountAmount,
    required this.deliveryFee,
    required this.totalAmount,
    required this.paymentMethod,
    required this.deliveryAddress,
    required this.contactPhone,
    this.notes,
    required this.customerName,
    this.riderName,
    required this.items,
    required this.events,
    required this.createdAt,
    required this.updatedAt,
  });

  final String publicId;
  final String status;
  final double subtotalAmount;
  final double discountAmount;
  final double deliveryFee;
  final double totalAmount;
  final String paymentMethod;
  final String deliveryAddress;
  final String contactPhone;
  final String? notes;
  final String customerName;
  final String? riderName;
  final List<OrderItemModel> items;
  final List<OrderEventModel> events;
  final DateTime createdAt;
  final DateTime updatedAt;

  bool get isDelivered => status == 'DELIVERED';

  factory OrderModel.fromJson(Map<String, dynamic> json) => OrderModel(
        publicId: json['publicId'] as String,
        status: json['status'] as String,
        subtotalAmount: (json['subtotalAmount'] as num).toDouble(),
        discountAmount: (json['discountAmount'] as num).toDouble(),
        deliveryFee: (json['deliveryFee'] as num).toDouble(),
        totalAmount: (json['totalAmount'] as num).toDouble(),
        paymentMethod: json['paymentMethod'] as String,
        deliveryAddress: json['deliveryAddress'] as String,
        contactPhone: json['contactPhone'] as String,
        notes: json['notes'] as String?,
        customerName: json['customerName'] as String? ?? 'Customer',
        riderName: json['riderName'] as String?,
        items: (json['items'] as List<dynamic>? ?? [])
            .map((e) => OrderItemModel.fromJson(e as Map<String, dynamic>))
            .toList(),
        events: (json['events'] as List<dynamic>? ?? [])
            .map((e) => OrderEventModel.fromJson(e as Map<String, dynamic>))
            .toList(),
        createdAt: DateTime.parse(json['createdAt'] as String),
        updatedAt: DateTime.parse(json['updatedAt'] as String),
      );
}
