package com.company.ipcamera.shared.domain.model

import com.company.ipcamera.shared.common.nowMillis
import kotlinx.serialization.Serializable

/**
 * Правило аналитики для автоматической обработки событий
 *
 * Правила определяют условия, при которых должны создаваться события и отправляться уведомления
 */
@Serializable
data class AnalyticsRule(
    /**
     * Уникальный идентификатор правила
     */
    val id: String,
    /**
     * Название правила
     */
    val name: String,
    /**
     * Описание правила
     */
    val description: String? = null,
    /**
     * ID камеры, к которой применяется правило (null = все камеры)
     */
    val cameraId: String? = null,
    /**
     * Тип аналитики, на который реагирует правило
     */
    val analyticsType: AnalyticsRuleType,
    /**
     * Условия правила
     */
    val conditions: AnalyticsRuleConditions,
    /**
     * Действия при срабатывании правила
     */
    val actions: AnalyticsRuleActions,
    /**
     * Включено ли правило
     */
    val enabled: Boolean = true,
    /**
     * Приоритет правила (чем выше, тем раньше проверяется)
     */
    val priority: Int = 0,
    /**
     * Время создания
     */
    val createdAt: Long = nowMillis(),
    /**
     * Время последнего обновления
     */
    val updatedAt: Long = nowMillis(),
)

/**
 * Тип аналитики для правила
 */
@Serializable
enum class AnalyticsRuleType {
    /**
     * Детекция движения
     */
    MOTION_DETECTION,

    /**
     * Детекция объектов
     */
    OBJECT_DETECTION,

    /**
     * Детекция лиц
     */
    FACE_DETECTION,

    /**
     * Распознавание номерных знаков
     */
    LICENSE_PLATE_RECOGNITION,

    /**
     * Комплексный анализ (несколько типов)
     */
    COMPLEX_ANALYSIS,
}

/**
 * Условия правила аналитики
 */
@Serializable
data class AnalyticsRuleConditions(
    /**
     * Минимальная уверенность (confidence) для срабатывания (0.0 - 1.0)
     */
    val minConfidence: Float = 0.5f,
    /**
     * Типы объектов для детекции (для OBJECT_DETECTION)
     */
    val objectTypes: List<String> = emptyList(),
    /**
     * Зоны детекции (если пусто, то все зоны)
     */
    val zones: List<String> = emptyList(),
    /**
     * Время суток, когда правило активно (null = всегда)
     * Формат: "HH:mm-HH:mm" (например, "09:00-18:00")
     */
    val timeWindow: String? = null,
    /**
     * Дни недели, когда правило активно (1-7, где 1 = понедельник)
     * Если пусто, то все дни
     */
    val daysOfWeek: List<Int> = emptyList(),
    /**
     * Минимальное количество объектов для срабатывания
     */
    val minObjectCount: Int = 1,
    /**
     * Максимальное количество объектов для срабатывания (null = без ограничений)
     */
    val maxObjectCount: Int? = null,
    /**
     * Дополнительные условия (ключ-значение)
     */
    val additionalConditions: Map<String, String> = emptyMap(),
)

/**
 * Действия при срабатывании правила
 */
@Serializable
data class AnalyticsRuleActions(
    /**
     * Создавать ли событие
     */
    val createEvent: Boolean = true,
    /**
     * Тип события (если createEvent = true)
     */
    val eventType: EventType = EventType.MOTION_DETECTION,
    /**
     * Важность события
     */
    val eventSeverity: EventSeverity = EventSeverity.INFO,
    /**
     * Отправлять ли уведомление
     */
    val sendNotification: Boolean = false,
    /**
     * Отправлять in-app уведомление (БД + WebSocket)
     */
    val notifyInApp: Boolean = true,
    /**
     * Отправлять копию уведомления по email
     */
    val notifyEmail: Boolean = false,
    /**
     * Отправлять копию уведомления в Telegram
     */
    val notifyTelegram: Boolean = false,
    /**
     * Тип уведомления
     */
    val notificationType: NotificationType? = null,
    /**
     * Запускать ли запись
     */
    val startRecording: Boolean = false,
    /**
     * Длительность записи в секундах (если startRecording = true)
     */
    val recordingDuration: Long = 60L,
    /**
     * Выполнять ли HTTP запрос (webhook)
     */
    val sendWebhook: Boolean = false,
    /**
     * URL для webhook (если sendWebhook = true)
     */
    val webhookUrl: String? = null,
    /**
     * Дополнительные действия (ключ-значение)
     */
    val additionalActions: Map<String, String> = emptyMap(),
)
