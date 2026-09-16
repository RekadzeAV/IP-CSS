package com.company.ipcamera.android.services

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

class PushNotificationService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_CAMERA_EVENTS = "camera_events"
        private const val CHANNEL_SYSTEM_ALERTS = "system_alerts"
        private const val CHANNEL_RECORDINGS = "recordings"
        private const val NOTIFICATION_PREFIX = "ipcss_"

        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"] ?: "unknown"
        val title = message.notification?.title ?: message.data["title"] ?: getDefaultTitle(type)
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val cameraId = message.data["cameraId"]
        showNotification(type, title, body, cameraId)
    }

    private fun sendTokenToServer(token: String) {
        android.util.Log.d("PushNotification", "FCM Token: $token")
    }

    private fun showNotification(type: String, title: String, body: String, cameraId: String?) {
        val notificationId = "${NOTIFICATION_PREFIX}${System.currentTimeMillis()}".hashCode()
        val channelId = getChannelId(type)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("cameraId", cameraId)
            putExtra("eventType", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("PushNotification", "Missing POST_NOTIFICATIONS permission")
        }
    }

    private fun getChannelId(type: String): String {
        return when {
            type.startsWith("motion") || type.startsWith("object") || type.startsWith("face") ->
                CHANNEL_CAMERA_EVENTS
            type.startsWith("camera_") || type == "error" ->
                CHANNEL_SYSTEM_ALERTS
            type.startsWith("recording") ->
                CHANNEL_RECORDINGS
            else -> CHANNEL_SYSTEM_ALERTS
        }
    }

    private fun getDefaultTitle(type: String): String {
        return when (type) {
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
    }
}
