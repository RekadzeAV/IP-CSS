package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Конфигурация Telegram бота
 */
@Serializable
data class TelegramBotConfig(
    val enabled: Boolean = false,
    val token: String = "",
    val webhookUrl: String? = null,
    val allowedUserIds: List<String> = emptyList(),
    val notifyOnMotion: Boolean = true,
    val notifyOnObject: Boolean = true,
    val notifyOnCameraOffline: Boolean = true,
    val maxNotificationsPerHour: Int = 20,
)

/**
 * Подписка пользователя на уведомления
 */
@Serializable
data class TelegramSubscription(
    val userId: String,
    val chatId: Long,
    val username: String?,
    val subscribedCameras: List<String>, // cameraId list, empty = all
    val notificationTypes: List<NotificationType>,
    val createdAt: Long,
    val isActive: Boolean = true,
) {
    @Serializable
    enum class NotificationType {
        MOTION,
        OBJECT_DETECTED,
        CAMERA_OFFLINE,
        SYSTEM_ALERT,
    }
}

/**
 * Команда Telegram бота
 */
@Serializable
data class TelegramCommand(
    val command: String, // /start, /status, etc.
    val args: List<String> = emptyList(),
    val chatId: Long,
    val userId: Long,
    val username: String?,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Сообщение от бота
 */
@Serializable
data class TelegramMessage(
    val chatId: Long,
    val text: String,
    val parseMode: String = "HTML",
    val replyToMessageId: Int? = null,
    val photo: ByteArray? = null,
    val keyboard: InlineKeyboardMarkup? = null,
) {
    @Serializable
    data class InlineKeyboardMarkup(
        val inlineKeyboard: List<List<InlineKeyboardButton>>,
    )

    @Serializable
    data class InlineKeyboardButton(
        val text: String,
        val callbackData: String? = null,
        val url: String? = null,
    )
}

/**
 * Статистика бота
 */
@Serializable
data class TelegramBotStats(
    val totalUsers: Int,
    val activeSubscriptions: Int,
    val notificationsSent: Long,
    val commandsProcessed: Long,
    val lastNotificationTime: Long?,
)
