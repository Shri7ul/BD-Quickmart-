import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../../core/widgets/customer_nav.dart';
import '../../cart/state/cart_controller.dart';
import '../../catalog/data/catalog_api.dart';
import '../../catalog/domain/catalog_models.dart';

/// Customer home: live categories and featured products from the backend.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late final CatalogApi _api;
  List<CategoryModel> _categories = const [];
  List<ProductModel> _products = const [];
  int? _selectedCategory;
  String _search = '';
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = CatalogApi(context.read<ApiClient>());
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final categories = await _api.categories();
      final products = await _api.products();
      if (!mounted) return;
      setState(() {
        _categories = categories;
        _products = products;
        _error = null;
      });
    } on Object catch (error) {
      if (!mounted) return;
      setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  List<ProductModel> get _filtered {
    return _products.where((product) {
      final categoryMatch = _selectedCategory == null ||
          product.categoryId == _selectedCategory;
      final searchMatch = _search.isEmpty ||
          product.name.toLowerCase().contains(_search.toLowerCase());
      return categoryMatch && searchMatch;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartController>();
    final filtered = _filtered;

    return AppScaffold(
      title: 'BD QuickMart',
      bottomNavigationBar: const CustomerNav(current: AppRoutes.home),
      actions: [
        Badge(
          label: Text('${cart.itemCount}'),
          isLabelVisible: cart.itemCount > 0,
          child: IconButton(
            icon: const Icon(Icons.shopping_cart_outlined),
            onPressed: () => context.go(AppRoutes.cart),
          ),
        ),
      ],
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            Text('Fresh groceries, delivered fast',
                style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: AppTheme.spacing * 2),
            TextField(
              onChanged: (value) => setState(() => _search = value),
              decoration: const InputDecoration(
                hintText: 'Search products',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: AppTheme.spacing * 3),
            Text('Categories', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: AppTheme.spacing),
            SizedBox(
              height: 44,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: _categories.length + 1,
                separatorBuilder: (_, __) => const SizedBox(width: 8),
                itemBuilder: (context, index) {
                  if (index == 0) {
                    return ChoiceChip(
                      label: const Text('All'),
                      selected: _selectedCategory == null,
                      onSelected: (_) => setState(() => _selectedCategory = null),
                    );
                  }
                  final category = _categories[index - 1];
                  return ChoiceChip(
                    label: Text(category.name),
                    selected: _selectedCategory == category.id,
                    onSelected: (_) => setState(() => _selectedCategory = category.id),
                  );
                },
              ),
            ),
            const SizedBox(height: AppTheme.spacing * 3),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Products', style: Theme.of(context).textTheme.titleMedium),
                TextButton(
                  onPressed: () => context.go(AppRoutes.products),
                  child: const Text('See all'),
                ),
              ],
            ),
            const SizedBox(height: AppTheme.spacing),
            if (_loading) const Padding(
              padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()),
            ),
            if (_error != null) Padding(
              padding: const EdgeInsets.all(16),
              child: Text(_error!, style: const TextStyle(color: AppTheme.error)),
            ),
            if (!_loading && _error == null && filtered.isEmpty)
              const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: Text('No products are available right now.')),
              ),
            if (!_loading && filtered.isNotEmpty)
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  childAspectRatio: 0.78,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                ),
                itemCount: filtered.length > 6 ? 6 : filtered.length,
                itemBuilder: (context, index) =>
                    _ProductCard(product: filtered[index], onAdd: _load),
              ),
          ],
        ),
      ),
    );
  }
}

/// Reusable product tile used by home and catalogue screens.
class ProductCard extends StatelessWidget {
  const ProductCard({super.key, required this.product, this.onAdd});

  final ProductModel product;
  final VoidCallback? onAdd;

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartController>();
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => context.go('${AppRoutes.products}/${product.publicId}'),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: product.imageUrl != null && product.imageUrl!.isNotEmpty
                  ? Image.network(product.imageUrl!, fit: BoxFit.cover, width: double.infinity,
                      errorBuilder: (_, __, ___) => const Icon(Icons.image_not_supported))
                  : Container(
                      width: double.infinity,
                      color: AppTheme.primaryLight,
                      child: const Icon(Icons.shopping_bag_outlined,
                          size: 48, color: AppTheme.primary),
                    ),
            ),
            Padding(
              padding: const EdgeInsets.all(10),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(product.name, maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.w700)),
                  Text('৳${product.effectivePrice.toStringAsFixed(2)}',
                      style: const TextStyle(color: AppTheme.primary,
                          fontWeight: FontWeight.w700)),
                  if (product.discountPrice != null)
                    Text('was ৳${product.basePrice.toStringAsFixed(2)}',
                        style: const TextStyle(
                            decoration: TextDecoration.lineThrough,
                            fontSize: 11, color: AppTheme.textSecondary)),
                  const SizedBox(height: 6),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      icon: const Icon(Icons.add, size: 16),
                      label: Text(product.inStock ? 'Add' : 'Out of stock'),
                      onPressed: product.inStock
                          ? () async {
                              await cart.add(product);
                              onAdd?.call();
                            }
                          : null,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

typedef _ProductCard = ProductCard;
