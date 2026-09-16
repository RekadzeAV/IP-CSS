package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.ApiError
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.data.datasource.remote.SettingsRemoteDataSource
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings

/**
 * Mock реализация SettingsRemoteDataSource для тестов
 */
class MockSettingsRemoteDataSource(
    private var settings: MutableMap<String, String> = mutableMapOf(),
) : SettingsRemoteDataSource {
    var systemSettings: SystemSettings? = null

    var shouldFailOnGet: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var networkError: Exception? = null

    private fun apiErr(): ApiError = (networkError ?: Exception("Mock network error")).let { ApiError.UnknownError(it) }

    override suspend fun getSettings(category: SettingsCategory?): ApiResult<List<Settings>> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val settingsList =
                settings.map { (key, value) ->
                    Settings(
                        id = key,
                        category = category ?: SettingsCategory.OTHER,
                        key = key,
                        value = value,
                        type = com.company.ipcamera.shared.domain.model.SettingsType.STRING,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
            ApiResult.Success(settingsList)
        }
    }

    override suspend fun getSetting(key: String): ApiResult<Settings> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val value = settings[key]
            if (value != null) {
                ApiResult.Success(
                    Settings(
                        id = key,
                        category = SettingsCategory.OTHER,
                        key = key,
                        value = value,
                        type = com.company.ipcamera.shared.domain.model.SettingsType.STRING,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("Setting not found: $key")))
            }
        }
    }

    override suspend fun updateSettings(settings: Map<String, String>): ApiResult<Int> {
        return if (shouldFailOnUpdate) {
            ApiResult.Error(apiErr())
        } else {
            this.settings.putAll(settings)
            ApiResult.Success(settings.size)
        }
    }

    override suspend fun updateSetting(
        key: String,
        value: String,
    ): ApiResult<Settings> {
        return if (shouldFailOnUpdate) {
            ApiResult.Error(apiErr())
        } else {
            settings[key] = value
            ApiResult.Success(
                Settings(
                    id = key,
                    category = SettingsCategory.OTHER,
                    key = key,
                    value = value,
                    type = com.company.ipcamera.shared.domain.model.SettingsType.STRING,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    override suspend fun deleteSetting(key: String): ApiResult<Unit> {
        settings.remove(key)
        return ApiResult.Success(Unit)
    }

    override suspend fun getSystemSettings(): ApiResult<SystemSettings> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            ApiResult.Success(systemSettings ?: SystemSettings())
        }
    }

    override suspend fun updateSystemSettings(settings: SystemSettings): ApiResult<Unit> {
        return if (shouldFailOnUpdate) {
            ApiResult.Error(apiErr())
        } else {
            systemSettings = settings
            ApiResult.Success(Unit)
        }
    }

    override suspend fun exportSettings(): ApiResult<Map<String, String>> {
        return ApiResult.Success(settings.toMap())
    }

    override suspend fun importSettings(settings: Map<String, String>): ApiResult<Unit> {
        this.settings.putAll(settings)
        return ApiResult.Success(Unit)
    }

    override suspend fun resetSettings(category: SettingsCategory?): ApiResult<Unit> {
        if (category != null) {
            // В реальной реализации здесь была бы фильтрация по категории
        }
        settings.clear()
        return ApiResult.Success(Unit)
    }

    fun clear() {
        settings.clear()
        systemSettings = null
    }

    fun addSettingDirectly(
        key: String,
        value: String,
    ) {
        settings[key] = value
    }
}
