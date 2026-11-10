import 'package:flutter/material.dart';

/// Localized strings for the rider application.
///
/// The rider vocabulary is deliberately different from the customer app:
/// riders talk about jobs, offers, pickups and payouts.
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
  String get appTitle => isBangla ? 'বিডি কুইকমার্ট রাইডার' : 'BD QuickMart Rider';

  // Navigation
  String get navDashboard => isBangla ? 'ড্যাশবোর্ড' : 'Dashboard';
  String get navAssignments => isBangla ? 'অ্যাসাইনমেন্ট' : 'Assignments';
  String get navDelivery => isBangla ? 'ডেলিভারি' : 'Delivery';
  String get navNavigation => isBangla ? 'নেভিগেশন' : 'Navigation';
  String get navEarnings => isBangla ? 'আয়' : 'Earnings';
  String get navProfile => isBangla ? 'প্রোফাইল' : 'Profile';

  // Availability
  String get online => isBangla ? 'অনলাইন' : 'Online';
  String get offline => isBangla ? 'অফলাইন' : 'Offline';
  String get goOnline => isBangla ? 'অনলাইন হোন' : 'Go online';
  String get goOffline => isBangla ? 'অফলাইন যান' : 'Go offline';

  // Assignments
  String get newOffer => isBangla ? 'নতুন অফার' : 'New offer';
  String get accept => isBangla ? 'গ্রহণ করুন' : 'Accept';
  String get reject => isBangla ? 'বাতিল করুন' : 'Reject';
  String get pickup => isBangla ? 'পিকআপ' : 'Pickup';
  String get dropOff => isBangla ? 'ড্রপ অফ' : 'Drop off';
  String get estimatedPay => isBangla ? 'আনুমানিক পেআউট' : 'Estimated pay';
  String get estimatedDistance => isBangla ? 'আনুমানিক দূরত্ব' : 'Estimated distance';

  // Delivery
  String get noActiveDelivery => isBangla ? 'সক্রিয় ডেলিভারি নেই' : 'No active delivery';
  String get deliveryCompleted => isBangla ? 'ডেলিভারি সম্পন্ন' : 'Delivery completed';
  String get customerLocation => isBangla ? 'গ্রাহকের ঠিকানা' : 'Customer location';

  // Earnings
  String get todayEarnings => isBangla ? 'আজকের আয়' : "Today's earnings";
  String get weeklyEarnings => isBangla ? 'সাপ্তাহিক আয়' : 'Weekly earnings';
  String get completedDeliveries => isBangla ? 'সম্পন্ন ডেলিভারি' : 'Completed deliveries';

  // Common
  String get retry => isBangla ? 'পুনরায় চেষ্টা করুন' : 'Retry';
  String get cancel => isBangla ? 'বাতিল' : 'Cancel';
  String get confirm => isBangla ? 'নিশ্চিত করুন' : 'Confirm';
  String get loading => isBangla ? 'লোড হচ্ছে...' : 'Loading...';
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
