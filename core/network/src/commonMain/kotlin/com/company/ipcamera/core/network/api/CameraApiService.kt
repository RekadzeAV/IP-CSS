package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.dto.*

/**
 * API сервис для работы с камерами
 */
class CameraApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/cameras"
) {
    
    /**
     * Получить список камер
     * 
     * @param page номер страницы (начиная с 1)
     * @param limit количество элементов на странице
     * @param status фильтр по статусу (ONLINE, OFFLINE, ERROR, CONNECTING, UNKNOWN)
     * @return список камер с пагинацией
     */
    suspend fun getCameras(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null
    ): ApiResult<PaginatedResponse<CameraResponse>> {
        val queryParams = mutableMapOf<String, String>()
        queryParams["page"] = page.toString()
        queryParams["limit"] = limit.toString()
        status?.let { queryParams["status"] = it }
        
        return apiClient.get<PaginatedResponse<CameraResponse>>(
            path = basePath,
            queryParameters = queryParams
        )
    }
    
    /**
     * Получить камеру по ID
     * 
     * @param id идентификатор камеры
     * @return информация о камере
     */
    suspend fun getCameraById(id: String): ApiResult<CameraResponse> {
        return apiClient.get<CameraResponse>(path = "$basePath/$id")
    }
    
    /**
     * Создать новую камеру
     * 
     * @param request данные для создания камеры
     * @return созданная камера
     */
    suspend fun createCamera(request: CreateCameraRequest): ApiResult<CameraResponse> {
        return apiClient.post<CameraResponse>(
            path = basePath,
            body = request
        )
    }
    
    /**
     * Обновить камеру
     * 
     * @param id идентификатор камеры
     * @param request данные для обновления
     * @return обновленная камера
     */
    suspend fun updateCamera(id: String, request: UpdateCameraRequest): ApiResult<CameraResponse> {
        return apiClient.put<CameraResponse>(
            path = "$basePath/$id",
            body = request
        )
    }
    
    /**
     * Удалить камеру
     * 
     * @param id идентификатор камеры
     */
    suspend fun deleteCamera(id: String): ApiResult<ApiResponse<Unit>> {
        return apiClient.delete<ApiResponse<Unit>>(path = "$basePath/$id")
    }
    
    /**
     * Протестировать подключение к камере
     * 
     * @param id идентификатор камеры
     * @return результат теста подключения
     */
    suspend fun testConnection(id: String): ApiResult<ApiResponse<Map<String, String>>> {
        return apiClient.post<ApiResponse<Map<String, String>>>(
            path = "$basePath/$id/test"
        )
    }
    
    /**
     * Управление камерой (PTZ, запись и т.д.)
     * 
     * @param id идентификатор камеры
     * @param request команда управления
     * @return результат выполнения команды
     */
    suspend fun controlCamera(id: String, request: CameraControlRequest): ApiResult<CameraControlResponse> {
        return apiClient.post<CameraControlResponse>(
            path = "$basePath/$id/control",
            body = request
        )
    }
    
    /**
     * Получить статус камеры
     * 
     * @param id идентификатор камеры
     * @return статус камеры
     */
    suspend fun getCameraStatus(id: String): ApiResult<ApiResponse<Map<String, String>>> {
        return apiClient.get<ApiResponse<Map<String, String>>>(path = "$basePath/$id/status")
    }
}

