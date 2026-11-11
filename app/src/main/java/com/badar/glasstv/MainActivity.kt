package com.badar.glasstv

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()
    private val hud = VolumeHud()
    private var notifAccess by mutableStateOf(false)

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            vm.setCustomWallpaper(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherRoot(
                vm = vm,
                hud = hud,
                notificationAccess = notifAccess,
                onLaunch = { app -> launchApp(app) },
                onAppInfo = { app -> openAppInfo(app) },
                onPickWallpaper = { pickWallpaper() },
                onNotificationAccess = { openNotificationAccess() },
                onWifiSettings = { safeStart(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                onSystemSettings = { safeStart(Intent(Settings.ACTION_SETTINGS)) }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refreshApps()
        notifAccess = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        ) {
            if (handleVolume(keyCode)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleVolume(keyCode: Int): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (am.isVolumeFixed) return false
        try {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP ->
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                KeyEvent.KEYCODE_VOLUME_DOWN ->
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0)
                else ->
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
            }
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            hud.show(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max, am.isStreamMute(AudioManager.STREAM_MUSIC))
            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun safeStart(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Not available on this TV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(app: AppEntry) {
        try {
            startActivity(app.launch)
        } catch (e: Exception) {
            Toast.makeText(this, "Can't open " + app.label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppInfo(app: AppEntry) {
        safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.pkg)))
    }

    private fun openNotificationAccess() {
        safeStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun pickWallpaper() {
        try {
            pickImage.launch(arrayOf("image/*"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No file picker on this TV. Install a file manager app.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun LauncherRoot(
    vm: LauncherViewModel,
    hud: VolumeHud,
    notificationAccess: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onPickWallpaper: () -> Unit,
    onNotificationAccess: () -> Unit,
    onWifiSettings: () -> Unit,
    onSystemSettings: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }
    var showWifi by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    val overlayOpen = showSettings || showWifi || menuApp != null

    var parallaxTarget by remember { mutableStateOf(0f) }
    val parallax = animateFloatAsState(
        targetValue = parallaxTarget,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "parallax"
    )
    val net by rememberNetworkStatus()

    // A home screen must never close on Back. Overlays register their own handlers on top of this.
    BackHandler(enabled = true) { }

    Box(Modifier.fillMaxSize()) {
        DepthWallpaperScene(
            preset = Wallpapers.byId(vm.wallpaperId),
            customBitmap = vm.customBitmap,
            useCustom = vm.wallpaperId == "custom",
            colors = vm.wallpaperColors,
            clockStyle = vm.clockStyle,
            tintMode = vm.tintMode,
            depth = vm.depth,
            parallax = { parallax.value },
            modifier = Modifier.fillMaxSize()
        )

        DynamicIslandOverlay(
            enabled = !overlayOpen,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
        )

        QuickControls(
            enabled = !overlayOpen,
            net = net,
            onWifi = { showWifi = true },
            onSettings = { showSettings = true },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 48.dp)
        )

        SingleRowGlassDock(
            apps = vm.apps,
            enabled = !overlayOpen,
            onLaunch = onLaunch,
            onMenu = { menuApp = it },
            onFocusIndex = { index, count ->
                parallaxTarget = if (count <= 1) 0f else (index.toFloat() / (count - 1)) * 2f - 1f
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 48.dp, end = 48.dp, bottom = 32.dp)
        )

        VolumeOverlay(hud, Modifier.align(Alignment.CenterEnd))

        if (overlayOpen) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }

        if (showSettings) {
            GlassSettingsDialog(
                vm = vm,
                notificationAccess = notificationAccess,
                onPickWallpaper = onPickWallpaper,
                onNotificationAccess = onNotificationAccess,
                onSystemSettings = onSystemSettings,
                onClose = { showSettings = false }
            )
        }
        if (showWifi) {
            WifiOverlay(net = net, onOpenSettings = onWifiSettings, onClose = { showWifi = false })
        }
        val m = menuApp
        if (m != null) {
            AppContextMenu(
                app = m,
                onMoveToFront = { vm.moveToFront(m.pkg) },
                onAppInfo = { onAppInfo(m) },
                onHide = { vm.hideApp(m.pkg) },
                onClose = { menuApp = null }
            )
        }
    }
}
