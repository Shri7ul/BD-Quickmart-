import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../../../core/storage/key_value_store.dart';
import '../../catalog/domain/catalog_models.dart';

class CartLine {
  CartLine({
    required this.product,
    required this.quantity,
  });

  final ProductModel product;
  int quantity;

  double get lineTotal => product.effectivePrice * quantity;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'product': <String, dynamic>{
          'publicId': product.publicId,
          'name': product.name,
          'description': product.description,
          'sku': product.sku,
          'categoryId': product.categoryId,
          'categoryName': product.categoryName,
          'basePrice': product.basePrice,
          'discountPrice': product.discountPrice,
          'effectivePrice': product.effectivePrice,
          'imageUrl': product.imageUrl,
          'available': product.available,
          'stockQuantity': product.stockQuantity,
          'inStock': product.inStock,
        },
        'quantity': quantity,
      };

  factory CartLine.fromJson(Map<String, dynamic> json) => CartLine(
        product: ProductModel.fromJson(json['product'] as Map<String, dynamic>),
        quantity: json['quantity'] as int,
      );
}

/// Persistent cart state. Prices are snapshots; checkout revalidates against
/// the backend before an order is created.
class CartController extends ChangeNotifier {
  static const String _storageKey = 'bdqm.cart.v1';
  static const double deliveryFee = 40.0;
  static const double freeDeliveryThreshold = 1000.0;

  CartController(this._store);

  final KeyValueStore _store;
  final List<CartLine> _lines = <CartLine>[];

  List<CartLine> get lines => List.unmodifiable(_lines);
  int get itemCount => _lines.fold(0, (sum, line) => sum + line.quantity);
  double get subtotal => _lines.fold(0.0, (sum, line) => sum + line.lineTotal);
  double get discount => _lines.fold(0.0, (sum, line) {
        final base = line.product.basePrice - line.product.effectivePrice;
        return sum + (base > 0 ? base * line.quantity : 0.0);
      });
  double get delivery => subtotal >= freeDeliveryThreshold ? 0.0 : (subtotal == 0 ? 0.0 : deliveryFee);
  double get total => subtotal - discount + delivery;

  Future<void> restore() async {
    final raw = await _store.read(_storageKey);
    if (raw == null || raw.isEmpty) return;
    try {
      final decoded = jsonDecode(raw) as List<dynamic>;
      _lines
        ..clear()
        ..addAll(decoded
            .map((e) => CartLine.fromJson(e as Map<String, dynamic>))
            .where((line) => line.quantity > 0));
      notifyListeners();
    } on Object {
      await clear();
    }
  }

  Future<void> _persist() async {
    await _store.write(_storageKey, jsonEncode(_lines.map((line) => line.toJson()).toList()));
  }

  Future<void> add(ProductModel product, {int quantity = 1}) async {
    if (!product.inStock) return;
    final existing = _lines.where((line) => line.product.publicId == product.publicId).toList();
    if (existing.isNotEmpty) {
      final line = existing.first;
      line.quantity = _clamp(line.quantity + quantity, line.product.stockQuantity);
    } else {
      _lines.add(CartLine(product: product, quantity: _clamp(quantity, product.stockQuantity)));
    }
    await _persist();
    notifyListeners();
  }

  Future<void> setQuantity(String productPublicId, int quantity) async {
    final line = _lines.where((line) => line.product.publicId == productPublicId).toList();
    if (line.isEmpty) return;
    if (quantity <= 0) {
      await remove(productPublicId);
      return;
    }
    line.first.quantity = _clamp(quantity, line.first.product.stockQuantity);
    await _persist();
    notifyListeners();
  }

  Future<void> remove(String productPublicId) async {
    _lines.removeWhere((line) => line.product.publicId == productPublicId);
    await _persist();
    notifyListeners();
  }

  Future<void> clear() async {
    _lines.clear();
    await _store.remove(_storageKey);
    notifyListeners();
  }

  /// Revalidates lines against the latest backend catalogue before checkout.
  /// Returns a problem description, or null when everything is still valid.
  String? revalidate(List<ProductModel> fresh) {
    final byId = <String, ProductModel>{
      for (final product in fresh) product.publicId: product,
    };
    for (final line in _lines) {
      final current = byId[line.product.publicId];
      if (current == null || !current.inStock) {
        return '${line.product.name} is no longer available.';
      }
      if (current.effectivePrice != line.product.effectivePrice) {
        return 'The price of ${line.product.name} changed. Please review your cart.';
      }
      if (line.quantity > current.stockQuantity) {
        return 'Only ${current.stockQuantity} units of ${line.product.name} remain.';
      }
    }
    return null;
  }

  List<Map<String, dynamic>> checkoutItems() => _lines
      .map((line) => <String, dynamic>{
            'productPublicId': line.product.publicId,
            'quantity': line.quantity,
          })
      .toList();

  int _clamp(int quantity, int stock) {
    if (quantity < 1) return 1;
    if (stock > 0 && quantity > stock) return stock;
    return quantity;
  }
}
