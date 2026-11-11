package com.badar.glasstv

import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private class ClockSpec(
    val size: TextUnit,
    val weight: FontWeight,
    val family: FontFamily,
    val spacing: TextUnit,
    val scaleX: Float,
    val scaleY: Float,
    val fillScale: Float,
    val rimWidth: Float
)

private fun specFor(style: IosClockStyle): ClockSpec = when (style) {
    IosClockStyle.TallLiquid ->
        ClockSpec(116.sp, FontWeight.SemiBold, FontFamily.Default, (-3).sp, 0.84f, 1.54f, 1.00f, 2.6f)
    IosClockStyle.UltraThin ->
        ClockSpec(118.sp, FontWeight.ExtraLight, FontFamily.Default, (-1).sp, 0.86f, 1.52f, 0.78f, 3.2f)
    IosClockStyle.EditorialSerif ->
        ClockSpec(112.sp, FontWeight.Medium, FontFamily.Serif, (-2).sp, 0.88f, 1.44f, 0.95f, 2.4f)
    IosClockStyle.BoldRounded ->
        ClockSpec(110.sp, FontWeight.Bold, FontFamily.SansSerif, (-2).sp, 0.92f, 1.40f, 1.10f, 2.6f)
}

/** Approximate height of the digit glyphs after vertical stretching. */
fun clockDigitsHeight(style: IosClockStyle): Dp {
    val s = specFor(style)
    return (s.size.value * 0.72f * s.scaleY).dp
}

/** Height of the (unscaled) text box. */
fun clockLayoutHeight(style: IosClockStyle): Dp = specFor(style).size.value.dp

@Composable
fun LiquidGlassClock(
    style: IosClockStyle,
    tintMode: GlassTintMode,
    colors: WallpaperColors,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(5_000)
        }
    }
    val is24 = remember { DateFormat.is24HourFormat(ctx) }
    val time = remember(now / 60_000L, is24) {
        SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(Date(now))
    }

    val auto = tintMode == GlassTintMode.AutoAdapt
    val tint by animateColorAsState(
        targetValue = if (auto) colors.vibrant else Color.White,
        animationSpec = tween(900),
        label = "glassTint"
    )
    val light by animateColorAsState(
        targetValue = if (auto) colors.lightVibrant else Color.White,
        animationSpec = tween(900),
        label = "glassLight"
    )

    val spec = specFor(style)
    val s = spec.fillScale

    val fill = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = (0.72f * s).coerceAtMost(1f)),
            tint.copy(alpha = (0.45f * s).coerceAtMost(1f)),
            Color.White.copy(alpha = (0.22f * s).coerceAtMost(1f))
        )
    )
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.85f), light.copy(alpha = 0.35f))
    )

    val base = TextStyle(
        fontFamily = spec.family,
        fontWeight = spec.weight,
        fontSize = spec.size,
        lineHeight = spec.size,
        letterSpacing = spec.spacing,
        fontFeatureSettings = "tnum",
        textAlign = TextAlign.Center
    )

    Box(
        modifier = modifier.height(spec.size.value.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = spec.scaleX
                scaleY = spec.scaleY
            }
        ) {
            // Pass 1: ambient depth shadow
            BasicText(
                text = time,
                maxLines = 1,
                softWrap = false,
                style = base.copy(
                    color = Color.Black.copy(alpha = 0.28f),
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.45f),
                        offset = Offset(0f, 10f),
                        blurRadius = 18f
                    )
                )
            )
            // Pass 2: frosted glass fill
            BasicText(
                text = time,
                maxLines = 1,
                softWrap = false,
                style = base.copy(brush = fill)
            )
            // Pass 3: specular rim
            BasicText(
                text = time,
                maxLines = 1,
                softWrap = false,
                style = base.copy(brush = rim, drawStyle = Stroke(width = spec.rimWidth))
            )
        }
    }
}

@Composable
fun IosDateHeader(modifier: Modifier = Modifier) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(30_000)
        }
    }
    val text = remember(now / 60_000L) {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now)).uppercase()
    }
    GlassText(
        text = text,
        modifier = modifier,
        size = 20.sp,
        color = Color.White.copy(alpha = 0.85f),
        weight = FontWeight.Medium,
        letterSpacing = 3.sp
    )
}
