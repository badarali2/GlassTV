package com.badar.glasstv

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = LauncherPrefs(app)
    private var allApps: List<AppEntry> = emptyList()
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var clockStyle by mutableStateOf(prefs.clockStyle)
        private set
    var tintMode by mutableStateOf(prefs.tintMode)
        private set
    var depth by mutableStateOf(prefs.depthOverlap)
        private set
    var wallpaperId by mutableStateOf(prefs.wallpaperId)
        private set
    var customBitmap by mutableStateOf<ImageBitmap?>(null)
        private set
    private var customColors by mutableStateOf<WallpaperColors?>(null)

    val wallpaperColors: WallpaperColors
        get() {
            val c = customColors
            return if (wallpaperId == "custom" && c != null) c
            else WallpaperColorExtractor.fromPreset(Wallpapers.byId(wallpaperId))
        }

    init {
        if (wallpaperId == "custom") {
            val u = prefs.customWallpaperUri
            if (u != null) loadCustom(Uri.parse(u), true) else wallpaperId = "twilight"
        }
        refreshApps()
    }

    // ---------- settings ----------
    fun setClockStyle(v: IosClockStyle) { clockStyle = v; prefs.clockStyle = v }
    fun setTintMode(v: GlassTintMode) { tintMode = v; prefs.tintMode = v }
    fun setDepth(v: DepthOverlap) { depth = v; prefs.depthOverlap = v }
    fun selectPreset(id: String) { wallpaperId = id; prefs.wallpaperId = id }

    fun setCustomWallpaper(uri: Uri) = loadCustom(uri, false)

    private fun loadCustom(uri: Uri, isRestore: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            val bmp = try { decodeSampled(ctx, uri) } catch (e: Exception) { null }
            if (bmp == null) {
                if (isRestore) {
                    withContext(Dispatchers.Main) { wallpaperId = "twilight"; prefs.wallpaperId = "twilight" }
                }
                return@launch
            }
            val colors = WallpaperColorExtractor.fromBitmap(bmp)
            withContext(Dispatchers.Main) {
                customBitmap = bmp.asImageBitmap()
                customColors = colors
                wallpaperId = "custom"
                prefs.wallpaperId = "custom"
                prefs.customWallpaperUri = uri.toString()
            }
        }
    }

    private fun decodeSampled(ctx: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1920) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    // ---------- apps ----------
    fun refreshApps() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = queryApps()
            withContext(Dispatchers.Main) {
                allApps = list
                rebuild()
            }
        }
    }

    private fun queryApps(): List<AppEntry> {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val found = LinkedHashMap<String, AppEntry>()
        val cats = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
        for (cat in cats) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                try {
                    val pkg = ri.activityInfo.packageName
                    if (pkg == ctx.packageName || found.containsKey(pkg)) continue
                    val launch = Intent(Intent.ACTION_MAIN)
                        .setClassName(pkg, ri.activityInfo.name)
                        .addCategory(cat)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    val icon = iconCache.getOrPut(pkg) { ri.loadIcon(pm).toBitmap(160, 160).asImageBitmap() }
                    found[pkg] = AppEntry(ri.loadLabel(pm).toString(), pkg, launch, icon)
                } catch (e: Exception) {
                    // skip apps whose icon or label cannot be loaded
                }
            }
        }
        return found.values.toList()
    }

    private fun rebuild() {
        val hidden = prefs.hidden
        val idx = prefs.order.withIndex().associate { it.value to it.index }
        apps = allApps
            .filter { it.pkg !in hidden }
            .sortedWith(
                compareBy<AppEntry>({ idx[it.pkg] ?: Int.MAX_VALUE }, { it.label.lowercase() })
            )
    }

    fun moveToFront(pkg: String) {
        prefs.order = listOf(pkg) + apps.map { it.pkg }.filter { it != pkg }
        rebuild()
    }

    fun hideApp(pkg: String) {
        prefs.hidden = prefs.hidden + pkg
        rebuild()
    }

    fun unhideAll() {
        prefs.hidden = emptySet()
        rebuild()
    }
}
