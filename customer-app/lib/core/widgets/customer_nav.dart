import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_theme.dart';

/// Bottom navigation shared by all authenticated customer screens.
class CustomerNav extends StatelessWidget {
  const CustomerNav({super.key, required this.current});

  final String current;

  @override
  Widget build(BuildContext context) {
    final items = <(String, IconData, String)>[
      (AppRoutes.home, Icons.home_outlined, 'Home'),
      (AppRoutes.products, Icons.grid_view_outlined, 'Products'),
      (AppRoutes.cart, Icons.shopping_cart_outlined, 'Cart'),
      (AppRoutes.orders, Icons.receipt_long_outlined, 'Orders'),
      (AppRoutes.profile, Icons.person_outline, 'Profile'),
    ];
    return NavigationBar(
      selectedIndex: items.indexWhere((item) => item.$1 == current),
      onDestinationSelected: (index) => context.go(items[index].$1),
      destinations: items
          .map((item) => NavigationDestination(icon: Icon(item.$2), label: item.$3))
          .toList(),
      backgroundColor: AppTheme.surface,
    );
  }
}
