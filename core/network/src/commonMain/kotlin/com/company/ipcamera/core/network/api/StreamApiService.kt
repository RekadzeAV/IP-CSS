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
     * Абсолютный URL плейлиста HLS для ExoPlayer / внешних плееров.
     * [apiClient.baseUrl] обычно `http://host:port` (без `/api/v1`).
     */
    fun absoluteHlsPlaylistUrl(cameraId: String): String {
        val base = apiClient.baseUrl.trimEnd('/')
        return "$base/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8"
    }

    /**
     * Сервер в [StreamStatusResponse.hlsUrl] может вернуть относительный путь `/api/v1/...` — для Media3 нужен абсолютный URL.
     */
    fun resolveHlsUrlForPlayback(cameraId: String, serverHlsUrl: String?): String {
        val raw = serverHlsUrl?.trim().orEmpty()
        if (raw.isEmpty()) return absoluteHlsPlaylistUrl(cameraId)
        if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
            return raw
        }
        val base = apiClient.baseUrl.trimEnd('/')
        val path = if (raw.startsWith("/")) raw else "/$raw"
        return base + path
    }

    /**
     * Начать трансляцию для камеры
     */
    suspend fun startStream(cameraId: String): ApiResult<String> {
        return apiClient.post<Unit, ApiResponse<String>>(
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
        return apiClient.post<Unit, ApiResponse<Unit>>(
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

    /**
     * Изменить качество потока
     * @param cameraId ID камеры
     * @param quality Качество потока: "low", "medium", "high", "ultra"
     */
    suspend fun setStreamQuality(cameraId: String, quality: String): ApiResult<Unit> {
        return apiClient.post<Unit, ApiResponse<Unit>>(
            path = "$basePath/$cameraId/stream/quality?quality=$quality"
        ).fold(
            onSuccess = { response ->
                if (response.success) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(
                        com.company.ipcamera.core.network.ApiError.UnknownError(
                            Exception(response.error?.message ?: "Failed to change stream quality")
                        )
                    )
                }
            },
            onError = { error -> ApiResult.Error(error) }
        )
    }
}
