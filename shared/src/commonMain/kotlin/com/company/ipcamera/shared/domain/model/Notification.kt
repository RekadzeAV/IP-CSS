package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель уведомления
 * 
 * Доменная модель уведомления для использования в бизнес-логике.
 * Для отображения уведомлений на платформе используется NotificationManager.
 */
@Serializable
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val cameraId: String? = null,
    val eventId: String? = null,
    val recordingId: String? = null,
    val channelId: String? = null,
    val icon: String? = null,
    val sound: Boolean = true,
    val vibration: Boolean = false,
    val read: Boolean = false,
    val readAt: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val extras: Map<String, String> = emptyMap()
) {
    /**
     * Проверка, прочитано ли уведомление
     */
    fun isRead(): Boolean = read
    
    /**
     * Проверка, требует ли уведомление немедленного внимания
     */
    fun requiresImmediateAttention(): Boolean = 
        priority == NotificationPriority.URGENT || priority == NotificationPriority.HIGH
    
    /**
     * Отметка уведомления как прочитанного
     */
    fun markAsRead(): Notification = copy(
        read = true,
        readAt = System.currentTimeMillis()
    )
}

@Serializable
enum class NotificationType {
    EVENT,
    ALERT,
    INFO,
    WARNING,
    ERROR,
    SYSTEM,
    RECORDING,
    LICENSE,
    USER
}

@Serializable
enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}
