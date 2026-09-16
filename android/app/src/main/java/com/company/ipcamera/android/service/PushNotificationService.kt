package com.company.ipcamera.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.company.ipcamera.android.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.context.GlobalContext

/**
 * FCM push-сервис главного Android-модуля (:android:app).
 *
 * Каналы уведомлений (camera_events / system_alerts / recordings) перенесены из
 * архивного дубля platforms/client-android (archive/client-android-dup-2026-09-04).
 *
 * Отличие от архивной версии: FCM-токен регистрируется на сервере по-настоящему —
 * POST /api/v1/notifications/push-tokens через DI OkHttpClient с
 * [com.company.ipcamera.android.media.ApiClientCookieJar] (JWT httpOnly cookies
 * прикладываются автоматически), вместо логирования в logcat.
 *
 * Активация: зависимость firebase-messaging включена всегда, манифест объявляет
 * сервис безусловно. Без google-services.json FirebaseInitProvider не
 * инициализирует Firebase — сервис не активируется, приложение работает штатно.
 */
class PushNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PushTokenRegistrar.register(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"] ?: "unknown"
        val title = message.notification?.title ?: message.data["title"] ?: defaultTitle(type)
        val body = message.notification?.body ?: message.data["body"] ?: ""
        showNotification(type, title, body, message.data["cameraId"])
    }

    private fun showNotification(type: String, title: String, body: String, cameraId: String?) {
        val notificationId = "${NOTIFICATION_PREFIX}${System.currentTimeMillis()}".hashCode()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("cameraId", cameraId)
            putExtra("eventType", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelIdFor(type))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.w(TAG, "Missing POST_NOTIFICATIONS permission")
        }
    }

    private fun channelIdFor(type: String): String = when {
        type.startsWith("motion") || type.startsWith("object") || type.startsWith("face") -> CHANNEL_CAMERA_EVENTS
        type.startsWith("camera_") || type == "error" -> CHANNEL_SYSTEM_ALERTS
        type.startsWith("recording") -> CHANNEL_RECORDINGS
        else -> CHANNEL_SYSTEM_ALERTS
    }

    private fun defaultTitle(type: String): String = when (type) {
        "motion" -> "Motion Detected"
        "object_detected" -> "Object Detected"
        "face_detected" -> "Face Recognized"
        "anpr" -> "License Plate Detected"
        "camera_offline" -> "Camera Offline"
        "camera_online" -> "Camera Online"
        "recording_started" -> "Recording Started"
        "recording_completed" -> "Recording Completed"
        else -> "IP-CSS Alert"
    }

    companion object {
        private const val TAG = "PushNotification"
        private const val NOTIFICATION_PREFIX = "ipcss_"
        const val CHANNEL_CAMERA_EVENTS = "camera_events"
        const val CHANNEL_SYSTEM_ALERTS = "system_alerts"
        const val CHANNEL_RECORDINGS = "recordings"

        /**
         * Создание каналов уведомлений. Вызывается из MainActivity.onCreate —
         * независимо от наличия google-services.json (каналы нужны и локальным
         * сервисам: RecordingService, CameraMonitoringService).
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channels = listOf(
                    NotificationChannel(
                        CHANNEL_CAMERA_EVENTS,
                        "Camera Events",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Motion detection, object detection, face recognition alerts"
                        enableVibration(true)
                        enableLights(true)
                    },
                    NotificationChannel(
                        CHANNEL_SYSTEM_ALERTS,
                        "System Alerts",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Camera offline, errors, warnings"
                    },
                    NotificationChannel(
                        CHANNEL_RECORDINGS,
                        "Recordings",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Recording status, exports"
                    }
                )
                channels.forEach { manager.createNotificationChannel(it) }
            }
        }
    }
}

/**
 * Регистрация FCM-токена на сервере: POST /api/v1/notifications/push-tokens
 * (контракт RegisterPushTokenRequest(token, platform)).
 *
 * Использует DI OkHttpClient с ApiClientCookieJar (JWT httpOnly cookies),
 * поэтому отдельная авторизация не требуется. Ошибки не роняют сервис.
 */
object PushTokenRegistrar {
    private const val TAG = "PushTokenRegistrar"
    private const val PUSH_TOKENS_PATH = "/api/v1/notifications/push-tokens"
    const val PLATFORM = "android"

    fun register(token: String) {
        Thread {
            try {
                val koin = GlobalContext.getOrNull() ?: return@Thread
                val okClient = koin.get<OkHttpClient>()
                val base = koin.get<com.company.ipcamera.core.network.ApiClientConfig>().baseUrl
                val url = base.trimEnd('/') + PUSH_TOKENS_PATH
                val body = """{"token":"$token","platform":"$PLATFORM"}"""
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).post(body).build()
                okClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        android.util.Log.i(TAG, "FCM token registered (HTTP ${resp.code})")
                    } else {
                        android.util.Log.w(
                            TAG,
                            "push-token register HTTP ${resp.code} (auth may be required)"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "push-token registration failed", e)
            }
        }.start()
    }
}
