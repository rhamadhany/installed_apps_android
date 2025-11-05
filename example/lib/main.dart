import 'package:flutter/material.dart';
import 'package:installed_apps_android/installed_apps.dart';

void main() {
  runApp(MaterialApp(home: MyApp()));
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  // List<AppInfo>? listApps;

  bool isLoading = false;

  @override
  Widget build(BuildContext context) {
    var listApps = InstalledApps.listApps;
    return Scaffold(
      appBar: AppBar(
        title: Text(
          "App Installed",
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        actions: [IconButton(onPressed: loadApps, icon: Icon(Icons.refresh))],
      ),
      body: Center(
        child: isLoading
            ? CircularProgressIndicator(color: Colors.deepPurple)
            : listApps.isNotEmpty
            ? ListView.builder(
                itemCount: listApps.length,
                itemBuilder: (context, index) {
                  final app = listApps[index];
                  return Card(
                    child: ListTile(
                      title: Text(
                        app.name,
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      leading: Image.memory(app.icon),
                      subtitle: Text(
                        "${app.packageName}\nversion: ${app.versionName}",
                      ),
                    ),
                  );
                },
              )
            : Text(
                "List apps is null!",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 28),
              ),
      ),
    );
  }

  void loadApps() async {
    setState(() {
      isLoading = true;
    });

    try {
      await InstalledApps.instance.getInstalledApps();
    } catch (e) {
      // print("failed to load apps: $e");
    }
    setState(() {
      isLoading = false;
    });
  }
}
