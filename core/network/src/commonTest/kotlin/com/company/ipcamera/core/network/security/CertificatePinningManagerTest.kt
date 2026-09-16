package com.company.ipcamera.core.network.security

import java.io.File
import kotlin.test.*

/**
 * Тесты для CertificatePinningManager
 */
class CertificatePinningManagerTest {

    @Test
    fun testValidatePinFormat_validSha256() {
        // Валидный SHA-256 pin (44 символа Base64)
        val validPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        assertTrue(
            CertificatePinningManager.validatePinFormat(validPin),
            "Valid SHA-256 pin should pass validation"
        )
    }

    @Test
    fun testValidatePinFormat_validSha256NoPadding() {
        // Валидный SHA-256 pin (43 символа Base64 без padding)
        val validPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        assertTrue(
            CertificatePinningManager.validatePinFormat(validPin),
            "Valid SHA-256 pin without padding should pass validation"
        )
    }

    @Test
    fun testValidatePinFormat_invalidPrefix() {
        // Неверный префикс
        val invalidPin = "sha1/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        assertFalse(
            CertificatePinningManager.validatePinFormat(invalidPin),
            "Pin with wrong prefix should fail validation"
        )
    }

    @Test
    fun testValidatePinFormat_missingPrefix() {
        // Отсутствует префикс
        val invalidPin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        assertFalse(
            CertificatePinningManager.validatePinFormat(invalidPin),
            "Pin without prefix should fail validation"
        )
    }

    @Test
    fun testValidatePinFormat_emptyBase64() {
        // Пустая Base64 часть
        val invalidPin = "sha256/"
        assertFalse(
            CertificatePinningManager.validatePinFormat(invalidPin),
            "Pin with empty Base64 should fail validation"
        )
    }

    @Test
    fun testValidatePinFormat_tooShort() {
        // Слишком короткий Base64
        val invalidPin = "sha256/AAAA"
        assertFalse(
            CertificatePinningManager.validatePinFormat(invalidPin),
            "Pin with too short Base64 should fail validation"
        )
    }

    @Test
    fun testValidatePinFormat_invalidCharacters() {
        // Недопустимые символы в Base64
        val invalidPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA@"
        assertFalse(
            CertificatePinningManager.validatePinFormat(invalidPin),
            "Pin with invalid characters should fail validation"
        )
    }

    @Test
    fun testCreateFromMap_validCertificates() {
        val certificates = mapOf(
            "api.example.com" to listOf(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            ),
            "cdn.example.com" to listOf(
                "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
            )
        )

        val config = CertificatePinningManager.createFromMap(certificates)

        assertTrue(config.enablePinning, "Pinning should be enabled")
        assertTrue(config.enforcePinning, "Enforce should be enabled")
        assertEquals(2, config.pinnedCertificates.size, "Should have 2 hosts")
        assertEquals(2, config.pinnedCertificates["api.example.com"]?.size, "Should have 2 pins for api.example.com")
        assertEquals(1, config.pinnedCertificates["cdn.example.com"]?.size, "Should have 1 pin for cdn.example.com")
    }

    @Test
    fun testCreateFromMap_invalidPinsFiltered() {
        val certificates = mapOf(
            "api.example.com" to listOf(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // valid
                "invalid-pin", // invalid
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=" // valid
            )
        )

        val config = CertificatePinningManager.createFromMap(certificates)

        assertEquals(1, config.pinnedCertificates.size, "Should have 1 host")
        assertEquals(2, config.pinnedCertificates["api.example.com"]?.size, "Should have 2 valid pins")
        assertTrue(
            config.pinnedCertificates["api.example.com"]?.contains(
                "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
            ) == true,
            "Should contain first valid pin"
        )
        assertTrue(
            config.pinnedCertificates["api.example.com"]?.contains(
                "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
            ) == true,
            "Should contain second valid pin"
        )
        assertFalse(
            config.pinnedCertificates["api.example.com"]?.contains("invalid-pin") == true,
            "Should not contain invalid pin"
        )
    }

    @Test
    fun testCreateFromMap_allInvalidPins() {
        val certificates = mapOf(
            "api.example.com" to listOf(
                "invalid-pin-1",
                "invalid-pin-2"
            )
        )

        val config = CertificatePinningManager.createFromMap(certificates)

        assertFalse(config.enablePinning, "Pinning should be disabled when no valid pins")
        assertEquals(0, config.pinnedCertificates.size, "Should have no hosts")
    }

    @Test
    fun testCreateFromMap_emptyMap() {
        val config = CertificatePinningManager.createFromMap(emptyMap())

        assertFalse(config.enablePinning, "Pinning should be disabled for empty map")
        assertEquals(0, config.pinnedCertificates.size, "Should have no hosts")
    }

    @Test
    fun testCreateFromMap_disablePinning() {
        val certificates = mapOf(
            "api.example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )

        val config = CertificatePinningManager.createFromMap(
            certificates,
            enablePinning = false,
            enforcePinning = false
        )

        assertFalse(config.enablePinning, "Pinning should be disabled")
        assertFalse(config.enforcePinning, "Enforce should be disabled")
    }

    @Test
    fun testLoadFromFile_validJson() {
        // Создаем временный JSON файл
        val tempFile = File.createTempFile("certificate-pins", ".json")
        tempFile.deleteOnExit()

        val jsonContent = """
            {
                "hosts": {
                    "api.example.com": [
                        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
                    ],
                    "cdn.example.com": [
                        "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
                    ]
                },
                "enablePinning": true,
                "enforcePinning": true
            }
        """.trimIndent()

        tempFile.writeText(jsonContent)

        val config = CertificatePinningManager.loadFromFile(tempFile.absolutePath)

        assertNotNull(config, "Config should be loaded")
        assertTrue(config.enablePinning, "Pinning should be enabled")
        assertEquals(2, config.pinnedCertificates.size, "Should have 2 hosts")
    }

    @Test
    fun testLoadFromFile_fileNotFound() {
        val config = CertificatePinningManager.loadFromFile("non-existent-file.json")

        assertNull(config, "Config should be null for non-existent file")
    }

    @Test
    fun testLoadFromFile_invalidJson() {
        val tempFile = File.createTempFile("certificate-pins", ".json")
        tempFile.deleteOnExit()
        tempFile.writeText("invalid json content")

        val config = CertificatePinningManager.loadFromFile(tempFile.absolutePath)

        assertNull(config, "Config should be null for invalid JSON")
    }

    @Test
    fun testLoadFromFile_ignoreUnknownKeys() {
        val tempFile = File.createTempFile("certificate-pins", ".json")
        tempFile.deleteOnExit()

        val jsonContent = """
            {
                "hosts": {
                    "api.example.com": [
                        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
                    ]
                },
                "enablePinning": true,
                "enforcePinning": true,
                "unknownKey": "should be ignored"
            }
        """.trimIndent()

        tempFile.writeText(jsonContent)

        val config = CertificatePinningManager.loadFromFile(tempFile.absolutePath)

        assertNotNull(config, "Config should be loaded even with unknown keys")
        assertEquals(1, config.pinnedCertificates.size, "Should have 1 host")
    }

    @Test
    fun testLoadFromFile_invalidPinsFiltered() {
        val tempFile = File.createTempFile("certificate-pins", ".json")
        tempFile.deleteOnExit()

        val jsonContent = """
            {
                "hosts": {
                    "api.example.com": [
                        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                        "invalid-pin"
                    ]
                },
                "enablePinning": true,
                "enforcePinning": true
            }
        """.trimIndent()

        tempFile.writeText(jsonContent)

        val config = CertificatePinningManager.loadFromFile(tempFile.absolutePath)

        assertNotNull(config, "Config should be loaded")
        assertEquals(1, config.pinnedCertificates["api.example.com"]?.size, "Should have 1 valid pin")
    }

    @Test
    fun testLoadFromFile_exampleJsonFormat() {
        // Формат config/certificate-pins.example.json: enabled, enforce, certificates
        val tempFile = File.createTempFile("certificate-pins", ".json")
        tempFile.deleteOnExit()
        val jsonContent = """
            {
                "enabled": true,
                "enforce": true,
                "certificates": {
                    "api.example.com": [
                        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
                    ],
                    "api2.example.com": ["sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="]
                }
            }
        """.trimIndent()
        tempFile.writeText(jsonContent)
        val config = CertificatePinningManager.loadFromFile(tempFile.absolutePath)
        assertNotNull(config, "Config should be loaded from example format")
        assertTrue(config.enablePinning, "Pinning should be enabled")
        assertTrue(config.enforcePinning, "Enforce should be true")
        assertEquals(2, config.pinnedCertificates.size, "Should have 2 hosts")
        assertEquals(2, config.pinnedCertificates["api.example.com"]?.size)
        assertEquals(1, config.pinnedCertificates["api2.example.com"]?.size)
    }

    @Test
    fun testCalculateSha256Pin() {
        val testData = "test certificate data".toByteArray()
        val pin = CertificatePinningManager.calculateSha256Pin(testData)

        assertTrue(pin.startsWith("sha256/"), "Pin should start with sha256/")
        assertTrue(pin.length > 50, "Pin should have reasonable length")

        // Проверяем, что это валидный формат
        assertTrue(
            CertificatePinningManager.validatePinFormat(pin),
            "Calculated pin should be in valid format"
        )
    }

    @Test
    fun testCalculateSha256Pin_consistency() {
        val testData = "test certificate data".toByteArray()
        val pin1 = CertificatePinningManager.calculateSha256Pin(testData)
        val pin2 = CertificatePinningManager.calculateSha256Pin(testData)

        assertEquals(pin1, pin2, "Same data should produce same pin")
    }

    @Test
    fun testLoadConfig_disabledWhenNoSource() {
        // Убеждаемся, что переменные окружения не установлены
        val originalEnv = System.getenv("CERTIFICATE_PINNING_ENABLED")

        val config = CertificatePinningManager.loadConfig("non-existent-file.json")

        assertFalse(config.enablePinning, "Pinning should be disabled when no config found")
        assertEquals(0, config.pinnedCertificates.size, "Should have no hosts")
    }

    @Test
    fun testLoadConfig_fromEnvironment() {
        // Этот тест требует установки переменных окружения, что сложно в unit тестах
        // В реальности это будет протестировано в integration тестах
        // Здесь просто проверяем, что метод не падает
        val config = CertificatePinningManager.loadConfig("non-existent-file.json")
        assertNotNull(config, "Should return a config (even if disabled)")
    }

    @Test
    fun testCertificatePinningConfig_disabled() {
        val config = CertificatePinningConfig.disabled()

        assertFalse(config.enablePinning, "Pinning should be disabled")
        assertFalse(config.enforcePinning, "Enforce should be disabled")
        assertTrue(config.pinnedCertificates.isEmpty(), "Should have no certificates")
    }

    @Test
    fun testCertificatePinningConfig_create() {
        val certificates = mapOf(
            "api.example.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )

        val config = CertificatePinningConfig.create(certificates)

        assertTrue(config.enablePinning, "Pinning should be enabled")
        assertTrue(config.enforcePinning, "Enforce should be enabled")
        assertEquals(1, config.pinnedCertificates.size, "Should have 1 host")
    }
}
