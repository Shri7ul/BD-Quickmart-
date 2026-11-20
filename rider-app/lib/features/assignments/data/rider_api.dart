import '../../../core/network/api_client.dart';
import '../domain/rider_order.dart';

/// Rider delivery API. All availability and status changes persist server-side.
class RiderApi {
  RiderApi(this._api);

  final ApiClient _api;

  Future<void> setAvailability(bool online) async {
    await _api.post('/rider/availability', data: {'online': online});
  }

  Future<List<RiderOrderModel>> assignedOrders() async {
    final body = await _api.get('/rider/orders/assigned');
    final list = (body['data'] as List<dynamic>?) ?? const <dynamic>[];
    return list
        .map((e) => RiderOrderModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<RiderOrderModel> order(String publicId) async {
    final body = await _api.get('/rider/orders/$publicId');
    return RiderOrderModel.fromJson(body);
  }

  Future<RiderOrderModel> setStatus(String publicId, String status, {String? note}) async {
    final body = await _api.post('/rider/orders/$publicId/status',
        data: {'status': status, 'note': note});
    return RiderOrderModel.fromJson(body);
  }
}
