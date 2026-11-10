import 'package:flutter/material.dart';

import '../localization/app_localizations.dart';
import '../theme/app_theme.dart';

/// Reusable page scaffold with consistent spacing and an optional bottom bar.
class AppScaffold extends StatelessWidget {
  const AppScaffold({
    super.key,
    required this.title,
    required this.body,
    this.bottomNavigationBar,
    this.actions,
    this.padded = true,
  });

  final String title;
  final Widget body;
  final Widget? bottomNavigationBar;
  final List<Widget>? actions;
  final bool padded;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(title), actions: actions),
      body: padded
          ? Padding(
              padding: const EdgeInsets.all(AppTheme.spacing * 2),
              child: body,
            )
          : body,
      bottomNavigationBar: bottomNavigationBar,
    );
  }
}

/// Reusable empty-state placeholder.
class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.message,
    this.icon = Icons.inbox_outlined,
    this.action,
  });

  final String message;
  final IconData icon;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 64, color: AppTheme.textSecondary),
          const SizedBox(height: AppTheme.spacing * 2),
          Text(
            message,
            textAlign: TextAlign.center,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: AppTheme.textSecondary),
          ),
          if (action != null) ...[
            const SizedBox(height: AppTheme.spacing * 3),
            action!,
          ],
          const SizedBox(height: AppTheme.spacing),
          Text(
            l10n.loading,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }
}

/// Reusable error placeholder with a retry affordance.
class ErrorState extends StatelessWidget {
  const ErrorState({super.key, required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppTheme.spacing * 3),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, size: 56, color: AppTheme.error),
            const SizedBox(height: AppTheme.spacing * 2),
            Text(message, textAlign: TextAlign.center),
            if (onRetry != null) ...[
              const SizedBox(height: AppTheme.spacing * 3),
              ElevatedButton(onPressed: onRetry, child: Text(l10n.retry)),
            ],
          ],
        ),
      ),
    );
  }
}
