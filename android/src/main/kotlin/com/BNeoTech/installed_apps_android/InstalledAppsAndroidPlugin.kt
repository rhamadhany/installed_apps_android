package com.BNeoTech.installed_apps_android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** InstalledAppsAndroidPlugin */
class InstalledAppsAndroidPlugin :
    FlutterPlugin,
    MethodCallHandler {
    private lateinit var context: Context

    // The MethodChannel that will the communication between Flutter and native Android
    //
    // This local reference serves to register the plugin with the Flutter Engine and unregister it
    // when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "com.BNeoTech.installed_apps_android/channel")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result
    ) {
        when (call.method) {
            "getInstalledApps" -> {
                InstalledApps.getInstalledApps(context, result)
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}


object InstalledApps {
    fun getInstalledApps(context: Context, result: Result) {
        CoroutineScope(Dispatchers.Default).launch {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)
            val parallelList =
                packages.mapNotNull { appInfo ->
                    async {
                        try {
                            val name = appInfo.applicationInfo?.loadLabel(pm)?.toString() ?: appInfo.packageName
                            val isSystem = appInfo.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
                            val icon = appInfo.applicationInfo?.loadIcon(pm)
                                .let { createBitmap(it) }

                            val map = mapOf<String, Any>(
                                "name" to name,
                                "packageName" to appInfo.packageName,
                                "icon" to icon,
                                "isSystem" to isSystem,
                                "versionName" to (appInfo.versionName ?: ""),
                                "versionCode" to appInfo.longVersionCode,
                                "installTime" to appInfo.firstInstallTime,
                                "lastUpdateTime" to appInfo.lastUpdateTime
                            )
                            map
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

            val listResult = parallelList.awaitAll().filterNotNull()
                .sortedBy {
                    (it["name"] as String)
                }

            withContext(Dispatchers.Main) {

                result.success(listResult)
            }
        }
    }

    fun createBitmap(drawable: Drawable?): ByteArray {
        if (drawable == null) return ByteArray(0)
        try {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(newBitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)



            return ByteArrayOutputStream().use { stream ->
                newBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                newBitmap.recycle()
                return stream.toByteArray()
            }
        } catch (e: Exception) {

            return ByteArray(0)
        }
    }
}