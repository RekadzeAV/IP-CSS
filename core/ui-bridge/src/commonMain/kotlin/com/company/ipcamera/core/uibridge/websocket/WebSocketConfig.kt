package com.company.ipcamera.core.uibridge.websocket

import kotlinx.coroutines.flow.StateFlow

/**
 * Конфигурация WebSocket подключения
 */
data class WebSocketConfig(
    val url: String,
    val reconnectDelay: Long = 1000L,
    val maxReconnectDelay: Long = 30000L,
    val reconnectMultiplier: Double = 2.0,
    val maxReconnectAttempts: Int = 10,
    val heartbeatInterval: Long = 30000L,
    val connectionTimeout: Long = 10000L
)

/**
 * Состояние WebSocket подключения
 */
sealed class WebSocketState {
    object Disconnected : WebSocketState()
    object Connecting : WebSocketState()
    object Connected : WebSocketState()
    data class Reconnecting(val attempt: Int, val delay: Long) : WebSocketState()
    data class Error(val message: String, val throwable: Throwable? = null) : WebSocketState()
}

/**
 * Типы событий WebSocket
 */
sealed class WebSocketMessage {
    data class CameraEvent(val event: CameraEventData) : WebSocketMessage()
    data class RecordingEvent(val event: RecordingEventData) : WebSocketMessage()
    data class SystemEvent(val event: SystemEventData) : WebSocketMessage()
    data class ErrorResponse(val error: String, val code: Int) : WebSocketMessage()
    object Heartbeat : WebSocketMessage()
    object HeartbeatAck : WebSocketMessage()
}

/**
 * Данные события камеры
 */
data class CameraEventData(
    val type: CameraEventType,
    val cameraId: String,
    val timestamp: Long,
    val data: Map<String, Any?>
)

enum class CameraEventType {
    MOTION_DETECTED,
    OBJECT_DETECTED,
    FACE_DETECTED,
    LICENSE_PLATE_DETECTED,
    CAMERA_OFFLINE,
    CAMERA_ONLINE,
    PTZ_UPDATED
}

/**
 * Данные события записи
 */
data class RecordingEventData(
    val type: RecordingEventType,
    val recordingId: String,
    val cameraId: String,
    val timestamp: Long,
    val data: Map<String, Any?>
)

enum class RecordingEventType {
    RECORDING_STARTED,
    RECORDING_STOPPED,
    RECORDING_PAUSED,
    RECORDING_RESUMED
}

/**
 * Данные системного события
 */
data class SystemEventData(
    val type: SystemEventType,
    val timestamp: Long,
    val data: Map<String, Any?>
)

enum class SystemEventType {
    CONNECTION_ESTABLISHED,
    CONNECTION_LOST,
    AUTH_REQUIRED,
    AUTH_SUCCESS,
    AUTH_FAILED,
    SERVER_ERROR
}
