import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Rider profile, vehicle details and language settings.
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navProfile,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Card(
            child: ListTile(
              leading: const CircleAvatar(
                backgroundColor: AppTheme.primaryLight,
                child: Icon(Icons.person, color: AppTheme.primary),
              ),
              title: const Text('Rider name'),
              subtitle: const Text('01XXXXXXXXX'),
              trailing: const Icon(Icons.edit_outlined),
              onTap: () {},
            ),
          ),
          const SizedBox(height: AppTheme.spacing * 3),
          Card(
            child: Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.motorcycle_outlined),
                  title: const Text('Vehicle'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {},
                ),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.receipt_outlined),
                  title: const Text('Documents'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () {},
                ),
              ],
            ),
          ),
          const SizedBox(height: AppTheme.spacing * 3),
          Text(
            l10n.isBangla ? 'ভাষা' : 'Language',
            style: Theme.of(context).textTheme.titleMedium,
          ),
          const SizedBox(height: AppTheme.spacing),
          Card(
            child: RadioGroup<String>(
              groupValue: l10n.isBangla ? 'bn' : 'en',
              onChanged: (_) {},
              child: const Column(
                children: [
                  RadioListTile<String>(
                    value: 'bn',
                    title: Text('বাংলা'),
                  ),
                  RadioListTile<String>(
                    value: 'en',
                    title: Text('English'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
