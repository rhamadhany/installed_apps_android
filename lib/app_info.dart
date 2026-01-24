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

  factory AppInfo.empty()=>
      AppInfo(
        name: "",
        packageName: "",
        icon: Uint8List(0),
        isSystem: false,
        versionName: "",
        versionCode: 0,
        installTime: 0,
        lastUpdateTime: 0,

      );

  factory AppInfo.fromJson(Map<String, dynamic> json) {
    final icon = json["icon"] is Uint8List ? json["icon"] : Uint8List.fromList(
        List<int>.from(json["icon"]));
    return AppInfo(
      name: json["name"],
      packageName: json["packageName"],
      icon: icon,

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
