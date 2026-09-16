package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.BoundingBox
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedFace
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.model.RecognizedLicensePlate

/**
 * Интерфейс для сервиса аналитики
 * Определяет методы для детекции движения, объектов, лиц и распознавания номерных знаков
 */
interface AnalyticsService {
    /**
     * Детектировать движение на кадре.
     * @param minArea Минимальная площадь области движения (пиксели). Если null — вычисляется от размера кадра (0.1%).
     */
    suspend fun detectMotion(
        camera: Camera,
        frameData: ByteArray,
        previousFrameData: ByteArray? = null,
        zones: List<DetectionZone> = emptyList(),
        threshold: Float = 0.5f,
        minArea: Int? = null,
    ): MotionDetectionResult

    /**
     * Детектировать объекты на кадре
     */
    suspend fun detectObjects(
        camera: Camera,
        frameData: ByteArray,
        objectTypes: List<String> = emptyList(),
        minConfidence: Float = 0.5f,
    ): ObjectDetectionResult

    /**
     * Детектировать лица на кадре
     */
    suspend fun detectFaces(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float = 0.5f,
        includeLandmarks: Boolean = false,
        includeEmbeddings: Boolean = false,
    ): FaceDetectionResult

    /**
     * Распознать номерные знаки на кадре
     */
    suspend fun recognizeLicensePlates(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float = 0.7f,
        country: String? = null,
    ): LicensePlateRecognitionResult

    /**
     * Трекинг объектов: детекция + присвоение стабильных ID между кадрами.
     * Возвращает список активных треков с trackId, типом, bbox, confidence и lastSeen.
     */
    suspend fun trackObjects(
        camera: Camera,
        frameData: ByteArray,
    ): List<TrackInfo>

    /**
     * Сравнение двух эмбеддингов лиц (блок 8.3). Возвращает сходство в диапазоне 0.0–1.0
     * (например, cosine similarity). Реализация в нативном слое.
     */
    suspend fun compareFaces(
        embedding1: FloatArray,
        embedding2: FloatArray,
    ): Float
}

/**
 * Информация об одном треке (объект с стабильным ID между кадрами)
 */
data class TrackInfo(
    val trackId: Int,
    val objectType: String,
    val boundingBox: BoundingBox,
    val confidence: Float,
    val lastSeen: Long,
)

/**
 * Результат детекции движения
 */
data class MotionDetectionResult(
    val detected: Boolean,
    val confidence: Float,
    val zones: List<MotionZone> = emptyList(),
    val timestamp: Long = nowMillis(),
)

/**
 * Зона с детектированным движением
 */
data class MotionZone(
    val zone: DetectionZone,
    val intensity: Float, // 0.0 - 1.0
)

/**
 * Результат детекции объектов
 */
data class ObjectDetectionResult(
    val objects: List<DetectedObject>,
    val timestamp: Long = nowMillis(),
)

/**
 * Результат детекции лиц
 */
data class FaceDetectionResult(
    val faces: List<DetectedFace>,
    val timestamp: Long = nowMillis(),
)

/**
 * Результат распознавания номерных знаков
 */
data class LicensePlateRecognitionResult(
    val plates: List<RecognizedLicensePlate>,
    val timestamp: Long = nowMillis(),
)

/**
 * Заглушка реализации AnalyticsService для платформ без нативной поддержки
 * Для JVM платформ используется AnalyticsServiceImpl.jvm.kt с нативными библиотеками
 */
expect class AnalyticsServiceImpl() : AnalyticsService {
    override suspend fun detectMotion(
        camera: Camera,
        frameData: ByteArray,
        previousFrameData: ByteArray?,
        zones: List<DetectionZone>,
        threshold: Float,
        minArea: Int?,
    ): MotionDetectionResult

    override suspend fun detectObjects(
        camera: Camera,
        frameData: ByteArray,
        objectTypes: List<String>,
        minConfidence: Float,
    ): ObjectDetectionResult

    override suspend fun detectFaces(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float,
        includeLandmarks: Boolean,
        includeEmbeddings: Boolean,
    ): FaceDetectionResult

    override suspend fun recognizeLicensePlates(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float,
        country: String?,
    ): LicensePlateRecognitionResult

    override suspend fun trackObjects(
        camera: Camera,
        frameData: ByteArray,
    ): List<TrackInfo>

    override suspend fun compareFaces(
        embedding1: FloatArray,
        embedding2: FloatArray,
    ): Float
}
