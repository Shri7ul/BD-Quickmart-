import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../../core/widgets/rider_nav.dart';
import '../../assignments/data/rider_api.dart';
import '../../assignments/domain/rider_order.dart';

/// Active delivery workflow: pickup → in transit → delivered.
class DeliveryScreen extends StatefulWidget {
  const DeliveryScreen({super.key, this.publicId});

  final String? publicId;

  @override
  State<DeliveryScreen> createState() => _DeliveryScreenState();
}

class _DeliveryScreenState extends State<DeliveryScreen> {
  late final RiderApi _api;
  RiderOrderModel? _order;
  List<RiderOrderModel> _active = const [];
  bool _loading = true;
  String? _error;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _api = RiderApi(context.read<ApiClient>());
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final orders = await _api.assignedOrders();
      final active = orders.where((order) => order.isActive).toList();
      RiderOrderModel? selected;
      if (widget.publicId != null) {
        final matches = orders.where((o) => o.publicId == widget.publicId).toList();
        selected = matches.isEmpty ? null : matches.first;
      } else if (active.isNotEmpty) {
        final detail = await _api.order(active.first.publicId);
        selected = detail;
      }
      if (!mounted) return;
      setState(() {
        _active = active;
        _order = selected;
        _error = null;
      });
    } on Object catch (error) {
      if (mounted) setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _update(String status) async {
    final order = _order;
    if (order == null) return;
    setState(() => _busy = true);
    try {
      final updated = await _api.setStatus(order.publicId, status,
          note: 'Updated by rider');
      if (!mounted) return;
      setState(() => _order = updated);
      await _load();
    } on Object catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final order = _order;
    return AppScaffold(
      title: order == null ? 'Delivery' : 'Order ${order.publicId.substring(0, 8)}',
      bottomNavigationBar: const RiderNav(current: AppRoutes.delivery),
      body: RefreshIndicator(
        onRefresh: _load,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? ListView(children: [Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(_error!),
                  )])
                : order == null
                    ? ListView(children: [
                        const Padding(
                          padding: EdgeInsets.all(48),
                          child: Center(child: Text('No active delivery.')),
                        ),
                        if (_active.isNotEmpty)
                          ..._active.map((item) => ListTile(
                                title: Text('Order ${item.publicId.substring(0, 8)}'),
                                subtitle: Text(item.status.replaceAll('_', ' ')),
                                onTap: () => context.go('${AppRoutes.delivery}/${item.publicId}'),
                              )),
                      ])
                    : ListView(
                        children: [
                          Card(
                            color: AppTheme.primaryLight,
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(order.status.replaceAll('_', ' '),
                                      style: Theme.of(context).textTheme.headlineSmall
                                          ?.copyWith(color: AppTheme.primary)),
                                  Text('Customer: ${order.customerName}'),
                                  Text('Phone: ${order.contactPhone}'),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 16),
                          Card(
                            child: Padding(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text('Pickup', style: TextStyle(fontWeight: FontWeight.w700)),
                                  const Text('BD QuickMart dark store'),
                                  const SizedBox(height: 12),
                                  const Text('Deliver to', style: TextStyle(fontWeight: FontWeight.w700)),
                                  Text(order.deliveryAddress),
                                  if (order.notes != null) Text('Note: ${order.notes}'),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 16),
                          Text('Items', style: Theme.of(context).textTheme.titleMedium),
                          ...order.items.map((item) => ListTile(
                                title: Text(item.productName),
                                trailing: Text('${item.quantity} × ৳${item.lineTotal.toStringAsFixed(2)}'),
                              )),
                          Text('Total: ৳${order.totalAmount.toStringAsFixed(2)}',
                              style: const TextStyle(fontWeight: FontWeight.w800)),
                          const SizedBox(height: 20),
                          if (order.status == 'ASSIGNED')
                            _action(Icons.shopping_bag, 'Confirm pickup', () => _update('PICKED_UP')),
                          if (order.status == 'PICKED_UP')
                            _action(Icons.two_wheeler, 'Out for delivery', () => _update('OUT_FOR_DELIVERY')),
                          if (order.status == 'OUT_FOR_DELIVERY') ...[
                            _action(Icons.check_circle, 'Complete delivery', () => _update('DELIVERED')),
                            const SizedBox(height: 8),
                            OutlinedButton.icon(
                              icon: const Icon(Icons.report_problem_outlined),
                              label: const Text('Report delivery failure'),
                              onPressed: _busy ? null : () => _update('FAILED_DELIVERY'),
                            ),
                          ],
                          if (!order.isActive)
                            Card(
                              child: Padding(
                                padding: const EdgeInsets.all(16),
                                child: Text('This delivery is ${order.status.replaceAll('_', ' ')}.'),
                              ),
                            ),
                          const SizedBox(height: 16),
                          Text('Status history', style: Theme.of(context).textTheme.titleMedium),
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

  Widget _action(IconData icon, String label, VoidCallback onPressed) => SizedBox(
        width: double.infinity,
        child: ElevatedButton.icon(
          icon: Icon(icon),
          label: Text(label),
          onPressed: _busy ? null : onPressed,
        ),
      );
}
