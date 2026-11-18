package com.example.launcher.system

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveNotificationItem(
  val key: String,
  val packageName: String,
  val appName: String,
  val title: String,
  val text: String,
  val postTime: Long,
  val isOngoing: Boolean,
  val isClearable: Boolean,
  val contentIntent: PendingIntent?,
)

data class ActiveMediaTrack(
  val title: String,
  val artist: String,
  val packageName: String,
  val isPlaying: Boolean,
)

object NotificationStreamHub {
  private val _isConnected = MutableStateFlow(false)
  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

  private val _notifications = MutableStateFlow<List<LiveNotificationItem>>(emptyList())
  val notifications: StateFlow<List<LiveNotificationItem>> = _notifications.asStateFlow()

  private val _activeMedia = MutableStateFlow<ActiveMediaTrack?>(null)
  val activeMedia: StateFlow<ActiveMediaTrack?> = _activeMedia.asStateFlow()

  @Volatile
  internal var activeService: AetherNotificationListenerService? = null

  fun isNotificationAccessGranted(context: Context): Boolean {
    val flat =
      Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        ?: return false
    val cn = ComponentName(context, AetherNotificationListenerService::class.java).flattenToString()
    val shortCn =
      ComponentName(context, AetherNotificationListenerService::class.java).flattenToShortString()
    return flat.split(":").any { it.equals(cn, ignoreCase = true) || it.equals(shortCn, ignoreCase = true) }
  }

  fun openNotificationAccessSettings(context: Context) {
    runCatching {
      val intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    }
  }

  fun dismissNotification(key: String) {
    runCatching { activeService?.cancelNotification(key) }
  }

  fun clearClearableNotifications() {
    runCatching {
      _notifications.value
        .filter { it.isClearable }
        .forEach { activeService?.cancelNotification(it.key) }
    }
  }

  internal fun setConnected(connected: Boolean) {
    _isConnected.value = connected
    if (!connected) {
      _notifications.value = emptyList()
      _activeMedia.value = null
    }
  }

  internal fun publishNotifications(items: List<LiveNotificationItem>) {
    _notifications.value = items
  }

  internal fun publishMedia(track: ActiveMediaTrack?) {
    _activeMedia.value = track
  }
}

class AetherNotificationListenerService : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    NotificationStreamHub.activeService = this
    NotificationStreamHub.setConnected(true)
    refreshAll()
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    NotificationStreamHub.activeService = null
    NotificationStreamHub.setConnected(false)
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    refreshAll()
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    refreshAll()
  }

  fun refreshAll() {
    val pm = packageManager
    val list =
      runCatching { activeNotifications }
        .getOrNull()
        ?.mapNotNull { sbn ->
          val extras = sbn.notification?.extras ?: return@mapNotNull null
          val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
          val text =
            (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
              ?: extras.getCharSequence(Notification.EXTRA_TEXT))
              ?.toString()
              ?.trim()
              .orEmpty()
          if (title.isBlank() && text.isBlank()) return@mapNotNull null
          val appLabel =
            runCatching {
              val ai = pm.getApplicationInfo(sbn.packageName, 0)
              pm.getApplicationLabel(ai).toString()
            }.getOrDefault(sbn.packageName.substringAfterLast('.'))

          LiveNotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appLabel,
            title = title.ifBlank { appLabel },
            text = text,
            postTime = sbn.postTime,
            isOngoing = sbn.isOngoing,
            isClearable = sbn.isClearable,
            contentIntent = sbn.notification.contentIntent,
          )
        }
        ?.sortedByDescending { it.postTime }
        ?.take(25)
        ?: emptyList()

    NotificationStreamHub.publishNotifications(list)
    refreshMediaSession()
  }

  private fun refreshMediaSession() {
    val track =
      runCatching {
        val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        val component = ComponentName(this, AetherNotificationListenerService::class.java)
        val controllers = msm.getActiveSessions(component)
        val primary =
          controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
          } ?: controllers.firstOrNull()
        val meta = primary?.metadata
        val state = primary?.playbackState?.state
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (title.isBlank()) null
        else {
          ActiveMediaTrack(
            title = title,
            artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty().ifBlank { "Now Playing" },
            packageName = primary?.packageName.orEmpty(),
            isPlaying = state == PlaybackState.STATE_PLAYING,
          )
        }
      }.getOrNull()
    NotificationStreamHub.publishMedia(track)
  }
}
