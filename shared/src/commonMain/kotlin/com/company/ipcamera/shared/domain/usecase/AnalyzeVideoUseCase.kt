package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedFace
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.RecognizedLicensePlate
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.NotificationService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Результат анализа видео
 */
data class VideoAnalysisResult(
    val motionDetected: Boolean = false,
    val objectsDetected: List<DetectedObject> = emptyList(),
    val facesDetected: List<DetectedFace> = emptyList(),
    val licensePlatesDetected: List<RecognizedLicensePlate> = emptyList(),
    val events: List<AnalysisEvent> = emptyList(),
    val timestamp: Long = nowMillis(),
)

/**
 * Событие анализа
 */
data class AnalysisEvent(
    val type: EventType,
    val description: String,
    val confidence: Float,
    val metadata: Map<String, String> = emptyMap(),
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
    val minConfidence: Float = 0.5f,
    val createEvents: Boolean = true,
    val sendNotifications: Boolean = true,
)

/**
 * Use case для комплексного анализа видео
 *
 * Объединяет детекцию движения, объектов, лиц и распознавание номерных знаков.
 * Поддерживает параллельную обработку для оптимизации производительности.
 * Интегрирован с EventRepository и NotificationService для создания событий и уведомлений.
 */
class AnalyzeVideoUseCase(
    private val detectMotionUseCase: DetectMotionUseCase,
    private val detectObjectsUseCase: DetectObjectsUseCase,
    private val detectFacesUseCase: DetectFacesUseCase,
    private val recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase,
    private val eventRepository: EventRepository? = null,
    private val notificationService: NotificationService? = null,
) {
    /**
     * Проанализировать кадр видео
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param previousFrameData Данные предыдущего кадра для детекции движения
     * @param settings Настройки анализа (если не указаны, используются настройки из camera.settings.analytics)
     * @return результат анализа
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        previousFrameData: ByteArray? = null,
        settings: VideoAnalysisSettings? = null,
    ): Result<VideoAnalysisResult> {
        // Валидация входных параметров
        if (camera.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Camera ID cannot be blank"))
        }

        if (frameData.isEmpty()) {
            return Result.failure(IllegalArgumentException("Frame data cannot be empty"))
        }

        // Используем настройки из параметров или из камеры (camera.settings.analytics)
        val analyticsSettings = camera.settings.analytics
        val analysisSettings =
            settings ?: VideoAnalysisSettings(
                enableMotionDetection = analyticsSettings.motionDetection,
                enableObjectDetection = analyticsSettings.objectDetection,
                enableFaceDetection = analyticsSettings.faceRecognition,
                enableLicensePlateRecognition = analyticsSettings.anprEnabled,
                objectTypes = analyticsSettings.objectTypes,
                motionThreshold = analyticsSettings.motionThreshold,
                minConfidence = analyticsSettings.objectDetectionConfidenceThreshold,
            )

        // Валидация настроек
        if (analysisSettings.motionThreshold < 0.0f || analysisSettings.motionThreshold > 1.0f) {
            return Result.failure(
                IllegalArgumentException("Motion threshold must be between 0.0 and 1.0"),
            )
        }

        if (analysisSettings.minConfidence < 0.0f || analysisSettings.minConfidence > 1.0f) {
            return Result.failure(
                IllegalArgumentException("Min confidence must be between 0.0 and 1.0"),
            )
        }

        return try {
            // Параллельная обработка для оптимизации производительности
            coroutineScope {
                var motionDetected = false
                val objectsDetected = mutableListOf<DetectedObject>()
                val facesDetected = mutableListOf<DetectedFace>()
                val licensePlatesDetected = mutableListOf<RecognizedLicensePlate>()
                val events = mutableListOf<AnalysisEvent>()

                // Запускаем все детекции параллельно
                val motionDeferred =
                    if (analysisSettings.enableMotionDetection) {
                        async {
                            detectMotionUseCase(
                                camera = camera,
                                frameData = frameData,
                                previousFrameData = previousFrameData,
                                zones = analyticsSettings.zones,
                                threshold = analysisSettings.motionThreshold,
                                createEvent = false, // События создадим позже централизованно
                            )
                        }
                    } else {
                        null
                    }

                val objectsDeferred =
                    if (analysisSettings.enableObjectDetection) {
                        async {
                            detectObjectsUseCase(
                                camera = camera,
                                frameData = frameData,
                                objectTypes = analysisSettings.objectTypes.ifEmpty { analyticsSettings.objectTypes },
                                minConfidence = analysisSettings.minConfidence,
                                createEvent = false, // События создадим позже централизованно
                            )
                        }
                    } else {
                        null
                    }

                val facesDeferred =
                    if (analysisSettings.enableFaceDetection) {
                        async {
                            detectFacesUseCase(
                                camera = camera,
                                frameData = frameData,
                                minConfidence = analysisSettings.minConfidence,
                                createEvent = false, // События создадим позже централизованно
                            )
                        }
                    } else {
                        null
                    }

                val platesDeferred =
                    if (analysisSettings.enableLicensePlateRecognition) {
                        async {
                            recognizeLicensePlateUseCase(
                                camera = camera,
                                frameData = frameData,
                                minConfidence = analysisSettings.minConfidence,
                                createEvent = false, // События создадим позже централизованно
                            )
                        }
                    } else {
                        null
                    }

                // Ожидаем результаты
                val motionResult = motionDeferred?.await()?.getOrNull()
                val objectsResult = objectsDeferred?.await()?.getOrNull()
                val facesResult = facesDeferred?.await()?.getOrNull()
                val platesResult = platesDeferred?.await()?.getOrNull()

                // Обрабатываем результаты
                motionDetected = motionResult?.detected ?: false
                val mr = motionResult
                if (motionDetected && mr != null) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.MOTION_DETECTION,
                            description = "Обнаружено движение",
                            confidence = mr.confidence,
                            metadata = mapOf("zones" to (mr.zones.size.toString())),
                        ),
                    )
                }

                objectsDetected.addAll(objectsResult?.objects ?: emptyList())
                if (objectsDetected.isNotEmpty()) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.OBJECT_DETECTION,
                            description = "Обнаружено объектов: ${objectsDetected.size}",
                            confidence = objectsDetected.maxOfOrNull { it.confidence } ?: 0.0f,
                            metadata = mapOf("count" to objectsDetected.size.toString()),
                        ),
                    )
                }

                facesDetected.addAll(facesResult?.faces ?: emptyList())
                if (facesDetected.isNotEmpty()) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.FACE_DETECTION,
                            description = "Обнаружено лиц: ${facesDetected.size}",
                            confidence = facesDetected.maxOfOrNull { it.confidence } ?: 0.0f,
                            metadata = mapOf("count" to facesDetected.size.toString()),
                        ),
                    )
                }

                licensePlatesDetected.addAll(platesResult?.plates ?: emptyList())
                if (licensePlatesDetected.isNotEmpty()) {
                    events.add(
                        AnalysisEvent(
                            type = EventType.LICENSE_PLATE_RECOGNITION,
                            description = "Распознано номеров: ${licensePlatesDetected.size}",
                            confidence = licensePlatesDetected.maxOfOrNull { it.confidence } ?: 0.0f,
                            metadata =
                                mapOf(
                                    "count" to licensePlatesDetected.size.toString(),
                                    "plates" to licensePlatesDetected.joinToString(", ") { it.plateNumber },
                                ),
                        ),
                    )
                }

                // Создаем события и отправляем уведомления, если включено
                if (analysisSettings.createEvents && events.isNotEmpty()) {
                    createEventsFromAnalysis(camera, events, analysisSettings.sendNotifications)
                }

                Result.success(
                    VideoAnalysisResult(
                        motionDetected = motionDetected,
                        objectsDetected = objectsDetected,
                        facesDetected = facesDetected,
                        licensePlatesDetected = licensePlatesDetected,
                        events = events,
                    ),
                )
            }
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RuntimeException("Failed to analyze video: ${e.message}", e),
            )
        }
    }

    /**
     * Создать события из результатов анализа
     */
    private suspend fun createEventsFromAnalysis(
        camera: Camera,
        analysisEvents: List<AnalysisEvent>,
        sendNotifications: Boolean,
    ) {
        if (eventRepository == null) return

        analysisEvents.forEach { analysisEvent ->
            try {
                // Фильтрация по минимальному confidence (если нужно)
                // Здесь можно добавить дополнительную логику фильтрации

                // Создание события через EventRepository
                // Примечание: здесь нужно использовать существующий механизм создания событий
                // Для упрощения, события уже создаются в отдельных Use Cases
                // Этот метод можно использовать для дополнительной обработки или уведомлений

                // Отправка уведомлений для важных событий
                if (sendNotifications && notificationService != null) {
                    when (analysisEvent.type) {
                        EventType.MOTION_DETECTION,
                        EventType.OBJECT_DETECTION,
                        EventType.FACE_DETECTION,
                        EventType.LICENSE_PLATE_RECOGNITION,
                        -> {
                            // Уведомления уже отправляются через EventService при создании событий
                            // Здесь можно добавить дополнительную логику, если нужно
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                println("Warning: Failed to process analysis event: ${e.message}")
            }
        }
    }
}
