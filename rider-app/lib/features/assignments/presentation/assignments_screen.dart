import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/router/app_router.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../../core/widgets/rider_nav.dart';
import '../data/rider_api.dart';
import '../domain/rider_order.dart';

/// All deliveries assigned to the signed-in rider.
class AssignmentsScreen extends StatefulWidget {
  const AssignmentsScreen({super.key});

  @override
  State<AssignmentsScreen> createState() => _AssignmentsScreenState();
}

class _AssignmentsScreenState extends State<AssignmentsScreen> {
  late final RiderApi _api;
  List<RiderOrderModel> _orders = const [];
  bool _loading = true;
  String? _error;

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
      title: 'Assigned deliveries',
      bottomNavigationBar: const RiderNav(current: AppRoutes.assignments),
      body: RefreshIndicator(
        onRefresh: _load,
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? ListView(children: [Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(_error!),
                  )])
                : _orders.isEmpty
                    ? ListView(children: [
                        const Padding(
                          padding: EdgeInsets.all(48),
                          child: Center(child: Text('No assignments yet.')),
                        )
                      ])
                    : ListView.builder(
                        itemCount: _orders.length,
                        itemBuilder: (context, index) {
                          final order = _orders[index];
                          return Card(
                            child: ListTile(
                              leading: Icon(
                                order.isActive
                                    ? Icons.delivery_dining
                                    : Icons.check_circle_outline,
                                color: order.isActive ? null : Colors.green,
                              ),
                              title: Text('Order ${order.publicId.substring(0, 8)}'),
                              subtitle: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(order.customerName),
                                  Text(order.status.replaceAll('_', ' ')),
                                ],
                              ),
                              trailing: Text('৳${order.totalAmount.toStringAsFixed(2)}'),
                              onTap: () => context.go('${AppRoutes.delivery}/${order.publicId}'),
                            ),
                          );
                        },
                      ),
      ),
    );
  }
}
