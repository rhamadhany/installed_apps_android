part of "installed_apps.dart";

abstract class _InstalledAppsPlatforms {
  final channel = MethodChannel("com.BNeoTech.installed_apps_android/channel");

  void clearList() {
    InstalledApps.listApps.clear();
    InstalledApps.userApps.clear();
    InstalledApps.systemApps.clear();
  }

  Future<List?> invokeGetInstalledApps() async {
    return await channel.invokeMethod("getInstalledApps");
  }
}
