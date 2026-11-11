package com.badar.glasstv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun GlassSettingsDialog(
    vm: LauncherViewModel,
    notificationAccess: Boolean,
    onPickWallpaper: () -> Unit,
    onNotificationAccess: () -> Unit,
    onSystemSettings: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        try { first.requestFocus() } catch (e: Exception) { }
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(end = 48.dp, top = 72.dp, bottom = 40.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            Modifier
                .width(360.dp)
                .fillMaxHeight()
                .liquidGlass(RoundedCornerShape(32.dp), 1.2f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassText("Settings", size = 24.sp, weight = FontWeight.Bold)

            GlassSectionTitle("Wallpaper")
            Wallpapers.presets.forEachIndexed { i, p ->
                GlassButton(
                    text = p.name,
                    modifier = Modifier.fillMaxWidth(),
                    selected = vm.wallpaperId == p.id,
                    focusRequester = if (i == 0) first else null
                ) { vm.selectPreset(p.id) }
            }
            GlassButton(
                text = "Custom image from storage",
                modifier = Modifier.fillMaxWidth(),
                selected = vm.wallpaperId == "custom"
            ) { onPickWallpaper() }

            GlassSectionTitle("Clock style")
            IosClockStyle.values().forEach { st ->
                GlassButton(
                    text = st.label,
                    modifier = Modifier.fillMaxWidth(),
                    selected = vm.clockStyle == st
                ) { vm.updateClockStyle(st) }
            }

            GlassSectionTitle("Glass tint")
            GlassTintMode.values().forEach { m ->
                GlassButton(
                    text = m.label,
                    modifier = Modifier.fillMaxWidth(),
                    selected = vm.tintMode == m
                ) { vm.updateTintMode(m) }
            }

            GlassSectionTitle("Depth overlap height")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DepthOverlap.values().forEach { d ->
                    GlassButton(
                        text = d.label,
                        modifier = Modifier.weight(1f),
                        selected = vm.depth == d
                    ) { vm.updateDepth(d) }
                }
            }

            GlassSectionTitle("Notifications")
            GlassButton(
                text = if (notificationAccess) "Notification access: ON" else "Enable notification access",
                modifier = Modifier.fillMaxWidth(),
                selected = notificationAccess
            ) { onNotificationAccess() }

            GlassSectionTitle("System")
            GlassButton("Show all hidden apps", Modifier.fillMaxWidth()) { vm.unhideAll() }
            GlassButton("Android TV system settings", Modifier.fillMaxWidth()) { onSystemSettings() }
            GlassButton("Close", Modifier.fillMaxWidth()) { onClose() }
        }
    }
}
