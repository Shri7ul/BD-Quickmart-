import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Rider earnings history and payout summary.
class EarningsScreen extends StatelessWidget {
  const EarningsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navEarnings,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(AppTheme.spacing * 2),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    l10n.weeklyEarnings,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  Text(
                    '৳0',
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: AppTheme.spacing * 3),
          const Expanded(
            child: EmptyState(
              message: 'No earnings records',
              icon: Icons.receipt_long_outlined,
            ),
          ),
        ],
      ),
    );
  }
}
