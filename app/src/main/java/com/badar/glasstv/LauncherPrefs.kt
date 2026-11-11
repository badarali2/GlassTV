package com.badar.glasstv

import android.content.Context

class LauncherPrefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("glass_launcher_prefs", Context.MODE_PRIVATE)

    private inline fun <reified T : Enum<T>> enumOr(default: T, name: String?): T =
        try {
            if (name == null) default else enumValueOf<T>(name)
        } catch (e: Exception) {
            default
        }

    var clockStyle: IosClockStyle
        get() = enumOr(IosClockStyle.TallLiquid, sp.getString("clock_style", null))
        set(v) { sp.edit().putString("clock_style", v.name).apply() }

    var tintMode: GlassTintMode
        get() = enumOr(GlassTintMode.AutoAdapt, sp.getString("tint_mode", null))
        set(v) { sp.edit().putString("tint_mode", v.name).apply() }

    var depthOverlap: DepthOverlap
        get() = enumOr(DepthOverlap.Medium, sp.getString("depth", null))
        set(v) { sp.edit().putString("depth", v.name).apply() }

    var wallpaperId: String
        get() = sp.getString("wallpaper_id", "twilight") ?: "twilight"
        set(v) { sp.edit().putString("wallpaper_id", v).apply() }

    var customWallpaperUri: String?
        get() = sp.getString("custom_wallpaper_uri", null)
        set(v) { sp.edit().putString("custom_wallpaper_uri", v).apply() }

    var order: List<String>
        get() = (sp.getString("order", "") ?: "").split("|").filter { it.isNotEmpty() }
        set(v) { sp.edit().putString("order", v.joinToString("|")).apply() }

    var hidden: Set<String>
        get() = (sp.getStringSet("hidden", emptySet()) ?: emptySet()).toSet()
        set(v) { sp.edit().putStringSet("hidden", HashSet(v)).apply() }
}
