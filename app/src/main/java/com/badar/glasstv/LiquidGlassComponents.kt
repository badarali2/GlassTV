package com.badar.glasstv

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Apple-like spring used for every focus animation. */
val IosSpring: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)

/** iOS squircle used for app icons. */
val Squircle = RoundedCornerShape(22.dp)

/**
 * Liquid glass surface: dark translucent base (works on every Android version),
 * soft white inner gradient, and a specular rim that is brighter at the top edge.
 */
fun Modifier.liquidGlass(shape: Shape, strength: Float = 1f): Modifier = this
    .clip(shape)
    .background(Color.Black.copy(alpha = (0.30f * strength).coerceIn(0f, 0.9f)))
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.05f))
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.08f))
        ),
        shape
    )

/** Click handling without the default ripple/overlay. Also makes the node focusable. */
fun Modifier.plainClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
fun GlassText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    color: Color = Color.White,
    weight: FontWeight = FontWeight.Medium,
    maxLines: Int = 1,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    align: TextAlign = TextAlign.Start
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            textAlign = align
        )
    )
}

@Composable
fun GlassSectionTitle(text: String) {
    GlassText(
        text = text.uppercase(),
        size = 12.sp,
        color = Color.White.copy(alpha = 0.6f),
        weight = FontWeight.SemiBold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp)
    )
}

@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, IosSpring, label = "btnScale")
    val shape = RoundedCornerShape(16.dp)
    val fill = when {
        focused -> Color.White.copy(alpha = 0.34f)
        selected -> Color.White.copy(alpha = 0.20f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(fill)
            .border(1.dp, Color.White.copy(alpha = if (focused) 0.85f else 0.22f), shape)
            .plainClickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassText(
            text = text,
            size = 16.sp,
            weight = if (selected) FontWeight.Bold else FontWeight.Medium,
            align = TextAlign.Center
        )
    }
}
