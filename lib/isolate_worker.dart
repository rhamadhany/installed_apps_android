import 'app_info.dart';

/// Top-level entry point for isolate processing.
/// Must be top-level (not a method) because it's passed to Isolate.run.
Future<({List<AppInfo> apps, List<AppInfo> system, List<AppInfo> user, List<String> packages})> processAppsInIsolate(
    (List<dynamic>, List<String>) args) async {
  final (rawData, excludes) = args;
  final excludeSet = excludes.toSet();

  // Parse & filter
  final apps = rawData
      .map((e) => AppInfo.fromJson(Map<String, dynamic>.from(e)))
      .where((app) => !excludeSet.contains(app.packageName))
      .toList()
    ..sort((a, b) => a.name.compareTo(b.name));

  // Categorize
  final system = <AppInfo>[];
  final user = <AppInfo>[];
  for (final app in apps) {
    if (app.isSystem) {
      system.add(app);
    } else {
      user.add(app);
    }
  }

  final packages = apps.map((e) => e.packageName).toList();

  return (apps: apps, system: system, user: user, packages: packages);
}
