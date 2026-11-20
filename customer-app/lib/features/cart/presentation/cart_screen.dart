import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/router/app_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../../core/widgets/customer_nav.dart';
import '../state/cart_controller.dart';

/// Cart with quantity controls, live totals and checkout entry point.
class CartScreen extends StatelessWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartController>();

    return AppScaffold(
      title: 'Your cart (${cart.itemCount})',
      bottomNavigationBar: const CustomerNav(current: AppRoutes.cart),
      body: cart.lines.isEmpty
          ? const Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.shopping_cart_outlined, size: 72, color: AppTheme.textSecondary),
                  SizedBox(height: 16),
                  Text('Your cart is empty'),
                ],
              ),
            )
          : Column(
              children: [
                Expanded(
                  child: ListView.builder(
                    itemCount: cart.lines.length,
                    itemBuilder: (context, index) {
                      final line = cart.lines[index];
                      return Card(
                        child: ListTile(
                          leading: const CircleAvatar(
                            backgroundColor: AppTheme.primaryLight,
                            child: Icon(Icons.shopping_bag_outlined, color: AppTheme.primary),
                          ),
                          title: Text(line.product.name),
                          subtitle: Text(
                            '৳${line.product.effectivePrice.toStringAsFixed(2)} each · '
                            '${line.product.stockQuantity} in stock',
                          ),
                          trailing: SizedBox(
                            width: 132,
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.end,
                              children: [
                                IconButton(
                                  icon: const Icon(Icons.remove_circle_outline),
                                  onPressed: () => cart.setQuantity(
                                      line.product.publicId, line.quantity - 1),
                                ),
                                Text('${line.quantity}'),
                                IconButton(
                                  icon: const Icon(Icons.add_circle_outline),
                                  onPressed: () => cart.setQuantity(
                                      line.product.publicId, line.quantity + 1),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    },
                  ),
                ),
                const Divider(),
                _SummaryRow(label: 'Subtotal', value: cart.subtotal),
                if (cart.discount > 0)
                  _SummaryRow(label: 'Discount', value: -cart.discount),
                _SummaryRow(label: 'Delivery', value: cart.delivery),
                _SummaryRow(label: 'Total', value: cart.total, bold: true),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    onPressed: () => context.go('/home/checkout'),
                    child: const Text('Proceed to checkout'),
                  ),
                ),
              ],
            ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({required this.label, required this.value, this.bold = false});

  final String label;
  final double value;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(fontWeight: bold ? FontWeight.w800 : FontWeight.w500)),
          Text('৳${value.toStringAsFixed(2)}',
              style: TextStyle(
                  fontWeight: bold ? FontWeight.w800 : FontWeight.w500,
                  color: bold ? AppTheme.primary : null)),
        ],
      ),
    );
  }
}
