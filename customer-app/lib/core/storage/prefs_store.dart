import 'package:shared_preferences/shared_preferences.dart';

import 'key_value_store.dart';

/// [KeyValueStore] backed by `shared_preferences`.
///
/// Used only for non-sensitive settings (the runtime server URL override).
/// Authentication tokens are stored separately in the secure token store.
class PrefsKeyValueStore implements KeyValueStore {
  PrefsKeyValueStore(this._prefs);

  final SharedPreferences _prefs;

  /// Creates the store after the platform plugin is ready.
  static Future<PrefsKeyValueStore> create() async =>
      PrefsKeyValueStore(await SharedPreferences.getInstance());

  @override
  Future<String?> read(String key) async => _prefs.getString(key);

  @override
  Future<void> write(String key, String value) async {
    await _prefs.setString(key, value);
  }

  @override
  Future<void> remove(String key) async {
    await _prefs.remove(key);
  }
}
