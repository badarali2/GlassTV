package com.badar.glasstv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

private val BACK_RANGE = listOf(
    -0.05f to 0.74f, 0.12f to 0.64f, 0.22f to 0.70f, 0.34f to 0.56f, 0.46f to 0.66f,
    0.58f to 0.60f, 0.70f to 0.68f, 0.82f to 0.58f, 0.92f to 0.66f, 1.05f to 0.62f
)

private val FRONT_RIDGE = listOf(
    -0.05f to 0.88f, 0.12f to 0.82f, 0.24f to 0.72f, 0.34f to 0.60f, 0.43f to 0.54f,
    0.50f to 0.52f, 0.57f to 0.54f, 0.66f to 0.60f, 0.76f to 0.70f, 0.88f to 0.80f, 1.05f to 0.86f
)

private fun buildRidge(w: Float, h: Float, pts: List<Pair<Float, Float>>, closed: Boolean): Path {
    val p = Path()
    pts.forEachIndexed { i, pt ->
        if (i == 0) p.moveTo(pt.first * w, pt.second * h) else p.lineTo(pt.first * w, pt.second * h)
    }
    if (closed) {
        p.lineTo(w * 1.05f, h)
        p.lineTo(-w * 0.05f, h)
        p.close()
    }
    return p
}

/**
 * Three-layer depth scene:
 *  Layer 1: wallpaper (built-in sky + distant range, or your own image)
 *  Layer 2: date + liquid glass clock
 *  Layer 3: foreground ridge that overlaps the lower part of the clock (built-in wallpapers only)
 */
@Composable
fun DepthWallpaperScene(
    preset: WallpaperPreset,
    customBitmap: ImageBitmap?,
    useCustom: Boolean,
    colors: WallpaperColors,
    clockStyle: IosClockStyle,
    tintMode: GlassTintMode,
    depth: DepthOverlap,
    parallax: () -> Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val h = maxHeight
        val peakY = h * 0.52f
        val digitsH = clockDigitsHeight(clockStyle)
        val digitTop = maxOf(150.dp, peakY - digitsH * (1f - depth.fraction))
        val layoutTop = digitTop + (digitsH - clockLayoutHeight(clockStyle)) / 2f
        val dateTop = maxOf(70.dp, digitTop - 54.dp)
        val showCustom = useCustom && customBitmap != null

        // Layer 1
        if (showCustom && customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent, Color.Black.copy(alpha = 0.25f))
                        )
                    )
            )
        } else {
            SkyAndBackRange(preset, parallax)
        }

        // Layer 2
        IosDateHeader(Modifier.align(Alignment.TopCenter).offset(y = dateTop))
        LiquidGlassClock(
            style = clockStyle,
            tintMode = tintMode,
            colors = colors,
            modifier = Modifier.align(Alignment.TopCenter).offset(y = layoutTop)
        )

        // Layer 3
        if (!showCustom) {
            ForegroundRidge(preset, parallax)
        }
    }
}

@Composable
private fun SkyAndBackRange(preset: WallpaperPreset, parallax: () -> Float) {
    val stars = remember {
        val r = java.util.Random(7)
        List(90) { Triple(r.nextFloat(), r.nextFloat() * 0.55f, r.nextFloat()) }
    }
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(preset.skyTop, preset.skyMid, preset.skyBottom)))

        val gc = Offset(size.width * 0.5f, size.height * 0.55f)
        val gr = size.width * 0.45f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(preset.glow.copy(alpha = 0.55f), Color.Transparent),
                center = gc,
                radius = gr
            ),
            radius = gr,
            center = gc
        )

        if (preset.stars) {
            for (s in stars) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f + 0.6f * s.third),
                    radius = 0.8f + 1.8f * s.third,
                    center = Offset(s.first * size.width, s.second * size.height)
                )
            }
        }

        translate(left = -parallax() * 6f) {
            val back = buildRidge(size.width, size.height, BACK_RANGE, true)
            drawPath(
                back,
                Brush.verticalGradient(
                    listOf(preset.ridgeBack, preset.ridgeBack.copy(alpha = 0.7f)),
                    startY = size.height * 0.55f,
                    endY = size.height
                )
            )
        }
    }
}

@Composable
private fun ForegroundRidge(preset: WallpaperPreset, parallax: () -> Float) {
    Canvas(Modifier.fillMaxSize()) {
        translate(left = parallax() * 14f) {
            val fill = buildRidge(size.width, size.height, FRONT_RIDGE, true)
            val edge = buildRidge(size.width, size.height, FRONT_RIDGE, false)
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(preset.ridgeFront.copy(alpha = 0.96f), preset.ridgeFront),
                    startY = size.height * 0.5f,
                    endY = size.height
                )
            )
            drawPath(edge, Color.White.copy(alpha = 0.28f), style = Stroke(width = 2f))
        }
    }
}
