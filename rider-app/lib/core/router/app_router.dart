import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/assignments/presentation/assignments_screen.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/pending_screen.dart';
import '../../features/auth/presentation/register_screen.dart';
import '../../features/auth/presentation/splash_screen.dart';
import '../../features/auth/state/auth_controller.dart';
import '../../features/dashboard/presentation/dashboard_screen.dart';
import '../../features/delivery/presentation/delivery_screen.dart';
import '../../features/earnings/presentation/earnings_screen.dart';
import '../../features/profile/presentation/profile_screen.dart';

/// Route paths for the rider application (Goal 02).
class AppRoutes {
  AppRoutes._();

  static const String splash = '/';
  static const String login = '/login';
  static const String register = '/register';
  static const String pending = '/pending';
  static const String dashboard = '/dashboard';
  static const String assignments = '/assignments';
  static const String delivery = '/delivery';
  static const String earnings = '/earnings';
  static const String profile = '/profile';
}

/// Builds the router bound to [auth] (Goal 02).
///
/// [AuthController] is a ChangeNotifier, so `refreshListenable` re-evaluates
/// the top-level redirect whenever the session changes. The rider app allows
/// PENDING accounts to stay signed in on /pending until an administrator
/// approves them; every other authenticated route is gated behind ACTIVE.
GoRouter createAppRouter(AuthController auth) {
  return GoRouter(
    initialLocation: AppRoutes.splash,
    refreshListenable: auth,
    redirect: (BuildContext _, GoRouterState state) {
      final String location = state.matchedLocation;
      final bool onSplash = location == AppRoutes.splash;
      final bool onAuthPage =
          location == AppRoutes.login || location == AppRoutes.register;
      final bool onPending = location == AppRoutes.pending;

      return switch (auth.status) {
        AuthStatus.unknown => onSplash ? null : AppRoutes.splash,
        AuthStatus.unauthenticated => onAuthPage ? null : AppRoutes.login,
        AuthStatus.pending => onPending ? null : AppRoutes.pending,
        AuthStatus.authenticated => (onSplash || onAuthPage || onPending)
            ? AppRoutes.dashboard
            : null,
      };
    },
    routes: [
      GoRoute(
        path: AppRoutes.splash,
        builder: (_, __) => const SplashScreen(),
      ),
      GoRoute(
        path: AppRoutes.login,
        builder: (_, __) => const LoginScreen(),
      ),
      GoRoute(
        path: AppRoutes.register,
        builder: (_, __) => const RegisterScreen(),
      ),
      GoRoute(
        path: AppRoutes.pending,
        builder: (_, __) => const PendingScreen(),
      ),
      GoRoute(
        path: AppRoutes.dashboard,
        builder: (_, __) => const DashboardScreen(),
      ),
      GoRoute(
        path: AppRoutes.assignments,
        builder: (_, __) => const AssignmentsScreen(),
      ),
      GoRoute(
        path: AppRoutes.delivery,
        builder: (_, __) => const DeliveryScreen(),
      ),
      GoRoute(
        path: AppRoutes.earnings,
        builder: (_, __) => const EarningsScreen(),
      ),
      GoRoute(
        path: AppRoutes.profile,
        builder: (_, __) => const ProfileScreen(),
      ),
    ],
    errorBuilder: (BuildContext _, GoRouterState state) => Scaffold(
      appBar: AppBar(title: const Text('BD QuickMart Rider')),
      body: Center(child: Text('Route not found: ${state.uri}')),
    ),
  );
}
