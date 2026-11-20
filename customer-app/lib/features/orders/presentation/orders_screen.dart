import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../../core/widgets/customer_nav.dart';
import '../data/order_api.dart';
import '../domain/order_models.dart';

/// Order history. Pull-to-refresh loads the authoritative backend list.
class OrdersScreen extends StatefulWidget {
  const OrdersScreen({super.key});

  @override
  State<OrdersScreen> createState() => _OrdersScreenState();
}

class _OrdersScreenState extends State<OrdersScreen> {
  late final OrderApi _api;
  List<OrderModel> _orders = const [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = OrderApi(context.read<ApiClient>());
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final orders = await _api.orders();
      if (!mounted) return;
      setState(() {
        _orders = orders;
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
    return AppScaffold(
      title: 'Your orders',
      bottomNavigationBar: const CustomerNav(current: AppRoutes.orders),
      body: RefreshIndicator(
        onRefresh: _load,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? ListView(children: [
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: Text(_error!, style: const TextStyle(color: AppTheme.error)),
                    )
                  ])
                : _orders.isEmpty
                    ? ListView(children: [
                        const Padding(
                          padding: EdgeInsets.all(48),
                          child: Center(child: Text('No orders yet.')),
                        )
                      ])
                    : ListView.builder(
                        itemCount: _orders.length,
                        itemBuilder: (context, index) {
                          final order = _orders[index];
                          return Card(
                            child: ListTile(
                              title: Text('Order ${order.publicId.substring(0, 8)}'),
                              subtitle: Text(
                                '${order.items.length} item(s) · '
                                '${order.createdAt.toLocal()}',
                              ),
                              trailing: Column(
                                mainAxisAlignment: MainAxisAlignment.center,
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text('৳${order.totalAmount.toStringAsFixed(2)}',
                                      style: const TextStyle(fontWeight: FontWeight.w700)),
                                  Text(order.status.replaceAll('_', ' '),
                                      style: const TextStyle(
                                          fontSize: 11, color: AppTheme.primary)),
                                ],
                              ),
                              onTap: () => context.go('${AppRoutes.orders}/${order.publicId}'),
                            ),
                          );
                        },
                      ),
      ),
    );
  }
}

/// Order detail with reliable polling-based live status updates.
///
/// The backend WebSocket handler is a transport-only placeholder, so the
/// customer app polls the authoritative order endpoint every 5 seconds
/// instead of displaying a fake live indicator.
class OrderDetailScreen extends StatefulWidget {
  const OrderDetailScreen({super.key, required this.publicId});

  final String publicId;

  @override
  State<OrderDetailScreen> createState() => _OrderDetailScreenState();
}

class _OrderDetailScreenState extends State<OrderDetailScreen> {
  late final OrderApi _api;
  Timer? _timer;
  OrderModel? _order;
  String? _error;

  @override
  void initState() {
    super.initState();
    _api = OrderApi(context.read<ApiClient>());
    _poll();
    _timer = Timer.periodic(const Duration(seconds: 5), (_) => _poll());
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _poll() async {
    try {
      final order = await _api.order(widget.publicId);
      if (!mounted) return;
      setState(() {
        _order = order;
        _error = null;
      });
    } on Object catch (error) {
      if (mounted) setState(() => _error = error.toString());
    }
  }

  @override
  Widget build(BuildContext context) {
    final order = _order;
    return AppScaffold(
      title: 'Order ${widget.publicId.substring(0, 8)}',
      body: order == null
          ? Center(child: _error != null
              ? Text(_error!, style: const TextStyle(color: AppTheme.error))
              : const CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _poll,
              child: ListView(
                children: [
                  Card(
                    color: AppTheme.primaryLight,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Status', style: Theme.of(context).textTheme.bodySmall),
                          Text(order.status.replaceAll('_', ' '),
                              style: Theme.of(context).textTheme.headlineSmall
                                  ?.copyWith(color: AppTheme.primary)),
                          if (order.riderName != null)
                            Text('Rider: ${order.riderName}'),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text('Items', style: Theme.of(context).textTheme.titleMedium),
                  ...order.items.map((item) => ListTile(
                        title: Text(item.productName),
                        subtitle: Text('${item.quantity} × ৳${item.unitPrice.toStringAsFixed(2)}'),
                        trailing: Text('৳${item.lineTotal.toStringAsFixed(2)}'),
                      )),
                  const Divider(),
                  _row('Subtotal', order.subtotalAmount),
                  _row('Discount', -order.discountAmount),
                  _row('Delivery', order.deliveryFee),
                  _row('Total', order.totalAmount, bold: true),
                  const SizedBox(height: 12),
                  Text('Deliver to', style: Theme.of(context).textTheme.titleMedium),
                  Text(order.deliveryAddress),
                  Text('Phone: ${order.contactPhone}'),
                  const SizedBox(height: 16),
                  Text('Progress', style: Theme.of(context).textTheme.titleMedium),
                  ...order.events.map((event) => ListTile(
                        leading: const Icon(Icons.check_circle_outline, color: AppTheme.success),
                        title: Text(event.status.replaceAll('_', ' ')),
                        subtitle: Text(event.at.toLocal().toString()),
                      )),
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
                style: TextStyle(fontWeight: bold ? FontWeight.w800 : null)),
          ],
        ),
      );
}
