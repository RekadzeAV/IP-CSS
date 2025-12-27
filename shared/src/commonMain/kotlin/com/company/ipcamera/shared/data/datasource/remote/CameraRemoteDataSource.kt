package com.company.ipcamera.shared.data.datasource.remote

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.domain.model.Camera

/**
 * Удаленный источник данных для камер.
 * Отвечает за работу с REST API.
 */
interface CameraRemoteDataSource {
    /**
     * Получить список камер с сервера
     */
    suspend fun getCameras(page: Int = 1, limit: Int = 20, status: String? = null): ApiResult<List<Camera>>

    /**
     * Получить камеру по ID с сервера
     */
    suspend fun getCameraById(id: String): ApiResult<Camera>

    /**
     * Создать камеру на сервере
     */
    suspend fun createCamera(camera: Camera): ApiResult<Camera>

    /**
     * Обновить камеру на сервере
     */
    suspend fun updateCamera(id: String, camera: Camera): ApiResult<Camera>

    /**
     * Удалить камеру с сервера
     */
    suspend fun deleteCamera(id: String): ApiResult<Unit>

    /**
     * Протестировать подключение к камере
     */
    suspend fun testConnection(id: String): ApiResult<Map<String, String>>

    /**
     * Получить статус камеры с сервера
     */
    suspend fun getCameraStatus(id: String): ApiResult<Map<String, String>>
}

