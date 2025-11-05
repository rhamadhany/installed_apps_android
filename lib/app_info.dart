import 'dart:typed_data';

class AppInfo {
  final String name;
  final String packageName;
  final Uint8List icon;
  final bool isSystem;
  final String versionName;
  final int versionCode;
  final int installTime;
  final int lastUpdateTime;

  AppInfo({
    required this.name,
    required this.packageName,
    required this.icon,
    required this.isSystem,
    required this.versionName,
    required this.versionCode,
    required this.installTime,
    required this.lastUpdateTime,
  });

  factory AppInfo.fromJson(Map<String, dynamic> json) {
    return AppInfo(
      name: json["name"],
      packageName: json["packageName"],
      icon: json["icon"],
      isSystem: json["isSystem"],
      versionName: json["versionName"],
      versionCode: json["versionCode"],
      installTime: json["installTime"],
      lastUpdateTime: json["lastUpdateTime"],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      "name": name,
      "packageName": packageName,
      "icon": icon,
      "isSystem": isSystem,
      "versionName": versionName,
      "versionCode": versionCode,
      "installTime": installTime,
      "lastUpdateTime": lastUpdateTime,
    };
  }
}
