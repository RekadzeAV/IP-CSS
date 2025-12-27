package com.company.ipcamera.shared.domain.model

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import kotlinx.serialization.Serializable

/**
 * Модель IP-камеры
 */
@Serializable
data class Camera(
    val id: String,
    val name: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val model: String? = null,
    val status: CameraStatus = CameraStatus.UNKNOWN,
    val resolution: Resolution? = null,
    val fps: Int = 25,
    val bitrate: Int = 4096,
    val codec: String = "H.264",
    val audio: Boolean = false,
    val ptz: PTZConfig? = null,
    val streams: List<StreamConfig> = emptyList(),
    val settings: CameraSettings = CameraSettings(),
    val statistics: CameraStatistics? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSeen: Long? = null
)

@Serializable
data class PTZConfig(
    val enabled: Boolean = false,
    val type: PTZType = PTZType.PTZ,
    val presets: List<String> = emptyList()
)

@Serializable
enum class PTZType {
    PTZ,
    PT,
    FIXED
}

@Serializable
data class StreamConfig(
    val type: StreamType,
    val resolution: Resolution,
    val fps: Int,
    val bitrate: Int
)

@Serializable
enum class StreamType {
    MAIN,
    SUB,
    AUDIO,
    METADATA
}

@Serializable
data class CameraSettings(
    val recording: RecordingSettings = RecordingSettings(),
    val analytics: AnalyticsSettings = AnalyticsSettings(),
    val notifications: NotificationSettings = NotificationSettings()
)

@Serializable
data class RecordingSettings(
    val enabled: Boolean = true,
    val mode: RecordingMode = RecordingMode.CONTINUOUS,
    val quality: Quality = Quality.HIGH,
    val schedule: String = "24/7"
)

@Serializable
enum class RecordingMode {
    CONTINUOUS,
    SCHEDULED,
    EVENT,
    MANUAL
}

@Serializable
enum class Quality {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}

@Serializable
data class AnalyticsSettings(
    val motionDetection: Boolean = true,
    val zones: List<DetectionZone> = emptyList(),
    val objectDetection: Boolean = false,
    val objectTypes: List<String> = emptyList()
)

@Serializable
data class DetectionZone(
    val name: String,
    val polygon: List<List<Int>>,
    val sensitivity: Int = 80
)

@Serializable
data class NotificationSettings(
    val enabled: Boolean = true,
    val channels: List<String> = emptyList(),
    val events: List<String> = emptyList()
)

@Serializable
data class CameraStatistics(
    val uptime: Double = 0.0,
    val recordedHours: Long = 0,
    val eventsCount: Long = 0,
    val storageUsed: Long = 0
)

