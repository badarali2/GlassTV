package com.badar.glasstv

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// ---------------- network status ----------------

data class NetStatus(val connected: Boolean, val isWifi: Boolean, val level: Int)

@Composable
fun rememberNetworkStatus(): State<NetStatus> {
    val appCtx = LocalContext.current.applicationContext
    return produceState(NetStatus(false, false, 0)) {
        while (true) {
            value = readNetwork(appCtx)
            delay(5_000)
        }
    }
}

@Suppress("DEPRECATION")
private fun readNetwork(ctx: Context): NetStatus {
    return try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val eth = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        var level = 0
        if (wifi) {
            val wm = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
            level = WifiManager.calculateSignalLevel(wm.connectionInfo.rssi, 4) + 1
        } else if (eth) {
            level = 4
        }
        NetStatus(wifi || eth, wifi, level)
    } catch (e: Exception) {
        NetStatus(false, false, 0)
    }
}

// ---------------- icons ----------------

@Composable
fun WifiIcon(level: Int, connected: Boolean, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier) {
        val w = size.width
        val c = Offset(w / 2f, size.height * 0.88f)
        val stroke = w * 0.09f
        val radii = floatArrayOf(w * 0.16f, w * 0.31f, w * 0.46f)
        for (i in 0 until 3) {
            val active = connected && level >= i + 2
            val r = radii[i]
            drawArc(
                color = Color.White.copy(alpha = if (active) 0.95f else 0.30f),
                startAngle = -135f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(2f * r, 2f * r),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        drawCircle(
            color = Color.White.copy(alpha = if (connected) 0.95f else 0.30f),
            radius = stroke * 0.9f,
            center = c
        )
    }
}

@Composable
fun GearIcon(modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier) {
        val w = size.width
        val c = Offset(w / 2f, size.height / 2f)
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = w * 0.2f,
            center = c,
            style = Stroke(width = w * 0.1f)
        )
        for (k in 0 until 8) {
            val a = Math.toRadians(k * 45.0)
            val ca = cos(a).toFloat()
            val sa = sin(a).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.95f),
                start = Offset(c.x + ca * w * 0.30f, c.y + sa * w * 0.30f),
                end = Offset(c.x + ca * w * 0.44f, c.y + sa * w * 0.44f),
                strokeWidth = w * 0.12f,
                cap = StrokeCap.Round
            )
        }
    }
}

// ---------------- top-right quick controls ----------------

@Composable
fun GlassIconButton(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, IosSpring, label = "iconBtn")
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(if (focused) Color.White.copy(alpha = 0.32f) else Color.Transparent)
            .plainClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun QuickControls(
    enabled: Boolean,
    net: NetStatus,
    onWifi: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .liquidGlass(RoundedCornerShape(30.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        GlassIconButton(enabled, onWifi) { WifiIcon(net.level, net.connected) }
        GlassIconButton(enabled, onSettings) { GearIcon() }
    }
}

@Composable
fun WifiOverlay(net: NetStatus, onOpenSettings: () -> Unit, onClose: () -> Unit) {
    BackHandler { onClose() }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        try { first.requestFocus() } catch (e: Exception) { }
    }
    val strength = when (net.level) {
        4 -> "Excellent"
        3 -> "Good"
        2 -> "Fair"
        else -> "Weak"
    }
    val status = when {
        !net.connected -> "Not connected"
        net.isWifi -> "Wi-Fi connected, signal: $strength"
        else -> "Ethernet connected"
    }
    Box(Modifier.fillMaxSize().padding(top = 84.dp, end = 48.dp), contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier
                .width(320.dp)
                .liquidGlass(RoundedCornerShape(28.dp), 1.2f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassText("Network", size = 20.sp, weight = FontWeight.Bold)
            GlassText(status, size = 15.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
            GlassButton("Open Wi-Fi settings", Modifier.fillMaxWidth(), focusRequester = first) {
                onOpenSettings(); onClose()
            }
            GlassButton("Close", Modifier.fillMaxWidth()) { onClose() }
        }
    }
}

// ---------------- volume overlay ----------------

class VolumeHud {
    var level by mutableStateOf(0f)
    var muted by mutableStateOf(false)
    var token by mutableStateOf(0)

    fun show(newLevel: Float, isMuted: Boolean) {
        level = newLevel
        muted = isMuted
        token += 1
    }
}

@Composable
fun VolumeOverlay(hud: VolumeHud, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(hud.token) {
        if (hud.token > 0) {
            visible = true
            delay(3_000)
            visible = false
        }
    }
    val level by animateFloatAsState(
        targetValue = if (hud.muted) 0f else hud.level,
        animationSpec = tween(180),
        label = "volLevel"
    )
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it }
    ) {
        Box(
            Modifier
                .padding(end = 40.dp)
                .width(64.dp)
                .height(280.dp)
                .liquidGlass(RoundedCornerShape(32.dp), 1.2f)
                .padding(8.dp)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(level.coerceIn(0f, 1f))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.60f))
                            )
                        )
                )
                GlassText(
                    text = if (hud.muted) "MUTE" else "${(hud.level * 100).toInt()}",
                    size = 14.sp,
                    color = if (level > 0.88f) Color.Black.copy(alpha = 0.75f) else Color.White,
                    weight = FontWeight.Bold,
                    align = TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
                )
            }
        }
    }
}
