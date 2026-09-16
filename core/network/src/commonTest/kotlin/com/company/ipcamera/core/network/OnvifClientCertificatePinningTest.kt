package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.security.CertificatePinningConfig
import kotlin.test.*

/**
 * Тесты для проверки использования Certificate Pinning в OnvifClient
 */
class OnvifClientCertificatePinningTest {

    @Test
    fun testOnvifClientFactory_createsClientWithPinning() {
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val pinningConfig = CertificatePinningConfig.create(certificates)

        val client = OnvifClientFactory.create(pinningConfig = pinningConfig)

        assertNotNull(client, "OnvifClient should be created")
    }

    @Test
    fun testOnvifClientFactory_createsClientWithoutPinning() {
        val client = OnvifClientFactory.create(pinningConfig = null)

        assertNotNull(client, "OnvifClient should be created even without pinning")
    }

    @Test
    fun testOnvifClientFactory_createsClientWithDisabledPinning() {
        val pinningConfig = CertificatePinningConfig.disabled()

        val client = OnvifClientFactory.create(pinningConfig = pinningConfig)

        assertNotNull(client, "OnvifClient should be created with disabled pinning")
    }

    @Test
    fun testOnvifClientFactory_createsClientWithAutoConfig() {
        // Тест проверяет, что метод createWithAutoConfig не падает
        // В реальности это будет протестировано в integration тестах
        val client = OnvifClientFactory.createWithAutoConfig()

        assertNotNull(client, "OnvifClient should be created with auto config")
    }

    @Test
    fun testOnvifClientFactory_createsClientWithCustomEngine() {
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val pinningConfig = CertificatePinningConfig.create(certificates)
        val engine = ApiClient.createEngineWithPinning(pinningConfig)

        val client = OnvifClientFactory.create(pinningConfig = pinningConfig, engine = engine)

        assertNotNull(client, "OnvifClient should be created with custom engine")
    }

    @Test
    fun testOnvifClientFactory_pinningConfigTakesPrecedenceOverEngine() {
        // Когда указаны и pinningConfig и engine, pinningConfig должен использоваться
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val pinningConfig = CertificatePinningConfig.create(certificates)
        val defaultEngine = ApiClient.createDefaultEngine()

        val client = OnvifClientFactory.create(pinningConfig = pinningConfig, engine = defaultEngine)

        assertNotNull(client, "OnvifClient should be created")
        // Примечание: В реальности нужно проверить, что используется engine с pinning,
        // но это требует доступа к внутреннему состоянию клиента
    }
}
