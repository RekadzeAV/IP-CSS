package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.dto.*
import kotlinx.serialization.builtins.serializer

/**
 * API сервис для работы с лицензиями
 */
class LicenseApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/license"
) {
    
    /**
     * Получить информацию о лицензии
     * 
     * @return информация о текущей лицензии
     */
    suspend fun getLicense(): LicenseResponse {
        return apiClient.get(
            path = basePath,
            responseType = LicenseResponse.serializer()
        )
    }
    
    /**
     * Активировать лицензию
     * 
     * @param request данные для активации
     * @return результат активации
     */
    suspend fun activateLicense(request: ActivateLicenseRequest): ActivateLicenseResponse {
        return apiClient.post(
            path = "$basePath/activate",
            body = request,
            responseType = ActivateLicenseResponse.serializer()
        )
    }
    
    /**
     * Валидировать лицензию
     * 
     * @return результат валидации
     */
    suspend fun validateLicense(): ValidateLicenseResponse {
        return apiClient.get(
            path = "$basePath/validate",
            responseType = ValidateLicenseResponse.serializer()
        )
    }
    
    /**
     * Деактивировать лицензию
     * 
     * @return результат деактивации
     */
    suspend fun deactivateLicense(): ApiResponse<Unit> {
        return apiClient.post(
            path = "$basePath/deactivate",
            responseType = ApiResponse.serializer(Unit.serializer())
        )
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
        
        return apiClient.post(
            path = "$basePath/transfer",
            body = TransferLicenseRequest(newDeviceId),
            responseType = ApiResponse.serializer(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    String.serializer()
                )
            )
        )
    }
    
    /**
     * Получить доступные функции лицензии
     * 
     * @return список доступных функций
     */
    suspend fun getAvailableFeatures(): ApiResponse<List<String>> {
        return apiClient.get(
            path = "$basePath/features",
            responseType = ApiResponse.serializer(
                kotlinx.serialization.builtins.ListSerializer(String.serializer())
            )
        )
    }
    
    /**
     * Проверить доступность функции
     * 
     * @param featureName название функции
     * @return доступность функции
     */
    suspend fun checkFeatureAvailability(featureName: String): ApiResponse<Map<String, Boolean>> {
        return apiClient.get(
            path = "$basePath/features/$featureName",
            responseType = ApiResponse.serializer(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    Boolean.serializer()
                )
            )
        )
    }
}

