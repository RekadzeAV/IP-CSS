package com.company.ipcamera.core.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для создания/обновления камеры
 */
@Serializable
data class CreateCameraRequest(
    val name: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val model: String? = null,
    val resolution: ResolutionDto? = null,
    val fps: Int = 25,
    val bitrate: Int = 4096,
    val codec: String = "H.264",
    val audio: Boolean = false,
    val ptz: PTZConfigDto? = null,
    val streams: List<StreamConfigDto> = emptyList(),
    val settings: CameraSettingsDto? = null
)

@Serializable
data class UpdateCameraRequest(
    val name: String? = null,
    val url: String? = null,
    val username: String? = null,
    val password: String? = null,
    val model: String? = null,
    val resolution: ResolutionDto? = null,
    val fps: Int? = null,
    val bitrate: Int? = null,
    val codec: String? = null,
    val audio: Boolean? = null,
    val ptz: PTZConfigDto? = null,
    val streams: List<StreamConfigDto>? = null,
    val settings: CameraSettingsDto? = null
)

@Serializable
data class CameraResponse(
    val id: String,
    val name: String,
    val url: String,
    val username: String? = null,
    val model: String? = null,
    val status: String,
    val resolution: ResolutionDto? = null,
    val fps: Int,
    val bitrate: Int,
    val codec: String,
    val audio: Boolean,
    val ptz: PTZConfigDto? = null,
    val streams: List<StreamConfigDto> = emptyList(),
    val settings: CameraSettingsDto? = null,
    val statistics: CameraStatisticsDto? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSeen: Long? = null
)

@Serializable
data class ResolutionDto(
    val width: Int,
    val height: Int
)

@Serializable
data class PTZConfigDto(
    val enabled: Boolean = false,
    val type: String = "PTZ",
    val presets: List<String> = emptyList()
)

@Serializable
data class StreamConfigDto(
    val type: String,
    val resolution: ResolutionDto,
    val fps: Int,
    val bitrate: Int
)

@Serializable
data class CameraSettingsDto(
    val recording: RecordingSettingsDto? = null,
    val analytics: AnalyticsSettingsDto? = null,
    val notifications: NotificationSettingsDto? = null
)

@Serializable
data class RecordingSettingsDto(
    val enabled: Boolean = true,
    val mode: String = "CONTINUOUS",
    val quality: String = "HIGH",
    val schedule: String = "24/7"
)

@Serializable
data class AnalyticsSettingsDto(
    val motionDetection: Boolean = true,
    val zones: List<DetectionZoneDto> = emptyList(),
    val objectDetection: Boolean = false,
    val objectTypes: List<String> = emptyList()
)

@Serializable
data class DetectionZoneDto(
    val name: String,
    val polygon: List<List<Int>>,
    val sensitivity: Int = 80
)

@Serializable
data class NotificationSettingsDto(
    val enabled: Boolean = true,
    val channels: List<String> = emptyList(),
    val events: List<String> = emptyList()
)

@Serializable
data class CameraStatisticsDto(
    val uptime: Double = 0.0,
    val recordedHours: Long = 0,
    val eventsCount: Long = 0,
    val storageUsed: Long = 0
)

@Serializable
data class CameraControlRequest(
    val action: String,
    val parameters: Map<String, String> = emptyMap()
)

@Serializable
data class CameraControlResponse(
    val success: Boolean,
    val message: String? = null
)


