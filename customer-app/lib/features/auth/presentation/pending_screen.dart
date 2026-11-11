import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_theme.dart';
import '../state/auth_controller.dart';

/// Shown when a signed-in account is still PENDING administrator approval.
/// Primarily used by the rider app; kept here so both apps share the flow.
class PendingScreen extends StatefulWidget {
  const PendingScreen({super.key});

  @override
  State<PendingScreen> createState() => _PendingScreenState();
}

class _PendingScreenState extends State<PendingScreen> {
  bool _checking = false;

  Future<void> _checkStatus() async {
    setState(() => _checking = true);
    try {
      await context.read<AuthController>().refreshSession();
      // If the account was approved, refreshListenable redirects to /home.
    } finally {
      if (mounted) {
        setState(() => _checking = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final AuthController auth = context.watch<AuthController>();
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppTheme.spacing * 3),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Icon(Icons.hourglass_top_rounded,
                      size: 72, color: AppTheme.warning),
                  const SizedBox(height: AppTheme.spacing * 2),
                  Text(
                    'Awaiting approval',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.headlineSmall,
                  ),
                  const SizedBox(height: AppTheme.spacing * 2),
                  Text(
                    'Your account has been created but an administrator must '
                    'approve it before you can continue. Check back after '
                    'approval.',
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodyMedium,
                  ),
                  if (auth.user != null) ...[
                    const SizedBox(height: AppTheme.spacing * 3),
                    Card(
                      child: ListTile(
                        leading: const CircleAvatar(
                          backgroundColor: AppTheme.primaryLight,
                          child: Icon(Icons.person, color: AppTheme.primary),
                        ),
                        title: Text(auth.user!.fullName),
                        subtitle: Text(auth.user!.email),
                      ),
                    ),
                  ],
                  const SizedBox(height: AppTheme.spacing * 3),
                  ElevatedButton(
                    onPressed: _checking ? null : _checkStatus,
                    child: _checking
                        ? const SizedBox(
                            height: 20,
                            width: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Text('Check approval status'),
                  ),
                  const SizedBox(height: AppTheme.spacing),
                  TextButton(
                    onPressed: () => context.read<AuthController>().logout(),
                    child: const Text('Sign out'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
