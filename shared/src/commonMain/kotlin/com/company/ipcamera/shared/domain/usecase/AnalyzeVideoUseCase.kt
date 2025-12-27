package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedFace
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.RecognizedLicensePlate

/**
 * Результат анализа видео
 */
data class VideoAnalysisResult(
    val motionDetected: Boolean = false,
    val objectsDetected: List<DetectedObject> = emptyList(),
    val facesDetected: List<DetectedFace> = emptyList(),
    val licensePlatesDetected: List<RecognizedLicensePlate> = emptyList(),
    val events: List<AnalysisEvent> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Событие анализа
 */
data class AnalysisEvent(
    val type: EventType,
    val description: String,
    val confidence: Float,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Настройки анализа видео
 */
data class VideoAnalysisSettings(
    val enableMotionDetection: Boolean = true,
    val enableObjectDetection: Boolean = false,
    val enableFaceDetection: Boolean = false,
    val enableLicensePlateRecognition: Boolean = false,
    val objectTypes: List<String> = emptyList(),
    val motionThreshold: Float = 0.5f,
    val minConfidence: Float = 0.5f
)

/**
 * Use case для комплексного анализа видео
 *
 * Объединяет детекцию движения, объектов, лиц и распознавание номерных знаков
 */
class AnalyzeVideoUseCase(
    private val detectMotionUseCase: DetectMotionUseCase,
    private val detectObjectsUseCase: DetectObjectsUseCase,
    private val detectFacesUseCase: DetectFacesUseCase,
    private val recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase
) {
    /**
     * Проанализировать кадр видео
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param previousFrameData Данные предыдущего кадра для детекции движения
     * @param settings Настройки анализа
     * @return результат анализа
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        previousFrameData: ByteArray? = null,
        settings: VideoAnalysisSettings = VideoAnalysisSettings()
    ): Result<VideoAnalysisResult> {
        return try {
            var motionDetected = false
            val objectsDetected = mutableListOf<DetectedObject>()
            val facesDetected = mutableListOf<DetectedFace>()
            val licensePlatesDetected = mutableListOf<RecognizedLicensePlate>()
            val events = mutableListOf<AnalysisEvent>()

            // Детекция движения
            if (settings.enableMotionDetection) {
                val motionResult = detectMotionUseCase(
                    camera = camera,
                    frameData = frameData,
                    previousFrameData = previousFrameData,
                    zones = camera.settings.analytics.zones,
                    threshold = settings.motionThreshold
                ).getOrNull()

                motionDetected = motionResult?.detected ?: false
                if (motionDetected) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.MOTION_DETECTION,
                            description = "Обнаружено движение",
                            confidence = motionResult?.confidence ?: 0.0f,
                            metadata = mapOf("zones" to (motionResult?.zones?.size?.toString() ?: "0"))
                        )
                    )
                }
            }

            // Детекция объектов
            if (settings.enableObjectDetection) {
                val objectsResult = detectObjectsUseCase(
                    camera = camera,
                    frameData = frameData,
                    objectTypes = settings.objectTypes.ifEmpty { camera.settings.analytics.objectTypes },
                    minConfidence = settings.minConfidence
                ).getOrNull()

                objectsDetected.addAll(objectsResult?.objects ?: emptyList())
                if (objectsDetected.isNotEmpty()) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.OBJECT_DETECTION,
                            description = "Обнаружено объектов: ${objectsDetected.size}",
                            confidence = objectsDetected.maxOfOrNull { it.confidence } ?: 0.0f,
                            metadata = mapOf("count" to objectsDetected.size.toString())
                        )
                    )
                }
            }

            // Детекция лиц
            if (settings.enableFaceDetection) {
                val facesResult = detectFacesUseCase(
                    camera = camera,
                    frameData = frameData,
                    minConfidence = settings.minConfidence
                ).getOrNull()

                facesDetected.addAll(facesResult?.faces ?: emptyList())
                if (facesDetected.isNotEmpty()) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.FACE_DETECTION,
                            description = "Обнаружено лиц: ${facesDetected.size}",
                            confidence = facesDetected.maxOfOrNull { it.confidence } ?: 0.0f,
                            metadata = mapOf("count" to facesDetected.size.toString())
                        )
                    )
                }
            }

            // Распознавание номерных знаков
            if (settings.enableLicensePlateRecognition) {
                val platesResult = recognizeLicensePlateUseCase(
                    camera = camera,
                    frameData = frameData,
                    minConfidence = settings.minConfidence
                ).getOrNull()

                licensePlatesDetected.addAll(platesResult?.plates ?: emptyList())
                if (licensePlatesDetected.isNotEmpty()) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.LICENSE_PLATE_RECOGNITION,
                            description = "Распознано номеров: ${licensePlatesDetected.size}",
                            confidence = licensePlatesDetected.maxOfOrNull { it.confidence } ?: 0.0f,
                            metadata = mapOf(
                                "count" to licensePlatesDetected.size.toString(),
                                "plates" to licensePlatesDetected.joinToString(", ") { it.plateNumber }
                            )
                        )
                    )
                }
            }

            Result.success(
                VideoAnalysisResult(
                    motionDetected = motionDetected,
                    objectsDetected = objectsDetected,
                    facesDetected = facesDetected,
                    licensePlatesDetected = licensePlatesDetected,
                    events = events
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

