package com.company.ipcamera.core.license

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable

/**
 * Менеджер лицензий для кроссплатформенной системы видеонаблюдения
 * 
 * Поддерживает:
 * - Онлайн и офлайн активацию
 * - Проверку лицензий
 * - Управление функциями
 * - Перенос лицензий между устройствами
 */
@Serializable
data class LicenseValidationResult(
    val isValid: Boolean,
    val license: ActivatedLicense? = null,
    val error: LicenseError? = null,
    val warnings: List<LicenseWarning> = emptyList()
)

@Serializable
data class LicenseActivationResult(
    val success: Boolean,
    val license: ActivatedLicense? = null,
    val error: LicenseError? = null,
    val requiresOnlineVerification: Boolean = false
)

@Serializable
enum class LicenseError {
    NO_LICENSE_FOUND,
    INVALID_LICENSE_XML,
    INVALID_SIGNATURE,
    LICENSE_EXPIRED,
    PLATFORM_NOT_SUPPORTED,
    DEVICE_MISMATCH,
    MAX_DEVICES_EXCEEDED,
    FEATURE_NOT_AVAILABLE,
    OFFLINE_LICENSE_EXPIRED,
    LICENSE_REVOKED,
    NETWORK_ERROR,
    SERVER_ERROR,
    TAMPER_DETECTED,
    INVALID_ACTIVATION_CODE,
    OFFLINE_CODE_EXPIRED,
    ACTIVATION_LIMIT_EXCEEDED,
    HARDWARE_CHANGED
}

@Serializable
enum class LicenseWarning {
    LICENSE_EXPIRING_SOON,
    OFFLINE_MODE_ACTIVE,
    PERIODIC_CHECK_REQUIRED,
    FEATURE_DEGRADED
}

class LicenseManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: LicenseManager? = null
        
        fun getInstance(): LicenseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LicenseManager().also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val platformCrypto: PlatformCrypto = getPlatformCrypto()
    private val licenseRepository: LicenseRepository = LicenseRepositoryImpl()
    
    /**
     * Активация лицензии
     */
    suspend fun activateLicense(
        licenseXml: String,
        activationCode: String,
        isOffline: Boolean = false
    ): LicenseActivationResult = withContext(Dispatchers.Default) {
        return@withContext try {
            if (isOffline) {
                activateOfflineLicense(activationCode)
            } else {
                activateOnlineLicense(licenseXml, activationCode)
            }
        } catch (e: LicenseException) {
            LicenseActivationResult(
                success = false,
                error = e.error,
                requiresOnlineVerification = e.requiresOnlineVerification
            )
        } catch (e: Exception) {
            LicenseActivationResult(
                success = false,
                error = LicenseError.SERVER_ERROR
            )
        }
    }
    
    private suspend fun activateOnlineLicense(
        licenseXml: String,
        activationCode: String
    ): LicenseActivationResult {
        // Реализация онлайн активации
        // TODO: Реализовать согласно спецификации
        return LicenseActivationResult(success = false, error = LicenseError.NETWORK_ERROR)
    }
    
    private suspend fun activateOfflineLicense(
        offlineCode: String
    ): LicenseActivationResult {
        // Реализация офлайн активации
        // TODO: Реализовать согласно спецификации
        return LicenseActivationResult(success = false, error = LicenseError.INVALID_ACTIVATION_CODE)
    }
    
    /**
     * Валидация лицензии при запуске
     */
    fun validateOnStartup(): LicenseValidationResult {
        val activatedLicense = licenseRepository.loadLicense() ?: return LicenseValidationResult(
            isValid = false,
            error = LicenseError.NO_LICENSE_FOUND
        )
        
        // Проверка целостности
        if (!checkLicenseIntegrity(activatedLicense)) {
            return LicenseValidationResult(
                isValid = false,
                error = LicenseError.TAMPER_DETECTED
            )
        }
        
        // Для офлайн лицензии
        if (activatedLicense.isOffline) {
            return validateOfflineLicense(activatedLicense)
        }
        
        // Для онлайн лицензии
        return validateOnlineLicense(activatedLicense)
    }
    
    private fun validateOfflineLicense(license: ActivatedLicense): LicenseValidationResult {
        // Проверка срока действия офлайн периода
        if (license.isOfflineExpired()) {
            return LicenseValidationResult(
                isValid = false,
                error = LicenseError.OFFLINE_LICENSE_EXPIRED
            )
        }
        
        val warnings = mutableListOf<LicenseWarning>()
        
        // Проверка необходимости онлайн верификации
        if (license.requiresOnlineValidation) {
            warnings.add(LicenseWarning.PERIODIC_CHECK_REQUIRED)
        }
        
        // Проверка оставшегося времени
        val daysRemaining = license.getOfflineDaysRemaining()
        if (daysRemaining in 1..3) {
            warnings.add(LicenseWarning.LICENSE_EXPIRING_SOON)
        }
        
        return LicenseValidationResult(
            isValid = true,
            license = license,
            warnings = warnings
        )
    }
    
    private fun validateOnlineLicense(license: ActivatedLicense): LicenseValidationResult {
        // Проверка срока действия
        if (license.isExpired()) {
            return LicenseValidationResult(
                isValid = false,
                error = LicenseError.LICENSE_EXPIRED
            )
        }
        
        // Проверка привязки к устройству
        val currentFingerprint = platformCrypto.getSecureDeviceFingerprint()
        if (license.activationData.deviceFingerprint != currentFingerprint) {
            return LicenseValidationResult(
                isValid = false,
                error = LicenseError.HARDWARE_CHANGED
            )
        }
        
        val warnings = mutableListOf<LicenseWarning>()
        
        // Проверка оставшегося времени до истечения
        val daysRemaining = license.getDaysRemaining()
        if (daysRemaining in 1..7) {
            warnings.add(LicenseWarning.LICENSE_EXPIRING_SOON)
        }
        
        return LicenseValidationResult(
            isValid = true,
            license = license,
            warnings = warnings
        )
    }
    
    /**
     * Проверка доступности функции
     */
    suspend fun isFeatureEnabled(feature: String): Boolean {
        val validation = validateOnStartup()
        if (!validation.isValid) return false
        
        return validation.license?.license?.features
            ?.find { it.name == feature }
            ?.value == "true"
    }
    
    /**
     * Получение лимита функции
     */
    suspend fun getFeatureLimit(feature: String): Int? {
        val validation = validateOnStartup()
        if (!validation.isValid) return null
        
        return validation.license?.license?.limitations
            ?.find { it.name == feature }
            ?.value?.toIntOrNull()
    }
    
    private fun checkLicenseIntegrity(license: ActivatedLicense): Boolean {
        // Проверка целостности лицензии
        // TODO: Реализовать проверку подписи и хеша
        return true
    }
}

