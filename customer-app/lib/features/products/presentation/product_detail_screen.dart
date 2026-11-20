import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../cart/state/cart_controller.dart';
import '../../catalog/data/catalog_api.dart';
import '../../catalog/domain/catalog_models.dart';

/// Product detail with live stock/price and add-to-cart.
class ProductDetailScreen extends StatefulWidget {
  const ProductDetailScreen({super.key, required this.publicId});

  final String publicId;

  @override
  State<ProductDetailScreen> createState() => _ProductDetailScreenState();
}

class _ProductDetailScreenState extends State<ProductDetailScreen> {
  late final CatalogApi _api;
  ProductModel? _product;
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _api = CatalogApi(context.read<ApiClient>());
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final product = await _api.product(widget.publicId);
      if (!mounted) return;
      setState(() {
        _product = product;
        _error = null;
      });
    } on Object catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartController>();
    final product = _product;

    return AppScaffold(
      title: product?.name ?? 'Product',
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Text(_error!, style: const TextStyle(color: AppTheme.error)))
              : product == null
                  ? const Center(child: Text('Product not found.'))
                  : Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        SizedBox(
                          height: 220,
                          child: product.imageUrl != null && product.imageUrl!.isNotEmpty
                              ? Image.network(product.imageUrl!, fit: BoxFit.cover,
                                  errorBuilder: (_, __, ___) => const Icon(Icons.image_not_supported, size: 80))
                              : Container(
                                  color: AppTheme.primaryLight,
                                  child: const Icon(Icons.shopping_bag_outlined,
                                      size: 80, color: AppTheme.primary),
                                ),
                        ),
                        const SizedBox(height: 16),
                        Text(product.name, style: Theme.of(context).textTheme.headlineSmall),
                        if (product.categoryName != null)
                          Text(product.categoryName!, style: const TextStyle(color: AppTheme.textSecondary)),
                        const SizedBox(height: 8),
                        Text('৳${product.effectivePrice.toStringAsFixed(2)}',
                            style: Theme.of(context).textTheme.titleLarge
                                ?.copyWith(color: AppTheme.primary)),
                        if (product.discountPrice != null)
                          Text('Regular: ৳${product.basePrice.toStringAsFixed(2)}',
                              style: const TextStyle(
                                  decoration: TextDecoration.lineThrough,
                                  color: AppTheme.textSecondary)),
                        const SizedBox(height: 12),
                        Text(product.description ?? 'No description provided.'),
                        const SizedBox(height: 12),
                        Text(product.inStock
                            ? 'In stock: ${product.stockQuantity}'
                            : 'Out of stock',
                            style: TextStyle(
                                color: product.inStock ? AppTheme.success : AppTheme.error,
                                fontWeight: FontWeight.w700)),
                        const Spacer(),
                        ElevatedButton.icon(
                          icon: const Icon(Icons.add_shopping_cart),
                          label: Text(product.inStock
                              ? 'Add to cart (${cart.itemCount} items)'
                              : 'Currently unavailable'),
                          onPressed: product.inStock
                              ? () async {
                                  await cart.add(product);
                                  if (!context.mounted) return;
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    const SnackBar(content: Text('Added to cart')),
                                  );
                                }
                              : null,
                        ),
                        const SizedBox(height: 8),
                        OutlinedButton.icon(
                          icon: const Icon(Icons.shopping_cart),
                          label: const Text('Go to cart'),
                          onPressed: () => context.go(AppRoutes.cart),
                        ),
                      ],
                    ),
    );
  }
}
