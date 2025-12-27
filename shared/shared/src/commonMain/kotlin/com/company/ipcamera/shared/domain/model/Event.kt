package com.company.ipcamera.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель события
 */
@Serializable
data class Event(
    val id: String,
    val cameraId: String,
    val cameraName: String? = null,
    val type: EventType,
    val severity: EventSeverity = EventSeverity.INFO,
    val timestamp: Long,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val acknowledged: Boolean = false,
    val acknowledgedAt: Long? = null,
    val acknowledgedBy: String? = null,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null
) {
    /**
     * Проверка, подтверждено ли событие
     */
    fun isAcknowledged(): Boolean = acknowledged
    
    /**
     * Проверка, критическое ли событие
     */
    fun isCritical(): Boolean = severity == EventSeverity.CRITICAL
}

@Serializable
enum class EventType {
    MOTION_DETECTION,
    OBJECT_DETECTION,
    FACE_DETECTION,
    LICENSE_PLATE_RECOGNITION,
    CAMERA_OFFLINE,
    CAMERA_ONLINE,
    RECORDING_STARTED,
    RECORDING_STOPPED,
    STORAGE_FULL,
    SYSTEM_ERROR,
    USER_ACTION,
    OTHER
}

@Serializable
enum class EventSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}
