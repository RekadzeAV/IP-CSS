package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.dto.ApiResponse
import com.company.ipcamera.core.network.dto.RtspStreamUrlResponse
import com.company.ipcamera.core.network.dto.StreamStatusResponse

/**
 * API сервис для работы с видеопотоками
 */
class StreamApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/cameras"
) {
    
    /**
     * Начать трансляцию для камеры
     */
    suspend fun startStream(cameraId: String): ApiResult<String> {
        return apiClient.post<ApiResponse<String>>(
            path = "$basePath/$cameraId/stream/start"
        ).fold(
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(
                        com.company.ipcamera.core.network.ApiError.UnknownError(
                            Exception(response.error?.message ?: "Failed to start stream")
                        )
                    )
                }
            },
            onError = { error -> ApiResult.Error(error) }
        )
    }
    
    /**
     * Остановить трансляцию для камеры
     */
    suspend fun stopStream(cameraId: String): ApiResult<Unit> {
        return apiClient.post<ApiResponse<Unit>>(
            path = "$basePath/$cameraId/stream/stop"
        ).fold(
            onSuccess = { response ->
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(
                        com.company.ipcamera.core.network.ApiError.UnknownError(
                            Exception(response.error?.message ?: "Failed to stop stream")
                        )
                    )
                }
            },
            onError = { error -> ApiResult.Error(error) }
        )
    }
    
    /**
     * Получить статус трансляции
     */
    suspend fun getStreamStatus(cameraId: String): ApiResult<StreamStatusResponse> {
        return apiClient.get<ApiResponse<StreamStatusResponse>>(
            path = "$basePath/$cameraId/stream/status"
        ).fold(
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data)
                } else {
                    ApiResult.Error(
                        com.company.ipcamera.core.network.ApiError.UnknownError(
                            Exception(response.error?.message ?: "Failed to get stream status")
                        )
                    )
                }
            },
            onError = { error -> ApiResult.Error(error) }
        )
    }
    
    /**
     * Получить RTSP URL для прямой трансляции
     */
    suspend fun getRtspUrl(cameraId: String): ApiResult<String> {
        return apiClient.get<ApiResponse<RtspStreamUrlResponse>>(
            path = "$basePath/$cameraId/stream/rtsp"
        ).fold(
            onSuccess = { response ->
                if (response.success && response.data != null) {
                    ApiResult.Success(response.data.rtspUrl)
                } else {
                    ApiResult.Error(
                        com.company.ipcamera.core.network.ApiError.UnknownError(
                            Exception(response.error?.message ?: "Failed to get RTSP URL")
                        )
                    )
                }
            },
            onError = { error -> ApiResult.Error(error) }
        )
    }
}

