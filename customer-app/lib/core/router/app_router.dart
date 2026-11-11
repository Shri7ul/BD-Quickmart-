import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/login_screen.dart';
import '../../features/auth/presentation/pending_screen.dart';
import '../../features/auth/presentation/register_screen.dart';
import '../../features/auth/presentation/splash_screen.dart';
import '../../features/auth/state/auth_controller.dart';
import '../../features/cart/presentation/cart_screen.dart';
import '../../features/home/presentation/home_screen.dart';
import '../../features/orders/presentation/orders_screen.dart';
import '../../features/products/presentation/products_screen.dart';
import '../../features/profile/presentation/profile_screen.dart';

/// Route paths for the customer application.
class AppRoutes {
  AppRoutes._();

  static const String splash = '/';
  static const String login = '/login';
  static const String register = '/register';
  static const String pending = '/pending';
  static const String home = '/home';
  static const String products = '/home/products';
  static const String cart = '/home/cart';
  static const String orders = '/home/orders';
  static const String profile = '/home/profile';
}

/// Builds the router bound to [auth] (Goal 02).
///
/// [AuthController] is a ChangeNotifier, so `refreshListenable` re-evaluates
/// the top-level redirect whenever the session changes. That single redirect
/// enforces: signed-out -> /login, pending -> /pending, signed-in -> /home,
/// and keeps the public auth pages reachable only in the matching state.
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
            ? AppRoutes.home
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
        path: AppRoutes.home,
        builder: (_, __) => const HomeScreen(),
        routes: [
          GoRoute(
            path: 'products',
            builder: (_, __) => const ProductsScreen(),
          ),
          GoRoute(
            path: 'cart',
            builder: (_, __) => const CartScreen(),
          ),
          GoRoute(
            path: 'orders',
            builder: (_, __) => const OrdersScreen(),
          ),
          GoRoute(
            path: 'profile',
            builder: (_, __) => const ProfileScreen(),
          ),
        ],
      ),
    ],
    errorBuilder: (BuildContext _, GoRouterState state) => Scaffold(
      appBar: AppBar(title: const Text('BD QuickMart')),
      body: Center(child: Text('Route not found: ${state.uri}')),
    ),
  );
}
