package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.dto.*

/**
 * API сервис для работы с лицензиями
 */
class LicenseApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/license"
) {

    private suspend inline fun <T> ApiResult<T>.getOrThrow(): T = fold(
        onSuccess = { it },
        onError = { throw it }
    )

    /**
     * Получить информацию о лицензии
     *
     * @return информация о текущей лицензии
     */
    suspend fun getLicense(): LicenseResponse {
        return apiClient.get<LicenseResponse>(path = basePath).getOrThrow()
    }

    /**
     * Активировать лицензию
     *
     * @param request данные для активации
     * @return результат активации
     */
    suspend fun activateLicense(request: ActivateLicenseRequest): ActivateLicenseResponse {
        return apiClient.post<ActivateLicenseRequest, ActivateLicenseResponse>(
            path = "$basePath/activate",
            body = request
        ).getOrThrow()
    }

    /**
     * Валидировать лицензию
     *
     * @return результат валидации
     */
    suspend fun validateLicense(): ValidateLicenseResponse {
        return apiClient.get<ValidateLicenseResponse>(path = "$basePath/validate").getOrThrow()
    }

    /**
     * Деактивировать лицензию
     *
     * @return результат деактивации
     */
    suspend fun deactivateLicense(): ApiResponse<Unit> {
        return apiClient.post<Unit, ApiResponse<Unit>>(path = "$basePath/deactivate").getOrThrow()
    }

    /**
     * Перенести лицензию на другое устройство
     *
     * @param newDeviceId ID нового устройства
     * @return результат переноса
     */
    suspend fun transferLicense(newDeviceId: String): ApiResponse<Map<String, String>> {
        @kotlinx.serialization.Serializable
        data class TransferLicenseRequest(val newDeviceId: String)

        return apiClient.post<TransferLicenseRequest, ApiResponse<Map<String, String>>>(
            path = "$basePath/transfer",
            body = TransferLicenseRequest(newDeviceId)
        ).getOrThrow()
    }

    /**
     * Получить доступные функции лицензии
     *
     * @return список доступных функций
     */
    suspend fun getAvailableFeatures(): ApiResponse<List<String>> {
        return apiClient.get<ApiResponse<List<String>>>(path = "$basePath/features").getOrThrow()
    }

    /**
     * Проверить доступность функции
     *
     * @param featureName название функции
     * @return доступность функции
     */
    suspend fun checkFeatureAvailability(featureName: String): ApiResponse<Map<String, Boolean>> {
        return apiClient.get<ApiResponse<Map<String, Boolean>>>(path = "$basePath/features/$featureName").getOrThrow()
    }
}
