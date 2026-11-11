package com.badar.glasstv

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DynamicIslandOverlay(enabled: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val latest by NotificationHub.latest.collectAsState()
    var shown by remember { mutableStateOf<IslandNotification?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(latest) {
        val n = latest
        if (n != null) {
            shown = n
            expanded = true
            delay(4_000)
            while (focused) delay(500)
            expanded = false
        }
    }

    val current = shown
    Box(
        modifier = modifier
            .animateContentSize(spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow))
            .liquidGlass(RoundedCornerShape(if (expanded) 28.dp else 16.dp), 1.25f)
            .onFocusChanged { focused = it.isFocused }
            .plainClickable(enabled = enabled && expanded) {
                val pm = ctx.packageManager
                val pkg = current?.pkg
                val li = if (pkg != null) pm.getLaunchIntentForPackage(pkg) else null
                if (li != null) {
                    li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { ctx.startActivity(li) } catch (e: Exception) { }
                }
                expanded = false
                focused = false
            }
    ) {
        if (expanded && current != null) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (current.icon != null) {
                    Image(
                        bitmap = current.icon,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF202028))
                    )
                }
                Column(Modifier.widthIn(max = 420.dp)) {
                    GlassText(current.title, size = 16.sp, weight = FontWeight.Bold)
                    if (current.text.isNotBlank()) {
                        GlassText(current.text, size = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        } else {
            Box(Modifier.size(width = 92.dp, height = 26.dp))
        }
    }
}
