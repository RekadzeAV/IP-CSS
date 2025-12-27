package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.License

/**
 * Репозиторий для работы с лицензиями
 */
interface LicenseRepository {
    /**
     * Получить текущую лицензию
     */
    suspend fun getLicense(): License?
    
    /**
     * Активировать лицензию
     */
    suspend fun activateLicense(licenseKey: String, deviceId: String? = null): Result<License>
    
    /**
     * Валидировать лицензию
     */
    suspend fun validateLicense(): Result<License>
    
    /**
     * Деактивировать лицензию
     */
    suspend fun deactivateLicense(): Result<Unit>
    
    /**
     * Перенести лицензию на другое устройство
     */
    suspend fun transferLicense(newDeviceId: String): Result<License>
    
    /**
     * Получить доступные функции лицензии
     */
    suspend fun getAvailableFeatures(): Result<List<String>>
    
    /**
     * Проверить доступность функции
     */
    suspend fun checkFeatureAvailability(featureName: String): Result<Boolean>
}



