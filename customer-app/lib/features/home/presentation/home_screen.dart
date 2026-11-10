import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Customer home screen.
///
/// Establishes the primary navigation surface. Product catalogue, AI
/// recommendations and store discovery are populated in later goals.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.appTitle,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            l10n.homeGreeting,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          const SizedBox(height: AppTheme.spacing * 2),
          TextField(
            readOnly: true,
            decoration: InputDecoration(
              hintText: l10n.homeSearchHint,
              prefixIcon: const Icon(Icons.search),
              suffixIcon: const Icon(Icons.qr_code_scanner),
            ),
          ),
          const SizedBox(height: AppTheme.spacing * 3),
          Card(
            child: ListTile(
              leading: const Icon(Icons.location_on_outlined,
                  color: AppTheme.primary),
              title: Text(l10n.homeDeliveryAddress),
              subtitle: const Text('Set your delivery location'),
              trailing: const Icon(Icons.chevron_right),
              onTap: () {},
            ),
          ),
          const SizedBox(height: AppTheme.spacing * 3),
          Text(
            l10n.navProducts,
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppTheme.spacing * 2),
          const Expanded(child: EmptyState(message: 'Categories')),
        ],
      ),
    );
  }
}
