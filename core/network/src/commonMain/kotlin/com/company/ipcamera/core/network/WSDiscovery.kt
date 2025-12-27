package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WS-Discovery протокол для обнаружения ONVIF устройств
 * Использует UDP multicast на 239.255.255.250:3702
 */
expect class WSDiscovery {
    /**
     * Отправить Probe запрос и получить ответы
     * @param timeoutMillis таймаут ожидания ответов
     * @return список обнаруженных устройств
     */
    suspend fun discover(timeoutMillis: Long = 5000): List<DiscoveredDevice>

    /**
     * Закрыть соединение
     */
    fun close()
}

/**
 * Обнаруженное устройство через WS-Discovery
 */
data class DiscoveredDevice(
    val xAddrs: List<String>, // Адреса сервисов
    val types: List<String>, // Типы устройств
    val scopes: List<String>, // Области действия
    val metadataVersion: Long = 1
)


