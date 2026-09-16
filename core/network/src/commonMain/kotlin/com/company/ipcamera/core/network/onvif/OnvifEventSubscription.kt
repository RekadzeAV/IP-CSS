package com.company.ipcamera.core.network.onvif

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Модель подписки на события ONVIF
 */
@Serializable
data class OnvifEventSubscription(
    /**
     * Уникальный идентификатор подписки
     */
    val id: String,

    /**
     * URL камеры
     */
    val cameraUrl: String,

    /**
     * URL для получения уведомлений (Notification Consumer URL)
     */
    val notificationConsumerUrl: String,

    /**
     * URL подписки (Subscription Reference)
     */
    val subscriptionReference: String,

    /**
     * Время истечения подписки (Unix timestamp в миллисекундах)
     */
    val expirationTime: Long,

    /**
     * Время создания подписки (Unix timestamp в миллисекундах)
     */
    val createdAt: Long,

    /**
     * Имя пользователя для обновления/поллинга/отписки.
     */
    val username: String? = null,

    /**
     * Пароль для обновления/поллинга/отписки.
     */
    val password: String? = null,

    /**
     * Фильтр событий (если применен)
     */
    val filter: OnvifEventFilter? = null,

    /**
     * Статус подписки
     */
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE
) {
    /**
     * Проверка, истекла ли подписка
     */
    fun isExpired(currentTime: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
        return currentTime >= expirationTime
    }

    /**
     * Проверка, нужно ли продлить подписку (за 5 минут до истечения)
     */
    fun needsRenewal(currentTime: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
        val renewalThreshold = expirationTime - (5 * 60 * 1000) // 5 минут
        return currentTime >= renewalThreshold && !isExpired(currentTime)
    }
}

/**
 * Статус подписки
 */
@Serializable
enum class SubscriptionStatus {
    /**
     * Подписка активна
     */
    ACTIVE,

    /**
     * Подписка истекла
     */
    EXPIRED,

    /**
     * Подписка отменена
     */
    CANCELLED,

    /**
     * Подписка в процессе обновления
     */
    RENEWING
}

/**
 * Фильтр событий ONVIF
 */
@Serializable
data class OnvifEventFilter(
    /**
     * Topic Expression для фильтрации (например, "tns1:VideoSource/MotionAlarm")
     */
    val topicExpression: String? = null,

    /**
     * Message Content фильтр
     */
    val messageContent: String? = null,

    /**
     * Включенные типы событий
     */
    val includedTopics: List<String> = emptyList(),

    /**
     * Исключенные типы событий
     */
    val excludedTopics: List<String> = emptyList()
) {
    /**
     * Проверка, соответствует ли событие фильтру
     */
    fun matches(event: OnvifEvent): Boolean {
        // Проверка включенных топиков
        if (includedTopics.isNotEmpty()) {
            if (!includedTopics.any { event.topic.contains(it, ignoreCase = true) }) {
                return false
            }
        }

        // Проверка исключенных топиков
        if (excludedTopics.isNotEmpty()) {
            if (excludedTopics.any { event.topic.contains(it, ignoreCase = true) }) {
                return false
            }
        }

        // Проверка topic expression
        if (topicExpression != null) {
            if (!event.topic.matches(Regex(topicExpression))) {
                return false
            }
        }

        return true
    }
}
