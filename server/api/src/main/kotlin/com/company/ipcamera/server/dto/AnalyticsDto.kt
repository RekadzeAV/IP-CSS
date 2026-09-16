package com.company.ipcamera.server.dto

import com.company.ipcamera.core.network.onvif.*
import com.company.ipcamera.shared.domain.model.AnalyticsExecutionLocation
import com.company.ipcamera.shared.domain.model.AnalyticsSettings
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.service.*
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * DTO для конфигурации аналитики (полный набор для REST API)
 */
@Serializable
data class AnalyticsConfigDto(
    val motionDetection: Boolean = true,
    val motionThreshold: Float = 0.5f,
    val motionMinArea: Int? = null,
    val motionEventCooldownMs: Long = 5000L,
    val zones: List<DetectionZoneDto> = emptyList(),
    val objectDetection: Boolean = false,
    val objectTypes: List<String> = emptyList(),
    val objectDetectionModelPath: String? = null,
    val objectDetectionConfidenceThreshold: Float = 0.5f,
    val objectDetectionMaxObjects: Int = 20,
    val objectDetectionUseGPU: Boolean = false,
    val objectDetectionInputSize: Int = 640,
    val anprEnabled: Boolean = false,
    val anprConfidenceThreshold: Float = 0.7f,
    val anprLanguage: String? = null,
    val anprListMode: String? = null,  // ALLOWLIST | BLOCKLIST
    val anprAllowList: List<String> = emptyList(),
    val anprBlockList: List<String> = emptyList(),
    val anprBlockListNotificationEnabled: Boolean = true,
    val faceRecognition: Boolean = false,
    val faceDetectionCascadePath: String? = null,
    val faceRecognitionModelPath: String? = null,
    val faceRecognitionConfidenceThreshold: Float = 0.7f,
    /** ON_PREM | CUSTOMER_SERVER | VSAAS */
    val executionLocation: String? = null
)

/**
 * DTO для зоны детекции
 */
@Serializable
data class DetectionZoneDto(
    val name: String,
    val polygon: List<List<Int>>,
    val sensitivity: Int = 80
)

/**
 * DTO для результата аналитики
 */
@Serializable
data class AnalyticsResultDto(
    val cameraId: String,
    val timestamp: Long,
    val motionDetection: MotionDetectionResultDto? = null,
    val objectDetection: ObjectDetectionResultDto? = null,
    val faceDetection: FaceDetectionResultDto? = null,
    val licensePlateRecognition: LicensePlateRecognitionResultDto? = null
)

/**
 * DTO для результата детекции движения
 */
@Serializable
data class MotionDetectionResultDto(
    val detected: Boolean,
    val confidence: Float,
    val zones: List<MotionZoneDto> = emptyList(),
    val timestamp: Long
)

/**
 * DTO для зоны с движением
 */
@Serializable
data class MotionZoneDto(
    val zone: DetectionZoneDto,
    val intensity: Float
)

/**
 * DTO для результата детекции объектов
 */
@Serializable
data class ObjectDetectionResultDto(
    val objects: List<DetectedObjectDto>,
    val timestamp: Long
)

/**
 * DTO для обнаруженного объекта
 */
