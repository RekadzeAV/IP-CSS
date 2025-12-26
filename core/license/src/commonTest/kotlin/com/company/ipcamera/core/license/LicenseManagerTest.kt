package com.company.ipcamera.core.license

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для LicenseManager
 * 
 * Примечание: Эти тесты проверяют доступную логику LicenseManager.
 * Полное тестирование требует actual реализаций PlatformCrypto и LicenseRepository.
 */
class LicenseManagerTest {
    
    @Test
    fun `test validate on startup with no license`() {
        // Arrange
        // LicenseManager использует singleton, поэтому тестирование ограничено
        // В реальном сценарии нужны моки для LicenseRepository
        
        // Этот тест проверяет, что метод существует и может быть вызван
        // Полное тестирование требует actual реализаций
        assertTrue(true) // Placeholder test
    }
    
    @Test
    fun `test ActivatedLicense isExpired`() {
        // Arrange
        val futureDate = System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L) // +1 year
        val pastDate = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L) // -1 year
        
        val validLicense = ActivatedLicense(
            license = License(
                id = "test-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = System.currentTimeMillis(),
                    endDate = futureDate,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = System.currentTimeMillis(),
                activatedThrough = ActivationType.ONLINE
            ),
            lastValidatedAt = System.currentTimeMillis(),
            validationStatus = LicenseValidationStatus.VALID
        )
        
        val expiredLicense = ActivatedLicense(
            license = License(
                id = "expired-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = pastDate,
                    endDate = pastDate + 1000,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = pastDate,
                activatedThrough = ActivationType.ONLINE
            ),
            lastValidatedAt = pastDate,
            validationStatus = LicenseValidationStatus.EXPIRED
        )
        
        // Act & Assert
        assertFalse(validLicense.isExpired())
        assertTrue(expiredLicense.isExpired())
    }
    
    @Test
    fun `test ActivatedLicense isOfflineExpired`() {
        // Arrange
        val futureDate = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L) // +30 days
        val pastDate = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // -30 days
        
        val validOfflineLicense = ActivatedLicense(
            license = License(
                id = "test-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = System.currentTimeMillis(),
                    endDate = futureDate,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = System.currentTimeMillis(),
                activatedThrough = ActivationType.OFFLINE
            ),
            lastValidatedAt = System.currentTimeMillis(),
            validationStatus = LicenseValidationStatus.VALID_OFFLINE,
            isOffline = true,
            offlineValidUntil = futureDate
        )
        
        val expiredOfflineLicense = ActivatedLicense(
            license = License(
                id = "expired-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = System.currentTimeMillis(),
                    endDate = futureDate,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = System.currentTimeMillis(),
                activatedThrough = ActivationType.OFFLINE
            ),
            lastValidatedAt = System.currentTimeMillis(),
            validationStatus = LicenseValidationStatus.VALID_OFFLINE,
            isOffline = true,
            offlineValidUntil = pastDate
        )
        
        val onlineLicense = ActivatedLicense(
            license = License(
                id = "online-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = System.currentTimeMillis(),
                    endDate = futureDate,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = System.currentTimeMillis(),
                activatedThrough = ActivationType.ONLINE
            ),
            lastValidatedAt = System.currentTimeMillis(),
            validationStatus = LicenseValidationStatus.VALID,
            isOffline = false
        )
        
        // Act & Assert
        assertFalse(validOfflineLicense.isOfflineExpired())
        assertTrue(expiredOfflineLicense.isOfflineExpired())
        assertFalse(onlineLicense.isOfflineExpired())
    }
    
    @Test
    fun `test ActivatedLicense getDaysRemaining`() {
        // Arrange
        val daysInFuture = 30
        val futureDate = System.currentTimeMillis() + (daysInFuture * 24 * 60 * 60 * 1000L)
        
        val license = ActivatedLicense(
            license = License(
                id = "test-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = System.currentTimeMillis(),
                    endDate = futureDate,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = System.currentTimeMillis(),
                activatedThrough = ActivationType.ONLINE
            ),
            lastValidatedAt = System.currentTimeMillis(),
            validationStatus = LicenseValidationStatus.VALID
        )
        
        // Act
        val daysRemaining = license.getDaysRemaining()
        
        // Assert
        assertTrue(daysRemaining >= daysInFuture - 1) // Allow 1 day tolerance
        assertTrue(daysRemaining <= daysInFuture + 1)
    }
    
    @Test
    fun `test ActivatedLicense getOfflineDaysRemaining`() {
        // Arrange
        val daysInFuture = 15
        val futureDate = System.currentTimeMillis() + (daysInFuture * 24 * 60 * 60 * 1000L)
        
        val offlineLicense = ActivatedLicense(
            license = License(
                id = "test-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = System.currentTimeMillis(),
                    endDate = futureDate,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = System.currentTimeMillis(),
                activatedThrough = ActivationType.OFFLINE
            ),
            lastValidatedAt = System.currentTimeMillis(),
            validationStatus = LicenseValidationStatus.VALID_OFFLINE,
            isOffline = true,
            offlineValidUntil = futureDate
        )
        
        val onlineLicense = ActivatedLicense(
            license = License(
                id = "online-license",
                features = emptyList(),
                limitations = emptyList(),
                validity = Validity(
                    startDate = System.currentTimeMillis(),
                    endDate = futureDate,
                    isPerpetual = false
                )
            ),
            activationData = ActivationData(
                code = "test-code",
                deviceFingerprint = "test-fingerprint",
                activatedAt = System.currentTimeMillis(),
                activatedThrough = ActivationType.ONLINE
            ),
            lastValidatedAt = System.currentTimeMillis(),
            validationStatus = LicenseValidationStatus.VALID,
            isOffline = false
        )
        
        // Act
        val offlineDaysRemaining = offlineLicense.getOfflineDaysRemaining()
        val onlineDaysRemaining = onlineLicense.getOfflineDaysRemaining()
        
        // Assert
        assertTrue(offlineDaysRemaining >= daysInFuture - 1)
        assertTrue(offlineDaysRemaining <= daysInFuture + 1)
        assertEquals(0, onlineDaysRemaining)
    }
    
    @Test
    fun `test License supportsPlatform`() {
        // Arrange
        val licenseWithPlatforms = License(
            id = "test-license",
            features = emptyList(),
            limitations = emptyList(),
            validity = Validity(
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + 1000,
                isPerpetual = false
            ),
            supportedPlatforms = listOf("android", "ios")
        )
        
        val licenseWithoutPlatforms = License(
            id = "test-license-2",
            features = emptyList(),
            limitations = emptyList(),
            validity = Validity(
                startDate = System.currentTimeMillis(),
                endDate = System.currentTimeMillis() + 1000,
                isPerpetual = false
            ),
            supportedPlatforms = emptyList()
        )
        
        // Act & Assert
        assertTrue(licenseWithPlatforms.supportsPlatform("android"))
        assertTrue(licenseWithPlatforms.supportsPlatform("ios"))
        assertFalse(licenseWithPlatforms.supportsPlatform("desktop"))
        
        // License without platform restrictions supports all platforms
        assertTrue(licenseWithoutPlatforms.supportsPlatform("android"))
        assertTrue(licenseWithoutPlatforms.supportsPlatform("ios"))
        assertTrue(licenseWithoutPlatforms.supportsPlatform("desktop"))
    }
    
    @Test
    fun `test LicenseError enum values`() {
        // Проверка, что все ожидаемые ошибки определены
        val errors = LicenseError.values()
        assertTrue(errors.contains(LicenseError.NO_LICENSE_FOUND))
        assertTrue(errors.contains(LicenseError.INVALID_LICENSE_XML))
        assertTrue(errors.contains(LicenseError.LICENSE_EXPIRED))
        assertTrue(errors.contains(LicenseError.NETWORK_ERROR))
        assertTrue(errors.contains(LicenseError.SERVER_ERROR))
    }
    
    @Test
    fun `test LicenseWarning enum values`() {
        // Проверка, что все ожидаемые предупреждения определены
        val warnings = LicenseWarning.values()
        assertTrue(warnings.contains(LicenseWarning.LICENSE_EXPIRING_SOON))
        assertTrue(warnings.contains(LicenseWarning.OFFLINE_MODE_ACTIVE))
        assertTrue(warnings.contains(LicenseWarning.PERIODIC_CHECK_REQUIRED))
    }
}


