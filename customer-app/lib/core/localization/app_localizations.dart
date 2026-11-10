import 'package:flutter/material.dart';

/// Localized strings for the customer application.
///
/// Strings are held in a single class rather than generated ARB files for now;
/// this keeps the foundation lightweight while allowing the full
/// `flutter_localizations` + ARB workflow to be introduced later without
/// changing call sites.
class AppLocalizations {
  AppLocalizations(this.locale);

  final Locale locale;

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  static const List<Locale> supportedLocales = [
    Locale('bn'),
    Locale('en'),
  ];

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations) ??
        AppLocalizations(const Locale('bn'));
  }

  bool get isBangla => locale.languageCode == 'bn';

  // Application
  String get appTitle => isBangla ? 'বিডি কুইকমার্ট' : 'BD QuickMart';

  // Navigation
  String get navHome => isBangla ? 'হোম' : 'Home';
  String get navProducts => isBangla ? 'পণ্য' : 'Products';
  String get navCart => isBangla ? 'কার্ট' : 'Cart';
  String get navOrders => isBangla ? 'অর্ডার' : 'Orders';
  String get navProfile => isBangla ? 'প্রোফাইল' : 'Profile';

  // Common actions
  String get retry => isBangla ? 'পুনরায় চেষ্টা করুন' : 'Retry';
  String get cancel => isBangla ? 'বাতিল' : 'Cancel';
  String get confirm => isBangla ? 'নিশ্চিত করুন' : 'Confirm';
  String get search => isBangla ? 'খুঁজুন' : 'Search';
  String get loading => isBangla ? 'লোড হচ্ছে...' : 'Loading...';

  // Home
  String get homeGreeting => isBangla ? 'স্বাগতম' : 'Welcome';
  String get homeDeliveryAddress => isBangla ? 'ডেলিভারি ঠিকানা' : 'Delivery address';
  String get homeSearchHint =>
      isBangla ? 'আপনার প্রয়োজনীয় পণ্য খুঁজুন' : 'Search for what you need';

  // Cart
  String get cartEmpty => isBangla ? 'আপনার কার্ট খালি' : 'Your cart is empty';
  String get cartTotal => isBangla ? 'সর্বমোট' : 'Total';
  String get cartCheckout => isBangla ? 'চেকআউট' : 'Checkout';

  // Orders
  String get ordersEmpty => isBangla ? 'কোনো অর্ডার নেই' : 'No orders yet';
  String get orderStatus => isBangla ? 'অর্ডারের অবস্থা' : 'Order status';

  // Errors
  String get errorGeneric =>
      isBangla ? 'কিছু একটা সমস্যা হয়েছে' : 'Something went wrong';
  String get errorNetwork =>
      isBangla ? 'ইন্টারনেট সংযোগে সমস্যা হচ্ছে' : 'Network connection problem';

  // Localization
  String get languageEnglish => 'English';
  String get languageBangla => 'বাংলা';
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => ['bn', 'en'].contains(locale.languageCode);

  @override
  Future<AppLocalizations> load(Locale locale) async => AppLocalizations(locale);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
