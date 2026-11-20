import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../../core/widgets/customer_nav.dart';
import '../../catalog/data/catalog_api.dart';
import '../../catalog/domain/catalog_models.dart';
import '../../home/presentation/home_screen.dart';

/// Full product catalogue: search, category filter and sorting.
class ProductsScreen extends StatefulWidget {
  const ProductsScreen({super.key});

  @override
  State<ProductsScreen> createState() => _ProductsScreenState();
}

class _ProductsScreenState extends State<ProductsScreen> {
  late final CatalogApi _api;
  List<CategoryModel> _categories = const [];
  List<ProductModel> _products = const [];
  int? _categoryId;
  String _search = '';
  String _sort = 'name';
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
      final products = await _api.products(
        search: _search.isEmpty ? null : _search,
        categoryId: _categoryId,
      );
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

  List<ProductModel> get _sorted {
    final list = List<ProductModel>.from(_products);
    switch (_sort) {
      case 'price_asc':
        list.sort((a, b) => a.effectivePrice.compareTo(b.effectivePrice));
        break;
      case 'price_desc':
        list.sort((a, b) => b.effectivePrice.compareTo(a.effectivePrice));
        break;
      default:
        list.sort((a, b) => a.name.compareTo(b.name));
    }
    return list;
  }

  @override
  Widget build(BuildContext context) {
    return AppScaffold(
      title: 'Products',
      bottomNavigationBar: const CustomerNav(current: AppRoutes.products),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            TextField(
              onSubmitted: (_) => _load(),
              onChanged: (value) => _search = value,
              decoration: const InputDecoration(
                hintText: 'Search products',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: DropdownButtonFormField<int?>(
                    initialValue: _categoryId,
                    decoration: const InputDecoration(labelText: 'Category',
                        border: OutlineInputBorder()),
                    items: [
                      const DropdownMenuItem<int?>(value: null, child: Text('All')),
                      ..._categories.map((c) =>
                          DropdownMenuItem<int?>(value: c.id, child: Text(c.name))),
                    ],
                    onChanged: (value) {
                      setState(() => _categoryId = value);
                      _load();
                    },
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: _sort,
                    decoration: const InputDecoration(labelText: 'Sort',
                        border: OutlineInputBorder()),
                    items: const [
                      DropdownMenuItem(value: 'name', child: Text('Name A–Z')),
                      DropdownMenuItem(value: 'price_asc', child: Text('Price: low to high')),
                      DropdownMenuItem(value: 'price_desc', child: Text('Price: high to low')),
                    ],
                    onChanged: (value) {
                      if (value != null) setState(() => _sort = value);
                    },
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ElevatedButton(onPressed: _load, child: const Text('Apply search')),
            const SizedBox(height: 16),
            if (_loading) const Padding(
              padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()),
            ),
            if (_error != null) Text(_error!, style: const TextStyle(color: AppTheme.error)),
            if (!_loading && _error == null && _sorted.isEmpty)
              const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: Text('No products found.')),
              ),
            if (!_loading && _sorted.isNotEmpty)
              GridView.builder(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2,
                  childAspectRatio: 0.78,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                ),
                itemCount: _sorted.length,
                itemBuilder: (context, index) =>
                    ProductCard(product: _sorted[index], onAdd: _load),
              ),
          ],
        ),
      ),
    );
  }
}
