package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/** Интервал повторных попыток переподписки при недоступности камеры/сети (мс). */
const val DEFAULT_ONVIF_RETRY_INTERVAL_MS = 5 * 60 * 1000L // 5 минут

/**
 * Сервис для управления мониторингом событий камер.
 *
 * Автоматически запускает мониторинг при старте приложения,
 * при ошибках (нет сети, камера недоступна) логирует и не падает.
 * Периодически повторяет попытки подписки для камер без активного мониторинга;
 * при возврате в приложение можно вызвать [ensureMonitoring] для немедленной переподписки.
 */
class CameraEventMonitoringService(
    private val cameraRepository: CameraRepository,
    private val eventIntegrationService: OnvifEventIntegrationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val retryIntervalMs: Long = DEFAULT_ONVIF_RETRY_INTERVAL_MS,
) {
    private var isInitialized = false
    private var periodicRetryJob: Job? = null

    /**
     * Инициализация сервиса — запуск мониторинга для всех камер и фоновой задачи переподписки.
     */
    suspend fun initialize() {
        if (isInitialized) {
            logger.warn { "CameraEventMonitoringService already initialized" }
            return
        }

        logger.info { "Initializing CameraEventMonitoringService..." }

        try {
            val cameras = cameraRepository.getCameras()
            logger.info { "Found ${cameras.size} cameras to monitor" }
            startMonitoringForCameras(cameras)
            isInitialized = true
            logger.info { "CameraEventMonitoringService initialized successfully" }

            periodicRetryJob =
                scope.launch {
                    while (isActive && isInitialized) {
                        delay(retryIntervalMs)
                        ensureMonitoringForAllCameras()
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize CameraEventMonitoringService" }
        }
    }

    /**
     * Одна попытка поднять мониторинг для всех подходящих камер, которые ещё не в мониторинге.
     * Вызывается по таймеру и при возврате в приложение (Android onResume и т.п.).
     */
    suspend fun ensureMonitoring() {
        if (!isInitialized) return
        ensureMonitoringForAllCameras()
    }

    private suspend fun ensureMonitoringForAllCameras() {
        try {
            val cameras = cameraRepository.getCameras()
            val monitored = eventIntegrationService.getMonitoredCameras().toSet()
            val toStart = cameras.filter { supportsOnvifEvents(it) && it.id !in monitored }
            if (toStart.isEmpty()) return
            logger.info { "Retrying monitoring for ${toStart.size} camera(s) (network/camera may have recovered)" }
            startMonitoringForCameras(toStart)
        } catch (e: Exception) {
            logger.warn(e) { "Error during monitoring retry cycle" }
        }
    }

    private suspend fun startMonitoringForCameras(cameras: List<Camera>) {
        cameras.forEach { camera ->
            if (!supportsOnvifEvents(camera)) return@forEach
            scope.launch {
                try {
                    val result =
                        eventIntegrationService.startMonitoring(
                            cameraId = camera.id,
                            cameraName = camera.name,
                            cameraUrl = camera.url,
                            username = camera.username,
                            password = camera.password,
                            pullInterval = 5000,
                        )
                    result.fold(
                        onSuccess = {
                            logger.info { "Started monitoring for camera: ${camera.id} (${camera.name})" }
                        },
                        onFailure = { error ->
                            logger.warn(error) {
                                "Failed to start monitoring for camera: ${camera.id} (${camera.name})"
                            }
                        },
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Exception starting monitoring for camera: ${camera.id}" }
                }
            }
        }
    }

    /**
     * Остановка мониторинга для всех камер и фоновой задачи переподписки.
     */
    suspend fun shutdown() {
        logger.info { "Shutting down CameraEventMonitoringService..." }
        periodicRetryJob?.cancel()
        periodicRetryJob = null
        try {
            eventIntegrationService.stopAll()
            isInitialized = false
            logger.info { "CameraEventMonitoringService shut down successfully" }
        } catch (e: Exception) {
            logger.error(e) { "Error shutting down CameraEventMonitoringService" }
        }
    }

    /**
     * Получить статус мониторинга для камеры
     */
    fun isMonitoring(cameraId: String): Boolean {
        return eventIntegrationService.isMonitoring(cameraId)
    }

    /**
     * Получить список камер с активным мониторингом
     */
    fun getMonitoredCameras(): List<String> {
        return eventIntegrationService.getMonitoredCameras()
    }

    /**
     * Проверка, поддерживает ли камера ONVIF события
     */
    private fun supportsOnvifEvents(camera: Camera): Boolean {
        return camera.url.startsWith("http://", ignoreCase = true) ||
            camera.url.startsWith("https://", ignoreCase = true)
    }
}
