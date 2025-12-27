package com.company.ipcamera.server.dto

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.*
import kotlinx.serialization.Serializable

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
    val status: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateCameraRequest(
    val name: String,
    val url: String,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class UpdateCameraRequest(
    val name: String? = null,
    val url: String? = null,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class DiscoveredCameraDto(
    val name: String,
    val url: String,
    val model: String? = null,
    val manufacturer: String? = null,
    val ipAddress: String,
    val port: Int = 554
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
        status = this.status.name,
        createdAt = this.createdAt.toEpochMilliseconds(),
        updatedAt = this.updatedAt.toEpochMilliseconds()
    )
}

fun CreateCameraRequest.toDomain(): Camera {
    return Camera(
        id = "", // Будет сгенерирован в репозитории
        name = this.name,
        url = this.url,
        username = this.username,
        password = this.password,
        status = com.company.ipcamera.core.common.model.CameraStatus.UNKNOWN,
        createdAt = kotlinx.datetime.Clock.System.now(),
        updatedAt = kotlinx.datetime.Clock.System.now()
    )
}

fun DiscoveredCamera.toDto(): DiscoveredCameraDto {
    return DiscoveredCameraDto(
        name = this.name,
        url = this.url,
        model = this.model,
        manufacturer = this.manufacturer,
        ipAddress = this.ipAddress,
        port = this.port
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
    val rtspUrl: String?
)

@Serializable
data class RtspStreamUrlDto(
    val rtspUrl: String
)


