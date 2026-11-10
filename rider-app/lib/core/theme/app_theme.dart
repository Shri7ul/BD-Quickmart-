import 'package:flutter/material.dart';

/// Theme definition for the rider application.
///
/// The rider app uses a deep indigo/blue palette instead of the customer
/// app's green, so the two applications are instantly distinguishable on a
/// shared device. The palette is also higher contrast, because the rider
/// interface is used outdoors, in bright sunlight, often one-handed.
class AppTheme {
  AppTheme._();

  // Brand colours - deliberately distinct from the customer app.
  static const Color primary = Color(0xFF1A3C8B);
  static const Color primaryDark = Color(0xFF0F2760);
  static const Color primaryLight = Color(0xFFE6ECFA);
  static const Color secondary = Color(0xFFFF6B35);
  static const Color accent = Color(0xFF00B8D4);

  // Semantic colours
  static const Color success = Color(0xFF2E7D32);
  static const Color warning = Color(0xFFF9A825);
  static const Color error = Color(0xFFC62828);
  static const Color info = Color(0xFF0277BD);

  // Neutrals
  static const Color textPrimary = Color(0xFF1A1C1E);
  static const Color textSecondary = Color(0xFF5F6368);
  static const Color divider = Color(0xFFE0E0E0);
  static const Color surface = Color(0xFFFFFFFF);
  static const Color background = Color(0xFFF5F6FA);

  /// Corner radius used across cards and buttons.
  static const double radius = 12;

  /// Base spacing unit; all padding/gaps are multiples of this.
  static const double spacing = 8;

  static ThemeData get light {
    final colorScheme = ColorScheme.fromSeed(
      seedColor: primary,
      primary: primary,
      secondary: secondary,
      surface: surface,
      error: error,
    );

    return ThemeData(
      useMaterial3: true,
      colorScheme: colorScheme,
      scaffoldBackgroundColor: background,
      appBarTheme: const AppBarTheme(
        backgroundColor: primary,
        foregroundColor: Colors.white,
        elevation: 0,
        centerTitle: true,
      ),
      cardTheme: CardThemeData(
        elevation: 1,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radius),
        ),
        margin: EdgeInsets.zero,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          minimumSize: const Size.fromHeight(52),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(radius),
          ),
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radius),
          borderSide: const BorderSide(color: divider),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radius),
          borderSide: const BorderSide(color: divider),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(radius),
          borderSide: const BorderSide(color: primary, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: spacing * 2,
          vertical: spacing * 1.5,
        ),
      ),
      chipTheme: ChipThemeData(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(radius),
        ),
      ),
      dividerTheme: const DividerThemeData(color: divider, thickness: 1),
      textTheme: const TextTheme(
        headlineSmall: TextStyle(fontWeight: FontWeight.w700),
        titleLarge: TextStyle(fontWeight: FontWeight.w700),
        titleMedium: TextStyle(fontWeight: FontWeight.w600),
        bodyMedium: TextStyle(fontSize: 14),
        bodySmall: TextStyle(fontSize: 12, color: textSecondary),
      ),
    );
  }
}
