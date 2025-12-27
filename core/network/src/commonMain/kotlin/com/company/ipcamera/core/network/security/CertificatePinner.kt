package com.company.ipcamera.core.network.security

import io.ktor.client.engine.HttpClientEngine

/**
 * Конфигурация для certificate pinning
 *
 * @param pinnedCertificates Map, где ключ - хост (например, "api.example.com"),
 *                           значение - список SHA-256 fingerprints сертификатов
 * @param enablePinning Включить или выключить certificate pinning
 */
data class CertificatePinningConfig(
    val pinnedCertificates: Map<String, List<String>> = emptyMap(),
    val enablePinning: Boolean = true,
    val enforcePinning: Boolean = true // Если true, отклоняет соединение при несовпадении
) {
    companion object {
        /**
         * Создает пустую конфигурацию (pinning отключен)
         */
        fun disabled() = CertificatePinningConfig(
            pinnedCertificates = emptyMap(),
            enablePinning = false,
            enforcePinning = false
        )

        /**
         * Создает конфигурацию с заданными сертификатами
         *
         * @param certificates Map хостов и их SHA-256 fingerprints
         *                     Пример: mapOf("api.example.com" to listOf("sha256/AAAAAAAA..."))
         */
        fun create(certificates: Map<String, List<String>>) = CertificatePinningConfig(
            pinnedCertificates = certificates,
            enablePinning = true,
            enforcePinning = true
        )
    }
}

/**
 * Интерфейс для platform-specific реализации certificate pinner
 */
expect class CertificatePinner(config: CertificatePinningConfig) {
    /**
     * Применяет certificate pinning к HTTP engine
     *
     * @param engine HTTP engine для настройки
     * @return Настроенный HTTP engine с certificate pinning
     */
    fun applyToEngine(engine: HttpClientEngine): HttpClientEngine

    /**
     * Проверяет, поддерживается ли certificate pinning на текущей платформе
     */
    fun isSupported(): Boolean
}


