package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.common.newRandomId
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.StoredLicensePlate
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.LicensePlateRecognitionResult

/**
 * Use case для распознавания номерных знаков на кадре видео
 *
 * Выполняет распознавание номерных знаков (ANPR - Automatic Number Plate Recognition)
 * с возможностью сохранения в БД и создания событий при обнаружении номеров.
 */
class RecognizeLicensePlateUseCase(
    private val analyticsService: AnalyticsService,
    private val eventRepository: EventRepository? = null,
    private val licensePlateRepository: LicensePlateRepository? = null,
) {
    /**
     * Распознать номерные знаки на кадре
     *
     * @param camera Камера
     * @param frameData Данные кадра (байты изображения)
     * @param minConfidence Минимальная уверенность (0.0 - 1.0)
     * @param country Код страны для валидации формата номера (опционально)
     * @param createEvent Создавать ли событие при обнаружении номеров (по умолчанию true, если eventRepository предоставлен)
     * @return результат распознавания номерных знаков
     */
    suspend operator fun invoke(
        camera: Camera,
        frameData: ByteArray,
        minConfidence: Float = 0.7f,
        country: String? = null,
        createEvent: Boolean = true,
    ): Result<LicensePlateRecognitionResult> {
        // Валидация входных параметров
        if (camera.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Camera ID cannot be blank"))
        }

        if (frameData.isEmpty()) {
            return Result.failure(IllegalArgumentException("Frame data cannot be empty"))
        }

        if (minConfidence < 0.0f || minConfidence > 1.0f) {
            return Result.failure(
                IllegalArgumentException("Min confidence must be between 0.0 and 1.0, got: $minConfidence"),
            )
        }

        // Проверка размера данных кадра
        val expectedSize =
            camera.resolution?.let { res ->
                res.width * res.height * 3 // RGB24
            }

        if (expectedSize != null && frameData.size < expectedSize) {
            return Result.failure(
                IllegalArgumentException(
                    "Frame data size (${frameData.size}) is less than expected ($expectedSize) for resolution ${camera.resolution}",
                ),
            )
        }

        return try {
            val result =
                analyticsService.recognizeLicensePlates(
                    camera = camera,
                    frameData = frameData,
                    minConfidence = minConfidence,
                    country = country,
                )

            // Фильтрация по чёрному/белому списку
            val filteredPlates = filterByLicensePlateLists(result.plates, camera)

            val filteredResult = LicensePlateRecognitionResult(filteredPlates, result.timestamp)

            val now = nowMillis()

            // Сохранение в БД при успешном распознавании (Фаза 5)
            if (filteredResult.plates.isNotEmpty() && licensePlateRepository != null) {
                filteredResult.plates.forEach { plate ->
                    val stored =
                        StoredLicensePlate(
                            id = newRandomId(),
                            cameraId = camera.id,
                            timestamp = now,
                            plateNumber = plate.plateNumber,
                            confidence = plate.confidence,
                            country = plate.country ?: country,
                            bboxX = plate.boundingBox.x,
                            bboxY = plate.boundingBox.y,
                            bboxWidth = plate.boundingBox.width,
                            bboxHeight = plate.boundingBox.height,
                            createdAt = now,
                        )
                    licensePlateRepository.insert(stored).onFailure {
                        println("Warning: Failed to store license plate: ${it.message}")
                    }
                }
            }

            // Создание события при обнаружении номерных знаков
            if (filteredResult.plates.isNotEmpty() && createEvent && eventRepository != null) {
                try {
                    createLicensePlateEvent(camera, filteredResult, country)
                } catch (e: Exception) {
                    println("Warning: Failed to create license plate event: ${e.message}")
                }
            }

            // Проверка чёрного списка и создание события/уведомления для заблокированных номеров
            val blockedPlates = result.plates - filteredPlates.toSet()
            if (blockedPlates.isNotEmpty() && camera.settings.analytics.anprBlockListNotificationEnabled) {
                try {
                    createBlockedPlateEvent(camera, blockedPlates, country)
                } catch (e: Exception) {
                    println("Warning: Failed to create blocked plate event: ${e.message}")
                }
            }

            Result.success(filteredResult)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                RuntimeException("Failed to recognize license plates: ${e.message}", e),
            )
        }
    }

    /**
     * Создать событие распознавания номерных знаков
     */
    private suspend fun createLicensePlateEvent(
        camera: Camera,
        result: LicensePlateRecognitionResult,
        country: String?,
    ) {
        val eventId = newRandomId()
        val timestamp = nowMillis()

        val metadata =
            mutableMapOf<String, String>().apply {
                put("platesCount", result.plates.size.toString())
                result.plates.forEachIndexed { index, plate ->
                    put("plate${index}Number", plate.plateNumber)
                    put("plate${index}Confidence", plate.confidence.toString())
                    plate.country?.let { put("plate${index}Country", it) }
                }
                country?.let { put("requestedCountry", it) }
                val maxConfidence = result.plates.maxOfOrNull { it.confidence } ?: 0.0f
                put("maxConfidence", maxConfidence.toString())
            }

        val plateNumbers = result.plates.joinToString(", ") { it.plateNumber }
        val description =
            buildString {
                append("Распознано номерных знаков: ${result.plates.size}")
                if (result.plates.isNotEmpty()) {
                    append(" ($plateNumbers)")
                }
                if (country != null) {
                    append(" [страна: $country]")
                }
            }

        val event =
            Event(
                id = eventId,
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.LICENSE_PLATE_RECOGNITION,
                severity = EventSeverity.INFO,
                timestamp = timestamp,
                description = description,
                metadata = metadata,
                acknowledged = false,
                acknowledgedAt = null,
                acknowledgedBy = null,
                thumbnailUrl = null,
                videoUrl = null,
            )

        eventRepository?.addEvent(event)?.fold(
            onSuccess = { createdEvent ->
                // Событие успешно создано
                println("License plate recognition event created: ${createdEvent.id}")
            },
            onFailure = { error ->
                // Ошибка при создании события
                println("Error creating license plate recognition event: ${error.message}")
            },
        )
    }

    /**
     * Фильтрация распознанных номеров по чёрному/белому списку
     */
    private fun filterByLicensePlateLists(
        plates: List<com.company.ipcamera.shared.domain.model.RecognizedLicensePlate>,
        camera: Camera,
    ): List<com.company.ipcamera.shared.domain.model.RecognizedLicensePlate> {
        val settings = camera.settings.analytics
        val listMode = settings.anprListMode ?: return plates // Списки не настроены

        val allowList = settings.anprAllowList.map { it.uppercase() }
        val blockList = settings.anprBlockList.map { it.uppercase() }

        return when (listMode) {
            com.company.ipcamera.shared.domain.model.AnprListMode.ALLOWLIST -> {
                // Возвращаем только номера из белого списка
                if (allowList.isEmpty()) {
                    emptyList() // Белый список пуст - ничего не разрешаем
                } else {
                    plates.filter { plate ->
                        allowList.any { it.contains(plate.plateNumber.uppercase()) }
                    }
                }
            }
            com.company.ipcamera.shared.domain.model.AnprListMode.BLOCKLIST -> {
                // Возвращаем все номера кроме заблокированных
                if (blockList.isEmpty()) {
                    plates // Чёрный список пуст - все разрешаем
                } else {
                    plates.filter { plate ->
                        !blockList.any { it.contains(plate.plateNumber.uppercase()) }
                    }
                }
            }
        }
    }

    /**
     * Создать событие обнаружения заблокированного номера
     */
    private suspend fun createBlockedPlateEvent(
        camera: Camera,
        plates: List<com.company.ipcamera.shared.domain.model.RecognizedLicensePlate>,
        country: String?,
    ) {
        val eventId = newRandomId()
        val timestamp = nowMillis()

        val metadata =
            mutableMapOf<String, String>().apply {
                put("blockedPlatesCount", plates.size.toString())
                put("severity", "HIGH")
                plates.forEachIndexed { index, plate ->
                    put("blockedPlate${index}Number", plate.plateNumber)
                    put("blockedPlate${index}Confidence", plate.confidence.toString())
                }
            }

        val plateNumbers = plates.joinToString(", ") { it.plateNumber }
        val description = "Обнаружен(ы) заблокированный(е) номер(а): $plateNumbers"

        val event =
            Event(
                id = eventId,
                cameraId = camera.id,
                cameraName = camera.name,
                type = EventType.LICENSE_PLATE_RECOGNITION,
                severity = EventSeverity.ERROR,
                timestamp = timestamp,
                description = description,
                metadata = metadata,
                acknowledged = false,
                acknowledgedAt = null,
                acknowledgedBy = null,
                thumbnailUrl = null,
                videoUrl = null,
            )

        eventRepository?.addEvent(event)?.fold(
            onSuccess = { createdEvent ->
                println("Blocked plate event created: ${createdEvent.id}")
            },
            onFailure = { error ->
                println("Error creating blocked plate event: ${error.message}")
            },
        )
    }
}
