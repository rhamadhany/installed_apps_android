part of "installed_apps.dart";

mixin _InstalledAppsImpl implements _InstalledAppsPlatforms {
  Future<List<AppInfo>> getInstalledApps() async {
    final result = await invokeGetInstalledApps();
    clearList();

    InstalledApps.listApps = (result as List).map((e) {
      var appInfo = AppInfo.fromJson(Map<String, dynamic>.from(e));
      if (appInfo.isSystem) {
        InstalledApps.systemApps.add(appInfo);
      } else {
        InstalledApps.userApps.add(appInfo);
      }
      return appInfo;
    }).toList();
    return InstalledApps.listApps;
  }
}
