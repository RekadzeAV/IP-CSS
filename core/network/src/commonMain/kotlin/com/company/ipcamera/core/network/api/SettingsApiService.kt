package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.dto.*
import kotlinx.serialization.builtins.serializer

/**
 * API сервис для работы с настройками
 */
class SettingsApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/settings"
) {
    
    /**
     * Получить все настройки
     * 
     * @param category фильтр по категории
     * @return список настроек
     */
    suspend fun getSettings(category: String? = null): List<SettingsResponse> {
        val queryParams = category?.let { mapOf("category" to it) }
        return apiClient.get(
            path = basePath,
            queryParameters = queryParams,
            responseType = kotlinx.serialization.builtins.ListSerializer(SettingsResponse.serializer())
        )
    }
    
    /**
     * Получить настройку по ключу
     * 
     * @param key ключ настройки
     * @return значение настройки
     */
    suspend fun getSetting(key: String): SettingsResponse {
        return apiClient.get(
            path = "$basePath/$key",
            responseType = SettingsResponse.serializer()
        )
    }
    
    /**
     * Обновить настройки
     * 
     * @param request настройки для обновления
     * @return результат обновления
     */
    suspend fun updateSettings(request: UpdateSettingsRequest): UpdateSettingsResponse {
        return apiClient.put(
            path = basePath,
            body = request,
            responseType = UpdateSettingsResponse.serializer()
        )
    }
    
    /**
     * Обновить одну настройку
     * 
     * @param key ключ настройки
     * @param value новое значение
     * @return обновленная настройка
     */
    suspend fun updateSetting(key: String, value: String): SettingsResponse {
        @kotlinx.serialization.Serializable
        data class UpdateSettingRequest(val value: String)
        
        return apiClient.put(
            path = "$basePath/$key",
            body = UpdateSettingRequest(value),
            responseType = SettingsResponse.serializer()
        )
    }
    
    /**
     * Удалить настройку
     * 
     * @param key ключ настройки
     */
    suspend fun deleteSetting(key: String): ApiResponse<Unit> {
        return apiClient.delete(
            path = "$basePath/$key",
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
    
    /**
     * Получить системные настройки
     * 
     * @return системные настройки
     */
    suspend fun getSystemSettings(): SystemSettingsResponse {
        return apiClient.get(
            path = "$basePath/system",
            responseType = SystemSettingsResponse.serializer()
        )
    }
    
    /**
     * Обновить системные настройки
     * 
     * @param settings системные настройки
     * @return результат обновления
     */
    suspend fun updateSystemSettings(settings: SystemSettingsResponse): ApiResponse<Unit> {
        return apiClient.put(
            path = "$basePath/system",
            body = settings,
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
    
    /**
     * Сбросить настройки к значениям по умолчанию
     * 
     * @param category категория настроек (опционально)
     * @return результат сброса
     */
    suspend fun resetSettings(category: String? = null): ApiResponse<Unit> {
        return apiClient.post(
            path = "$basePath/reset",
            queryParameters = category?.let { mapOf("category" to it) },
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
    
    /**
     * Экспортировать настройки
     * 
     * @return настройки в формате JSON
     */
    suspend fun exportSettings(): ApiResponse<Map<String, String>> {
        return apiClient.get(
            path = "$basePath/export",
            responseType = ApiResponse.serializer(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    String.serializer()
                )
            )
        )
    }
    
    /**
     * Импортировать настройки
     * 
     * @param settings настройки для импорта
     * @return результат импорта
     */
    suspend fun importSettings(settings: Map<String, String>): ApiResponse<Unit> {
        @kotlinx.serialization.Serializable
        data class ImportSettingsRequest(val settings: Map<String, String>)
        
        return apiClient.post(
            path = "$basePath/import",
            body = ImportSettingsRequest(settings),
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
}

