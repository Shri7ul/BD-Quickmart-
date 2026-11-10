import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Active delivery screen for the rider.
///
/// Live navigation, route optimization and ETA recalculation arrive in
/// Goal 08.
class DeliveryScreen extends StatelessWidget {
  const DeliveryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navDelivery,
      body: EmptyState(
        message: l10n.noActiveDelivery,
        icon: Icons.delivery_dining_outlined,
      ),
    );
  }
}
