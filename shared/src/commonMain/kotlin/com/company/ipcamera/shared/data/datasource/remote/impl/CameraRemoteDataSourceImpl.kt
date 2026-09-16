package com.company.ipcamera.shared.data.datasource.remote.impl

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.CameraApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.remote.CameraRemoteDataSource
import com.company.ipcamera.shared.domain.model.*
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraRemoteDataSource с использованием CameraApiService
 */
class CameraRemoteDataSourceImpl(
    private val cameraApiService: CameraApiService,
) : CameraRemoteDataSource {
    override suspend fun getCameras(
        page: Int,
        limit: Int,
        status: String?,
    ): ApiResult<List<Camera>> =
        withContext(
            Dispatchers.IO,
        ) {
            try {
                cameraApiService.getCameras(page, limit, status).fold(
                    onSuccess = { paginated -> ApiResult.Success(paginated.items.map { it.toDomain() }) },
                    onError = { ApiResult.Error(it) },
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting cameras from remote API" }
                ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
            }
        }

    override suspend fun getCameraById(id: String): ApiResult<Camera> =
        withContext(Dispatchers.IO) {
            try {
                cameraApiService.getCameraById(id).fold(
                    onSuccess = { ApiResult.Success(it.toDomain()) },
                    onError = { ApiResult.Error(it) },
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting camera by id from remote API: $id" }
                ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
            }
        }

    override suspend fun createCamera(camera: Camera): ApiResult<Camera> =
        withContext(Dispatchers.IO) {
            try {
                val request = camera.toCreateRequest()
                cameraApiService.createCamera(request).fold(
                    onSuccess = { ApiResult.Success(it.toDomain()) },
                    onError = { ApiResult.Error(it) },
                )
            } catch (e: Exception) {
                logger.error(e) { "Error creating camera on remote API: ${camera.id}" }
                ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
            }
        }

    override suspend fun updateCamera(
        id: String,
        camera: Camera,
    ): ApiResult<Camera> =
        withContext(Dispatchers.IO) {
            try {
                val request = camera.toUpdateRequest()
                cameraApiService.updateCamera(id, request).fold(
                    onSuccess = { ApiResult.Success(it.toDomain()) },
                    onError = { ApiResult.Error(it) },
                )
            } catch (e: Exception) {
                logger.error(e) { "Error updating camera on remote API: $id" }
                ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
            }
        }

    override suspend fun deleteCamera(id: String): ApiResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                cameraApiService.deleteCamera(id).fold(
                    onSuccess = { ApiResult.Success(Unit) },
                    onError = { ApiResult.Error(it) },
                )
            } catch (e: Exception) {
                logger.error(e) { "Error deleting camera from remote API: $id" }
                ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
            }
        }

    override suspend fun testConnection(id: String): ApiResult<Map<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                cameraApiService.testConnection(id).fold(
                    onSuccess = { apiResponse -> ApiResult.Success(apiResponse.data ?: emptyMap()) },
                    onError = { ApiResult.Error(it) },
                )
            } catch (e: Exception) {
                logger.error(e) { "Error testing camera connection on remote API: $id" }
                ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
            }
        }

    override suspend fun getCameraStatus(id: String): ApiResult<Map<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                cameraApiService.getCameraStatus(id).fold(
                    onSuccess = { apiResponse -> ApiResult.Success(apiResponse.data ?: emptyMap()) },
                    onError = { ApiResult.Error(it) },
                )
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
            lastSeen = lastSeen,
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
            settings = settings.toDto(),
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
            settings = settings.toDto(),
        )
    }

    // Вспомогательные функции маппинга
    private fun PTZConfigDto.toDomain(): PTZConfig {
        return PTZConfig(
            enabled = enabled,
            type = PTZType.valueOf(type),
            presets = presets,
        )
    }

    private fun PTZConfig.toDto(): PTZConfigDto {
        return PTZConfigDto(
            enabled = enabled,
            type = type.name,
            presets = presets,
        )
    }

    private fun StreamConfigDto.toDomain(): StreamConfig {
        return StreamConfig(
            type = StreamType.valueOf(type),
            resolution = Resolution(resolution.width, resolution.height),
            fps = fps,
            bitrate = bitrate,
        )
    }

    private fun StreamConfig.toDto(): StreamConfigDto {
        return StreamConfigDto(
            type = type.name,
            resolution = ResolutionDto(resolution.width, resolution.height),
            fps = fps,
            bitrate = bitrate,
        )
    }

    private fun CameraSettingsDto.toDomain(): CameraSettings {
        return CameraSettings(
            recording = recording?.toDomain() ?: RecordingSettings(),
            analytics = analytics?.toDomain() ?: AnalyticsSettings(),
            notifications = notifications?.toDomain() ?: NotificationSettings(),
            observation = observation?.toDomain() ?: ObservationSettings(),
        )
    }

    private fun CameraSettings.toDto(): CameraSettingsDto {
        return CameraSettingsDto(
            recording = recording.toDto(),
            analytics = analytics.toDto(),
            notifications = notifications.toDto(),
            observation = observation.toDto(),
        )
    }

    private fun ObservationSettingsDto.toDomain(): ObservationSettings {
        val zc = zoneClass?.let { runCatching { ObservationZoneClass.valueOf(it) }.getOrNull() }
        return ObservationSettings(
            zoneClass = zc,
            targetStreamFps = targetStreamFps,
            sceneWidthMeters = sceneWidthMeters,
            observationDistanceMeters = observationDistanceMeters,
            pixelsPerMeterOverride = pixelsPerMeterOverride,
            targetPixelsPerMeterMin = targetPixelsPerMeterMin,
        )
    }

    private fun ObservationSettings.toDto(): ObservationSettingsDto {
        return ObservationSettingsDto(
            zoneClass = zoneClass?.name,
            targetStreamFps = targetStreamFps,
            sceneWidthMeters = sceneWidthMeters,
            observationDistanceMeters = observationDistanceMeters,
            pixelsPerMeterOverride = pixelsPerMeterOverride,
            targetPixelsPerMeterMin = targetPixelsPerMeterMin,
        )
    }

    private fun RecordingSettingsDto.toDomain(): RecordingSettings {
        return RecordingSettings(
            enabled = enabled,
            mode = RecordingMode.valueOf(mode),
            quality = Quality.valueOf(quality),
            schedule = schedule,
        )
    }

    private fun RecordingSettings.toDto(): RecordingSettingsDto {
        return RecordingSettingsDto(
            enabled = enabled,
            mode = mode.name,
            quality = quality.name,
            schedule = schedule,
        )
    }

    private fun AnalyticsSettingsDto.toDomain(): AnalyticsSettings {
        val loc =
            executionLocation?.let { runCatching { AnalyticsExecutionLocation.valueOf(it) }.getOrNull() }
                ?: AnalyticsExecutionLocation.ON_PREM
        return AnalyticsSettings(
            motionDetection = motionDetection,
            zones = zones.map { it.toDomain() },
            objectDetection = objectDetection,
            objectTypes = objectTypes,
            executionLocation = loc,
        )
    }

    private fun AnalyticsSettings.toDto(): AnalyticsSettingsDto {
        return AnalyticsSettingsDto(
            motionDetection = motionDetection,
            zones = zones.map { it.toDto() },
            objectDetection = objectDetection,
            objectTypes = objectTypes,
            executionLocation = executionLocation.name,
        )
    }

    private fun DetectionZoneDto.toDomain(): DetectionZone {
        return DetectionZone(
            name = name,
            polygon = polygon,
            sensitivity = sensitivity,
        )
    }

    private fun DetectionZone.toDto(): DetectionZoneDto {
        return DetectionZoneDto(
            name = name,
            polygon = polygon,
            sensitivity = sensitivity,
        )
    }

    private fun NotificationSettingsDto.toDomain(): NotificationSettings {
        return NotificationSettings(
            enabled = enabled,
            channels = channels,
            events = events,
        )
    }

    private fun NotificationSettings.toDto(): NotificationSettingsDto {
        return NotificationSettingsDto(
            enabled = enabled,
            channels = channels,
            events = events,
        )
    }

    private fun CameraStatisticsDto.toDomain(): CameraStatistics {
        return CameraStatistics(
            uptime = uptime,
            recordedHours = recordedHours,
            eventsCount = eventsCount,
            storageUsed = storageUsed,
        )
    }
}
