part of "installed_apps.dart";

mixin _InstalledAppsImpl implements _InstalledAppsPlatforms {
  Future<List<AppInfo>> getInstalledApps(
      [List<String> excludes = const []]) async {
    final result = await invokeGetInstalledApps();
    clearList();

    final excludeSet = excludes.toSet();

    InstalledApps.listApps = (result as List)
        .map((e) => AppInfo.fromJson(Map<String, dynamic>.from(e)))
        .where((appInfo) => !excludeSet.contains(appInfo.packageName))
        .map((appInfo) {
      if (appInfo.isSystem) {
        InstalledApps.systemApps.add(appInfo);
      } else {
        InstalledApps.userApps.add(appInfo);
      }
      return appInfo;
    })
        .toList();

    InstalledApps.listAppsPackages =
        InstalledApps.listApps.map((e) => e.packageName,).toList();

    return InstalledApps.listApps;
  }

}
