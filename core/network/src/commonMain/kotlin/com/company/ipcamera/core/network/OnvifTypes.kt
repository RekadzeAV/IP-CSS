package com.company.ipcamera.core.network

/**
 * Обнаруженная камера
 */
data class DiscoveredCamera(
    val url: String,
    val name: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val capabilities: OnvifCapabilities? = null
)

/**
 * Информация о потоке
 */
data class StreamInfo(
    val type: String,
    val resolution: String,
    val fps: Int,
    val codec: String
)

/**
 * Возможности камеры
 */
data class CameraCapabilities(
    val ptz: Boolean,
    val audio: Boolean,
    val onvif: Boolean,
    val analytics: Boolean
)

/**
 * Код ошибки
 */
enum class ErrorCode {
    CONNECTION_FAILED,
    AUTHENTICATION_FAILED,
    TIMEOUT,
    INVALID_RESPONSE,
    UNKNOWN_ERROR
}

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

