package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.domain.model.Camera

/**
 * Репозиторий для работы с камерами
 */
interface CameraRepository {
    /**
     * Получить все камеры
     */
    suspend fun getCameras(): List<Camera>

    /**
     * Получить камеру по ID
     */
    suspend fun getCameraById(id: String): Camera?

    /**
     * Добавить новую камеру
     */
    suspend fun addCamera(camera: Camera): Result<Camera>

    /**
     * Обновить камеру
     */
    suspend fun updateCamera(camera: Camera): Result<Camera>

    /**
     * Удалить камеру
     */
    suspend fun removeCamera(id: String): Result<Unit>

    /**
     * Обнаружить камеры в сети
     */
    suspend fun discoverCameras(): List<DiscoveredCamera>

    /**
     * Проверить подключение к камере
     */
    suspend fun testConnection(camera: Camera): ConnectionTestResult

    /**
     * Получить статус камеры
     */
    suspend fun getCameraStatus(id: String): CameraStatus
}

/**
 * Обнаруженная камера в сети
 */
data class DiscoveredCamera(
    val name: String,
    val url: String,
    val model: String? = null,
    val manufacturer: String? = null,
    val ipAddress: String,
    val port: Int = 554
)

/**
 * Результат проверки подключения
 */
sealed class ConnectionTestResult {
    data class Success(
        val streams: List<StreamInfo>,
        val capabilities: CameraCapabilities
    ) : ConnectionTestResult()

    data class Failure(
        val error: String,
        val code: ErrorCode
    ) : ConnectionTestResult()
}

data class StreamInfo(
    val type: String,
    val resolution: String,
    val fps: Int,
    val codec: String
)

data class CameraCapabilities(
    val ptz: Boolean = false,
    val audio: Boolean = false,
    val onvif: Boolean = false,
    val analytics: Boolean = false
)

enum class ErrorCode {
    CONNECTION_FAILED,
    AUTHENTICATION_FAILED,
    UNSUPPORTED_FORMAT,
    TIMEOUT,
    UNKNOWN
}


