package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.SettingsApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SettingsType
import com.company.ipcamera.shared.domain.model.SystemSettings
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация SettingsRepository с использованием API сервиса
 */
class SettingsRepositoryImpl(
    private val settingsApiService: SettingsApiService
) : SettingsRepository {
    
    override suspend fun getSettings(category: SettingsCategory?): List<Settings> = withContext(Dispatchers.Default) {
        try {
            val categoryString = category?.name?.lowercase()
            // Note: SettingsApiService.getSettings returns List directly, not ApiResult
            // This needs to be wrapped in try-catch or updated API service to return ApiResult
            val dtoList = settingsApiService.getSettings(categoryString)
            dtoList.map { it.toDomain() }
        } catch (e: Exception) {
            logger.error(e) { "Error getting settings" }
            emptyList()
        }
    }
    
    override suspend fun getSetting(key: String): Settings? = withContext(Dispatchers.Default) {
        try {
            // Note: SettingsApiService.getSetting returns SettingsResponse directly
            val dto = settingsApiService.getSetting(key)
            dto.toDomain()
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting: $key" }
            null
        }
    }
    
    override suspend fun updateSettings(settings: Map<String, String>): Result<Int> = withContext(Dispatchers.Default) {
        try {
            val request = UpdateSettingsRequest(settings = settings)
            // Note: SettingsApiService.updateSettings returns UpdateSettingsResponse directly
            val response = settingsApiService.updateSettings(request)
            Result.success(response.updated)
        } catch (e: Exception) {
            logger.error(e) { "Error updating settings" }
            Result.failure(e)
        }
    }
    
    override suspend fun updateSetting(key: String, value: String): Result<Settings> = withContext(Dispatchers.Default) {
        try {
            // Note: SettingsApiService.updateSetting returns SettingsResponse directly
            val dto = settingsApiService.updateSetting(key, value)
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error updating setting: $key" }
            Result.failure(e)
        }
    }
    
    override suspend fun deleteSetting(key: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = settingsApiService.deleteSetting(key)
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error deleting setting: $key" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting setting: $key" }
            Result.failure(e)
        }
    }
    
    override suspend fun getSystemSettings(): SystemSettings? = withContext(Dispatchers.Default) {
        try {
            // Note: SettingsApiService.getSystemSettings returns SystemSettingsResponse directly
            val dto = settingsApiService.getSystemSettings()
            dto.toDomain()
        } catch (e: Exception) {
            logger.error(e) { "Error getting system settings" }
            null
        }
    }
    
    override suspend fun updateSystemSettings(settings: SystemSettings): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val dto = settings.toDto()
            val result = settingsApiService.updateSystemSettings(dto)
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error updating system settings" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error updating system settings" }
            Result.failure(e)
        }
    }
    
    override suspend fun resetSettings(category: SettingsCategory?): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val categoryString = category?.name?.lowercase()
            val result = settingsApiService.resetSettings(categoryString)
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error resetting settings" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error resetting settings" }
            Result.failure(e)
        }
    }
    
    override suspend fun exportSettings(): Result<Map<String, String>> = withContext(Dispatchers.Default) {
        try {
            val result = settingsApiService.exportSettings()
            result.fold(
                onSuccess = { response ->
                    Result.success(response.data ?: emptyMap())
                },
                onError = { error ->
                    logger.error(error) { "Error exporting settings" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error exporting settings" }
            Result.failure(e)
        }
    }
    
    override suspend fun importSettings(settings: Map<String, String>): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = settingsApiService.importSettings(settings)
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error importing settings" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error importing settings" }
            Result.failure(e)
        }
    }
    
    private fun SettingsResponse.toDomain(): Settings {
        return Settings(
            id = id,
            category = SettingsCategory.valueOf(category.uppercase()),
            key = key,
            value = value,
            type = SettingsType.valueOf(type.uppercase()),
            description = description,
            updatedAt = updatedAt
        )
    }
    
    private fun SystemSettingsResponse.toDomain(): SystemSettings {
        return SystemSettings(
            recording = recording?.let {
                com.company.ipcamera.shared.domain.model.RecordingSystemSettings(
                    defaultQuality = it.defaultQuality,
                    defaultFormat = it.defaultFormat,
                    maxDuration = it.maxDuration,
                    autoDelete = it.autoDelete,
                    retentionDays = it.retentionDays
                )
            },
            storage = storage?.let {
                com.company.ipcamera.shared.domain.model.StorageSettings(
                    maxStorageSize = it.maxStorageSize,
                    currentStorageUsed = it.currentStorageUsed,
                    storagePath = it.storagePath,
                    autoCleanup = it.autoCleanup
                )
            },
            notifications = notifications?.let {
                com.company.ipcamera.shared.domain.model.NotificationSystemSettings(
                    emailEnabled = it.emailEnabled,
                    smsEnabled = it.smsEnabled,
                    pushEnabled = it.pushEnabled,
                    webhookUrl = it.webhookUrl
                )
            },
            security = security?.let {
                com.company.ipcamera.shared.domain.model.SecuritySettings(
                    requireAuth = it.requireAuth,
                    sessionTimeout = it.sessionTimeout,
                    passwordPolicy = it.passwordPolicy?.let { policy ->
                        com.company.ipcamera.shared.domain.model.PasswordPolicy(
                            minLength = policy.minLength,
                            requireUppercase = policy.requireUppercase,
                            requireLowercase = policy.requireLowercase,
                            requireNumbers = policy.requireNumbers,
                            requireSpecialChars = policy.requireSpecialChars
                        )
                    }
                )
            },
            network = network?.let {
                com.company.ipcamera.shared.domain.model.NetworkSettings(
                    apiPort = it.apiPort,
                    websocketPort = it.websocketPort,
                    allowRemoteAccess = it.allowRemoteAccess,
                    sslEnabled = it.sslEnabled
                )
            }
        )
    }
    
    private fun SystemSettings.toDto(): SystemSettingsResponse {
        return SystemSettingsResponse(
            recording = recording?.let {
                RecordingSystemSettings(
                    defaultQuality = it.defaultQuality,
                    defaultFormat = it.defaultFormat,
                    maxDuration = it.maxDuration,
                    autoDelete = it.autoDelete,
                    retentionDays = it.retentionDays
                )
            },
            storage = storage?.let {
                StorageSettings(
                    maxStorageSize = it.maxStorageSize,
                    currentStorageUsed = it.currentStorageUsed,
                    storagePath = it.storagePath,
                    autoCleanup = it.autoCleanup
                )
            },
            notifications = notifications?.let {
                NotificationSystemSettings(
                    emailEnabled = it.emailEnabled,
                    smsEnabled = it.smsEnabled,
                    pushEnabled = it.pushEnabled,
                    webhookUrl = it.webhookUrl
                )
            },
            security = security?.let {
                SecuritySettings(
                    requireAuth = it.requireAuth,
                    sessionTimeout = it.sessionTimeout,
                    passwordPolicy = it.passwordPolicy?.let { policy ->
                        PasswordPolicy(
                            minLength = policy.minLength,
                            requireUppercase = policy.requireUppercase,
                            requireLowercase = policy.requireLowercase,
                            requireNumbers = policy.requireNumbers,
                            requireSpecialChars = policy.requireSpecialChars
                        )
                    }
                )
            },
            network = network?.let {
                NetworkSettings(
                    apiPort = it.apiPort,
                    websocketPort = it.websocketPort,
                    allowRemoteAccess = it.allowRemoteAccess,
                    sslEnabled = it.sslEnabled
                )
            }
        )
    }
}

