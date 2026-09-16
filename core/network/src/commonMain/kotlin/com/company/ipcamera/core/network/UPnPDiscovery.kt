package com.company.ipcamera.core.network

/**
 * UPnP протокол для обнаружения камер без ONVIF
 * Использует SSDP (Simple Service Discovery Protocol) через UDP multicast на 239.255.255.250:1900
 */
expect class UPnPDiscovery constructor() {
    /**
     * Отправить M-SEARCH запрос и получить ответы
     * @param timeoutMillis таймаут ожидания ответов
     * @return список обнаруженных устройств
     */
    suspend fun discover(timeoutMillis: Long = 5000): List<UPnPDevice>

    /**
     * Закрыть соединение
     */
    fun close()
}

/**
 * Обнаруженное устройство через UPnP
 */
data class UPnPDevice(
    val location: String, // URL для получения описания устройства
    val server: String, // Информация о сервере
    val usn: String, // Unique Service Name
    val st: String, // Search Target (тип устройства)
    val cacheControl: String? = null // Cache-Control заголовок
)
