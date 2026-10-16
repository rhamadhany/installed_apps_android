package com.BNeoTech.installed_apps_android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val TAG = "InstalledApps"

/**
 * Icon target size in pixels.
 *
 * Sized for a 48 dp icon at ~3x density (144 px). Previously we used the
 * drawable's intrinsic size, which on adaptive-icon devices is often
 * 512×512 — that is 4-30× more pixel data than any consumer needs.
 * Downscaling to 144 px cuts per-icon memory from ~1 MB (512×512 ARGB_8888)
 * to ~80 KB.
 */
private const val ICON_TARGET_PX = 144

/** WEBP lossy quality — 85 hits the "imperceptible to humans" mark. */
private const val ICON_WEBP_QUALITY = 85

/** InstalledAppsAndroidPlugin */
class InstalledAppsAndroidPlugin :
    FlutterPlugin,
    MethodCallHandler {
    private lateinit var context: Context

    // Long-lived scope owned by the plugin. Previously a fresh
    // `CoroutineScope(Dispatchers.Default)` was created on every call to
    // `getInstalledApps` — that leaked a Job per invocation, and if the
    // engine detached mid-flight the coroutine had no supervisor to cancel
    // it cleanly. SupervisorJob means one failing app does not cancel the
    // whole batch.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            "com.BNeoTech.installed_apps_android/channel",
        )
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result,
    ) {
        when (call.method) {
            "getInstalledApps" -> {
                InstalledApps.getInstalledApps(context, scope, result)
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}

object InstalledApps {
    fun getInstalledApps(
        context: Context,
        scope: CoroutineScope,
        result: Result,
    ) {
        scope.launch {
            try {
                val pm = context.packageManager
                val packages = pm.getInstalledPackages(0)

                val numCores = Runtime.getRuntime().availableProcessors()

                // Semaphore bounds *actual* concurrency. Each permit guards
                // one app's full pipeline (load label, load icon, compress,
                // encode). Bounded at numCores so heavy bitmap work does not
                // oversubscribe the CPU pool — that would trigger GC storms
                // and slow the whole scan on low-RAM devices.
                //
                // The previous code split into `numCores * 2` chunks and
                // dispatched them all at once. Result: 2× more coroutines
                // than threads AND no memory guard — 200+ apps could hold
                // dozens of 512×512 ARGB bitmaps in memory simultaneously.
                val concurrency = max(2, numCores)
                val semaphore = Semaphore(concurrency)

                // One coroutine per package (not per chunk). Each one
                // suspends on the semaphore until a permit is free — this is
                // cheap (no thread held) and keeps parallelism bounded even
                // when the device has 500+ installed apps.
                val results = packages
                    .map { appInfo ->
                        async(Dispatchers.Default) {
                            semaphore.withPermit {
                                processApp(appInfo, pm)
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()

                withContext(Dispatchers.Main) {
                    result.success(results)
                }
            } catch (e: Exception) {
                Log.e(TAG, "getInstalledApps failed", e)
                withContext(Dispatchers.Main) {
                    result.error("getInstalledAppsFailed", e.message, null)
                }
            }
        }
    }

    /**
     * Processes a single package: reads label + icon from the APK, renders
     * the icon to a downscaled bitmap, encodes as WEBP. Returns null if the
     * package is broken (e.g. uninstalled mid-scan) — caller filters those
     * out.
     */
    private suspend fun processApp(
        appInfo: PackageInfo,
        pm: PackageManager,
    ): Map<String, Any>? =
        try {
            // Label: reading from APK resources — I/O bound.
            val name = withContext(Dispatchers.IO) {
                appInfo.applicationInfo?.loadLabel(pm)?.toString()
                    ?: appInfo.packageName
            }

            // Icon drawable: also I/O bound (unzips from the APK, inflates
            // XML adaptive icons). Kept on IO so it does not steal a CPU
            // thread from the render/compress phase.
            val drawable = withContext(Dispatchers.IO) {
                runCatching { appInfo.applicationInfo?.loadIcon(pm) }.getOrNull()
            }

            // Render + encode: pure CPU. Runs on the current dispatcher
            // (Default) since the caller already holds a semaphore permit.
            val icon = createIconBytes(drawable)

            val isSystem = appInfo.applicationInfo?.flags
                ?.and(ApplicationInfo.FLAG_SYSTEM) != 0

            mapOf<String, Any>(
                "name" to name,
                "packageName" to appInfo.packageName,
                "icon" to icon,
                "isSystem" to isSystem,
                "versionName" to (appInfo.versionName ?: ""),
                "versionCode" to PackageInfoCompat.getLongVersionCode(appInfo),
                "installTime" to appInfo.firstInstallTime,
                "lastUpdateTime" to appInfo.lastUpdateTime,
            )
        } catch (e: Exception) {
            Log.w(TAG, "skip package ${appInfo.packageName}: ${e.message}")
            null
        }

    /**
     * Renders [drawable] into a fixed [ICON_TARGET_PX]×[ICON_TARGET_PX]
     * ARGB_8888 bitmap and encodes it as WEBP.
     *
     * Changes vs. the old implementation:
     *   - Fixed target size (144 px) instead of intrinsic size. Adaptive
     *     icons used to allocate a 512×512 ARGB buffer per icon; that is
     *     ~1 MB each. At 144 px it is ~80 KB — a 12× reduction.
     *   - WEBP_LOSSY @ 85 instead of PNG @ 100. PNG is lossless and slow;
     *     for icons (which are typically already JPEG/PNG-compressed source
     *     art) WEBP @ 85 is visually identical and 5-10× faster to encode,
     *     with smaller output.
     *   - Uses `try/finally` so the intermediate bitmap is always recycled
     *     even if `compress` throws.
     */
    private fun createIconBytes(drawable: Drawable?): ByteArray {
        if (drawable == null) return ByteArray(0)
        var target: Bitmap? = null
        return try {
            target = Bitmap.createBitmap(
                ICON_TARGET_PX,
                ICON_TARGET_PX,
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(target)
            drawable.setBounds(0, 0, ICON_TARGET_PX, ICON_TARGET_PX)
            drawable.draw(canvas)

            ByteArrayOutputStream().use { stream ->
                target.compress(iconCompressFormat(), ICON_WEBP_QUALITY, stream)
                stream.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(TAG, "createIconBytes failed: ${e.message}")
            ByteArray(0)
        } finally {
            target?.recycle()
        }
    }

    /**
     * Picks the best WEBP format for the current API level.
     *
     *   - API 30+ : WEBP_LOSSY (explicit, non-deprecated)
     *   - API 21-29: WEBP (deprecated on 30+, still the only WEBP encoder
     *                available pre-30, and was lossy by default)
     */
    private fun iconCompressFormat(): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
}
