import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Product listing screen. Populated by the product module in Goal 03/04.
class ProductsScreen extends StatelessWidget {
  const ProductsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navProducts,
      body: const EmptyState(message: 'Product catalogue'),
    );
  }
}
