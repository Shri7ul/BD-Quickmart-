import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';

/// List of delivery offers and their acceptance state.
class AssignmentsScreen extends StatelessWidget {
  const AssignmentsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navAssignments,
      body: const EmptyState(
        message: 'No assignments yet',
        icon: Icons.assignment_outlined,
      ),
    );
  }
}

/// Card representing a single delivery offer shown to the rider.
class AssignmentCard extends StatelessWidget {
  const AssignmentCard({
    super.key,
    required this.storeName,
    required this.pickupAddress,
    required this.dropOffAddress,
    required this.distanceKm,
    required this.etaMinutes,
    required this.payAmount,
    this.onAccept,
    this.onReject,
  });

  final String storeName;
  final String pickupAddress;
  final String dropOffAddress;
  final double distanceKm;
  final int etaMinutes;
  final double payAmount;
  final VoidCallback? onAccept;
  final VoidCallback? onReject;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacing * 2),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(storeName, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: AppTheme.spacing),
            _RouteRow(
              icon: Icons.storefront_outlined,
              label: l10n.pickup,
              value: pickupAddress,
            ),
            const SizedBox(height: AppTheme.spacing),
            _RouteRow(
              icon: Icons.location_on_outlined,
              label: l10n.dropOff,
              value: dropOffAddress,
            ),
            const Divider(height: AppTheme.spacing * 3),
            Row(
              children: [
                const Icon(Icons.straighten, size: 16, color: AppTheme.textSecondary),
                const SizedBox(width: AppTheme.spacing * 0.5),
                Text(
                  '${distanceKm.toStringAsFixed(1)} km',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(width: AppTheme.spacing * 2),
                const Icon(Icons.schedule, size: 16, color: AppTheme.textSecondary),
                const SizedBox(width: AppTheme.spacing * 0.5),
                Text(
                  '$etaMinutes min',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const Spacer(),
                Text(
                  '৳${payAmount.toStringAsFixed(0)}',
                  style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: AppTheme.primary,
                      ),
                ),
              ],
            ),
            const SizedBox(height: AppTheme.spacing * 2),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: onReject,
                    child: Text(l10n.reject),
                  ),
                ),
                const SizedBox(width: AppTheme.spacing * 2),
                Expanded(
                  child: ElevatedButton(
                    onPressed: onAccept,
                    child: Text(l10n.accept),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _RouteRow extends StatelessWidget {
  const _RouteRow({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, size: 18, color: AppTheme.primary),
        const SizedBox(width: AppTheme.spacing),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(label, style: Theme.of(context).textTheme.bodySmall),
              Text(value),
            ],
          ),
        ),
      ],
    );
  }
}
