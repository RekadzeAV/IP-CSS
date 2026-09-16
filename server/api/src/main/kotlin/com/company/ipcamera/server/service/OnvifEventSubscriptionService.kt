package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.onvif.*
import com.company.ipcamera.server.config.OnvifEventsConfig
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val logger = KotlinLogging.logger {}

/**
 * Сервис для управления подписками ONVIF Event Service
 *
 * Управляет жизненным циклом подписок на события камер,
 * автоматически продлевает подписки и обрабатывает входящие события.
 */
class OnvifEventSubscriptionService(
    private val onvifEventService: OnvifEventService,
    private val eventService: EventService,
    private val cameraRepository: CameraRepository,
    private val config: OnvifEventsConfig? = null
) : KoinComponent {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeSubscriptions = mutableMapOf<String, OnvifEventSubscription>()

    private val subscriptionTimeSeconds: Int get() = config?.subscriptionTimeSeconds ?: 3600
    private val pullTimeoutMs: Int get() = config?.pullTimeoutMs ?: 500
    private val pullIntervalMs: Long get() = config?.pullIntervalMs ?: 5000L
    private val renewalCheckIntervalMs: Long get() = config?.renewalCheckIntervalMs ?: 50 * 60 * 1000L

    /**
     * Подходит ли камера для подписки на ONVIF-события (URL http/https).
     */
    fun supportsOnvifEvents(camera: Camera): Boolean {
        return camera.url.startsWith("http://", ignoreCase = true) ||
            camera.url.startsWith("https://", ignoreCase = true)
    }

    /**
     * При старте API подписать все подходящие камеры на ONVIF-события.
     * Вызывается из Application при config.enabled.
     */
    suspend fun subscribeToAllCamerasAtStartup() {
        if (config?.enabled != true) return
        try {
            val cameras = cameraRepository.getCameras()
            var subscribed = 0
            cameras.forEach { camera ->
                if (supportsOnvifEvents(camera)) {
                    subscribeToCameraEvents(
                        cameraId = camera.id,
                        usePullPoint = config.usePullPointByDefault
                    ).fold(
                        onSuccess = { subscribed++ },
                        onFailure = { e ->
                            logger.warn(e) { "Failed to subscribe to ONVIF events for camera: ${camera.id} (${camera.name})" }
                        }
                    )
                }
            }
            logger.info { "ONVIF: subscribed to $subscribed of ${cameras.size} cameras at startup" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to subscribe to ONVIF cameras at startup" }
        }
    }

    /**
     * Подписаться на события камеры
     */
    suspend fun subscribeToCameraEvents(
        cameraId: String,
        usePullPoint: Boolean = false
    ): Result<Unit> {
        return try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return Result.failure(IllegalArgumentException("Camera not found: $cameraId"))

            val cameraUrl = camera.url
            val username = camera.username
            val password = camera.password

            // Создать фильтр для важных событий (опционально)
            val filter = OnvifEventFilter(
                includedTopics = listOf(
                    OnvifEventType.MOTION_ALARM,
                    OnvifEventType.INTRUSION_DETECTOR,
                    OnvifEventType.DEVICE_TAMPER_DETECTED,
                    OnvifEventType.DEVICE_IO_PORT_STATE
                )
            )

            val subscriptionTime = subscriptionTimeSeconds.toLong()

            val subscriptionResult = if (usePullPoint) {
                onvifEventService.createPullPointSubscription(
                    cameraUrl = cameraUrl,
                    username = username,
                    password = password,
                    filter = filter,
                    subscriptionTime = subscriptionTime
                )
            } else {
                onvifEventService.subscribeToEvents(
                    cameraUrl = cameraUrl,
                    username = username,
                    password = password,
                    filter = filter,
                    subscriptionTime = subscriptionTime
                )
            }

            subscriptionResult.fold(
                onSuccess = { subscription ->
                    activeSubscriptions["$cameraId:${subscription.id}"] = subscription

                    // Запустить фоновую задачу для продления подписки
                    scope.launch {
                        startSubscriptionRenewalTask(subscription.id)
                    }

                    // Если используется PullPoint, запустить задачу для периодического получения событий
                    if (usePullPoint) {
                        // Сразу выставляем sync-point для получения актуального состояния аналитики/датчиков.
                        onvifEventService.setSynchronizationPoint(subscription.id).onFailure { error ->
                            logger.warn(error) { "Failed to set synchronization point for subscription: ${subscription.id}" }
                        }
                        scope.launch {
                            startPullMessagesTask(cameraId, subscription.id)
                        }
                    }

                    logger.info { "Subscribed to events for camera: $cameraId, subscription ID: ${subscription.id}" }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to subscribe to events for camera: $cameraId" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error subscribing to camera events: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Отписаться от событий камеры
     */
    suspend fun unsubscribeFromCameraEvents(cameraId: String): Result<Unit> {
        return try {
            val subscriptionsToRemove = activeSubscriptions.filter {
                it.key.startsWith("$cameraId:")
            }

            val results = subscriptionsToRemove.map { (key, subscription) ->
                onvifEventService.unsubscribe(subscription.id).fold(
                    onSuccess = {
                        activeSubscriptions.remove(key)
                        Unit
                    },
                    onFailure = { error ->
                        logger.warn(error) { "Failed to unsubscribe: ${subscription.id}" }
                        error
                    }
                )
            }

            val failures = results.filterIsInstance<Exception>()
            if (failures.isNotEmpty()) {
                Result.failure(failures.first())
            } else {
                logger.info { "Unsubscribed from all events for camera: $cameraId" }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error unsubscribing from camera events: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Обработать входящее событие от камеры
     */
    suspend fun handleIncomingEvent(
        notificationMessage: String,
        subscriptionId: String? = null
    ): Result<Unit> {
        return try {
            // Парсить NotificationMessage
            val onvifEventResult = OnvifEventParser.parseNotificationMessage(notificationMessage)

            onvifEventResult.fold(
                onSuccess = { onvifEvent ->
                    // Найти камеру по subscription ID или по source URL
                    val cameraId = findCameraIdBySubscription(subscriptionId, onvifEvent.source)
                        ?: return Result.failure(IllegalStateException("Camera not found for subscription: $subscriptionId"))

                    // Преобразовать в доменное событие
                    val camera = cameraRepository.getCameraById(cameraId)
                    val domainEvent = OnvifEventMapper.mapToDomainEvent(
                        onvifEvent = onvifEvent,
                        cameraId = cameraId,
                        cameraName = camera?.name
                    )

                    // Создать событие через EventService
                    eventService.createEvent(
                        cameraId = domainEvent.cameraId,
                        cameraName = domainEvent.cameraName,
                        type = domainEvent.type,
                        severity = domainEvent.severity,
                        description = domainEvent.description,
                        metadata = domainEvent.metadata,
                        thumbnailUrl = domainEvent.thumbnailUrl,
                        videoUrl = domainEvent.videoUrl
                    )

                    logger.info { "Processed ONVIF event: ${onvifEvent.topic} for camera: $cameraId" }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to parse notification message" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error handling incoming event" }
            Result.failure(e)
        }
    }

    /**
     * Фоновая задача для продления подписок
     */
    private suspend fun startSubscriptionRenewalTask(subscriptionId: String) {
        while (currentCoroutineContext().isActive) {
            try {
                delay(renewalCheckIntervalMs)

                val subscription = activeSubscriptions.values.find { it.id == subscriptionId }
                    ?: break

                if (subscription.needsRenewal()) {
                    val renewResult = onvifEventService.renewSubscription(subscriptionId, subscriptionTimeSeconds.toLong())
                    renewResult.fold(
                        onSuccess = { renewed ->
                            // Сохраняем тот же ключ (cameraId:subId), чтобы отписка по cameraId работала
                            val key = activeSubscriptions.entries.find { it.value.id == subscriptionId }?.key
                            if (key != null) {
                                activeSubscriptions[key] = renewed
                            }
                            logger.info { "Renewed subscription: $subscriptionId" }
                        },
                        onFailure = { error ->
                            logger.error(error) { "Failed to renew subscription: $subscriptionId" }
                        }
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Error in subscription renewal task for: $subscriptionId" }
                delay(5 * 60 * 1000) // Повторить через 5 минут при ошибке
            }
        }
    }

    /**
     * Фоновая задача для получения событий через PullPoint
     */
    private suspend fun startPullMessagesTask(cameraId: String, subscriptionId: String) {
        while (currentCoroutineContext().isActive) {
            try {
                delay(pullIntervalMs)

                val pullResult = onvifEventService.pullMessages(
                    subscriptionId = subscriptionId,
                    timeout = pullTimeoutMs.toLong(),
                    maxMessages = 10
                )

                pullResult.fold(
                    onSuccess = { events ->
                        events.forEach { onvifEvent ->
                            // Найти камеру по subscription
                            val subscription = activeSubscriptions.values.find { it.id == subscriptionId }
                            if (subscription != null) {
                                val cameras = cameraRepository.getCameras()
                                val camera = cameras.find { it.url == subscription.cameraUrl }

                                if (camera != null) {
                                    // Преобразовать в доменное событие
                                    val domainEvent = OnvifEventMapper.mapToDomainEvent(
                                        onvifEvent = onvifEvent,
                                        cameraId = camera.id,
                                        cameraName = camera.name
                                    )

                                    // Создать событие через EventService
                                    eventService.createEvent(
                                        cameraId = domainEvent.cameraId,
                                        cameraName = domainEvent.cameraName,
                                        type = domainEvent.type,
                                        severity = domainEvent.severity,
                                        description = domainEvent.description,
                                        metadata = domainEvent.metadata,
                                        thumbnailUrl = domainEvent.thumbnailUrl,
                                        videoUrl = domainEvent.videoUrl
                                    )

                                    logger.debug { "Processed PullPoint event: ${onvifEvent.topic} for camera: ${camera.id}" }
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        // Игнорируем ошибки таймаута (это нормально для PullPoint)
                        val isTimeout = error.message?.contains("timeout", ignoreCase = true) == true
                        if (!isTimeout) {
                            logger.warn(error) { "Error pulling messages for subscription: $subscriptionId" }
                        }
                    }
                )
            } catch (e: Exception) {
                logger.error(e) { "Error in pull messages task for: $subscriptionId" }
                delay(5 * 1000) // Повторить через 5 секунд при ошибке
            }
        }
    }

    /**
     * Найти ID камеры по subscription ID или source URL
     */
    private suspend fun findCameraIdBySubscription(
        subscriptionId: String?,
        sourceUrl: String?
    ): String? {
        // Поиск по subscription ID
        subscriptionId?.let { id ->
            val subscription = activeSubscriptions.values.find { it.id == id }
            if (subscription != null) {
                val cameras = cameraRepository.getCameras()
                return cameras.find { it.url == subscription.cameraUrl }?.id
            }
        }

        // Поиск по source URL
        sourceUrl?.let { url ->
            val cameras = cameraRepository.getCameras()
            return cameras.find { camera ->
                url.contains(camera.url, ignoreCase = true) ||
                camera.url.contains(url, ignoreCase = true)
            }?.id
        }

        return null
    }

    /**
     * Остановить все подписки и фоновые задачи
     */
    fun stop() {
        scope.cancel()
        activeSubscriptions.clear()
    }
}
