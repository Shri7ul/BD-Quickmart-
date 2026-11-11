/// Minimal key/value abstraction for non-sensitive settings.
///
/// Keeping this behind an interface lets unit and widget tests supply an
/// in-memory implementation instead of touching platform channels.
abstract class KeyValueStore {
  Future<String?> read(String key);
  Future<void> write(String key, String value);
  Future<void> remove(String key);
}

/// In-memory [KeyValueStore] used by tests.
class InMemoryKeyValueStore implements KeyValueStore {
  final Map<String, String> _values = <String, String>{};

  @override
  Future<String?> read(String key) async => _values[key];

  @override
  Future<void> write(String key, String value) async {
    _values[key] = value;
  }

  @override
  Future<void> remove(String key) async {
    _values.remove(key);
  }
}