// Платформо-специфичные интерфейсы
expect class PlatformCrypto {
    fun getSecureDeviceFingerprint(): String
    fun decryptOfflineCode(code: String): OfflineActivationData
    fun schedulePeriodicCheck(checkCallback: (ActivatedLicense) -> Unit)
}

expect class LicenseRepository {
    fun saveLicense(license: ActivatedLicense)
    fun loadLicense(): ActivatedLicense?
    fun deleteLicense()
}

// Вспомогательные классы
@Serializable
data class ActivatedLicense(
    val license: License,
    val activationData: ActivationData,
    val lastValidatedAt: Long,
    val validationStatus: LicenseValidationStatus,
    val isOffline: Boolean = false,
    val offlineValidUntil: Long? = null,
    val requiresOnlineValidation: Boolean = false
) {
    fun isExpired(): Boolean {
        return license.validity.endDate < System.currentTimeMillis()
    }
    
    fun isOfflineExpired(): Boolean {
        if (!isOffline) return false
        return offlineValidUntil?.let { it < System.currentTimeMillis() } ?: false
    }
    
    fun getDaysRemaining(): Int {
        val remaining = license.validity.endDate - System.currentTimeMillis()
        return (remaining / (24 * 60 * 60 * 1000)).toInt()
    }
    
    fun getOfflineDaysRemaining(): Int {
        if (!isOffline) return 0
        val remaining = (offlineValidUntil ?: 0) - System.currentTimeMillis()
        return (remaining / (24 * 60 * 60 * 1000)).toInt()
    }
}

@Serializable
data class License(
    val id: String,
    val features: List<Feature>,
    val limitations: List<Limitation>,
    val validity: Validity,
    val supportsPlatform: (String) -> Boolean = { true }
)

@Serializable
data class Feature(val name: String, val value: String)

@Serializable
data class Limitation(val name: String, val value: String)

@Serializable
data class Validity(
    val startDate: Long,
    val endDate: Long,
    val isPerpetual: Boolean = false
)

@Serializable
data class ActivationData(
    val code: String,
    val deviceFingerprint: String,
    val activatedAt: Long,
    val activatedThrough: ActivationType
)

@Serializable
enum class ActivationType {
    ONLINE, OFFLINE, TRANSFER
}

@Serializable
enum class LicenseValidationStatus {
    VALID, VALID_OFFLINE, EXPIRED, REVOKED
}

@Serializable
data class OfflineActivationData(
    val licenseId: String,
    val validUntil: Long,
    val deviceFingerprint: String? = null,
    val requiresPeriodicCheck: Boolean = true
)

class LicenseException(
    val error: LicenseError,
    val requiresOnlineVerification: Boolean = false
) : Exception(error.name)

class LicenseRepositoryImpl : LicenseRepository {
    private var storedLicense: ActivatedLicense? = null
    
    override fun saveLicense(license: ActivatedLicense) {
        storedLicense = license
        // TODO: Сохранить в защищенное хранилище
    }
    
    override fun loadLicense(): ActivatedLicense? {
        return storedLicense
        // TODO: Загрузить из защищенного хранилища
    }
    
    override fun deleteLicense() {
        storedLicense = null
        // TODO: Удалить из хранилища
    }
}

fun getPlatformCrypto(): PlatformCrypto {
    return PlatformCrypto()
}

