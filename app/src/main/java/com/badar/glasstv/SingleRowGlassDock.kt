package com.badar.glasstv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val DockTotalHeight = 168.dp

@Composable
fun SingleRowGlassDock(
    apps: List<AppEntry>,
    enabled: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit,
    onFocusIndex: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val restore = remember { FocusRequester() }
    var lastPkg by remember { mutableStateOf<String?>(null) }
    val target = apps.firstOrNull { it.pkg == lastPkg } ?: apps.firstOrNull()

    LaunchedEffect(enabled, apps.isNotEmpty()) {
        if (enabled && apps.isNotEmpty()) {
            delay(80)
            try { restore.requestFocus() } catch (e: Exception) { }
        }
    }

    Box(modifier.fillMaxWidth().height(DockTotalHeight)) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(128.dp)
                .liquidGlass(RoundedCornerShape(48.dp))
        )
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                DockIcon(
                    app = app,
                    enabled = enabled,
                    requester = if (target != null && app.pkg == target.pkg) restore else null,
                    onFocused = {
                        lastPkg = app.pkg
                        onFocusIndex(index, apps.size)
                    },
                    onLaunch = { onLaunch(app) },
                    onMenu = { onMenu(app) }
                )
            }
        }
    }
}

@Composable
private fun DockIcon(
    app: AppEntry,
    enabled: Boolean,
    requester: FocusRequester?,
    onFocused: () -> Unit,
    onLaunch: () -> Unit,
    onMenu: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    var longFired by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.18f else 1f, IosSpring, label = "iconScale")
    val lift by animateFloatAsState(if (focused) -10f else 0f, IosSpring, label = "iconLift")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.drawBehind {
            if (focused) {
                val c = Offset(size.width / 2f, size.height - 20.dp.toPx())
                val r = 70.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        center = c,
                        radius = r
                    ),
                    radius = r,
                    center = c
                )
            }
        }
    ) {
        Box(
            Modifier.height(36.dp).wrapContentWidth(unbounded = true),
            contentAlignment = Alignment.Center
        ) {
            if (focused) {
                Box(
                    Modifier
                        .liquidGlass(RoundedCornerShape(18.dp), 1.3f)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    GlassText(app.label, size = 15.sp, weight = FontWeight.SemiBold)
                }
            }
        }
        Box(
            Modifier
                .size(88.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = lift
                }
                .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                .onFocusChanged { st ->
                    focused = st.isFocused
                    if (st.isFocused) onFocused()
                }
                .onKeyEvent { ev ->
                    val k = ev.nativeKeyEvent.keyCode
                    val isSelect = k == android.view.KeyEvent.KEYCODE_DPAD_CENTER ||
                        k == android.view.KeyEvent.KEYCODE_ENTER ||
                        k == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
                    if (!isSelect) {
                        false
                    } else {
                        when (ev.type) {
                            KeyEventType.KeyDown -> {
                                val n = ev.nativeKeyEvent
                                if (n.repeatCount == 0) longFired = false
                                if (!longFired && (n.isLongPress || n.repeatCount > 8)) {
                                    longFired = true
                                    onMenu()
                                }
                                true
                            }
                            KeyEventType.KeyUp -> {
                                if (!longFired) onLaunch()
                                longFired = false
                                true
                            }
                            else -> false
                        }
                    }
                }
                .then(if (enabled) Modifier.focusable() else Modifier)
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(Squircle)
                    .background(Color(0xFF202028))
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.05f))
                        ),
                        Squircle
                    )
            )
        }
    }
}

@Composable
fun AppContextMenu(
    app: AppEntry,
    onMoveToFront: () -> Unit,
    onAppInfo: () -> Unit,
    onHide: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        try { first.requestFocus() } catch (e: Exception) { }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .width(320.dp)
                .liquidGlass(RoundedCornerShape(28.dp), 1.2f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassText(app.label, size = 20.sp, weight = FontWeight.Bold)
            GlassButton("Move to Front", Modifier.fillMaxWidth(), focusRequester = first) {
                onMoveToFront(); onClose()
            }
            GlassButton("App Info", Modifier.fillMaxWidth()) {
                onAppInfo(); onClose()
            }
            GlassButton("Hide App", Modifier.fillMaxWidth()) {
                onHide(); onClose()
            }
            GlassButton("Cancel", Modifier.fillMaxWidth()) { onClose() }
        }
    }
}
