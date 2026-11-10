import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/presentation/login_screen.dart';
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
  static const String home = '/home';
  static const String products = '/products';
  static const String cart = '/cart';
  static const String orders = '/orders';
  static const String profile = '/profile';
}

/// Routing configuration for the customer application.
///
/// Route names are stable identifiers so that navigation intent is expressed
/// rather than raw paths scattered through the widget tree.
class AppRouter {
  AppRouter._();

  static const String splashRoute = 'splash';
  static const String loginRoute = 'login';
  static const String homeRoute = 'home';
  static const String productsRoute = 'products';
  static const String cartRoute = 'cart';
  static const String ordersRoute = 'orders';
  static const String profileRoute = 'profile';

  static final GoRouter router = GoRouter(
    initialLocation: AppRoutes.splash,
    routes: [
      GoRoute(
        path: AppRoutes.splash,
        name: splashRoute,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: AppRoutes.login,
        name: loginRoute,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: AppRoutes.home,
        name: homeRoute,
        builder: (context, state) => const HomeScreen(),
        routes: [
          GoRoute(
            path: 'products',
            name: productsRoute,
            builder: (context, state) => const ProductsScreen(),
          ),
          GoRoute(
            path: 'cart',
            name: cartRoute,
            builder: (context, state) => const CartScreen(),
          ),
          GoRoute(
            path: 'orders',
            name: ordersRoute,
            builder: (context, state) => const OrdersScreen(),
          ),
          GoRoute(
            path: 'profile',
            name: profileRoute,
            builder: (context, state) => const ProfileScreen(),
          ),
        ],
      ),
    ],
    errorBuilder: (context, state) => Scaffold(
      appBar: AppBar(title: const Text('BD QuickMart')),
      body: Center(
        child: Text('Route not found: ${state.uri}'),
      ),
    ),
  );
}
