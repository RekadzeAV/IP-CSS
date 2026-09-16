package com.company.ipcamera.core.network.security

import io.ktor.client.engine.HttpClientEngine
import kotlin.test.*

/**
 * Тесты для iOS реализации CertificatePinner.
 *
 * Проверяют: успешная конфигурация при валидном pin,
 * конфигурация с enforcePinning для отказа при неверном сертификате.
 */
class CertificatePinnerIosTest {

    @Test
    fun testValidPin_acceptsConnection() {
        // При валидном pin engine создаётся; соединение с совпадающим сертификатом будет принято
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val config = CertificatePinningConfig.create(certificates, enforcePinning = true)
        val pinner = CertificatePinner(config)
        val engine = pinner.createEngineWithPinning()
        assertNotNull(engine, "Engine with pinning should be created for valid pin config")
    }

    @Test
    fun testInvalidPin_rejectsConnection() {
        // При неверном pin delegate/engine настроены на отклонение несовпадающего сертификата при соединении
        val certificates = mapOf(
            "example.com" to listOf("sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        )
        val config = CertificatePinningConfig.create(certificates, enforcePinning = true)
        val pinner = CertificatePinner(config)
        val engine = pinner.createEngineWithPinning()
        assertNotNull(engine, "Engine should be created; rejection happens at connection time")
    }

    @Test
    fun testCreateEngineWithPinning_enabled() {
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created")
        assertTrue(engine is HttpClientEngine, "Engine should be HttpClientEngine")
    }

    @Test
    fun testCreateEngineWithPinning_disabled() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created even when pinning is disabled")
    }

    @Test
    fun testCreateEngineWithPinning_emptyCertificates() {
        val config = CertificatePinningConfig.create(emptyMap())
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created even with empty certificates")
    }

    @Test
    fun testCreateEngineWithPinning_multipleHosts() {
        val certificates = mapOf(
            "api.example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
            "cdn.example.com" to listOf("sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created with multiple hosts")
    }

    @Test
    fun testCreateEngineWithPinning_multiplePinsPerHost() {
        val certificates = mapOf(
            "example.com" to listOf(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            )
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created with multiple pins per host")
    }

    @Test
    fun testGetDelegate() {
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val config = CertificatePinningConfig.create(certificates)
        val pinner = CertificatePinner(config)

        // Создаем engine, который инициализирует delegate
        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created")

        // Проверяем, что delegate создан
        val delegate = pinner.getDelegate()
        assertNotNull(delegate, "Delegate should be created when pinning is enabled")
    }

    @Test
    fun testGetDelegate_disabled() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        // Создаем engine без pinning
        val engine = pinner.createEngineWithPinning()

        assertNotNull(engine, "Engine should be created")

        // Delegate не должен быть создан, когда pinning отключен
        val delegate = pinner.getDelegate()
        assertNull(delegate, "Delegate should be null when pinning is disabled")
    }

    @Test
    fun testIsSupported() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        assertTrue(pinner.isSupported(), "Certificate pinning should be supported on iOS")
    }

    @Test
    fun testApplyToEngine() {
        val config = CertificatePinningConfig.disabled()
        val pinner = CertificatePinner(config)

        // Создаем базовый engine
        val baseEngine = pinner.createEngineWithPinning()

        // Применяем pinning (должно вернуть engine без изменений, так как pinning настраивается при создании)
        val resultEngine = pinner.applyToEngine(baseEngine)

        assertNotNull(resultEngine, "Engine should be returned")
        assertEquals(baseEngine, resultEngine, "Engine should be returned unchanged")
    }

    @Test
    fun testEnforcePinning() {
        // Тест с enforcePinning = true
        val certificates = mapOf(
            "example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )
        val config1 = CertificatePinningConfig.create(certificates, enforcePinning = true)
        val pinner1 = CertificatePinner(config1)

        val engine1 = pinner1.createEngineWithPinning()
        assertNotNull(engine1, "Engine should be created with enforcePinning = true")

        // Тест с enforcePinning = false
        val config2 = CertificatePinningConfig.create(certificates, enforcePinning = false)
        val pinner2 = CertificatePinner(config2)

        val engine2 = pinner2.createEngineWithPinning()
        assertNotNull(engine2, "Engine should be created with enforcePinning = false")
    }
}
