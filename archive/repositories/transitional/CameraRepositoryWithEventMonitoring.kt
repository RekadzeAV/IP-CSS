package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.*
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.service.OnvifEventIntegrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Обертка для CameraRepository с автоматическим мониторингом ONVIF событий
 *
 * Автоматически запускает мониторинг событий при добавлении камеры
 * и останавливает при удалении или обновлении (если изменились учетные данные)
 */
class CameraRepositoryWithEventMonitoring(
    private val delegate: CameraRepository,
    private val eventIntegrationService: OnvifEventIntegrationService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : CameraRepository by delegate {
    /**
     * Проверка, поддерживает ли камера ONVIF события
     */
    private fun supportsOnvifEvents(camera: Camera): Boolean {
        // Проверяем, что камера имеет ONVIF URL и учетные данные
        return camera.url.startsWith("http://", ignoreCase = true) ||
            camera.url.startsWith("https://", ignoreCase = true)
    }

    override suspend fun addCamera(camera: Camera): Result<Camera> {
        val result = delegate.addCamera(camera)

        result.fold(
            onSuccess = { addedCamera ->
                // Автоматически запустить мониторинг событий для ONVIF камер
                if (supportsOnvifEvents(addedCamera)) {
                    scope.launch {
                        try {
                            val monitoringResult =
                                eventIntegrationService.startMonitoring(
                                    cameraId = addedCamera.id,
                                    cameraName = addedCamera.name,
                                    cameraUrl = addedCamera.url,
                                    username = addedCamera.username,
                                    password = addedCamera.password,
                                    pullInterval = 5000, // Стандартный интервал опроса
                                )

                            monitoringResult.fold(
                                onSuccess = {
                                    logger.info { "Started event monitoring for camera: ${addedCamera.id}" }
                                },
                                onFailure = { error ->
                                    logger.warn(error) {
                                        "Failed to start event monitoring for camera: ${addedCamera.id}. " +
                                            "Camera added but events will not be monitored."
                                    }
                                },
                            )
                        } catch (e: Exception) {
                            logger.error(e) {
                                "Exception while starting event monitoring for camera: ${addedCamera.id}"
                            }
                        }
                    }
                } else {
                    logger.debug { "Camera ${addedCamera.id} does not support ONVIF events" }
                }
            },
            onFailure = { /* Ошибка добавления камеры, ничего не делаем */ },
        )

        return result
    }

    override suspend fun updateCamera(camera: Camera): Result<Camera> {
        // Получаем старую версию камеры для сравнения
        val oldCamera = delegate.getCameraById(camera.id)

        val result = delegate.updateCamera(camera)

        result.fold(
            onSuccess = { updatedCamera ->
                // Проверяем, изменились ли учетные данные или URL
                val credentialsChanged =
                    oldCamera?.let { old ->
                        old.url != updatedCamera.url ||
                            old.username != updatedCamera.username ||
                            old.password != updatedCamera.password
                    } ?: false

                // Если изменились учетные данные или URL, перезапускаем мониторинг
                if (credentialsChanged && supportsOnvifEvents(updatedCamera)) {
                    scope.launch {
                        try {
                            // Останавливаем старый мониторинг
                            eventIntegrationService.stopMonitoring(updatedCamera.id)

                            // Запускаем новый мониторинг с обновленными данными
                            val monitoringResult =
                                eventIntegrationService.startMonitoring(
                                    cameraId = updatedCamera.id,
                                    cameraName = updatedCamera.name,
                                    cameraUrl = updatedCamera.url,
                                    username = updatedCamera.username,
                                    password = updatedCamera.password,
                                    pullInterval = 5000,
                                )

                            monitoringResult.fold(
                                onSuccess = {
                                    logger.info { "Restarted event monitoring for camera: ${updatedCamera.id}" }
                                },
                                onFailure = { error ->
                                    logger.warn(error) {
                                        "Failed to restart event monitoring for camera: ${updatedCamera.id}"
                                    }
                                },
                            )
                        } catch (e: Exception) {
                            logger.error(e) {
                                "Exception while restarting event monitoring for camera: ${updatedCamera.id}"
                            }
                        }
                    }
                } else if (!supportsOnvifEvents(updatedCamera)) {
                    // Если камера больше не поддерживает ONVIF, останавливаем мониторинг
                    scope.launch {
                        eventIntegrationService.stopMonitoring(updatedCamera.id)
                    }
                }
            },
            onFailure = { /* Ошибка обновления камеры, ничего не делаем */ },
        )

        return result
    }

    override suspend fun removeCamera(id: String): Result<Unit> {
        // Останавливаем мониторинг перед удалением
        scope.launch {
            try {
                eventIntegrationService.stopMonitoring(id)
                logger.debug { "Stopped event monitoring for camera: $id" }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to stop event monitoring for camera: $id" }
            }
        }

        return delegate.removeCamera(id)
    }

    /**
     * Запустить мониторинг для всех существующих камер
     * Полезно при старте приложения
     */
    suspend fun startMonitoringForAllCameras() {
        val cameras = delegate.getCameras()
        logger.info { "Starting event monitoring for ${cameras.size} cameras" }

        cameras.forEach { camera ->
            if (supportsOnvifEvents(camera)) {
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
                                logger.debug { "Started monitoring for camera: ${camera.id}" }
                            },
                            onFailure = { error ->
                                logger.warn(error) {
                                    "Failed to start monitoring for camera: ${camera.id}"
                                }
                            },
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Exception starting monitoring for camera: ${camera.id}" }
                    }
                }
            }
        }
    }

    /**
     * Остановить мониторинг для всех камер
     * Полезно при завершении приложения
     */
    suspend fun stopMonitoringForAllCameras() {
        logger.info { "Stopping event monitoring for all cameras" }
        eventIntegrationService.stopAll()
    }
}
