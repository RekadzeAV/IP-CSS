package com.company.ipcamera.shared.data.datasource.remote.impl

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.CameraApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.remote.CameraRemoteDataSource
import com.company.ipcamera.shared.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraRemoteDataSource с использованием CameraApiService
 */
class CameraRemoteDataSourceImpl(
    private val cameraApiService: CameraApiService
) : CameraRemoteDataSource {

    override suspend fun getCameras(page: Int, limit: Int, status: String?): ApiResult<List<Camera>> = withContext(Dispatchers.IO) {
        try {
            val result = cameraApiService.getCameras(page, limit, status)
            result.map { paginatedResponse ->
                paginatedResponse.items.map { it.toDomain() }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting cameras from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getCameraById(id: String): ApiResult<Camera> = withContext(Dispatchers.IO) {
        try {
            val result = cameraApiService.getCameraById(id)
            result.map { it.toDomain() }
        } catch (e: Exception) {
            logger.error(e) { "Error getting camera by id from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun createCamera(camera: Camera): ApiResult<Camera> = withContext(Dispatchers.IO) {
        try {
            val request = camera.toCreateRequest()
            val result = cameraApiService.createCamera(request)
            result.map { it.toDomain() }
        } catch (e: Exception) {
            logger.error(e) { "Error creating camera on remote API: ${camera.id}" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun updateCamera(id: String, camera: Camera): ApiResult<Camera> = withContext(Dispatchers.IO) {
        try {
            val request = camera.toUpdateRequest()
            val result = cameraApiService.updateCamera(id, request)
            result.map { it.toDomain() }
        } catch (e: Exception) {
            logger.error(e) { "Error updating camera on remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun deleteCamera(id: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = cameraApiService.deleteCamera(id)
            result.map { Unit }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting camera from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun testConnection(id: String): ApiResult<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val result = cameraApiService.testConnection(id)
            result.map { apiResponse ->
                apiResponse.data ?: emptyMap()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error testing camera connection on remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getCameraStatus(id: String): ApiResult<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val result = cameraApiService.getCameraStatus(id)
            result.map { apiResponse ->
                apiResponse.data ?: emptyMap()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting camera status from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    /**
     * Маппинг CameraResponse в Domain модель Camera
     */
    private fun CameraResponse.toDomain(): Camera {
        return Camera(
            id = id,
            name = name,
            url = url,
            username = username,
            password = null, // Пароли не возвращаются из API
            model = model,
            status = CameraStatus.valueOf(status),
            resolution = resolution?.let { Resolution(it.width, it.height) },
            fps = fps,
            bitrate = bitrate,
            codec = codec,
            audio = audio,
            ptz = ptz?.toDomain(),
            streams = streams.map { it.toDomain() },
            settings = settings?.toDomain() ?: CameraSettings(),
            statistics = statistics?.toDomain(),
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastSeen = lastSeen
        )
    }

    /**
     * Маппинг Domain модели Camera в CreateCameraRequest
     */
    private fun Camera.toCreateRequest(): CreateCameraRequest {
        return CreateCameraRequest(
            name = name,
            url = url,
            username = username,
            password = password,
            model = model,
            resolution = resolution?.let { ResolutionDto(it.width, it.height) },
            fps = fps,
            bitrate = bitrate,
            codec = codec,
            audio = audio,
            ptz = ptz?.toDto(),
            streams = streams.map { it.toDto() },
            settings = settings.toDto()
        )
    }

    /**
     * Маппинг Domain модели Camera в UpdateCameraRequest
     */
    private fun Camera.toUpdateRequest(): UpdateCameraRequest {
        return UpdateCameraRequest(
            name = name,
            url = url,
            username = username,
            password = password,
            model = model,
            resolution = resolution?.let { ResolutionDto(it.width, it.height) },
            fps = fps,
            bitrate = bitrate,
            codec = codec,
            audio = audio,
            ptz = ptz?.toDto(),
            streams = streams.map { it.toDto() },
            settings = settings.toDto()
        )
    }

    // Вспомогательные функции маппинга
    private fun PTZConfigDto.toDomain(): PTZConfig {
        return PTZConfig(
            enabled = enabled,
            type = PTZType.valueOf(type),
            presets = presets
        )
    }

    private fun PTZConfig.toDto(): PTZConfigDto {
        return PTZConfigDto(
            enabled = enabled,
            type = type.name,
            presets = presets
        )
    }

    private fun StreamConfigDto.toDomain(): StreamConfig {
        return StreamConfig(
            type = StreamType.valueOf(type),
            resolution = Resolution(resolution.width, resolution.height),
            fps = fps,
            bitrate = bitrate
        )
    }

    private fun StreamConfig.toDto(): StreamConfigDto {
        return StreamConfigDto(
            type = type.name,
            resolution = ResolutionDto(resolution.width, resolution.height),
            fps = fps,
            bitrate = bitrate
        )
    }

    private fun CameraSettingsDto.toDomain(): CameraSettings {
        return CameraSettings(
            recording = recording?.toDomain() ?: RecordingSettings(),
            analytics = analytics?.toDomain() ?: AnalyticsSettings(),
            notifications = notifications?.toDomain() ?: NotificationSettings()
        )
    }

    private fun CameraSettings.toDto(): CameraSettingsDto {
        return CameraSettingsDto(
            recording = recording.toDto(),
            analytics = analytics.toDto(),
            notifications = notifications.toDto()
        )
    }

    private fun RecordingSettingsDto.toDomain(): RecordingSettings {
        return RecordingSettings(
            enabled = enabled,
            mode = RecordingMode.valueOf(mode),
            quality = Quality.valueOf(quality),
            schedule = schedule
        )
    }

    private fun RecordingSettings.toDto(): RecordingSettingsDto {
        return RecordingSettingsDto(
            enabled = enabled,
            mode = mode.name,
            quality = quality.name,
            schedule = schedule
        )
    }

    private fun AnalyticsSettingsDto.toDomain(): AnalyticsSettings {
        return AnalyticsSettings(
            motionDetection = motionDetection,
            zones = zones.map { it.toDomain() },
            objectDetection = objectDetection,
            objectTypes = objectTypes
        )
    }

    private fun AnalyticsSettings.toDto(): AnalyticsSettingsDto {
        return AnalyticsSettingsDto(
            motionDetection = motionDetection,
            zones = zones.map { it.toDto() },
            objectDetection = objectDetection,
            objectTypes = objectTypes
        )
    }

    private fun DetectionZoneDto.toDomain(): DetectionZone {
        return DetectionZone(
            name = name,
            polygon = polygon,
            sensitivity = sensitivity
        )
    }

    private fun DetectionZone.toDto(): DetectionZoneDto {
        return DetectionZoneDto(
            name = name,
            polygon = polygon,
            sensitivity = sensitivity
        )
    }

    private fun NotificationSettingsDto.toDomain(): NotificationSettings {
        return NotificationSettings(
            enabled = enabled,
            channels = channels,
            events = events
        )
    }

    private fun NotificationSettings.toDto(): NotificationSettingsDto {
        return NotificationSettingsDto(
            enabled = enabled,
            channels = channels,
            events = events
        )
    }

    private fun CameraStatisticsDto.toDomain(): CameraStatistics {
        return CameraStatistics(
            uptime = uptime,
            recordedHours = recordedHours,
            eventsCount = eventsCount,
            storageUsed = storageUsed
        )
    }
}

