import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/network/api_client.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../../core/widgets/rider_nav.dart';
import '../../../core/router/app_router.dart';
import '../../assignments/data/rider_api.dart';
import '../../assignments/domain/rider_order.dart';

/// Earnings derived from real delivered assignments (no fabricated figures).
class EarningsScreen extends StatefulWidget {
  const EarningsScreen({super.key});

  @override
  State<EarningsScreen> createState() => _EarningsScreenState();
}

class _EarningsScreenState extends State<EarningsScreen> {
  late final RiderApi _api;
  List<RiderOrderModel> _delivered = const [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _api = RiderApi(context.read<ApiClient>());
    _load();
  }

  Future<void> _load() async {
    try {
      final orders = await _api.assignedOrders();
      if (!mounted) return;
      setState(() {
        _delivered = orders.where((order) => order.status == 'DELIVERED').toList();
      });
    } on Object catch (_) {
      // Keep the empty state; the dashboard surfaces the actual error.
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final total = _delivered.fold(0.0, (sum, order) => sum + order.totalAmount);
    return AppScaffold(
      title: 'Earnings',
      bottomNavigationBar: const RiderNav(current: AppRoutes.earnings),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            Card(
              color: AppTheme.primaryLight,
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    Text('Completed deliveries', style: Theme.of(context).textTheme.bodySmall),
                    Text('${_delivered.length}', style: Theme.of(context).textTheme.headlineLarge),
                    const SizedBox(height: 8),
                    Text('Total order value', style: Theme.of(context).textTheme.bodySmall),
                    Text('৳${total.toStringAsFixed(2)}',
                        style: Theme.of(context).textTheme.headlineMedium
                            ?.copyWith(color: AppTheme.primary)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            if (_loading) const Center(child: CircularProgressIndicator()),
            if (!_loading && _delivered.isEmpty)
              const Padding(
                padding: EdgeInsets.all(32), child: Center(child: Text('No completed deliveries yet.')),
              ),
            ..._delivered.map((order) => ListTile(
                  title: Text('Order ${order.publicId.substring(0, 8)}'),
                  subtitle: Text(order.createdAt.toLocal().toString()),
                  trailing: Text('৳${order.totalAmount.toStringAsFixed(2)}'),
                )),
          ],
        ),
      ),
    );
  }
}
