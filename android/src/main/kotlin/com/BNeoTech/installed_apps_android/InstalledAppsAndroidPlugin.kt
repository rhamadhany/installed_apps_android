package com.BNeoTech.installed_apps_android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.pm.PackageInfoCompat
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
            
            // Get number of available CPU cores
            val numCores = Runtime.getRuntime().availableProcessors()
            
            // Chunk packages based on number of cores
            val chunkSize = (packages.size + numCores - 1) / numCores
            val chunks = packages.chunked(chunkSize.coerceAtLeast(1))
            
            // Process each chunk in parallel
            val parallelChunks = chunks.map { chunk ->
                async {
                    chunk.mapNotNull { appInfo ->
                        try {
                            val name = appInfo.applicationInfo?.loadLabel(pm)?.toString() ?: appInfo.packageName
                            val isSystem = appInfo.applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
                            val icon = appInfo.applicationInfo?.loadIcon(pm)
                                .let { createBitmap(it) }

                            val versionCode = PackageInfoCompat.getLongVersionCode(appInfo)

                            val map = mapOf<String, Any>(
                                "name" to name,
                                "packageName" to appInfo.packageName,
                                "icon" to icon,
                                "isSystem" to isSystem,
                                "versionName" to (appInfo.versionName ?: ""),
                                "versionCode" to versionCode,
                                "installTime" to appInfo.firstInstallTime,
                                "lastUpdateTime" to appInfo.lastUpdateTime
                            )
                            map
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }

            // Await all chunks and flatten results
            val listResult = parallelChunks.awaitAll()
                .flatten()
                .filterNotNull()
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