package com.company.ipcamera.core.network.onvif

import kotlinx.serialization.Serializable

/**
 * Свойства Event Service камеры (результат GetEventProperties)
 */
@Serializable
data class OnvifEventProperties(
    /**
     * URL Event Service
     */
    val eventServiceUrl: String,

    /**
     * Поддерживаемые типы событий
     */
    val supportedTopics: List<String> = emptyList(),

    /**
     * Поддерживаемые фильтры
     */
    val supportedFilters: List<String> = emptyList(),

    /**
     * Максимальное время подписки (в секундах)
     */
    val maxSubscriptionTime: Long? = null,

    /**
     * Минимальное время подписки (в секундах)
     */
    val minSubscriptionTime: Long? = null,

    /**
     * Поддержка PullPoint подписок
     */
    val supportsPullPoint: Boolean = false,

    /**
     * Поддержка Push подписок
     */
    val supportsPush: Boolean = true
)
