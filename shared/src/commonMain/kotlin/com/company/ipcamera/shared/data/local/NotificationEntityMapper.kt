package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.database.Notification as DbNotification
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.model.NotificationPriority
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Маппер между сущностью базы данных и доменной моделью Notification
 */
internal class NotificationEntityMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toDomain(dbNotification: DbNotification): Notification {
        return Notification(
            id = dbNotification.id,
            title = dbNotification.title,
            message = dbNotification.message,
            type = NotificationType.valueOf(dbNotification.type),
            priority = NotificationPriority.valueOf(dbNotification.priority),
            cameraId = dbNotification.camera_id,
            eventId = dbNotification.event_id,
            recordingId = dbNotification.recording_id,
            channelId = dbNotification.channel_id,
            icon = dbNotification.icon,
            sound = dbNotification.sound == 1L,
            vibration = dbNotification.vibration == 1L,
            read = dbNotification.read == 1L,
            readAt = dbNotification.read_at,
            timestamp = dbNotification.timestamp,
            extras = dbNotification.extras?.let {
                json.decodeFromString<Map<String, String>>(it)
            } ?: emptyMap()
        )
    }

    fun toDatabase(notification: Notification): DbNotification {
        return DbNotification(
            id = notification.id,
            title = notification.title,
            message = notification.message,
            type = notification.type.name,
            priority = notification.priority.name,
            camera_id = notification.cameraId,
            event_id = notification.eventId,
            recording_id = notification.recordingId,
            channel_id = notification.channelId,
            icon = notification.icon,
            sound = if (notification.sound) 1L else 0L,
            vibration = if (notification.vibration) 1L else 0L,
            read = if (notification.read) 1L else 0L,
            read_at = notification.readAt,
            timestamp = notification.timestamp,
            extras = if (notification.extras.isNotEmpty()) {
                json.encodeToString(notification.extras)
            } else null
        )
    }
}

