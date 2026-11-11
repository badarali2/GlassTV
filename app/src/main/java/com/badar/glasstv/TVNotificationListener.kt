package com.badar.glasstv

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class IslandNotification(
    val key: String,
    val pkg: String,
    val title: String,
    val text: String,
    val icon: ImageBitmap?,
    val stamp: Long = System.nanoTime()
)

/** In-memory only. Nothing is stored on disk or sent anywhere. */
object NotificationHub {
    private val _latest = MutableStateFlow<IslandNotification?>(null)
    val latest: StateFlow<IslandNotification?> = _latest

    fun post(n: IslandNotification) {
        _latest.value = n
    }
}

class TVNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        if (n.packageName == packageName) return
        val notif = n.notification ?: return
        if ((notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((notif.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = notif.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty()
        if (title.isEmpty() && text.isEmpty()) return

        val appLabel = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(n.packageName, 0)).toString()
        } catch (e: Exception) {
            n.packageName
        }
        val icon = try {
            packageManager.getApplicationIcon(n.packageName).toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            null
        }
        NotificationHub.post(
            IslandNotification(n.key ?: n.packageName, n.packageName, title.ifEmpty { appLabel }, text, icon)
        )
    }
}
