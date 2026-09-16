package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.dto.*

/**
 * API сервис для работы с настройками
 */
class SettingsApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/settings"
) {

    private suspend inline fun <T> ApiResult<T>.getOrThrow(): T = fold(
        onSuccess = { it },
        onError = { throw it }
    )

    suspend fun getSettings(category: String? = null): List<SettingsResponse> {
        val queryParams = category?.let { mapOf("category" to it) } ?: emptyMap()
        return apiClient.get<List<SettingsResponse>>(path = basePath, queryParameters = queryParams).getOrThrow()
    }

    suspend fun getSetting(key: String): SettingsResponse {
        return apiClient.get<SettingsResponse>(path = "$basePath/$key").getOrThrow()
    }

    suspend fun updateSettings(request: UpdateSettingsRequest): UpdateSettingsResponse {
        return apiClient.put<UpdateSettingsRequest, UpdateSettingsResponse>(path = basePath, body = request).getOrThrow()
    }

    suspend fun updateSetting(key: String, value: String): SettingsResponse {
        @kotlinx.serialization.Serializable
        data class UpdateSettingRequest(val value: String)
        return apiClient.put<UpdateSettingRequest, SettingsResponse>(
            path = "$basePath/$key",
            body = UpdateSettingRequest(value)
        ).getOrThrow()
    }

    suspend fun deleteSetting(key: String): ApiResponse<Unit> {
        return apiClient.delete<ApiResponse<Unit>>(path = "$basePath/$key").getOrThrow()
    }

    suspend fun getSystemSettings(): SystemSettingsResponse {
        return apiClient.get<SystemSettingsResponse>(path = "$basePath/system").getOrThrow()
    }

    suspend fun updateSystemSettings(settings: SystemSettingsResponse): ApiResponse<Unit> {
        return apiClient.put<SystemSettingsResponse, ApiResponse<Unit>>(path = "$basePath/system", body = settings).getOrThrow()
    }

    suspend fun resetSettings(category: String? = null): ApiResponse<Unit> {
        val path = if (category != null) "$basePath/reset?category=$category" else "$basePath/reset"
        return apiClient.post<Unit, ApiResponse<Unit>>(path = path).getOrThrow()
    }

    suspend fun exportSettings(): ApiResponse<Map<String, String>> {
        return apiClient.get<ApiResponse<Map<String, String>>>(path = "$basePath/export").getOrThrow()
    }

    suspend fun importSettings(settings: Map<String, String>): ApiResponse<Unit> {
        @kotlinx.serialization.Serializable
        data class ImportSettingsRequest(val settings: Map<String, String>)
        return apiClient.post<ImportSettingsRequest, ApiResponse<Unit>>(
            path = "$basePath/import",
            body = ImportSettingsRequest(settings)
        ).getOrThrow()
    }
}
