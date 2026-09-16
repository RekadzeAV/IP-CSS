package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedFace
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.RecognizedLicensePlate
import com.company.ipcamera.shared.domain.service.FaceDetectionResult
import com.company.ipcamera.shared.domain.model.AnalyticsRuleType
import com.company.ipcamera.shared.domain.service.LicensePlateRecognitionResult
import com.company.ipcamera.shared.domain.service.MotionDetectionResult
import com.company.ipcamera.shared.domain.service.ObjectDetectionResult
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * Сервис для генерации событий на основе результатов аналитики
 *
 * Обеспечивает:
 * - Создание событий (motion/object/face/ANPR) в едином формате и запись в БД через EventService
 * - Определение важности событий на основе правил
 * - Фильтрацию по правилам аналитики (AnalyticsRuleService.applyRules) — уведомления, webhook и т.д.
 * - Cooldown и агрегацию для предотвращения спама
 * - Интеграцию с EventService
 */
class AnalyticsEventGenerator(
    private val eventService: EventService,
    private val analyticsRuleService: com.company.ipcamera.server.service.AnalyticsRuleService? = null,
    private val cooldownPeriod: Long = 5000L, // Минимальный интервал между событиями одного типа (мс)
    private val aggregationWindow: Long = 10000L // Окно агрегации для группировки событий (мс)
) {
    private val eventCooldowns = ConcurrentHashMap<String, MutableMap<EventType, Long>>()
    private val eventAggregator = ConcurrentHashMap<String, EventAggregation>()
    private val cleanupScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var cleanupJob: Job? = null

    init {
        // Запускаем периодическую очистку старых cooldown записей
        startCleanupTask()
    }

    /**
     * Генерация события на основе детекции движения
     */
    suspend fun generateMotionEvent(
        camera: Camera,
        frame: RtspFrame,
        result: MotionDetectionResult
    ): Result<Unit> {
        return try {
            if (!result.detected || result.confidence < 0.3f) {
                return Result.success(Unit)
            }

            val eventKey = "${camera.id}:${EventType.MOTION_DETECTION}"

            // Проверка cooldown периода
            if (shouldSkipEvent(camera.id, EventType.MOTION_DETECTION)) {
                logger.debug { "Skipping motion event due to cooldown for camera: ${camera.id}" }
                return Result.success(Unit)
            }

            // Определение важности события
            val severity = determineMotionSeverity(result, camera)

            // Формирование описания события
            val description = buildString {
                append("Обнаружено движение")
                if (result.zones.isNotEmpty()) {
                    append(" в зонах: ${result.zones.joinToString { it.zone.name }}")
                }
                append(" (уверенность: ${String.format("%.1f", result.confidence * 100)}%)")
            }

            // Формирование метаданных
            val metadata = buildMap {
                put("confidence", String.format("%.3f", result.confidence))
                put("timestamp", result.timestamp.toString())
                put("frameTimestamp", frame.timestamp.toString())
                if (result.zones.isNotEmpty()) {
                    put("zones", result.zones.joinToString { it.zone.name })
                    put("zoneCount", result.zones.size.toString())
                    result.zones.maxByOrNull { it.intensity }?.let { maxZone ->
                        put("maxIntensity", String.format("%.3f", maxZone.intensity))
                    }
                }
            }

            // Создание события
            val eventResult = eventService.createEvent(
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.MOTION_DETECTION,
                severity = severity,
                description = description,
                metadata = metadata
            )

            eventResult.fold(
                onSuccess = {
                    updateCooldown(camera.id, EventType.MOTION_DETECTION)
                    logger.debug { "Motion event generated for camera ${camera.id}: severity=$severity" }
                    // Применяем правила аналитики (фильтрация по правилам, уведомления, webhook)
                    analyticsRuleService?.applyRules(
                        camera.id,
                        AnalyticsRuleType.MOTION_DETECTION,
                        result,
                        result.confidence
                    )?.onFailure { e -> logger.debug(e) { "Rules apply failed for motion: ${camera.id}" } }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to generate motion event for camera: ${camera.id}" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error generating motion event for camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    /**
     * Генерация события на основе детекции объектов
     */
    suspend fun generateObjectDetectionEvent(
        camera: Camera,
        frame: RtspFrame,
        result: ObjectDetectionResult
    ): Result<Unit> {
        return try {
            if (result.objects.isEmpty()) {
                return Result.success(Unit)
            }

            val eventKey = "${camera.id}:${EventType.OBJECT_DETECTION}"

            // Проверка cooldown периода
            if (shouldSkipEvent(camera.id, EventType.OBJECT_DETECTION)) {
                logger.debug { "Skipping object detection event due to cooldown for camera: ${camera.id}" }
                return Result.success(Unit)
            }

            // Группировка объектов по типам
            val objectsByType = result.objects.groupBy { it.type }

            // Генерация событий для каждого типа объектов
            val results = objectsByType.map { (objectType, objects) ->
                generateObjectTypeEvent(camera, frame, objectType, objects, result.timestamp)
            }

            // Проверяем успешность всех операций
            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                logger.warn { "Some object detection events failed for camera: ${camera.id}" }
            }

            updateCooldown(camera.id, EventType.OBJECT_DETECTION)
            // Применяем правила аналитики по результату детекции объектов
            val maxConfidence = result.objects.maxOfOrNull { it.confidence } ?: 0f
            analyticsRuleService?.applyRules(
                camera.id,
                AnalyticsRuleType.OBJECT_DETECTION,
                result,
                maxConfidence
            )?.onFailure { e -> logger.debug(e) { "Rules apply failed for object: ${camera.id}" } }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error generating object detection event for camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    /**
     * Генерация события для конкретного типа объектов
     */
    private suspend fun generateObjectTypeEvent(
        camera: Camera,
        frame: RtspFrame,
        objectType: String,
        objects: List<DetectedObject>,
        timestamp: Long
    ): Result<Unit> {
        return try {
            // Фильтрация объектов по минимальной уверенности
            val filteredObjects = objects.filter { it.confidence >= 0.5f }
            if (filteredObjects.isEmpty()) {
                return Result.success(Unit)
            }

            // Определение важности события
            val maxConfidence = filteredObjects.maxOfOrNull { it.confidence } ?: 0f
            val avgConfidence = filteredObjects.map { it.confidence }.average().toFloat()
            val severity = determineObjectDetectionSeverity(objectType, filteredObjects.size, maxConfidence)

            // Формирование описания
            val description = buildString {
                append("Обнаружен")
                when (filteredObjects.size) {
                    1 -> append(" объект типа '$objectType'")
                    else -> append("о ${filteredObjects.size} объектов типа '$objectType'")
                }
                append(" (уверенность: ${String.format("%.1f", avgConfidence * 100)}%)")
            }

            // Формирование метаданных
            val metadata = buildMap {
                put("objectType", objectType)
                put("objectCount", filteredObjects.size.toString())
                put("maxConfidence", String.format("%.3f", maxConfidence))
                put("avgConfidence", String.format("%.3f", avgConfidence))
                put("timestamp", timestamp.toString())
                put("frameTimestamp", frame.timestamp.toString())

                // Информация о позициях объектов
                if (filteredObjects.size <= 5) {
                    put("objects", filteredObjects.joinToString(";") { obj ->
                        "${obj.type}:${String.format("%.2f", obj.confidence)}:" +
                        "${obj.boundingBox.x},${obj.boundingBox.y}," +
                        "${obj.boundingBox.width}x${obj.boundingBox.height}"
                    })
                }
            }

            // Создание события
            val eventResult = eventService.createEvent(
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.OBJECT_DETECTION,
                severity = severity,
                description = description,
                metadata = metadata
            )

            eventResult.fold(
                onSuccess = {
                    logger.debug {
                        "Object detection event generated for camera ${camera.id}: " +
                        "type=$objectType, count=${filteredObjects.size}, severity=$severity"
                    }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) {
                        "Failed to generate object detection event for camera: ${camera.id}, type: $objectType"
                    }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error generating object type event for camera: ${camera.id}, type: $objectType" }
            Result.failure(e)
        }
    }

    /**
     * Генерация события на основе детекции лиц
     */
    suspend fun generateFaceDetectionEvent(
        camera: Camera,
        frame: RtspFrame,
        result: FaceDetectionResult
    ): Result<Unit> {
        return try {
            if (result.faces.isEmpty()) {
                return Result.success(Unit)
            }

            // Проверка cooldown периода
            if (shouldSkipEvent(camera.id, EventType.FACE_DETECTION)) {
                logger.debug { "Skipping face detection event due to cooldown for camera: ${camera.id}" }
                return Result.success(Unit)
            }

            // Фильтрация лиц по минимальной уверенности
            val filteredFaces = result.faces.filter { it.confidence >= 0.7f }
            if (filteredFaces.isEmpty()) {
                return Result.success(Unit)
            }

            // Определение важности события
            val maxConfidence = filteredFaces.maxOfOrNull { it.confidence } ?: 0f
            val avgConfidence = filteredFaces.map { it.confidence }.average().toFloat()
            val severity = determineFaceDetectionSeverity(filteredFaces.size, maxConfidence)

            // Формирование описания
            val description = buildString {
                when (filteredFaces.size) {
                    1 -> append("Обнаружено лицо")
                    else -> append("Обнаружено ${filteredFaces.size} лиц")
                }
                append(" (уверенность: ${String.format("%.1f", avgConfidence * 100)}%)")
            }

            // Формирование метаданных
            val metadata = buildMap {
                put("faceCount", filteredFaces.size.toString())
                put("maxConfidence", String.format("%.3f", maxConfidence))
                put("avgConfidence", String.format("%.3f", avgConfidence))
                put("timestamp", result.timestamp.toString())
                put("frameTimestamp", frame.timestamp.toString())

                // Информация о позициях лиц
                if (filteredFaces.size <= 5) {
                    put("faces", filteredFaces.joinToString(";") { face ->
                        "${String.format("%.2f", face.confidence)}:" +
                        "${face.boundingBox.x},${face.boundingBox.y}," +
                        "${face.boundingBox.width}x${face.boundingBox.height}"
                    })
                }
            }

            // Создание события
            val eventResult = eventService.createEvent(
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.FACE_DETECTION,
                severity = severity,
                description = description,
                metadata = metadata
            )

            eventResult.fold(
                onSuccess = {
                    updateCooldown(camera.id, EventType.FACE_DETECTION)
                    logger.debug {
                        "Face detection event generated for camera ${camera.id}: " +
                        "count=${filteredFaces.size}, severity=$severity"
                    }
                    // Применяем правила аналитики
                    analyticsRuleService?.applyRules(
                        camera.id,
                        AnalyticsRuleType.FACE_DETECTION,
                        result,
                        maxConfidence
                    )?.onFailure { e -> logger.debug(e) { "Rules apply failed for face: ${camera.id}" } }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to generate face detection event for camera: ${camera.id}" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error generating face detection event for camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    /**
     * Генерация события на основе распознавания номерных знаков
     */
    suspend fun generateLicensePlateEvent(
        camera: Camera,
        frame: RtspFrame,
        result: LicensePlateRecognitionResult
    ): Result<Unit> {
        return try {
            if (result.plates.isEmpty()) {
                return Result.success(Unit)
            }

            // Проверка cooldown периода
            if (shouldSkipEvent(camera.id, EventType.LICENSE_PLATE_RECOGNITION)) {
                logger.debug { "Skipping license plate event due to cooldown for camera: ${camera.id}" }
                return Result.success(Unit)
            }

            // Фильтрация номеров по минимальной уверенности
            val filteredPlates = result.plates.filter { it.confidence >= 0.7f }
            if (filteredPlates.isEmpty()) {
                return Result.success(Unit)
            }

            // Генерация события для каждого распознанного номера
            val results = filteredPlates.map { plate ->
                generateSingleLicensePlateEvent(camera, frame, plate, result.timestamp)
            }

            // Проверяем успешность всех операций
            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                logger.warn { "Some license plate events failed for camera: ${camera.id}" }
            }

            updateCooldown(camera.id, EventType.LICENSE_PLATE_RECOGNITION)
            // Применяем правила аналитики по результату ANPR
            val maxPlateConfidence = result.plates.maxOfOrNull { it.confidence } ?: 0f
            analyticsRuleService?.applyRules(
                camera.id,
                AnalyticsRuleType.LICENSE_PLATE_RECOGNITION,
                result,
                maxPlateConfidence
            )?.onFailure { e -> logger.debug(e) { "Rules apply failed for license plate: ${camera.id}" } }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error generating license plate event for camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    /**
     * Генерация события для одного номерного знака
     */
    private suspend fun generateSingleLicensePlateEvent(
        camera: Camera,
        frame: RtspFrame,
        plate: RecognizedLicensePlate,
        timestamp: Long
    ): Result<Unit> {
        return try {
            // Определение важности события
            val severity = determineLicensePlateSeverity(plate.confidence)

            // Формирование описания
            val description = buildString {
                append("Распознан номерной знак: ${plate.plateNumber}")
                plate.country?.let { append(" ($it)") }
                append(" (уверенность: ${String.format("%.1f", plate.confidence * 100)}%)")
            }

            // Формирование метаданных
            val metadata = buildMap {
                put("plateNumber", plate.plateNumber)
                put("confidence", String.format("%.3f", plate.confidence))
                put("timestamp", timestamp.toString())
                put("frameTimestamp", frame.timestamp.toString())
                plate.country?.let { put("country", it) }
                put("boundingBox", "${plate.boundingBox.x},${plate.boundingBox.y}," +
                    "${plate.boundingBox.width}x${plate.boundingBox.height}")
            }

            // Создание события
            val eventResult = eventService.createEvent(
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.LICENSE_PLATE_RECOGNITION,
                severity = severity,
                description = description,
                metadata = metadata
            )

            eventResult.fold(
                onSuccess = {
                    logger.debug {
                        "License plate event generated for camera ${camera.id}: " +
                        "plate=${plate.plateNumber}, severity=$severity"
                    }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) {
                        "Failed to generate license plate event for camera: ${camera.id}, plate: ${plate.plateNumber}"
                    }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error generating license plate event for camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    /**
     * Определение важности события движения
     */
    private fun determineMotionSeverity(
        result: MotionDetectionResult,
        camera: Camera
    ): EventSeverity {
        return when {
            // Критическое: движение в важных зонах с высокой уверенностью
            result.zones.isNotEmpty() && result.confidence >= 0.9f -> EventSeverity.CRITICAL

            // Ошибка: высокая уверенность движения
            result.confidence >= 0.8f -> EventSeverity.ERROR

            // Предупреждение: средняя уверенность
            result.confidence >= 0.5f -> EventSeverity.WARNING

            // Информация: низкая уверенность
            else -> EventSeverity.INFO
        }
    }

    /**
     * Определение важности события детекции объектов
     */
    private fun determineObjectDetectionSeverity(
        objectType: String,
        count: Int,
        maxConfidence: Float
    ): EventSeverity {
        // Критические типы объектов
        val criticalTypes = setOf("person", "weapon", "knife", "gun", "suspicious")
        val warningTypes = setOf("car", "truck", "bus", "motorcycle")

        return when {
            // Критическое: критический тип объекта с высокой уверенностью
            criticalTypes.contains(objectType.lowercase()) && maxConfidence >= 0.8f -> EventSeverity.CRITICAL

            // Критическое: много критических объектов
            criticalTypes.contains(objectType.lowercase()) && count >= 3 -> EventSeverity.CRITICAL

            // Ошибка: критический тип объекта со средней уверенностью
            criticalTypes.contains(objectType.lowercase()) && maxConfidence >= 0.6f -> EventSeverity.ERROR

            // Предупреждение: важный тип объекта или много объектов
            warningTypes.contains(objectType.lowercase()) && maxConfidence >= 0.7f -> EventSeverity.WARNING

            // Предупреждение: много объектов любого типа
            count >= 5 -> EventSeverity.WARNING

            // Информация: обычные объекты
            else -> EventSeverity.INFO
        }
    }

    /**
     * Определение важности события детекции лиц
     */
    private fun determineFaceDetectionSeverity(
        count: Int,
        maxConfidence: Float
    ): EventSeverity {
        return when {
            // Критическое: много лиц с высокой уверенностью
            count >= 5 && maxConfidence >= 0.9f -> EventSeverity.CRITICAL

            // Ошибка: несколько лиц с высокой уверенностью
            count >= 3 && maxConfidence >= 0.85f -> EventSeverity.ERROR

            // Предупреждение: лица обнаружены
            maxConfidence >= 0.8f -> EventSeverity.WARNING

            // Информация: низкая уверенность
            else -> EventSeverity.INFO
        }
    }

    /**
     * Определение важности события распознавания номерных знаков
     */
    private fun determineLicensePlateSeverity(confidence: Float): EventSeverity {
        return when {
            // Критическое: очень высокая уверенность
            confidence >= 0.95f -> EventSeverity.CRITICAL

            // Ошибка: высокая уверенность
            confidence >= 0.85f -> EventSeverity.ERROR

            // Предупреждение: средняя уверенность
            confidence >= 0.75f -> EventSeverity.WARNING

            // Информация: низкая уверенность
            else -> EventSeverity.INFO
        }
    }

    /**
     * Проверка, нужно ли пропустить событие из-за cooldown периода
     */
    private fun shouldSkipEvent(cameraId: String, eventType: EventType): Boolean {
        val cameraCooldowns = eventCooldowns[cameraId] ?: return false
        val lastEventTime = cameraCooldowns[eventType] ?: return false
        val currentTime = System.currentTimeMillis()

        return (currentTime - lastEventTime) < cooldownPeriod
    }

    /**
     * Обновление времени последнего события
     */
    private fun updateCooldown(cameraId: String, eventType: EventType) {
        val cameraCooldowns = eventCooldowns.getOrPut(cameraId) { mutableMapOf() }
        cameraCooldowns[eventType] = System.currentTimeMillis()
    }

    /**
     * Очистка старых cooldown записей
     */
    fun cleanupOldCooldowns() {
        val currentTime = System.currentTimeMillis()
        val maxAge = cooldownPeriod * 10 // Храним записи в 10 раз дольше cooldown периода

        eventCooldowns.values.forEach { cooldowns ->
            cooldowns.entries.removeIf { (currentTime - it.value) > maxAge }
        }
        eventCooldowns.entries.removeIf { it.value.isEmpty() }
    }

    /**
     * Запустить фоновую задачу для очистки старых cooldown записей
     */
    private fun startCleanupTask() {
        cleanupJob = cleanupScope.launch {
            while (isActive) {
                try {
                    delay(3600000) // Каждый час
                    cleanupOldCooldowns()
                    logger.debug { "Cleaned up old event cooldowns" }
                } catch (e: CancellationException) {
                    logger.info { "Cleanup task cancelled" }
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Error in cleanup task" }
                }
            }
        }
    }

    /**
     * Остановить фоновую задачу очистки
     */
    fun stopCleanupTask() {
        cleanupJob?.cancel()
        cleanupJob = null
        cleanupScope.cancel()
    }

    /**
     * Данные для агрегации событий
     */
    private data class EventAggregation(
        val eventType: EventType,
        val count: Int,
        val firstTimestamp: Long,
        val lastTimestamp: Long
    )
}
