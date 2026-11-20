import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_theme.dart';

class RiderNav extends StatelessWidget {
  const RiderNav({super.key, required this.current});

  final String current;

  @override
  Widget build(BuildContext context) {
    final items = <(String, IconData, String)>[
      (AppRoutes.dashboard, Icons.dashboard_outlined, 'Home'),
      (AppRoutes.assignments, Icons.assignment_outlined, 'Orders'),
      (AppRoutes.delivery, Icons.delivery_dining_outlined, 'Delivery'),
      (AppRoutes.earnings, Icons.payments_outlined, 'Earnings'),
    ];
    return NavigationBar(
      selectedIndex: items.indexWhere((item) => item.$1.startsWith(current)),
      onDestinationSelected: (index) => context.go(items[index].$1),
      destinations: items
          .map((item) => NavigationDestination(icon: Icon(item.$2), label: item.$3))
          .toList(),
      backgroundColor: AppTheme.surface,
    );
  }
}
