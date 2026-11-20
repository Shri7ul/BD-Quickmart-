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

/// Rider home: real availability, assignment counts and active delivery.
class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  late final RiderApi _api;
  List<RiderOrderModel> _orders = const [];
  bool _online = false;
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

  Future<void> _toggle(bool value) async {
    try {
      await _api.setAvailability(value);
      if (!mounted) return;
      setState(() => _online = value);
    } on Object catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.toString())),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final active = _orders.where((order) => order.isActive).toList();
    final delivered = _orders.where((order) => order.status == 'DELIVERED').length;

    return AppScaffold(
      title: 'Rider dashboard',
      bottomNavigationBar: const RiderNav(current: AppRoutes.dashboard),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            Card(
              color: _online ? AppTheme.primaryLight : AppTheme.surface,
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    Icon(Icons.circle,
                        color: _online ? AppTheme.success : AppTheme.textSecondary),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(_online ? 'Online — ready for deliveries' : 'Offline',
                              style: Theme.of(context).textTheme.titleMedium),
                          const Text('Set your availability for new assignments'),
                        ],
                      ),
                    ),
                    Switch(value: _online, onChanged: _toggle),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(child: _metric('Active deliveries', '${active.length}')),
                const SizedBox(width: 12),
                Expanded(child: _metric('Completed', '$delivered')),
              ],
            ),
            const SizedBox(height: 20),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Active delivery', style: Theme.of(context).textTheme.titleMedium),
                TextButton(
                  onPressed: () => context.go(AppRoutes.assignments),
                  child: const Text('All orders'),
                ),
              ],
            ),
            if (_loading) const Padding(
              padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()),
            ),
            if (_error != null) Padding(
              padding: const EdgeInsets.all(16),
              child: Text(_error!, style: const TextStyle(color: AppTheme.error)),
            ),
            if (!_loading && active.isEmpty)
              const Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: Text('No active delivery right now.')),
              ),
            if (!_loading && active.isNotEmpty)
              Card(
                child: ListTile(
                  leading: const Icon(Icons.delivery_dining, color: AppTheme.primary),
                  title: Text('Order ${active.first.publicId.substring(0, 8)}'),
                  subtitle: Text(active.first.status.replaceAll('_', ' ')),
                  trailing: Text('৳${active.first.totalAmount.toStringAsFixed(2)}'),
                  onTap: () => context.go('${AppRoutes.delivery}/${active.first.publicId}'),
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _metric(String label, String value) => Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(value, style: Theme.of(context).textTheme.headlineMedium
                  ?.copyWith(color: AppTheme.primary)),
              Text(label, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
      );
}
