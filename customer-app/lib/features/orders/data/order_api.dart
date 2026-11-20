import 'dart:math';

import '../../../core/network/api_client.dart';
import '../domain/order_models.dart';

/// Customer checkout and order tracking API.
class OrderApi {
  OrderApi(this._api);

  final ApiClient _api;

  Future<OrderModel> checkout({
    required String idempotencyKey,
    required List<Map<String, dynamic>> items,
    required String deliveryAddress,
    required String contactPhone,
    String? notes,
  }) async {
    final body = await _api.post('/customer/checkout', data: {
      'idempotencyKey': idempotencyKey,
      'items': items,
      'deliveryAddress': deliveryAddress,
      'contactPhone': contactPhone,
      'notes': notes,
      'paymentMethod': 'COD',
    });
    return OrderModel.fromJson(body);
  }

  Future<List<OrderModel>> orders() async {
    final body = await _api.get('/customer/orders');
    final list = (body['data'] as List<dynamic>?) ?? const <dynamic>[];
    return list.map((e) => OrderModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<OrderModel> order(String publicId) async {
    final body = await _api.get('/customer/orders/$publicId');
    return OrderModel.fromJson(body);
  }
}

/// Generates a collision-resistant idempotency key (UUID v4 style).
String newIdempotencyKey() {
  final random = Random();
  String hex(int length) => List<int>.generate(length, (_) => random.nextInt(16))
      .map((n) => n.toRadixString(16)).join();
  return '${hex(8)}-${hex(4)}-4${hex(3)}-a${hex(3)}-${hex(12)}';
}
