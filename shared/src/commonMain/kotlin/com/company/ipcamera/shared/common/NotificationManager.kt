package com.company.ipcamera.shared.common

import kotlinx.coroutines.flow.Flow

/**
 * Модель уведомления
 */
data class NotificationData(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val channelId: String? = null,
    val icon: String? = null,
    val sound: Boolean = true,
    val vibration: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val extras: Map<String, String> = emptyMap()
)

enum class NotificationType {
    EVENT,
    ALERT,
    INFO,
    WARNING,
    ERROR,
    SYSTEM
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * Менеджер уведомлений для кроссплатформенной системы
 * 
 * Поддерживает:
 * - Отправку уведомлений
 * - Управление каналами уведомлений
 * - Обработку действий пользователя
 * - Группировку уведомлений
 */
expect class NotificationManager(context: Any?) {
    /**
     * Инициализация менеджера уведомлений
     */
    fun initialize()
    
    /**
     * Отправка уведомления
     */
    suspend fun showNotification(notification: NotificationData)
    
    /**
     * Отправка уведомления с действиями
     */
    suspend fun showNotificationWithActions(
        notification: NotificationData,
        actions: List<NotificationAction>
    )
    
    /**
     * Отмена уведомления по ID
     */
    fun cancelNotification(notificationId: String)
    
    /**
     * Отмена всех уведомлений
     */
    fun cancelAllNotifications()
    
    /**
     * Создание канала уведомлений (Android)
     */
    fun createNotificationChannel(
        channelId: String,
        channelName: String,
        description: String,
        importance: NotificationPriority
    )
    
    /**
     * Проверка разрешения на отправку уведомлений
     */
    suspend fun hasPermission(): Boolean
    
    /**
     * Запрос разрешения на отправку уведомлений
     */
    suspend fun requestPermission(): Boolean
    
    /**
     * Получение потока событий уведомлений
     */
    fun getNotificationEvents(): Flow<NotificationEvent>
}

/**
 * Действие для уведомления
 */
data class NotificationAction(
    val id: String,
    val title: String,
    val icon: String? = null
)

/**
 * Событие уведомления
 */
sealed class NotificationEvent {
    data class Clicked(val notificationId: String, val actionId: String? = null) : NotificationEvent()
    data class Dismissed(val notificationId: String) : NotificationEvent()
    data class PermissionChanged(val granted: Boolean) : NotificationEvent()
}

