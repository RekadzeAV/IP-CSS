package com.company.ipcamera.server.service

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Сервис для управления камерами
 *
 * Управляет мониторингом статуса камер, проверкой их доступности,
 * обновлением статусов и отправкой уведомлений о проблемах.
 */
class CameraService(
    private val cameraRepository: CameraRepository,
    private val notificationService: NotificationService? = null,
    private val eventService: EventService? = null,
    private val monitoringIntervalMinutes: Long = 5, // Проверка каждые 5 минут
    private val offlineThresholdMinutes: Long = 10 // Считается offline после 10 минут недоступности
) {
    private val monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lastStatusCheck = ConcurrentHashMap<String, Long>()
    private val lastOnlineTime = ConcurrentHashMap<String, Long>()
    private var monitoringJob: Job? = null

    /**
     * Начать мониторинг камер
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) {
            logger.warn { "Camera monitoring already started" }
            return
        }

        monitoringJob = monitoringScope.launch {
            logger.info { "Started camera monitoring (interval: ${monitoringIntervalMinutes} minutes)" }

            while (isActive) {
                try {
                    checkAllCamerasStatus()
                    delay(monitoringIntervalMinutes * 60 * 1000) // Конвертируем минуты в миллисекунды
                } catch (e: CancellationException) {
                    logger.info { "Camera monitoring cancelled" }
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Error in camera monitoring loop" }
                    delay(60000) // Ждем 1 минуту перед повтором при ошибке
                }
            }
        }
    }

    /**
     * Остановить мониторинг камер
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        logger.info { "Stopped camera monitoring" }
    }

    /**
     * Проверить статус всех камер
     */
    suspend fun checkAllCamerasStatus() = withContext(Dispatchers.IO) {
        try {
            val cameras = cameraRepository.getCameras()
            logger.debug { "Checking status for ${cameras.size} cameras" }

            cameras.forEach { camera ->
                try {
                    checkCameraStatus(camera)
                } catch (e: Exception) {
                    logger.error(e) { "Error checking status for camera: ${camera.id}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error checking cameras status" }
        }
    }

    /**
     * Проверить статус конкретной камеры
     *
     * @param cameraId ID камеры
     * @return обновленная камера или null, если камера не найдена
     */
    suspend fun checkCameraStatus(cameraId: String): Camera? = withContext(Dispatchers.IO) {
        val camera = cameraRepository.getCameraById(cameraId)
        if (camera != null) {
            checkCameraStatus(camera)
            cameraRepository.getCameraById(cameraId)
        } else {
            logger.warn { "Camera not found: $cameraId" }
            null
        }
    }

    /**
     * Проверить статус камеры
     */
    private suspend fun checkCameraStatus(camera: Camera) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            lastStatusCheck[camera.id] = currentTime

            // Проверяем подключение к камере
            val testResult = cameraRepository.testConnection(camera)

            val newStatus = when (testResult) {
                is ConnectionTestResult.Success -> {
                    lastOnlineTime[camera.id] = currentTime
                    CameraStatus.ONLINE
                }
                is ConnectionTestResult.Failure -> {
                    // Проверяем, как долго камера была недоступна
                    val lastOnline = lastOnlineTime[camera.id] ?: camera.lastSeen ?: 0L
                    val offlineDuration = currentTime - lastOnline

                    if (offlineDuration > offlineThresholdMinutes * 60 * 1000) {
                        CameraStatus.OFFLINE
                    } else {
                        CameraStatus.ERROR
                    }
                }
            }

            // Обновляем статус камеры, если он изменился
            val oldStatus = camera.status
            if (oldStatus != newStatus) {
                updateCameraStatus(camera, newStatus, oldStatus)
            } else if (newStatus == CameraStatus.ONLINE) {
                // Обновляем lastSeen даже если статус не изменился
                val updatedCamera = camera.copy(
                    lastSeen = currentTime,
                    updatedAt = currentTime
                )
                cameraRepository.updateCamera(updatedCamera)
            }

        } catch (e: Exception) {
            logger.error(e) { "Error checking camera status: ${camera.id}" }

            // При ошибке проверки обновляем статус на ERROR
            if (camera.status != CameraStatus.ERROR) {
                updateCameraStatus(camera, CameraStatus.ERROR, camera.status)
            }
        }
    }

    /**
     * Обновить статус камеры
     */
    private suspend fun updateCameraStatus(
        camera: Camera,
        newStatus: CameraStatus,
        oldStatus: CameraStatus
    ) = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val updatedCamera = camera.copy(
                status = newStatus,
                lastSeen = if (newStatus == CameraStatus.ONLINE) currentTime else camera.lastSeen,
                updatedAt = currentTime
            )

            val result = cameraRepository.updateCamera(updatedCamera)
            result.fold(
                onSuccess = { updated ->
                    logger.info {
                        "Camera status updated: ${camera.name} (${camera.id}) " +
                        "from $oldStatus to $newStatus"
                    }

                    // Отправляем WebSocket событие об изменении статуса
                    sendStatusChangeWebSocketEvent(camera, oldStatus, newStatus)

                    // Создаем событие об изменении статуса камеры
                    createCameraStatusEvent(updated, oldStatus, newStatus)

                    // Отправляем уведомление при проблемах с камерой
                    if (newStatus == CameraStatus.OFFLINE || newStatus == CameraStatus.ERROR) {
                        sendCameraStatusNotification(updated, oldStatus, newStatus)
                    }
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to update camera status: ${camera.id}" }
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error updating camera status: ${camera.id}" }
        }
    }

    /**
     * Отправить WebSocket событие об изменении статуса камеры
     */
    private fun sendStatusChangeWebSocketEvent(
        camera: Camera,
        oldStatus: CameraStatus,
        newStatus: CameraStatus
    ) {
        try {
            WebSocketManager.broadcastEvent(
                WebSocketChannel.CAMERAS,
                "camera_status_changed",
                jsonObject {
                    put("cameraId", camera.id)
                    put("cameraName", camera.name)
                    put("oldStatus", oldStatus.name)
                    put("newStatus", newStatus.name)
                    put("timestamp", System.currentTimeMillis())
                }
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to send WebSocket event for camera status change" }
        }
    }

    /**
     * Создать событие об изменении статуса камеры
     */
    private suspend fun createCameraStatusEvent(
        camera: Camera,
        oldStatus: CameraStatus,
        newStatus: CameraStatus
    ) {
        try {
            eventService?.let { service ->
                when (newStatus) {
                    CameraStatus.OFFLINE -> {
                        service.createCameraOfflineEvent(
                            cameraId = camera.id,
                            cameraName = camera.name,
                            description = "Камера недоступна (была: ${oldStatus.name})"
                        )
                    }
                    CameraStatus.ONLINE -> {
                        if (oldStatus == CameraStatus.OFFLINE || oldStatus == CameraStatus.ERROR) {
                            service.createCameraOnlineEvent(
                                cameraId = camera.id,
                                cameraName = camera.name,
                                description = "Камера восстановлена (была: ${oldStatus.name})"
                            )
                        }
                    }
                    CameraStatus.ERROR -> {
                        service.createSystemErrorEvent(
                            error = "Ошибка подключения к камере '${camera.name}'",
                            cameraId = camera.id,
                            cameraName = camera.name
                        )
                    }
                    else -> {
                        // Не создаем события для других статусов
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating camera status event" }
        }
    }

    /**
     * Отправить уведомление об изменении статуса камеры
     */
    private suspend fun sendCameraStatusNotification(
        camera: Camera,
        oldStatus: CameraStatus,
        newStatus: CameraStatus
    ) {
        try {
            notificationService?.let { service ->
                when (newStatus) {
                    CameraStatus.OFFLINE -> {
                        service.sendWarningNotification(
                            title = "Камера недоступна",
                            message = "Камера '${camera.name}' недоступна (OFFLINE)",
                            cameraId = camera.id
                        )
                    }
                    CameraStatus.ERROR -> {
                        service.sendErrorNotification(
                            title = "Ошибка камеры",
                            message = "Обнаружена ошибка при подключении к камере '${camera.name}'",
                            cameraId = camera.id
                        )
                    }
                    CameraStatus.ONLINE -> {
                        if (oldStatus == CameraStatus.OFFLINE || oldStatus == CameraStatus.ERROR) {
                            service.sendNotification(
                                title = "Камера восстановлена",
                                message = "Камера '${camera.name}' снова доступна",
                                type = NotificationType.INFO,
                                cameraId = camera.id
                            )
                        }
                    }
                    else -> {
                        // Не отправляем уведомления для других статусов
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error sending camera status notification" }
        }
    }

    /**
     * Получить статистику мониторинга камер
     */
    fun getMonitoringStats(): MonitoringStats {
        val cameras = runBlocking { cameraRepository.getCameras() }
        val stats = cameras.groupingBy { it.status }.eachCount()

        return MonitoringStats(
            totalCameras = cameras.size,
            onlineCameras = stats.getOrDefault(CameraStatus.ONLINE, 0),
            offlineCameras = stats.getOrDefault(CameraStatus.OFFLINE, 0),
            errorCameras = stats.getOrDefault(CameraStatus.ERROR, 0),
            unknownCameras = stats.getOrDefault(CameraStatus.UNKNOWN, 0),
            isMonitoringActive = monitoringJob?.isActive == true
        )
    }

    /**
     * Закрыть сервис и освободить ресурсы
     */
    fun close() {
        stopMonitoring()
        monitoringScope.cancel()
        lastStatusCheck.clear()
        lastOnlineTime.clear()
    }

    /**
     * Статистика мониторинга камер
     */
    data class MonitoringStats(
        val totalCameras: Int,
        val onlineCameras: Int,
        val offlineCameras: Int,
        val errorCameras: Int,
        val unknownCameras: Int,
        val isMonitoringActive: Boolean
    )
}

