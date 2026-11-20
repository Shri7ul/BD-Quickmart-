import 'dart:convert';
import 'dart:typed_data';

import 'package:customer_app/core/config/server_config.dart';
import 'package:customer_app/core/network/api_client.dart';
import 'package:customer_app/core/storage/key_value_store.dart';
import 'package:customer_app/features/cart/state/cart_controller.dart';
import 'package:customer_app/features/catalog/data/catalog_api.dart';
import 'package:customer_app/features/catalog/domain/catalog_models.dart';
import 'package:customer_app/features/home/presentation/home_screen.dart';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

/// Regression coverage for: an admin-created product reaching the customer app
/// and remaining visible (with an "Out of stock" state) until stock is added.

ResponseBody _json(Map<String, dynamic> body) => ResponseBody.fromString(
      jsonEncode(body),
      200,
      headers: <String, List<String>>{'content-type': <String>['application/json']},
    );

class _FakeAdapter implements HttpClientAdapter {
  _FakeAdapter(this._handle);

  final ResponseBody Function(RequestOptions options) _handle;

  @override
  Future<ResponseBody> fetch(RequestOptions options, Stream<Uint8List>? requestStream,
      Future<void>? cancelFuture) async => _handle(options);

  @override
  void close({bool force = false}) {}
}

ApiClient _apiClient(_FakeAdapter adapter) {
  final ServerConfig config = ServerConfig(InMemoryKeyValueStore());
  final Dio dio = Dio(BaseOptions(
    baseUrl: 'http://test/api/v1',
    contentType: Headers.jsonContentType,
    responseType: ResponseType.json,
    validateStatus: (int? status) => status != null && status < 300,
  ))..httpClientAdapter = adapter;
  return ApiClient(serverConfig: config, dio: dio);
}

Map<String, dynamic> _product({
  String name = 'Tomato',
  int stock = 0,
}) =>
    <String, dynamic>{
      'publicId': 'prod-1',
      'name': name,
      'description': 'Fresh tomatoes',
      'sku': 'TOM-1',
      'categoryId': 1,
      'categoryName': 'Vegetables',
      'basePrice': 40.0,
      'discountPrice': null,
      'effectivePrice': 40.0,
      'imageUrl': null,
      'available': true,
      'stockQuantity': stock,
      'inStock': stock > 0,
    };

ResponseBody _handle(RequestOptions options, {required String name, required int stock}) {
  final String path = options.path;
  if (path.endsWith('/customer/categories')) {
    return _json(<String, dynamic>{
      'data': <Map<String, dynamic>>[
        <String, dynamic>{'id': 1, 'publicId': 'cat-1', 'name': 'Vegetables'}
      ],
    });
  }
  return _json(<String, dynamic>{
    'data': <Map<String, dynamic>>[_product(name: name, stock: stock)],
  });
}

void main() {
  test('ProductModel parses the out-of-stock projection returned by the API', () {
    final ProductModel product = ProductModel.fromJson(_product(stock: 0));

    expect(product.publicId, 'prod-1');
    expect(product.name, 'Tomato');
    expect(product.available, isTrue);
    expect(product.stockQuantity, 0);
    expect(product.inStock, isFalse);
    expect(product.effectivePrice, 40.0);
  });

  test('CatalogApi returns a newly created zero-stock product from the envelope', () async {
    final ApiClient api = _apiClient(_FakeAdapter((RequestOptions options) =>
        _handle(options, name: 'Tomato', stock: 0)));

    final List<ProductModel> products = await CatalogApi(api).products();

    expect(products, hasLength(1));
    expect(products.first.name, 'Tomato');
    expect(products.first.inStock, isFalse);
  });

  testWidgets('HomeScreen displays the admin-created product with an Out of stock state',
      (WidgetTester tester) async {
    final ApiClient api = _apiClient(_FakeAdapter((RequestOptions options) =>
        _handle(options, name: 'Tomato', stock: 0)));
    final CartController cart = CartController(InMemoryKeyValueStore());

    await tester.pumpWidget(MaterialApp(
      home: MultiProvider(
        providers: [
          Provider<ApiClient>.value(value: api),
          ChangeNotifierProvider<CartController>.value(value: cart),
        ],
        child: const HomeScreen(),
      ),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Tomato'), findsWidgets);
    expect(find.text('Out of stock'), findsOneWidget);
  });

  testWidgets('Pull-to-refresh re-fetches updated product data', (WidgetTester tester) async {
    int productCalls = 0;
    final ApiClient api = _apiClient(_FakeAdapter((RequestOptions options) {
      if (options.path.endsWith('/customer/categories')) {
        return _handle(options, name: 'Tomato', stock: 0);
      }
      productCalls++;
      return _handle(
        options,
        name: productCalls == 1 ? 'Tomato' : 'Tomato fresh batch',
        stock: productCalls == 1 ? 0 : 12,
      );
    }));
    final CartController cart = CartController(InMemoryKeyValueStore());

    await tester.pumpWidget(MaterialApp(
      home: MultiProvider(
        providers: [
          Provider<ApiClient>.value(value: api),
          ChangeNotifierProvider<CartController>.value(value: cart),
        ],
        child: const HomeScreen(),
      ),
    ));
    await tester.pumpAndSettle();
    expect(find.text('Out of stock'), findsOneWidget);

    await tester.drag(find.byType(RefreshIndicator), const Offset(0, 300));
    await tester.pumpAndSettle();

    expect(productCalls, greaterThanOrEqualTo(2));
    expect(find.text('Tomato fresh batch'), findsWidgets);
    expect(find.text('Out of stock'), findsNothing);
    expect(find.text('Add'), findsWidgets);
  });
}
