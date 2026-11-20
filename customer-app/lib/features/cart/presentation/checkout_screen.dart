import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../auth/state/auth_controller.dart';
import '../../cart/state/cart_controller.dart';
import '../../catalog/data/catalog_api.dart';
import '../../orders/data/order_api.dart';

/// Checkout: address, COD payment, server-validated order placement.
class CheckoutScreen extends StatefulWidget {
  const CheckoutScreen({super.key});

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  final _address = TextEditingController();
  final _phone = TextEditingController();
  final _notes = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  late final String _idempotencyKey;
  late final ApiClient _apiClient;
  bool _submitting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _idempotencyKey = newIdempotencyKey();
    _apiClient = context.read<ApiClient>();
    final user = context.read<AuthController>().user;
    if (user != null) _phone.text = user.phone;
  }

  @override
  void dispose() {
    _address.dispose();
    _phone.dispose();
    _notes.dispose();
    super.dispose();
  }

  Future<void> _placeOrder() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _submitting = true;
      _error = null;
    });
    final cart = context.read<CartController>();
    try {
      // Re-fetch live prices/stock before the server validates the order.
      final fresh = await CatalogApi(_apiClient).products();
      final problem = cart.revalidate(fresh);
      if (problem != null) {
        setState(() => _error = problem);
        return;
      }
      final order = await OrderApi(_apiClient).checkout(
        idempotencyKey: _idempotencyKey,
        items: cart.checkoutItems(),
        deliveryAddress: _address.text.trim(),
        contactPhone: _phone.text.trim(),
        notes: _notes.text.trim().isEmpty ? null : _notes.text.trim(),
      );
      await cart.clear();
      if (!mounted) return;
      context.go('${AppRoutes.orders}/${order.publicId}');
    } on Object catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cart = context.watch<CartController>();

    return AppScaffold(
      title: 'Checkout',
      body: Form(
        key: _formKey,
        child: ListView(
          children: [
            TextFormField(
              controller: _address,
              decoration: const InputDecoration(
                labelText: 'Delivery address',
                border: OutlineInputBorder(),
              ),
              maxLines: 3,
              validator: (value) => value == null || value.trim().length < 5
                  ? 'Enter a complete delivery address'
                  : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _phone,
              decoration: const InputDecoration(
                labelText: 'Contact phone',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.phone,
              validator: (value) => value == null || value.trim().length < 8
                  ? 'Enter a valid phone number'
                  : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _notes,
              decoration: const InputDecoration(
                labelText: 'Notes (optional)',
                border: OutlineInputBorder(),
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 20),
            Text('Order summary', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            ...cart.lines.map((line) => ListTile(
                  title: Text(line.product.name),
                  trailing: Text('${line.quantity} × ৳${line.product.effectivePrice.toStringAsFixed(2)}'),
                )),
            const Divider(),
            _row('Subtotal', cart.subtotal),
            if (cart.discount > 0) _row('Discount', -cart.discount),
            _row('Delivery', cart.delivery),
            _row('Total', cart.total, bold: true),
            const SizedBox(height: 8),
            const Text('Payment method: Cash on Delivery (COD)'),
            const SizedBox(height: 16),
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Text(_error!, style: const TextStyle(color: AppTheme.error)),
              ),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _submitting || cart.lines.isEmpty ? null : _placeOrder,
                child: Text(_submitting ? 'Placing order…' : 'Place COD order'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _row(String label, double value, {bool bold = false}) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 3),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(label, style: TextStyle(fontWeight: bold ? FontWeight.w800 : null)),
            Text('৳${value.toStringAsFixed(2)}',
                style: TextStyle(
                    fontWeight: bold ? FontWeight.w800 : null,
                    color: bold ? AppTheme.primary : null)),
          ],
        ),
      );
}
