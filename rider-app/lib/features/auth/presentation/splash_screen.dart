import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';

/// Brief startup placeholder while the session is being restored.
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.delivery_dining_rounded,
                size: 64, color: AppTheme.primary),
            SizedBox(height: AppTheme.spacing * 3),
            CircularProgressIndicator(),
            SizedBox(height: AppTheme.spacing * 2),
            Text('Restoring your session…'),
          ],
        ),
      ),
    );
  }
}
