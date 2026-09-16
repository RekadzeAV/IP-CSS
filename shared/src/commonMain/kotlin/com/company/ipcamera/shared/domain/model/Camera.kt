package com.company.ipcamera.shared.domain.model

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.common.nowMillis
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
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val lastSeen: Long? = null,
)

@Serializable
data class PTZConfig(
    val enabled: Boolean = false,
    val type: PTZType = PTZType.PTZ,
    val presets: List<String> = emptyList(),
)

@Serializable
enum class PTZType {
    PTZ,
    PT,
    FIXED,
}

@Serializable
data class StreamConfig(
    val type: StreamType,
    val resolution: Resolution,
    val fps: Int,
    val bitrate: Int,
)

@Serializable
enum class StreamType {
    MAIN,
    SUB,
    AUDIO,
    METADATA,
}

@Serializable
data class CameraSettings(
    val recording: RecordingSettings = RecordingSettings(),
    val analytics: AnalyticsSettings = AnalyticsSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val observation: ObservationSettings = ObservationSettings(),
)

@Serializable
data class RecordingSettings(
    val enabled: Boolean = true,
    val mode: RecordingMode = RecordingMode.CONTINUOUS,
    val quality: Quality = Quality.HIGH,
    val schedule: String = "24/7",
)

@Serializable
enum class RecordingMode {
    CONTINUOUS,
    SCHEDULED,
    EVENT,
    MANUAL,
}

@Serializable
enum class Quality {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA,
}

@Serializable
data class AnalyticsSettings(
    val motionDetection: Boolean = true,
    /** Порог чувствительности детекции движения (0.0–1.0). Выше — меньше ложных срабатываний. */
    val motionThreshold: Float = 0.5f,
    /** Минимальная площадь области движения в пикселях. Если null — вычисляется от размера кадра (0.1%). */
    val motionMinArea: Int? = null,
    /** Минимальный интервал между событиями движения (мс), чтобы не дублировать события. */
    val motionEventCooldownMs: Long = 5000L,
    val zones: List<DetectionZone> = emptyList(),
    val objectDetection: Boolean = false,
    /** Типы объектов для детекции (person, vehicle, bicycle, motorcycle). Пусто = все типы. */
    val objectTypes: List<String> = emptyList(),
    val objectDetectionModelPath: String? = "data/models/yolov8n.onnx",
    val objectDetectionConfidenceThreshold: Float = 0.5f,
    val objectDetectionMaxObjects: Int = 20,
    val objectDetectionUseGPU: Boolean = false,
    /** Размер входа модели (ширина/высота, например 640 для YOLO). */
    val objectDetectionInputSize: Int = 640,
    // ANPR (распознавание номерных знаков)
    val anprEnabled: Boolean = false,
    val anprConfidenceThreshold: Float = 0.7f,
    /** Код языка для OCR (например "eng", "rus"). null = eng по умолчанию. */
    val anprLanguage: String? = null,
    /** Режим списка номеров: ALLOWLIST (только разрешённые) или BLOCKLIST (только заблокированные). null = оба списка отключены. */
    val anprListMode: AnprListMode? = null,
    /** Разрешённые номера (белый список). Пусто = все разрешены. */
    val anprAllowList: List<String> = emptyList(),
    /** Заблокированные номера (чёрный список). Пусто = все разрешены. */
    val anprBlockList: List<String> = emptyList(),
    /** Включить уведомление при обнаружении номера из чёрного списка. */
    val anprBlockListNotificationEnabled: Boolean = true,
    // Face detection / recognition (детекция лиц в потоке)
    val faceRecognition: Boolean = false,
    /** Путь к каскаду Haar/OpenCV для детекции лиц (XML). Если null — используется встроенный/дефолтный. */
    val faceDetectionCascadePath: String? = null,
    val faceRecognitionModelPath: String? = "data/models/face-recognition/insightface.onnx",
    val faceRecognitionConfidenceThreshold: Float = 0.7f,
    // Behavior Analysis
    val behaviorAnalysis: Boolean = false,
    val behaviorAnalysisModelPath: String? = "data/models/behavior-analysis/yolov8n-pose.onnx",
    val fallDetectionEnabled: Boolean = false,
    val fallDetectionSensitivity: Float = 0.6f,
    // Crowd Density
    val crowdDensityAnalysis: Boolean = false,
    val crowdDensityModelPath: String? = "data/models/crowd-analysis/csrnet.onnx",
    val useYOLOForCounting: Boolean = true, // Использовать YOLOv8 для подсчета вместо CSRNet
    val maxCrowdDensity: Int? = null, // Максимальная плотность для предупреждений (люди/м²)
    val executionLocation: AnalyticsExecutionLocation = AnalyticsExecutionLocation.ON_PREM,
)

@Serializable
data class DetectionZone(
    val name: String,
    val polygon: List<List<Int>>,
    val sensitivity: Int = 80,
)

@Serializable
data class NotificationSettings(
    val enabled: Boolean = true,
    val channels: List<String> = emptyList(),
    val events: List<String> = emptyList(),
)

@Serializable
data class CameraStatistics(
    val uptime: Double = 0.0,
    val recordedHours: Long = 0,
    val eventsCount: Long = 0,
    val storageUsed: Long = 0,
)
