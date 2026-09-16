package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.security.CertificatePinningConfig
import com.company.ipcamera.core.network.security.CertificatePinningManager
import io.ktor.client.engine.HttpClientEngine

/**
 * Фабрика для создания OnvifClient с поддержкой certificate pinning
 */
object OnvifClientFactory {
    /**
     * Создает OnvifClient с certificate pinning из конфигурации
     *
     * @param pinningConfig Конфигурация certificate pinning (опционально)
     * @param engine HTTP engine (опционально, будет создан с pinning если указан pinningConfig)
     * @return OnvifClient с настроенным certificate pinning
     */
    fun create(
        pinningConfig: CertificatePinningConfig? = null,
        engine: HttpClientEngine? = null
    ): OnvifClient {
        val actualEngine = engine ?: if (pinningConfig != null && pinningConfig.enablePinning) {
            ApiClient.createEngineWithPinning(pinningConfig.copy(enforcePinning = true))
        } else {
            ApiClient.createDefaultEngine()
        }

        return OnvifClient(actualEngine)
    }

    /**
     * Создает OnvifClient с автоматической загрузкой конфигурации certificate pinning.
     * При configFilePath == null используется [CertificatePinningManager.DEFAULT_CONFIG_PATH]
     * (config/certificate-pins.json) на всех платформах.
     *
     * @param configFilePath Путь к файлу конфигурации (опционально)
     * @return OnvifClient с настроенным certificate pinning
     */
    fun createWithAutoConfig(configFilePath: String? = null): OnvifClient {
        val pinningConfig = if (configFilePath != null) {
            CertificatePinningManager.loadFromFile(configFilePath)
        } else {
            CertificatePinningManager.loadConfig()
        }

        return create(pinningConfig = pinningConfig)
    }
}
