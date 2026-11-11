package com.badar.glasstv

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette

object WallpaperColorExtractor {

    fun fromBitmap(bitmap: Bitmap): WallpaperColors {
        val w = 160
        val h = (160f * bitmap.height / bitmap.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val p = Palette.from(small).maximumColorCount(24).generate()
        val dominant = p.getDominantColor(0xFF3A3A5A.toInt())
        val vibrant = p.getVibrantColor(p.getLightVibrantColor(dominant))
        val light = p.getLightVibrantColor(vibrant)
        return WallpaperColors(Color(vibrant), Color(light), Color(dominant))
    }

    fun fromPreset(preset: WallpaperPreset): WallpaperColors = preset.colors
}