@Serializable
data class DetectedObjectDto(
    val type: String,
    val confidence: Float,
    val boundingBox: BoundingBoxDto,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * DTO для ограничивающего прямоугольника
 */
@Serializable
data class BoundingBoxDto(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

/**
 * DTO для результата детекции лиц
 */
@Serializable
data class FaceDetectionResultDto(
    val faces: List<DetectedFaceDto>,
    val timestamp: Long
)

/**
 * DTO для обнаруженного лица
 */
@Serializable
data class DetectedFaceDto(
    val boundingBox: BoundingBoxDto,
    val confidence: Float,
    val landmarks: List<FaceLandmarkDto>? = null,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * DTO для точки на лице
 */
@Serializable
data class FaceLandmarkDto(
    val x: Int,
    val y: Int,
    val type: String
)

/**
 * DTO для результата распознавания номерных знаков
 */
@Serializable
data class LicensePlateRecognitionResultDto(
    val plates: List<RecognizedLicensePlateDto>,
    val timestamp: Long
)

/**
 * DTO для распознанного номерного знака
 */
@Serializable
data class RecognizedLicensePlateDto(
    val plateNumber: String,
    val confidence: Float,
    val country: String? = null,
    val boundingBox: BoundingBoxDto,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * DTO для записи распознанного номера из БД (ANPR).
 */
@Serializable
data class StoredLicensePlateDto(
    val id: String,
    val cameraId: String,
    val timestamp: Long,
    val plateNumber: String,
    val confidence: Float,
    val country: String?,
    val bboxX: Int,
    val bboxY: Int,
    val bboxWidth: Int,
    val bboxHeight: Int,
    val createdAt: Long
)

/**
 * DTO для сохраненного лица (Face Gallery, блок 8.3).
 * embedding представлен как List<Float> для корректной сериализации.
 */
@Serializable
data class StoredFaceDto(
    val id: String,
    val label: String,
    val embedding: List<Float>,
    val cameraId: String? = null,
    val createdAt: Long,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class CreateFaceRequestDto(
    val id: String? = null,
    val label: String,
    val embedding: List<Float>,
    val cameraId: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class FaceSearchRequestDto(
    val embedding: List<Float>,
    val topK: Int = 10,
    val minSimilarity: Float = 0.7f
)

@Serializable
data class FaceSearchMatchDto(
    val face: StoredFaceDto,
    val similarity: Float
)

/**
 * Точка траектории (позиция объекта в момент времени)
 */
@Serializable
data class TrajectoryPointDto(
    val timestamp: Long,
    val x: Int,
    val y: Int
)

/**
 * DTO для одного трека (объект с стабильным ID между кадрами).
 * @param trajectory История позиций (центр bbox) для отображения траектории; опционально.
 */
@Serializable
data class TrackInfoDto(
    val trackId: Int,
    val objectType: String,
    val boundingBox: BoundingBoxDto,
    val confidence: Float,
    val lastSeen: Long,
    val trajectory: List<TrajectoryPointDto> = emptyList()
)

fun com.company.ipcamera.shared.domain.service.TrackInfo.toDto(trajectory: List<TrajectoryPointDto> = emptyList()): TrackInfoDto {
    return TrackInfoDto(
        trackId = trackId,
        objectType = objectType,
        boundingBox = boundingBox.toDto(),
        confidence = confidence,
        lastSeen = lastSeen,
        trajectory = trajectory
    )
}

/**
 * DTO для статистики аналитики
 */
@Serializable
data class AnalyticsStatsDto(
    val cameraId: String,
    val frameCount: Long,
    val isActive: Boolean,
    val motionDetectionsCount: Long = 0,
    val objectDetectionsCount: Long = 0,
    val faceDetectionsCount: Long = 0,
    val licensePlateRecognitionsCount: Long = 0,
    val lastProcessedTimestamp: Long? = null
)

@Serializable
data class AnalyticsStatusDto(
    val cameraId: String,
    val isRunning: Boolean,
    val activeDetectors: List<String>
)

@Serializable
data class AnalyticsMetricsDto(
    val cameraId: String,
    val processedFrames: Long,
    val skippedFrames: Long,
    /** См. [com.company.ipcamera.server.service.analytics.AnalyticsFrameSourceKind] */
    val frameSource: String? = null,
    val lastError: String? = null,
    val lastErrorAt: Long? = null
)

@Serializable
data class AnalyticsStartRequestDto(
    val restartStream: Boolean = false
)

@Serializable
data class AnalyticsStartResponseDto(
    val streamId: String,
    val analyticsStatus: AnalyticsStatusDto
)

@Serializable
data class AnalyticsStopRequestDto(
    val keepStreamAlive: Boolean = false
)

@Serializable
data class AnalyticsStopResponseDto(
    val cameraId: String,
    val streamActive: Boolean,
    val analyticsRunning: Boolean
)

@Serializable
data class WebhookDeliveryLogsClearResponseDto(
    val removed: Int,
    val ruleId: String? = null,
    val cameraId: String? = null,
    val success: Boolean? = null,
    val from: Long? = null,
    val to: Long? = null
)

@Serializable
data class PaginatedWebhookDeliveryLogsResponseDto(
    val items: List<com.company.ipcamera.server.service.WebhookDeliveryLogEntry>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

@Serializable
data class WebhookDeliveryStatsResponseDto(
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val successRatePercent: Double,
    val ruleId: String? = null,
    val cameraId: String? = null,
    val from: Long? = null,
    val to: Long? = null
)

@Serializable
data class WebhookDeliverySummaryEntryDto(
    val ruleId: String,
    val cameraId: String,
    val totalAttempts: Int,
    val successfulAttempts: Int,
    val failedAttempts: Int,
    val successRatePercent: Double,
    val lastAttemptTimestamp: Long? = null
)

/**
 * DTO для пагинированного ответа с результатами аналитики
 */
@Serializable
data class PaginatedAnalyticsResultResponse(
    val items: List<AnalyticsResultDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Extension функции для конвертации
 */
fun AnalyticsSettings.toDto(): AnalyticsConfigDto {
    return AnalyticsConfigDto(
        motionDetection = this.motionDetection,
        motionThreshold = this.motionThreshold,
        motionMinArea = this.motionMinArea,
        motionEventCooldownMs = this.motionEventCooldownMs,
        zones = this.zones.map { it.toDto() },
        objectDetection = this.objectDetection,
        objectTypes = this.objectTypes,
        objectDetectionModelPath = this.objectDetectionModelPath,
        objectDetectionConfidenceThreshold = this.objectDetectionConfidenceThreshold,
        objectDetectionMaxObjects = this.objectDetectionMaxObjects,
        objectDetectionUseGPU = this.objectDetectionUseGPU,
        objectDetectionInputSize = this.objectDetectionInputSize,
        anprEnabled = this.anprEnabled,
        anprConfidenceThreshold = this.anprConfidenceThreshold,
        anprLanguage = this.anprLanguage,
        anprListMode = this.anprListMode?.name,
        anprAllowList = this.anprAllowList,
        anprBlockList = this.anprBlockList,
        anprBlockListNotificationEnabled = this.anprBlockListNotificationEnabled,
        faceRecognition = this.faceRecognition,
        faceDetectionCascadePath = this.faceDetectionCascadePath,
        faceRecognitionModelPath = this.faceRecognitionModelPath,
        faceRecognitionConfidenceThreshold = this.faceRecognitionConfidenceThreshold,
        executionLocation = this.executionLocation.name
    )
}

fun AnalyticsConfigDto.toDomain(): AnalyticsSettings = toDomain(current = null)

/** Обновление конфига с сохранением полей, не передаваемых в API (merge с current). */
fun AnalyticsConfigDto.toDomain(current: AnalyticsSettings?): AnalyticsSettings {
    val c = current ?: AnalyticsSettings()
    val mergedExecutionLocation = when {
        executionLocation.isNullOrBlank() -> c.executionLocation
        else -> runCatching { AnalyticsExecutionLocation.valueOf(executionLocation!!) }
            .getOrElse { c.executionLocation }
    }
    return AnalyticsSettings(
        motionDetection = this.motionDetection,
        motionThreshold = this.motionThreshold,
        motionMinArea = this.motionMinArea,
        motionEventCooldownMs = this.motionEventCooldownMs,
        zones = this.zones.map { it.toDomain() },
        objectDetection = this.objectDetection,
        objectTypes = this.objectTypes,
        objectDetectionModelPath = this.objectDetectionModelPath,
        objectDetectionConfidenceThreshold = this.objectDetectionConfidenceThreshold,
        objectDetectionMaxObjects = this.objectDetectionMaxObjects,
        objectDetectionUseGPU = this.objectDetectionUseGPU,
        objectDetectionInputSize = this.objectDetectionInputSize,
        anprEnabled = this.anprEnabled,
        anprConfidenceThreshold = this.anprConfidenceThreshold,
        anprLanguage = this.anprLanguage,
        anprListMode = this.anprListMode?.let { runCatching { com.company.ipcamera.shared.domain.model.AnprListMode.valueOf(it) }.getOrNull() } ?: c.anprListMode,
        anprAllowList = this.anprAllowList.ifEmpty { c.anprAllowList },
        anprBlockList = this.anprBlockList.ifEmpty { c.anprBlockList },
        anprBlockListNotificationEnabled = if (this.anprBlockListNotificationEnabled) this.anprBlockListNotificationEnabled else c.anprBlockListNotificationEnabled,
        faceRecognition = this.faceRecognition,
        faceDetectionCascadePath = this.faceDetectionCascadePath,
        faceRecognitionModelPath = this.faceRecognitionModelPath ?: c.faceRecognitionModelPath,
        faceRecognitionConfidenceThreshold = this.faceRecognitionConfidenceThreshold,
        behaviorAnalysis = c.behaviorAnalysis,
        behaviorAnalysisModelPath = c.behaviorAnalysisModelPath,
        fallDetectionEnabled = c.fallDetectionEnabled,
        fallDetectionSensitivity = c.fallDetectionSensitivity,
        crowdDensityAnalysis = c.crowdDensityAnalysis,
        crowdDensityModelPath = c.crowdDensityModelPath,
        useYOLOForCounting = c.useYOLOForCounting,
        maxCrowdDensity = c.maxCrowdDensity,
        executionLocation = mergedExecutionLocation
    )
}

fun DetectionZone.toDto(): DetectionZoneDto {
    return DetectionZoneDto(
        name = this.name,
        polygon = this.polygon,
        sensitivity = this.sensitivity
    )
}

fun DetectionZoneDto.toDomain(): DetectionZone {
    return DetectionZone(
        name = this.name,
        polygon = this.polygon,
        sensitivity = this.sensitivity
    )
}

fun MotionDetectionResult.toDto(): MotionDetectionResultDto {
    return MotionDetectionResultDto(
        detected = this.detected,
        confidence = this.confidence,
        zones = this.zones.map { it.toDto() },
        timestamp = this.timestamp
    )
}

fun MotionZone.toDto(): MotionZoneDto {
    return MotionZoneDto(
        zone = this.zone.toDto(),
        intensity = this.intensity
    )
}

fun ObjectDetectionResult.toDto(): ObjectDetectionResultDto {
    return ObjectDetectionResultDto(
        objects = this.objects.map { it.toDto() },
        timestamp = this.timestamp
    )
}

fun com.company.ipcamera.shared.domain.model.DetectedObject.toDto(): DetectedObjectDto {
    return DetectedObjectDto(
        type = this.type,
        confidence = this.confidence,
        boundingBox = this.boundingBox.toDto(),
        attributes = this.attributes
    )
}

fun com.company.ipcamera.shared.domain.model.BoundingBox.toDto(): BoundingBoxDto {
    return BoundingBoxDto(
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height
    )
}

fun FaceDetectionResult.toDto(): FaceDetectionResultDto {
    return FaceDetectionResultDto(
        faces = this.faces.map { it.toDto() },
        timestamp = this.timestamp
    )
}

fun com.company.ipcamera.shared.domain.model.DetectedFace.toDto(): DetectedFaceDto {
    return DetectedFaceDto(
        boundingBox = this.boundingBox.toDto(),
        confidence = this.confidence,
        landmarks = this.landmarks?.map { it.toDto() },
        attributes = this.attributes
    )
}

fun com.company.ipcamera.shared.domain.model.FaceLandmark.toDto(): FaceLandmarkDto {
    return FaceLandmarkDto(
        x = this.x,
        y = this.y,
        type = this.type.name
    )
}

fun LicensePlateRecognitionResult.toDto(): LicensePlateRecognitionResultDto {
    return LicensePlateRecognitionResultDto(
        plates = this.plates.map { it.toDto() },
        timestamp = this.timestamp
    )
}

fun com.company.ipcamera.shared.domain.model.RecognizedLicensePlate.toDto(): RecognizedLicensePlateDto {
    return RecognizedLicensePlateDto(
        plateNumber = this.plateNumber,
        confidence = this.confidence,
        country = this.country,
        boundingBox = this.boundingBox.toDto(),
        attributes = this.attributes
    )
}

// ========== ONVIF Analytics Engines DTOs ==========

/**
 * DTO для ONVIF аналитического движка
 */
@Serializable
data class OnvifAnalyticsEngineDto(
    val token: String,
    val name: String? = null,
    val type: String? = null,
    val configuration: OnvifAnalyticsEngineConfigurationDto? = null,
    val status: String = "UNKNOWN"
)

/**
 * DTO для конфигурации ONVIF аналитического движка
 */
@Serializable
data class OnvifAnalyticsEngineConfigurationDto(
    val parameters: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
    val settings: Map<String, String> = emptyMap()
)

/**
 * DTO для входных данных ONVIF аналитического движка
 */
@Serializable
data class OnvifAnalyticsEngineInputDto(
    val token: String,
    val type: String? = null,
    val sourceToken: String? = null,
    val configuration: OnvifAnalyticsEngineInputConfigurationDto? = null
)

/**
 * DTO для конфигурации входных данных ONVIF аналитического движка
 */
@Serializable
data class OnvifAnalyticsEngineInputConfigurationDto(
    val parameters: Map<String, String> = emptyMap(),
    val enabled: Boolean = true
)

/**
 * DTO для запроса создания аналитического движка
 */
@Serializable
data class CreateAnalyticsEngineRequest(
    val name: String? = null,
    val type: String? = null,
    val configuration: OnvifAnalyticsEngineConfigurationDto
)

/**
 * DTO для запроса обновления аналитического движка
 */
@Serializable
data class UpdateAnalyticsEngineRequest(
    val configuration: OnvifAnalyticsEngineConfigurationDto
)

/**
 * DTO для запроса установки входных данных
 */
@Serializable
data class SetAnalyticsEngineInputRequest(
    val configuration: OnvifAnalyticsEngineInputConfigurationDto
)

// Extension функции для конвертации ONVIF типов в DTO

fun OnvifAnalyticsEngine.toDto(): OnvifAnalyticsEngineDto {
    return OnvifAnalyticsEngineDto(
        token = this.token,
        name = this.name,
        type = this.type,
        configuration = this.configuration?.toDto(),
        status = this.status.name
    )
}

fun OnvifAnalyticsEngineConfiguration.toDto(): OnvifAnalyticsEngineConfigurationDto {
    return OnvifAnalyticsEngineConfigurationDto(
        parameters = this.parameters,
        enabled = this.enabled,
        settings = this.settings.mapValues { it.value.toString() }
    )
}

fun OnvifAnalyticsEngineConfigurationDto.toDomain(): OnvifAnalyticsEngineConfiguration {
    return OnvifAnalyticsEngineConfiguration(
        parameters = this.parameters,
        enabled = this.enabled,
        settings = this.settings
    )
}

fun OnvifAnalyticsEngineInput.toDto(): OnvifAnalyticsEngineInputDto {
    return OnvifAnalyticsEngineInputDto(
        token = this.token,
        type = this.type,
        sourceToken = this.sourceToken,
        configuration = this.configuration?.toDto()
    )
}

fun OnvifAnalyticsEngineInputConfiguration.toDto(): OnvifAnalyticsEngineInputConfigurationDto {
    return OnvifAnalyticsEngineInputConfigurationDto(
        parameters = this.parameters,
        enabled = this.enabled
    )
}

fun OnvifAnalyticsEngineInputConfigurationDto.toDomain(): OnvifAnalyticsEngineInputConfiguration {
    return OnvifAnalyticsEngineInputConfiguration(
        parameters = this.parameters,
        enabled = this.enabled
    )
}

// ========== Analytics Rules DTOs ==========

/**
 * DTO для правила аналитики
 */
@Serializable
data class AnalyticsRuleDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val cameraId: String? = null,
    val analyticsType: String,
    val conditions: AnalyticsRuleConditionsDto,
    val actions: AnalyticsRuleActionsDto,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * DTO для условий правила
 */
@Serializable
data class AnalyticsRuleConditionsDto(
    val minConfidence: Float = 0.5f,
    val objectTypes: List<String> = emptyList(),
    val zones: List<String> = emptyList(),
    val timeWindow: String? = null,
    val daysOfWeek: List<Int> = emptyList(),
    val minObjectCount: Int = 1,
    val maxObjectCount: Int? = null,
    val additionalConditions: Map<String, String> = emptyMap()
)

/**
 * DTO для действий правила
 */
@Serializable
data class AnalyticsRuleActionsDto(
    val createEvent: Boolean = true,
    val eventType: String = "MOTION_DETECTION",
    val eventSeverity: String = "INFO",
    val sendNotification: Boolean = false,
    val notifyInApp: Boolean = true,
    val notifyEmail: Boolean = false,
    val notifyTelegram: Boolean = false,
    val notificationType: String? = null,
    val startRecording: Boolean = false,
    val recordingDuration: Long = 60L,
    val sendWebhook: Boolean = false,
    val webhookUrl: String? = null,
    val additionalActions: Map<String, String> = emptyMap()
)

/**
 * DTO для запроса создания правила
 */
@Serializable
data class CreateAnalyticsRuleRequest(
    val name: String,
    val description: String? = null,
    val cameraId: String? = null,
    val analyticsType: String,
    val conditions: AnalyticsRuleConditionsDto,
    val actions: AnalyticsRuleActionsDto,
    val enabled: Boolean = true,
    val priority: Int = 0
)

/**
 * DTO для запроса обновления правила
 */
@Serializable
data class UpdateAnalyticsRuleRequest(
    val name: String? = null,
    val description: String? = null,
    val cameraId: String? = null,
    val analyticsType: String? = null,
    val conditions: AnalyticsRuleConditionsDto? = null,
    val actions: AnalyticsRuleActionsDto? = null,
    val enabled: Boolean? = null,
    val priority: Int? = null
)

@Serializable
data class TestAnalyticsRuleNotificationRequestDto(
    val userId: String? = null,
    val cameraId: String? = null,
    val notifyInApp: Boolean? = null,
    val notifyEmail: Boolean? = null,
    val notifyTelegram: Boolean? = null
)

@Serializable
data class TestAnalyticsRuleNotificationResponseDto(
    val ruleId: String,
    val notificationId: String,
    val channels: List<String>
)

@Serializable
data class AnalyticsRuleNotificationPolicyDto(
    val ruleId: String,
    val sendNotification: Boolean,
    val notifyInApp: Boolean,
    val notifyEmail: Boolean,
    val notifyTelegram: Boolean,
    val notificationType: String? = null
)

@Serializable
data class UpdateAnalyticsRuleNotificationPolicyRequestDto(
    val sendNotification: Boolean? = null,
    val notifyInApp: Boolean? = null,
    val notifyEmail: Boolean? = null,
    val notifyTelegram: Boolean? = null,
    val notificationType: String? = null
)

// Extension функции для конвертации AnalyticsRule

fun CreateAnalyticsRuleRequest.toDomain(): com.company.ipcamera.shared.domain.model.AnalyticsRule {
    val now = System.currentTimeMillis()
    return com.company.ipcamera.shared.domain.model.AnalyticsRule(
        id = java.util.UUID.randomUUID().toString(),
        name = this.name,
        description = this.description,
        cameraId = this.cameraId,
        analyticsType = com.company.ipcamera.shared.domain.model.AnalyticsRuleType.valueOf(this.analyticsType),
        conditions = this.conditions.toDomain(),
        actions = this.actions.toDomain(),
        enabled = this.enabled,
        priority = this.priority,
        createdAt = now,
        updatedAt = now
    )
}

fun com.company.ipcamera.shared.domain.model.AnalyticsRule.toDto(): AnalyticsRuleDto {
    return AnalyticsRuleDto(
        id = this.id,
        name = this.name,
        description = this.description,
        cameraId = this.cameraId,
        analyticsType = this.analyticsType.name,
        conditions = this.conditions.toDto(),
        actions = this.actions.toDto(),
        enabled = this.enabled,
        priority = this.priority,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun AnalyticsRuleDto.toDomain(): com.company.ipcamera.shared.domain.model.AnalyticsRule {
    return com.company.ipcamera.shared.domain.model.AnalyticsRule(
        id = this.id,
        name = this.name,
        description = this.description,
        cameraId = this.cameraId,
        analyticsType = com.company.ipcamera.shared.domain.model.AnalyticsRuleType.valueOf(this.analyticsType),
        conditions = this.conditions.toDomain(),
        actions = this.actions.toDomain(),
        enabled = this.enabled,
        priority = this.priority,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

fun com.company.ipcamera.shared.domain.model.AnalyticsRuleConditions.toDto(): AnalyticsRuleConditionsDto {
    return AnalyticsRuleConditionsDto(
        minConfidence = this.minConfidence,
        objectTypes = this.objectTypes,
        zones = this.zones,
        timeWindow = this.timeWindow,
        daysOfWeek = this.daysOfWeek,
        minObjectCount = this.minObjectCount,
        maxObjectCount = this.maxObjectCount,
        additionalConditions = this.additionalConditions
    )
}

fun AnalyticsRuleConditionsDto.toDomain(): com.company.ipcamera.shared.domain.model.AnalyticsRuleConditions {
    return com.company.ipcamera.shared.domain.model.AnalyticsRuleConditions(
        minConfidence = this.minConfidence,
        objectTypes = this.objectTypes,
        zones = this.zones,
        timeWindow = this.timeWindow,
        daysOfWeek = this.daysOfWeek,
        minObjectCount = this.minObjectCount,
        maxObjectCount = this.maxObjectCount,
        additionalConditions = this.additionalConditions
    )
}

fun com.company.ipcamera.shared.domain.model.AnalyticsRuleActions.toDto(): AnalyticsRuleActionsDto {
    return AnalyticsRuleActionsDto(
        createEvent = this.createEvent,
        eventType = this.eventType.name,
        eventSeverity = this.eventSeverity.name,
        sendNotification = this.sendNotification,
        notifyInApp = this.notifyInApp,
        notifyEmail = this.notifyEmail,
        notifyTelegram = this.notifyTelegram,
        notificationType = this.notificationType?.name,
        startRecording = this.startRecording,
        recordingDuration = this.recordingDuration,
        sendWebhook = this.sendWebhook,
        webhookUrl = this.webhookUrl,
        additionalActions = this.additionalActions
    )
}

fun AnalyticsRuleActionsDto.toDomain(): com.company.ipcamera.shared.domain.model.AnalyticsRuleActions {
    return com.company.ipcamera.shared.domain.model.AnalyticsRuleActions(
        createEvent = this.createEvent,
        eventType = com.company.ipcamera.shared.domain.model.EventType.valueOf(this.eventType),
        eventSeverity = com.company.ipcamera.shared.domain.model.EventSeverity.valueOf(this.eventSeverity),
        sendNotification = this.sendNotification,
        notifyInApp = this.notifyInApp,
        notifyEmail = this.notifyEmail,
        notifyTelegram = this.notifyTelegram,
        notificationType = this.notificationType?.let { com.company.ipcamera.shared.domain.model.NotificationType.valueOf(it) },
        startRecording = this.startRecording,
        recordingDuration = this.recordingDuration,
        sendWebhook = this.sendWebhook,
        webhookUrl = this.webhookUrl,
        additionalActions = this.additionalActions
    )
}

fun com.company.ipcamera.server.service.AnalyticsRuleService.RuleNotificationPolicy.toDto(): AnalyticsRuleNotificationPolicyDto =
    AnalyticsRuleNotificationPolicyDto(
        ruleId = ruleId,
        sendNotification = sendNotification,
        notifyInApp = notifyInApp,
        notifyEmail = notifyEmail,
        notifyTelegram = notifyTelegram,
        notificationType = notificationType?.name
    )
