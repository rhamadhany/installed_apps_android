import 'package:flutter/services.dart';

import 'app_info.dart';

part 'installed_apps_impl.dart';
part 'installed_apps_platforms.dart';

class InstalledApps extends _InstalledAppsPlatforms with _InstalledAppsImpl {
  static final _instance = InstalledApps._internal();

  static List<AppInfo> listApps = [];

  static List<AppInfo> systemApps = [];

  static List<AppInfo> userApps = [];
  static InstalledApps get instance => _instance;
  InstalledApps._internal();
}
