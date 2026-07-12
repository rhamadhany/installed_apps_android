part of "installed_apps.dart";

mixin _InstalledAppsImpl implements _InstalledAppsPlatforms {
  Future<List<AppInfo>> getInstalledApps(
      [List<String> excludes = const []]) async {
    final result = await invokeGetInstalledApps();

    // Offload heavy processing to a separate isolate
    final processed = await Isolate.run(() => processAppsInIsolate((result as List<dynamic>, excludes)));

    InstalledApps.listApps = processed.apps;
    InstalledApps.systemApps = processed.system;
    InstalledApps.userApps = processed.user;
    InstalledApps.listAppsPackages = processed.packages;

    return InstalledApps.listApps;
  }

}
