package com.company.ipcamera.shared.data.datasource.remote

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.domain.model.Settings
import com.company.ipcamera.shared.domain.model.SettingsCategory
import com.company.ipcamera.shared.domain.model.SystemSettings

/**
 * Удаленный источник данных для настроек.
 * Отвечает за работу с REST API.
 */
interface SettingsRemoteDataSource {
    /**
     * Получить все настройки с сервера
     */
    suspend fun getSettings(category: SettingsCategory? = null): ApiResult<List<Settings>>

    /**
     * Получить настройку по ключу с сервера
     */
    suspend fun getSetting(key: String): ApiResult<Settings>

    /**
     * Обновить настройки на сервере
     */
    suspend fun updateSettings(settings: Map<String, String>): ApiResult<Int>

    /**
     * Обновить настройку на сервере
     */
    suspend fun updateSetting(key: String, value: String): ApiResult<Settings>

    /**
     * Удалить настройку с сервера
     */
    suspend fun deleteSetting(key: String): ApiResult<Unit>

    /**
     * Получить системные настройки с сервера
     */
    suspend fun getSystemSettings(): ApiResult<SystemSettings>

    /**
     * Обновить системные настройки на сервере
     */
    suspend fun updateSystemSettings(settings: SystemSettings): ApiResult<Unit>

    /**
     * Экспортировать настройки
     */
    suspend fun exportSettings(): ApiResult<Map<String, String>>

    /**
     * Импортировать настройки
     */
    suspend fun importSettings(settings: Map<String, String>): ApiResult<Unit>

    /**
     * Сбросить настройки
     */
    suspend fun resetSettings(category: SettingsCategory? = null): ApiResult<Unit>
}

