package com.company.ipcamera.core.network.onvif

import kotlinx.serialization.Serializable

/**
 * Аналитический движок ONVIF
 */
@Serializable
data class OnvifAnalyticsEngine(
    /**
     * Токен движка (уникальный идентификатор)
     */
    val token: String,

    /**
     * Имя движка
     */
    val name: String? = null,

    /**
     * Тип движка (например, "MotionDetector", "ObjectDetector")
     */
    val type: String? = null,

    /**
     * Конфигурация движка
     */
    val configuration: OnvifAnalyticsEngineConfiguration? = null,

    /**
     * Статус движка
     */
    val status: AnalyticsEngineStatus = AnalyticsEngineStatus.UNKNOWN
)

/**
 * Статус аналитического движка
 */
@Serializable
enum class AnalyticsEngineStatus {
    /**
     * Движок активен и работает
     */
    ACTIVE,

    /**
     * Движок неактивен
     */
    INACTIVE,

    /**
     * Движок в процессе инициализации
     */
    INITIALIZING,

    /**
     * Движок в состоянии ошибки
     */
    ERROR,

    /**
     * Статус неизвестен
     */
    UNKNOWN
}

/**
 * Конфигурация аналитического движка
 */
@Serializable
data class OnvifAnalyticsEngineConfiguration(
    /**
     * Параметры конфигурации (ключ-значение)
     */
    val parameters: Map<String, String> = emptyMap(),

    /**
     * Включен ли движок
     */
    val enabled: Boolean = true,

    /**
     * Дополнительные настройки (ключ-значение; для сложных типов использовать JSON-строку)
     */
    val settings: Map<String, String> = emptyMap()
)

/**
 * Входные данные аналитического движка
 */
@Serializable
data class OnvifAnalyticsEngineInput(
    /**
     * Токен входных данных
     */
    val token: String,

    /**
     * Тип входных данных (например, "VideoSource", "MetadataStream")
     */
    val type: String? = null,

    /**
     * Источник входных данных (например, токен видеоисточника)
     */
    val sourceToken: String? = null,

    /**
     * Конфигурация входных данных
     */
    val configuration: OnvifAnalyticsEngineInputConfiguration? = null
)

/**
 * Конфигурация входных данных аналитического движка
 */
@Serializable
data class OnvifAnalyticsEngineInputConfiguration(
    /**
     * Параметры конфигурации
     */
    val parameters: Map<String, String> = emptyMap(),

    /**
     * Включены ли входные данные
     */
    val enabled: Boolean = true
)
