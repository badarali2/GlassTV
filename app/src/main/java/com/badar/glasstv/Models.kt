package com.badar.glasstv

import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

enum class IosClockStyle(val label: String) {
    TallLiquid("Tall Liquid Condensed"),
    UltraThin("Ultra-Thin Air Glass"),
    EditorialSerif("Editorial Tall Serif"),
    BoldRounded("Bold Spatial Rounded")
}

enum class GlassTintMode(val label: String) {
    AutoAdapt("Auto-Adapt Wallpaper"),
    PureCrystal("Pure Crystal Clear")
}

enum class DepthOverlap(val label: String, val fraction: Float) {
    Low("Low", 0.12f),
    Medium("Medium", 0.25f),
    High("High", 0.38f)
}

data class WallpaperColors(val vibrant: Color, val lightVibrant: Color, val dominant: Color)

class WallpaperPreset(
    val id: String,
    val name: String,
    val skyTop: Color,
    val skyMid: Color,
    val skyBottom: Color,
    val ridgeFront: Color,
    val ridgeBack: Color,
    val glow: Color,
    val colors: WallpaperColors,
    val stars: Boolean
)

object Wallpapers {
    val presets: List<WallpaperPreset> = listOf(
        WallpaperPreset(
            id = "twilight", name = "Evening Purple Mountain",
            skyTop = Color(0xFF120B2E), skyMid = Color(0xFF3A1F6B), skyBottom = Color(0xFFB5527E),
            ridgeFront = Color(0xFF0A0714), ridgeBack = Color(0xFF2B1650), glow = Color(0xFFFF8FB8),
            colors = WallpaperColors(Color(0xFFD36BFF), Color(0xFFFFB3E6), Color(0xFF4A2A82)),
            stars = true
        ),
        WallpaperPreset(
            id = "ocean", name = "Midnight Blue Peak",
            skyTop = Color(0xFF04122E), skyMid = Color(0xFF0B3A7A), skyBottom = Color(0xFF3CA6D8),
            ridgeFront = Color(0xFF031020), ridgeBack = Color(0xFF0C2A55), glow = Color(0xFF7FE0FF),
            colors = WallpaperColors(Color(0xFF3FD0FF), Color(0xFFA6F0FF), Color(0xFF0B3A7A)),
            stars = true
        ),
        WallpaperPreset(
            id = "sunset", name = "Amber Sunset Ridge",
            skyTop = Color(0xFF2A0F2E), skyMid = Color(0xFFB23A48), skyBottom = Color(0xFFFFA24B),
            ridgeFront = Color(0xFF1A0A10), ridgeBack = Color(0xFF5A1F3A), glow = Color(0xFFFFD27A),
            colors = WallpaperColors(Color(0xFFFFA23A), Color(0xFFFFD9A0), Color(0xFFB23A48)),
            stars = false
        )
    )

    fun byId(id: String): WallpaperPreset = presets.firstOrNull { it.id == id } ?: presets[0]
}

class AppEntry(
    val label: String,
    val pkg: String,
    val launch: Intent,
    val icon: ImageBitmap
)
