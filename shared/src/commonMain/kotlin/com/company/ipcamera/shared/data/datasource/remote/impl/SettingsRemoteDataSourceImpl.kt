package com.company.ipcamera.shared.data.datasource.remote.impl

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.SettingsApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.remote.SettingsRemoteDataSource
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация SettingsRemoteDataSource с использованием SettingsApiService
 */
class SettingsRemoteDataSourceImpl(
    private val settingsApiService: SettingsApiService
) : SettingsRemoteDataSource {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getSettings(category: SettingsCategory?): ApiResult<List<Settings>> = withContext(Dispatchers.IO) {
        try {
            val response = settingsApiService.getSettings(category?.name)
            val settings = response.map { it.toDomain() }
            ApiResult.Success(settings)
        } catch (e: Exception) {
            logger.error(e) { "Error getting settings from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getSetting(key: String): ApiResult<Settings> = withContext(Dispatchers.IO) {
        try {
            val response = settingsApiService.getSetting(key)
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error getting setting from remote API: $key" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun updateSettings(settings: Map<String, String>): ApiResult<Int> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateSettingsRequest(settings = settings)
            val response = settingsApiService.updateSettings(request)
            ApiResult.Success(response.updated)
        } catch (e: Exception) {
            logger.error(e) { "Error updating settings on remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun updateSetting(key: String, value: String): ApiResult<Settings> = withContext(Dispatchers.IO) {
        try {
            val response = settingsApiService.updateSetting(key, value)
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error updating setting on remote API: $key" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun deleteSetting(key: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsApiService.deleteSetting(key)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting setting from remote API: $key" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getSystemSettings(): ApiResult<SystemSettings> = withContext(Dispatchers.IO) {
        try {
            val response = settingsApiService.getSystemSettings()
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error getting system settings from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun updateSystemSettings(settings: SystemSettings): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = settings.toDto()
            settingsApiService.updateSystemSettings(request)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating system settings on remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun exportSettings(): ApiResult<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = settingsApiService.exportSettings()
            ApiResult.Success(response.data ?: emptyMap())
        } catch (e: Exception) {
            logger.error(e) { "Error exporting settings from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun importSettings(settings: Map<String, String>): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsApiService.importSettings(settings)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error importing settings to remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun resetSettings(category: SettingsCategory?): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            settingsApiService.resetSettings(category?.name)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error resetting settings on remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    /**
     * Маппинг SettingsResponse в Domain модель Settings
     */
    private fun SettingsResponse.toDomain(): Settings {
        return Settings(
            id = id,
            category = SettingsCategory.valueOf(category),
            key = key,
            value = value,
            type = com.company.ipcamera.shared.domain.model.SettingsType.valueOf(type.uppercase()),
            description = description,
            updatedAt = updatedAt
        )
    }

    /**
     * Маппинг SystemSettingsResponse в Domain модель SystemSettings
     */
    private fun SystemSettingsResponse.toDomain(): SystemSettings {
        return SystemSettings(
            recording = recording?.let {
                com.company.ipcamera.shared.domain.model.RecordingSystemSettings(
                    defaultQuality = com.company.ipcamera.shared.domain.model.Quality.valueOf(it.defaultQuality),
                    defaultFormat = com.company.ipcamera.shared.domain.model.RecordingFormat.valueOf(it.defaultFormat.uppercase()),
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

    /**
     * Маппинг SystemSettings в SystemSettingsResponse
     */
    private fun SystemSettings.toDto(): SystemSettingsResponse {
        return SystemSettingsResponse(
            recording = recording?.let {
                RecordingSystemSettings(
                    defaultQuality = it.defaultQuality.name,
                    defaultFormat = it.defaultFormat.name.lowercase(),
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

