import 'package:flutter/material.dart';

import '../../../core/localization/app_localizations.dart';
import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/app_scaffold.dart';

/// Shopping cart screen. Cart state and checkout arrive in Goal 05.
class CartScreen extends StatelessWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return AppScaffold(
      title: l10n.navCart,
      body: Column(
        children: [
          const Expanded(child: EmptyState(message: 'Cart items')),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(AppTheme.spacing * 2),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          l10n.cartTotal,
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        Text(
                          '৳0',
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                      ],
                    ),
                  ),
                  SizedBox(
                    width: 160,
                    child: ElevatedButton(
                      onPressed: () {},
                      child: Text(l10n.cartCheckout),
                    ),
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
