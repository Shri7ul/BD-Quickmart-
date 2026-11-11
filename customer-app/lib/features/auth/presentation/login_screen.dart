import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../core/config/server_config.dart';
import '../../../core/localization/app_localizations.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/theme/app_theme.dart';
import '../auth_exception.dart';
import '../state/auth_controller.dart';

/// Sign-in screen. Wires the Goal 02 auth API and offers a runtime server URL
/// override so a physical device can reach a backend on the LAN.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _identifierController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();

  bool _obscurePassword = true;
  bool _submitting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    final AuthController auth = context.read<AuthController>();
    if (auth.message != null) {
      _error = auth.message;
    }
  }

  @override
  void dispose() {
    _identifierController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) {
      return;
    }
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await context.read<AuthController>().login(
            _identifierController.text.trim(),
            _passwordController.text,
          );
      // go_router's refreshListenable redirects to /home on success.
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() => _error = _errorMessage(error));
    } finally {
      if (mounted) {
        setState(() => _submitting = false);
      }
    }
  }

  String _errorMessage(Object error) {
    if (error is ApiException || error is AuthException) {
      return error.toString();
    }
    return 'Unable to sign in. Check your connection and server URL.';
  }

  Future<void> _openServerSettings() async {
    final ServerConfig config = context.read<ServerConfig>();
    final TextEditingController urlController =
        TextEditingController(text: config.baseUrl);

    await showDialog<void>(
      context: context,
      builder: (BuildContext dialogContext) {
        String? dialogError;
        return StatefulBuilder(
          builder: (BuildContext ctx, StateSetter setDialogState) {
            return AlertDialog(
              title: const Text('Server URL'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Backend base URL including /api/v1. On a physical device '
                    'use your computer\'s LAN IP instead of 10.0.2.2.',
                  ),
                  const SizedBox(height: AppTheme.spacing * 2),
                  TextField(
                    controller: urlController,
                    keyboardType: TextInputType.url,
                    decoration: const InputDecoration(labelText: 'Base URL'),
                  ),
                  if (dialogError != null)
                    Padding(
                      padding: const EdgeInsets.only(top: AppTheme.spacing),
                      child: Text(
                        dialogError!,
                        style: const TextStyle(color: AppTheme.error),
                      ),
                    ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () async {
                    await config.setBaseUrl('');
                    if (dialogContext.mounted) {
                      Navigator.of(dialogContext).pop();
                    }
                  },
                  child: const Text('Reset'),
                ),
                TextButton(
                  onPressed: () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    try {
                      await config.setBaseUrl(urlController.text);
                      if (dialogContext.mounted) {
                        Navigator.of(dialogContext).pop();
                      }
                    } catch (error) {
                      setDialogState(() => dialogError = error.toString());
                    }
                  },
                  child: const Text('Save'),
                ),
              ],
            );
          },
        );
      },
    );
    urlController.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final AppLocalizations l10n = AppLocalizations.of(context);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(AppTheme.spacing * 3),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const SizedBox(height: AppTheme.spacing * 4),
                    const Icon(
                      Icons.shopping_basket_rounded,
                      size: 72,
                      color: AppTheme.primary,
                    ),
                    const SizedBox(height: AppTheme.spacing * 2),
                    Text(
                      l10n.appTitle,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: AppTheme.spacing),
                    Text(
                      l10n.homeGreeting,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: AppTheme.textSecondary,
                          ),
                    ),
                    const SizedBox(height: AppTheme.spacing * 4),
                    TextFormField(
                      controller: _identifierController,
                      keyboardType: TextInputType.text,
                      textInputAction: TextInputAction.next,
                      decoration: const InputDecoration(
                        labelText: 'Email or phone',
                        prefixIcon: Icon(Icons.person_outline),
                      ),
                      validator: (String? value) {
                        if (value == null || value.trim().isEmpty) {
                          return 'Enter your email or phone number';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: AppTheme.spacing * 2),
                    TextFormField(
                      controller: _passwordController,
                      obscureText: _obscurePassword,
                      textInputAction: TextInputAction.done,
                      onFieldSubmitted: (_) {
                        if (!_submitting) {
                          _submit();
                        }
                      },
                      decoration: InputDecoration(
                        labelText: 'Password',
                        prefixIcon: const Icon(Icons.lock_outline),
                        suffixIcon: IconButton(
                          icon: Icon(
                            _obscurePassword
                                ? Icons.visibility_outlined
                                : Icons.visibility_off_outlined,
                          ),
                          onPressed: () => setState(
                            () => _obscurePassword = !_obscurePassword,
                          ),
                        ),
                      ),
                      validator: (String? value) {
                        if (value == null || value.isEmpty) {
                          return 'Password is required';
                        }
                        return null;
                      },
                    ),
                    if (_error != null)
                      Padding(
                        padding: const EdgeInsets.only(top: AppTheme.spacing * 2),
                        child: Text(
                          _error!,
                          style: const TextStyle(color: AppTheme.error),
                        ),
                      ),
                    const SizedBox(height: AppTheme.spacing * 3),
                    ElevatedButton(
                      onPressed: _submitting ? null : _submit,
                      child: _submitting
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(l10n.confirm),
                    ),
                    const SizedBox(height: AppTheme.spacing),
                    TextButton(
                      onPressed: _submitting
                          ? null
                          : () => context.push('/register'),
                      child: Text(
                        l10n.isBangla
                            ? 'অ্যাকাউন্ট নেই? রেজিস্টার করুন'
                            : 'No account? Register',
                      ),
                    ),
                    const SizedBox(height: AppTheme.spacing * 2),
                    Consumer<ServerConfig>(
                      builder: (BuildContext context, ServerConfig config, _) {
                        return Column(
                          children: [
                            Text(
                              'Server: ${config.baseUrl}',
                              textAlign: TextAlign.center,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                            TextButton(
                              onPressed: _openServerSettings,
                              child: const Text('Change server URL'),
                            ),
                          ],
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
