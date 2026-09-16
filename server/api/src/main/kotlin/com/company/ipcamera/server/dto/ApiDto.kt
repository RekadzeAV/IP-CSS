package com.company.ipcamera.server.dto

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraObservationSummary
import com.company.ipcamera.shared.domain.repository.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String
)

@Serializable
data class CameraDto(
    val id: String,
    val name: String,
    val url: String,
    val username: String? = null,
    val model: String? = null,
    val status: String,
    val resolution: ResolutionDto? = null,
    val fps: Int = 25,
    val bitrate: Int = 4096,
    val codec: String = "H.264",
    val audio: Boolean = false,
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
    val notifications: NotificationSettingsDto? = null,
    val observation: ObservationSettingsDto? = null
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
    val objectTypes: List<String> = emptyList(),
    val executionLocation: String? = null
)

@Serializable
data class ObservationSettingsDto(
    val zoneClass: String? = null,
    val targetStreamFps: Int? = null,
    val sceneWidthMeters: Double? = null,
    val observationDistanceMeters: Double? = null,
    val pixelsPerMeterOverride: Double? = null,
    val targetPixelsPerMeterMin: Double? = null
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
data class CameraObservationSummaryDto(
    val computedPixelsPerMeter: Double?,
    val targetPixelsPerMeterMin: Double?,
    val lowResolutionWarning: Boolean
)

fun CameraObservationSummary.toDto(): CameraObservationSummaryDto = CameraObservationSummaryDto(
    computedPixelsPerMeter = computedPixelsPerMeter,
    targetPixelsPerMeterMin = targetPixelsPerMeterMin,
    lowResolutionWarning = lowResolutionWarning
)

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
data class DiscoveredCameraDto(
    val name: String,
    val url: String,
    val model: String? = null,
    val manufacturer: String? = null,
    val ipAddress: String,
    val port: Int = 554,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class ConnectionTestResultDto(
    val success: Boolean,
    val streams: List<StreamInfoDto>? = null,
    val capabilities: CameraCapabilitiesDto? = null,
    val error: String? = null,
    val errorCode: String? = null
)

@Serializable
data class StreamInfoDto(
    val type: String,
    val resolution: String,
    val fps: Int,
    val codec: String
)

@Serializable
data class CameraCapabilitiesDto(
    val ptz: Boolean = false,
    val audio: Boolean = false,
    val onvif: Boolean = false,
    val analytics: Boolean = false
)

// Extension functions для конвертации

fun Camera.toDto(): CameraDto {
    return CameraDto(
        id = this.id,
        name = this.name,
        url = this.url,
        username = this.username,
        model = this.model,
        status = this.status.name,
        resolution = this.resolution?.let { ResolutionDto(width = it.width, height = it.height) },
        fps = this.fps,
        bitrate = this.bitrate,
        codec = this.codec,
        audio = this.audio,
        ptz = this.ptz?.let { PTZConfigDto(enabled = it.enabled, type = it.type.name, presets = it.presets) },
        streams = this.streams.map { stream ->
            StreamConfigDto(
                type = stream.type.name,
                resolution = ResolutionDto(width = stream.resolution.width, height = stream.resolution.height),
                fps = stream.fps,
                bitrate = stream.bitrate
            )
        },
        settings = this.settings.let { s ->
            CameraSettingsDto(
                recording = RecordingSettingsDto(
                    enabled = s.recording.enabled,
                    mode = s.recording.mode.name,
                    quality = s.recording.quality.name,
                    schedule = s.recording.schedule
                ),
                analytics = AnalyticsSettingsDto(
                    motionDetection = s.analytics.motionDetection,
                    zones = s.analytics.zones.map { zone ->
                        DetectionZoneDto(name = zone.name, polygon = zone.polygon, sensitivity = zone.sensitivity)
                    },
                    objectDetection = s.analytics.objectDetection,
                    objectTypes = s.analytics.objectTypes,
                    executionLocation = s.analytics.executionLocation.name
                ),
                notifications = NotificationSettingsDto(
                    enabled = s.notifications.enabled,
                    channels = s.notifications.channels,
                    events = s.notifications.events
                ),
                observation = ObservationSettingsDto(
                    zoneClass = s.observation.zoneClass?.name,
                    targetStreamFps = s.observation.targetStreamFps,
                    sceneWidthMeters = s.observation.sceneWidthMeters,
                    observationDistanceMeters = s.observation.observationDistanceMeters,
                    pixelsPerMeterOverride = s.observation.pixelsPerMeterOverride,
                    targetPixelsPerMeterMin = s.observation.targetPixelsPerMeterMin
                )
            )
        },
        statistics = this.statistics?.let {
            CameraStatisticsDto(
                uptime = it.uptime,
                recordedHours = it.recordedHours,
                eventsCount = it.eventsCount,
                storageUsed = it.storageUsed
            )
        },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastSeen = this.lastSeen
    )
}

private fun CameraSettingsDto.toDomainSettings(): com.company.ipcamera.shared.domain.model.CameraSettings {
    return com.company.ipcamera.shared.domain.model.CameraSettings(
        recording = this.recording?.toDomainRecording() ?: com.company.ipcamera.shared.domain.model.RecordingSettings(),
        analytics = this.analytics?.toDomainAnalytics() ?: com.company.ipcamera.shared.domain.model.AnalyticsSettings(),
        notifications = this.notifications?.toDomainNotifications() ?: com.company.ipcamera.shared.domain.model.NotificationSettings(),
        observation = this.observation?.toDomainObservation() ?: com.company.ipcamera.shared.domain.model.ObservationSettings()
    )
}

fun CreateCameraRequest.toDomain(): Camera {
    return Camera(
        id = UUID.randomUUID().toString(),
        name = this.name,
        url = this.url,
        username = this.username,
        password = this.password,
        model = this.model,
        status = com.company.ipcamera.core.common.model.CameraStatus.UNKNOWN,
        resolution = this.resolution?.let { com.company.ipcamera.core.common.model.Resolution(it.width, it.height) },
        fps = this.fps,
        bitrate = this.bitrate,
        codec = this.codec,
        audio = this.audio,
        ptz = this.toDomainPtz(),
        streams = this.streams.map { it.toDomainStream() },
        settings = this.settings?.toDomainSettings() ?: com.company.ipcamera.shared.domain.model.CameraSettings(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

private fun CreateCameraRequest.toDomainPtz(): com.company.ipcamera.shared.domain.model.PTZConfig? {
    return this.ptz?.let {
        com.company.ipcamera.shared.domain.model.PTZConfig(
            enabled = it.enabled,
            type = com.company.ipcamera.shared.domain.model.PTZType.valueOf(it.type),
            presets = it.presets
        )
    }
}

private fun StreamConfigDto.toDomainStream(): com.company.ipcamera.shared.domain.model.StreamConfig {
    return com.company.ipcamera.shared.domain.model.StreamConfig(
        type = com.company.ipcamera.shared.domain.model.StreamType.valueOf(this.type),
        resolution = com.company.ipcamera.core.common.model.Resolution(this.resolution.width, this.resolution.height),
        fps = this.fps,
        bitrate = this.bitrate
    )
}

fun DiscoveredCamera.toDto(): DiscoveredCameraDto {
    return DiscoveredCameraDto(
        name = this.name,
        url = this.url,
        model = this.model,
        manufacturer = this.manufacturer,
        ipAddress = this.ipAddress,
        port = this.port,
        username = this.username,
        password = this.password
    )
}

private fun RecordingSettingsDto.toDomainRecording(): com.company.ipcamera.shared.domain.model.RecordingSettings {
    return com.company.ipcamera.shared.domain.model.RecordingSettings(
        enabled = this.enabled ?: true,
        mode = this.mode?.let { com.company.ipcamera.shared.domain.model.RecordingMode.valueOf(it) } ?: com.company.ipcamera.shared.domain.model.RecordingMode.CONTINUOUS,
        quality = this.quality?.let { com.company.ipcamera.shared.domain.model.Quality.valueOf(it) } ?: com.company.ipcamera.shared.domain.model.Quality.HIGH,
        schedule = this.schedule ?: "24/7"
    )
}

private fun AnalyticsSettingsDto.toDomainAnalytics(): com.company.ipcamera.shared.domain.model.AnalyticsSettings {
    return com.company.ipcamera.shared.domain.model.AnalyticsSettings(
        motionDetection = this.motionDetection ?: true,
        zones = this.zones?.map { zone ->
            com.company.ipcamera.shared.domain.model.DetectionZone(name = zone.name, polygon = zone.polygon, sensitivity = zone.sensitivity)
        } ?: emptyList(),
        objectDetection = this.objectDetection ?: false,
        objectTypes = this.objectTypes ?: emptyList(),
        executionLocation = this.executionLocation?.let { com.company.ipcamera.shared.domain.model.AnalyticsExecutionLocation.valueOf(it) } ?: com.company.ipcamera.shared.domain.model.AnalyticsExecutionLocation.ON_PREM
    )
}

private fun NotificationSettingsDto.toDomainNotifications(): com.company.ipcamera.shared.domain.model.NotificationSettings {
    return com.company.ipcamera.shared.domain.model.NotificationSettings(
        enabled = this.enabled ?: true,
        channels = this.channels ?: emptyList(),
        events = this.events ?: emptyList()
    )
}

private fun ObservationSettingsDto.toDomainObservation(): com.company.ipcamera.shared.domain.model.ObservationSettings {
    return com.company.ipcamera.shared.domain.model.ObservationSettings(
        zoneClass = this.zoneClass?.let { com.company.ipcamera.shared.domain.model.ObservationZoneClass.valueOf(it) },
        targetStreamFps = this.targetStreamFps,
        sceneWidthMeters = this.sceneWidthMeters,
        observationDistanceMeters = this.observationDistanceMeters,
        pixelsPerMeterOverride = this.pixelsPerMeterOverride,
        targetPixelsPerMeterMin = this.targetPixelsPerMeterMin
    )
}

fun ConnectionTestResult.toDto(): ConnectionTestResultDto {
    return when (this) {
        is ConnectionTestResult.Success -> ConnectionTestResultDto(
            success = true,
            streams = this.streams.map { it.toDto() },
            capabilities = this.capabilities.toDto()
        )
        is ConnectionTestResult.Failure -> ConnectionTestResultDto(
            success = false,
            error = this.error,
            errorCode = this.code.name
        )
    }
}

fun StreamInfo.toDto(): StreamInfoDto {
    return StreamInfoDto(
        type = this.type,
        resolution = this.resolution,
        fps = this.fps,
        codec = this.codec
    )
}

fun CameraCapabilities.toDto(): CameraCapabilitiesDto {
    return CameraCapabilitiesDto(
        ptz = this.ptz,
        audio = this.audio,
        onvif = this.onvif,
        analytics = this.analytics
    )
}

@Serializable
data class StreamStatusDto(
    val active: Boolean,
    val streamId: String?,
    val hlsUrl: String?,
    val rtspUrl: String?,
    val rtspDiagnostics: RtspRuntimeDiagnosticsDto? = null
)

@Serializable
data class RtspRuntimeDiagnosticsDto(
    val connectAttempts: Long,
    val connectSuccesses: Long,
    val connectFailures: Long,
    val reconnectAttempts: Long,
    val reconnectSuccesses: Long,
    val reconnectFailures: Long,
    val consecutiveFailures: Long,
    val lastError: String? = null,
    val lastErrorAt: Long? = null,
    val lastConnectedAt: Long? = null,
    val lastDisconnectedAt: Long? = null,
    val lastPlayingAt: Long? = null,
    val lastFrameAt: Long? = null
)

@Serializable
data class RtspStreamUrlDto(
    val rtspUrl: String
)

/**
 * DTO для запроса массового удаления камер
 */
@Serializable
data class BulkDeleteCamerasRequest(
    val ids: List<String> // Список ID камер для удаления
)

/**
 * DTO для ответа на массовое удаление камер
 */
@Serializable
data class BulkDeleteCamerasResponse(
    val deletedCount: Int,
    val failedCount: Int,
    val failedIds: List<String> = emptyList()
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

/**
 * DTO для пагинированного ответа
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)

/**
 * DTO для запроса изменения качества потока
 */
@Serializable
data class SetStreamQualityRequest(
    val quality: String // low, medium, high, ultra
)

/**
 * DTO для WebRTC offer запроса
 */
@Serializable
data class WebRtcOfferRequest(
    val offer: WebRtcOfferDto
)

@Serializable
data class WebRtcOfferDto(
    val type: String,
    val sdp: String
)



