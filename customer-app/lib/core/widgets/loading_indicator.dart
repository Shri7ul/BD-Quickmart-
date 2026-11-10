import 'package:flutter/material.dart';

import '../localization/app_localizations.dart';
import '../theme/app_theme.dart';

/// Centered loading indicator with a localized label.
class LoadingIndicator extends StatelessWidget {
  const LoadingIndicator({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const CircularProgressIndicator(),
          const SizedBox(height: AppTheme.spacing * 2),
          Text(l10n.loading, style: Theme.of(context).textTheme.bodySmall),
        ],
      ),
    );
  }
}
