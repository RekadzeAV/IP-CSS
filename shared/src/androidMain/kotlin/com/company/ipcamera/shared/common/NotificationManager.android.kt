package com.company.ipcamera.shared.common

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import android.app.NotificationManager as AndroidNotificationManager

actual class NotificationManager actual constructor(private val context: Any?) {
    private val androidContext: Context =
        context as? Context
            ?: throw IllegalArgumentException("Context is required for Android NotificationManager")

    private val notificationManager: AndroidNotificationManager by lazy {
        androidContext.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
    }

    private val notificationManagerCompat: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(androidContext)
    }

    private val notificationEventsState = MutableStateFlow<NotificationEvent?>(null)
    val notificationEventsFlow: Flow<NotificationEvent>
        get() = notificationEventsState.asStateFlow().filterNotNull()

    actual fun initialize() {
        // Создаем каналы по умолчанию
        createDefaultChannels()
    }

    actual suspend fun showNotification(notification: NotificationData) {
        val notificationId = notification.id.hashCode()

        val builder =
            NotificationCompat.Builder(androidContext, notification.channelId ?: DEFAULT_CHANNEL_ID)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setSmallIcon(getNotificationIcon(notification.icon))
                .setPriority(mapPriority(notification.priority))
                .setAutoCancel(true)
                .setSound(if (notification.sound) android.provider.Settings.System.DEFAULT_NOTIFICATION_URI else null)
                .setVibrate(if (notification.vibration) longArrayOf(0, 250, 250, 250) else null)
                .setWhen(notification.timestamp)

        // Добавляем экстра данные
        if (notification.extras.isNotEmpty()) {
            val extrasBundle = Bundle()
            notification.extras.forEach { (key, value) ->
                extrasBundle.putString(key, value)
            }
            builder.addExtras(extrasBundle)
        }

        // Создаем Intent для обработки клика
        val intent =
            Intent(androidContext, NotificationReceiverActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(NOTIFICATION_ID_EXTRA, notification.id)
            }

        val pendingIntent =
            PendingIntent.getActivity(
                androidContext,
                notificationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        builder.setContentIntent(pendingIntent)

        notificationManagerCompat.notify(notificationId, builder.build())
    }

    actual suspend fun showNotificationWithActions(
        notification: NotificationData,
        actions: List<NotificationAction>,
    ) {
        val notificationId = notification.id.hashCode()

        val builder =
            NotificationCompat.Builder(androidContext, notification.channelId ?: DEFAULT_CHANNEL_ID)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setSmallIcon(getNotificationIcon(notification.icon))
                .setPriority(mapPriority(notification.priority))
                .setAutoCancel(true)
                .setWhen(notification.timestamp)

        // Добавляем действия
        actions.forEach { action ->
            val actionIntent =
                Intent(androidContext, NotificationActionReceiver::class.java).apply {
                    putExtra(NOTIFICATION_ID_EXTRA, notification.id)
                    putExtra(ACTION_ID_EXTRA, action.id)
                }

            val actionPendingIntent =
                PendingIntent.getBroadcast(
                    androidContext,
                    action.id.hashCode(),
                    actionIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            builder.addAction(
                getActionIcon(action.icon),
                action.title,
                actionPendingIntent,
            )
        }

        notificationManagerCompat.notify(notificationId, builder.build())
    }

    actual fun cancelNotification(notificationId: String) {
        notificationManagerCompat.cancel(notificationId.hashCode())
    }

    actual fun cancelAllNotifications() {
        notificationManagerCompat.cancelAll()
    }

    actual fun createNotificationChannel(
        channelId: String,
        channelName: String,
        description: String,
        importance: NotificationPriority,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    channelId,
                    channelName,
                    mapImportance(importance),
                ).apply {
                    this.description = description
                }

            notificationManager.createNotificationChannel(channel)
        }
    }

    actual suspend fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationManagerCompat.areNotificationsEnabled()
        } else {
            true // Для старых версий Android разрешение не требуется
        }
    }

    actual suspend fun requestPermission(): Boolean {
        // На Android разрешение запрашивается через Activity
        // Это должно быть вызвано из Activity/Fragment
        return hasPermission()
    }

    actual fun getNotificationEvents(): Flow<NotificationEvent> {
        return notificationEventsFlow
    }

    private fun createDefaultChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels =
                listOf(
                    NotificationChannel(
                        DEFAULT_CHANNEL_ID,
                        "Default",
                        android.app.NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "Default notification channel"
                    },
                    NotificationChannel(
                        HIGH_PRIORITY_CHANNEL_ID,
                        "High Priority",
                        android.app.NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = "High priority notifications"
                    },
                )

            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    private fun mapPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.URGENT -> NotificationCompat.PRIORITY_MAX
        }
    }

    private fun mapImportance(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.LOW -> android.app.NotificationManager.IMPORTANCE_LOW
            NotificationPriority.NORMAL -> android.app.NotificationManager.IMPORTANCE_DEFAULT
            NotificationPriority.HIGH -> android.app.NotificationManager.IMPORTANCE_HIGH
            NotificationPriority.URGENT -> android.app.NotificationManager.IMPORTANCE_HIGH
        }
    }

    private fun getNotificationIcon(iconName: String?): Int {
        val resolved = resolveResourceIcon(iconName)
        if (resolved != null) return resolved
        return android.R.drawable.ic_dialog_info
    }

    private fun getActionIcon(iconName: String?): Int {
        val resolved = resolveResourceIcon(iconName)
        if (resolved != null) return resolved
        return android.R.drawable.ic_menu_more
    }

    private fun resolveResourceIcon(iconName: String?): Int? {
        val normalized = iconName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val packageName = androidContext.packageName
        val resources = androidContext.resources

        val directId = resources.getIdentifier(normalized, "drawable", packageName)
        if (directId != 0) return directId

        val mipmapId = resources.getIdentifier(normalized, "mipmap", packageName)
        if (mipmapId != 0) return mipmapId

        // Accept "drawable/icon_name" or "mipmap/icon_name" notation.
        val parts = normalized.split('/')
        if (parts.size == 2) {
            val typedId = resources.getIdentifier(parts[1], parts[0], packageName)
            if (typedId != 0) return typedId
        }

        return null
    }

    companion object {
        private const val DEFAULT_CHANNEL_ID = "default_channel"
        private const val HIGH_PRIORITY_CHANNEL_ID = "high_priority_channel"
        private const val NOTIFICATION_ID_EXTRA = "notification_id"
        private const val ACTION_ID_EXTRA = "action_id"
    }
}

// Вспомогательные классы для обработки уведомлений
// Эти классы должны быть реализованы в Android приложении
class NotificationReceiverActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationId = intent.getStringExtra("notification_id")
        if (!notificationId.isNullOrBlank()) {
            sendBroadcast(
                Intent(ACTION_NOTIFICATION_CLICKED).apply {
                    setPackage(packageName)
                    putExtra("notification_id", notificationId)
                },
            )
        }
        finish()
    }
}

class NotificationActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val notificationId = intent.getStringExtra("notification_id")
        val actionId = intent.getStringExtra("action_id")
        if (!notificationId.isNullOrBlank() && !actionId.isNullOrBlank()) {
            context.sendBroadcast(
                Intent(ACTION_NOTIFICATION_ACTION).apply {
                    setPackage(context.packageName)
                    putExtra("notification_id", notificationId)
                    putExtra("action_id", actionId)
                },
            )
        }
    }
}

const val ACTION_NOTIFICATION_CLICKED = "com.company.ipcamera.NOTIFICATION_CLICKED"
const val ACTION_NOTIFICATION_ACTION = "com.company.ipcamera.NOTIFICATION_ACTION"
