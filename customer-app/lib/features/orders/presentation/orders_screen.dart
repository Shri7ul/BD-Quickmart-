import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Order history screen. Order lifecycle arrives in Goal 06.
class OrdersScreen extends StatelessWidget {
  const OrdersScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navOrders,
      body: const EmptyState(message: 'Order history'),
    );
  }
}
