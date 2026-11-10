import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Rider dashboard: shift status, earnings snapshot and live offer feed.
///
/// Smart rider assignment (Goal 07) will populate the offer list from the
/// backend's assignment engine.
class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navDashboard,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppTheme.spacing * 2),
              child: Row(
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.online,
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      Text(
                        l10n.isBangla ? 'আজকের অর্ডার' : 'Orders today',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                  const Spacer(),
                  const Text(
                    '0',
                    style: TextStyle(fontSize: 32, fontWeight: FontWeight.w800),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppTheme.spacing * 3),
          Row(
            children: [
              Expanded(
                child: _MetricCard(
                  label: l10n.todayEarnings,
                  value: '৳0',
                  icon: Icons.payments_outlined,
                ),
              ),
              const SizedBox(width: AppTheme.spacing * 2),
              Expanded(
                child: _MetricCard(
                  label: l10n.completedDeliveries,
                  value: '0',
                  icon: Icons.local_shipping_outlined,
                ),
              ),
            ],
          ),
          const SizedBox(height: AppTheme.spacing * 3),
          Text(
            l10n.newOffer,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppTheme.spacing * 2),
          const Expanded(child: EmptyState(message: 'No new offers')),
        ],
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.label,
    required this.value,
    required this.icon,
  });

  final String label;
  final String value;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacing * 2),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: AppTheme.primary),
            const SizedBox(height: AppTheme.spacing),
            Text(value, style: Theme.of(context).textTheme.titleLarge),
            Text(label, style: Theme.of(context).textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}
