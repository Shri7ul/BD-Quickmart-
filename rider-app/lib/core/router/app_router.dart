import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/assignments/presentation/assignments_screen.dart';
import '../../features/dashboard/presentation/dashboard_screen.dart';
import '../../features/delivery/presentation/delivery_screen.dart';
import '../../features/earnings/presentation/earnings_screen.dart';
import '../../features/profile/presentation/profile_screen.dart';

/// Route paths for the rider application.
class AppRoutes {
  AppRoutes._();

  static const String dashboard = '/';
  static const String assignments = '/assignments';
  static const String delivery = '/delivery';
  static const String earnings = '/earnings';
  static const String profile = '/profile';
}

/// Routing configuration for the rider application.
///
/// The rider app has no customer-facing storefront routes; its navigation is
/// built around the shift loop: dashboard, offers, active delivery, earnings.
class AppRouter {
  AppRouter._();

  static const String dashboardRoute = 'dashboard';
  static const String assignmentsRoute = 'assignments';
  static const String deliveryRoute = 'delivery';
  static const String earningsRoute = 'earnings';
  static const String profileRoute = 'profile';

  static final GoRouter router = GoRouter(
    initialLocation: AppRoutes.dashboard,
    routes: [
      GoRoute(
        path: AppRoutes.dashboard,
        name: dashboardRoute,
        builder: (context, state) => const DashboardScreen(),
      ),
      GoRoute(
        path: AppRoutes.assignments,
        name: assignmentsRoute,
        builder: (context, state) => const AssignmentsScreen(),
      ),
      GoRoute(
        path: AppRoutes.delivery,
        name: deliveryRoute,
        builder: (context, state) => const DeliveryScreen(),
      ),
      GoRoute(
        path: AppRoutes.earnings,
        name: earningsRoute,
        builder: (context, state) => const EarningsScreen(),
      ),
      GoRoute(
        path: AppRoutes.profile,
        name: profileRoute,
        builder: (context, state) => const ProfileScreen(),
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      appBar: AppBar(title: const Text('BD QuickMart Rider')),
      body: Center(child: Text('Route not found: ${state.uri}')),
    ),
  );
}
