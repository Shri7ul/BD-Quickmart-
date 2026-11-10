import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Reusable page scaffold for the rider application.
class AppScaffold extends StatelessWidget {
  const AppScaffold({
    super.key,
    required this.title,
    required this.body,
    this.actions,
    this.padded = true,
  });

  final String title;
  final Widget body;
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
        ],
      ),
    );
  }
}

/// Status pill used for rider availability and assignment state.
class StatusPill extends StatelessWidget {
  const StatusPill({
    super.key,
    required this.label,
    required this.color,
    this.icon,
  });

  final String label;
  final Color color;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppTheme.spacing * 1.5,
        vertical: AppTheme.spacing * 0.75,
      ),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppTheme.radius),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 14, color: color),
            const SizedBox(width: AppTheme.spacing * 0.5),
          ],
          Text(
            label,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: color,
                  fontWeight: FontWeight.w600,
                ),
          ),
        ],
      ),
    );
  }
}
