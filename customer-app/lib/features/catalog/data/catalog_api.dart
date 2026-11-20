import '../../../core/network/api_client.dart';
import '../domain/catalog_models.dart';

/// Customer catalog API. All data comes from the live backend.
class CatalogApi {
  CatalogApi(this._api);

  final ApiClient _api;

  Future<List<CategoryModel>> categories() async {
    final body = await _api.get('/customer/categories');
    final list = (body['data'] as List<dynamic>?) ?? const <dynamic>[];
    return list
        .map((e) => CategoryModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<ProductModel>> products({String? search, int? categoryId}) async {
    final query = <String, dynamic>{
      if (search != null && search.isNotEmpty) 'search': search,
      if (categoryId != null) 'categoryId': categoryId,
    };
    final body = await _api.get('/customer/products', queryParameters: query);
    final list = (body['data'] as List<dynamic>?) ?? const <dynamic>[];
    return list
        .map((e) => ProductModel.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ProductModel> product(String publicId) async {
    final body = await _api.get('/customer/products/$publicId');
    return ProductModel.fromJson(body);
  }
}
