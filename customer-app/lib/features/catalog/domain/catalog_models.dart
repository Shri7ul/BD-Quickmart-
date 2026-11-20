/// Customer catalog models mirroring the backend ProductResponse/CategoryResponse.
class CategoryModel {
  CategoryModel({required this.id, required this.publicId, required this.name, this.description, this.imageUrl});

  final int id;
  final String publicId;
  final String name;
  final String? description;
  final String? imageUrl;

  factory CategoryModel.fromJson(Map<String, dynamic> json) => CategoryModel(
        id: json['id'] as int,
        publicId: json['publicId'] as String,
        name: json['name'] as String,
        description: json['description'] as String?,
        imageUrl: json['imageUrl'] as String?,
      );
}

class ProductModel {
  ProductModel({
    required this.publicId,
    required this.name,
    this.description,
    this.sku,
    this.categoryId,
    this.categoryName,
    required this.basePrice,
    this.discountPrice,
    required this.effectivePrice,
    this.imageUrl,
    required this.available,
    required this.stockQuantity,
    required this.inStock,
  });

  final String publicId;
  final String name;
  final String? description;
  final String? sku;
  final int? categoryId;
  final String? categoryName;
  final double basePrice;
  final double? discountPrice;
  final double effectivePrice;
  final String? imageUrl;
  final bool available;
  final int stockQuantity;
  final bool inStock;

  factory ProductModel.fromJson(Map<String, dynamic> json) => ProductModel(
        publicId: json['publicId'] as String,
        name: json['name'] as String,
        description: json['description'] as String?,
        sku: json['sku'] as String?,
        categoryId: json['categoryId'] as int?,
        categoryName: json['categoryName'] as String?,
        basePrice: (json['basePrice'] as num).toDouble(),
        discountPrice: json['discountPrice'] == null
            ? null
            : (json['discountPrice'] as num).toDouble(),
        effectivePrice: (json['effectivePrice'] as num).toDouble(),
        imageUrl: json['imageUrl'] as String?,
        available: json['available'] as bool? ?? false,
        stockQuantity: json['stockQuantity'] as int? ?? 0,
        inStock: json['inStock'] as bool? ?? false,
      );
}
