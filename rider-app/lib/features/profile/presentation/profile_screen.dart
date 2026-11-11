import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';
import '../../auth/state/auth_controller.dart';

/// Rider profile, vehicle details and language settings (Goal 02).
///
/// Shows the signed-in rider and wires the previously inert sign-out action
/// to [AuthController.logout], which revokes the refresh-token family first.
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);
    final AuthController auth = context.watch<AuthController>();
    final user = auth.user;

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
              title: Text(user?.fullName ?? 'Rider'),
              subtitle: Text(user?.email ?? ''),
            ),
          ),
          const SizedBox(height: AppTheme.spacing),
          if (user != null)
            Card(
              child: ListTile(
                leading: const Icon(Icons.badge_outlined),
                title: const Text('Account'),
                subtitle: Text('${user.role} · ${user.status}'),
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
          const SizedBox(height: AppTheme.spacing * 3),
          OutlinedButton.icon(
            onPressed: () => context.read<AuthController>().logout(),
            icon: const Icon(Icons.logout),
            label: const Text('Sign out'),
          ),
        ],
      ),
    );
  }
}
