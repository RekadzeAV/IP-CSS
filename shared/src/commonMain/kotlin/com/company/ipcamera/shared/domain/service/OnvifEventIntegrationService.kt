package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.network.onvif.OnvifEvent
import com.company.ipcamera.core.network.onvif.OnvifEventService
import com.company.ipcamera.core.network.onvif.OnvifEventSubscription
import com.company.ipcamera.core.network.onvif.OnvifEventType
import com.company.ipcamera.shared.common.newRandomId
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.isActive
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Сервис для интеграции ONVIF событий с EventRepository.
 * (Перенесён из core/network для устранения циклической зависимости shared -> core:network.)
 */
class OnvifEventIntegrationService(
    private val eventService: OnvifEventService,
    private val eventRepository: EventRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val activeSubscriptions = mutableMapOf<String, OnvifEventSubscription>()
    private val eventProcessingJobs = mutableMapOf<String, Job>()
    private var isStopped = false

    suspend fun startMonitoring(
        cameraId: String,
        cameraName: String,
        cameraUrl: String,
        username: String? = null,
        password: String? = null,
        pullInterval: Long = 5000,
    ): Result<Unit> =
        withContext(workerDispatcher) {
            try {
                if (activeSubscriptions.containsKey(cameraId)) {
                    logger.warn { "Monitoring already started for camera: $cameraId" }
                    return@withContext Result.success(Unit)
                }
                logger.info { "Starting event monitoring for camera: $cameraId ($cameraName)" }
                val subscriptionResult =
                    eventService.createPullPointSubscription(
                        cameraUrl = cameraUrl,
                        username = username,
                        password = password,
                        subscriptionTime = 3600,
                    )
                val subscription =
                    subscriptionResult.getOrElse { error ->
                        logger.error(error) { "Failed to create PullPoint subscription for camera: $cameraId" }
                        return@withContext Result.failure(error)
                    }
                activeSubscriptions[cameraId] = subscription
                eventService.setSynchronizationPoint(subscription.id)
                val job =
                    scope.launch {
                        processEventsLoop(cameraId, cameraName, subscription.id, pullInterval)
                    }
                eventProcessingJobs[cameraId] = job
                logger.info { "Event monitoring started for camera: $cameraId" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to start event monitoring for camera: $cameraId" }
                Result.failure(e)
            }
        }

    suspend fun stopMonitoring(cameraId: String): Result<Unit> =
        withContext(workerDispatcher) {
            try {
                val subscription = activeSubscriptions.remove(cameraId)
                val job = eventProcessingJobs.remove(cameraId)
                job?.cancel()
                subscription?.let { eventService.unsubscribe(it.id) }
                logger.info { "Event monitoring stopped for camera: $cameraId" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to stop event monitoring for camera: $cameraId" }
                Result.failure(e)
            }
        }

    suspend fun stopAll(): Result<Unit> =
        withContext(workerDispatcher) {
            try {
                isStopped = true
                eventProcessingJobs.values.forEach { it.cancel() }
                eventProcessingJobs.clear()
                activeSubscriptions.values.forEach { subscription ->
                    try {
                        eventService.unsubscribe(subscription.id)
                    } catch (
                        e: Exception,
                    ) {
                        logger.warn(e) { "Failed to unsubscribe: ${subscription.id}" }
                    }
                }
                activeSubscriptions.clear()
                logger.info { "All event monitoring stopped" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "Failed to stop all monitoring" }
                Result.failure(e)
            }
        }

    fun isMonitoring(cameraId: String): Boolean =
        activeSubscriptions.containsKey(cameraId) && eventProcessingJobs[cameraId]?.isActive == true

    fun getMonitoredCameras(): List<String> =
        activeSubscriptions.keys.filter { eventProcessingJobs[it]?.isActive == true }

    private suspend fun processEventsLoop(
        cameraId: String,
        cameraName: String,
        subscriptionId: String,
        pullInterval: Long,
    ) {
        logger.info { "Starting event processing loop for camera: $cameraId" }
        while (!isStopped && currentCoroutineContext().isActive) {
            try {
                activeSubscriptions[cameraId]?.let { checkAndRenewSubscription(cameraId, it) }
                val eventsResult =
                    eventService.pullMessages(
                        subscriptionId = subscriptionId,
                        timeout = pullInterval.coerceAtMost(5000),
                        maxMessages = 10,
                    )
                eventsResult.fold(
                    onSuccess = { onvifEvents ->
                        if (onvifEvents.isNotEmpty()) {
                            logger.debug { "Received ${onvifEvents.size} events from camera: $cameraId" }
                            onvifEvents.forEach { onvifEvent ->
                                try {
                                    processOnvifEvent(cameraId, cameraName, onvifEvent)
                                } catch (e: Exception) {
                                    logger.error(e) { "Failed to process ONVIF event: ${onvifEvent.topic}" }
                                }
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.warn(error) { "Failed to pull messages for camera: $cameraId" }
                        if (error.message?.contains("subscription", ignoreCase = true) == true ||
                            error.message?.contains("expired", ignoreCase = true) == true
                        ) {
                            logger.info { "Subscription may have expired for camera: $cameraId, attempting to renew..." }
                            renewSubscriptionIfNeeded(cameraId, cameraName, subscriptionId)
                        }
                    },
                )
                delay(pullInterval)
            } catch (e: CancellationException) {
                logger.info { "Event processing loop cancelled for camera: $cameraId" }
                break
            } catch (e: Exception) {
                logger.error(e) { "Error in event processing loop for camera: $cameraId" }
                var retryDelay = 1000L
                var retryCount = 0
                val maxRetries = 5
                var reconnected = false
                while (retryCount < maxRetries && currentCoroutineContext().isActive && !isStopped) {
                    try {
                        delay(retryDelay)
                        val subscription = activeSubscriptions[cameraId]
                        if (subscription != null) {
                            val testResult =
                                eventService.pullMessages(
                                    subscriptionId = subscription.id,
                                    timeout = 1000,
                                    maxMessages = 1,
                                )
                            if (testResult.isSuccess) {
                                logger.info { "Reconnected to camera: $cameraId after $retryCount retries" }
                                reconnected = true
                            }
                        }
                        if (reconnected) {
                            retryCount = maxRetries
                        } else {
                            retryCount++
                        }
                        retryDelay *= 2
                    } catch (retryException: Exception) {
                        logger.warn(retryException) { "Retry attempt $retryCount failed for camera: $cameraId" }
                        retryCount++
                        retryDelay *= 2
                    }
                }
                if (retryCount >= maxRetries) {
                    logger.warn {
                        "Failed to reconnect to camera: $cameraId after $maxRetries retries. Monitoring stopped; will be retried on next cycle or when app resumes."
                    }
                    stopMonitoring(cameraId)
                    break
                }
            }
        }
        logger.info { "Event processing loop stopped for camera: $cameraId" }
    }

    private suspend fun processOnvifEvent(
        cameraId: String,
        cameraName: String,
        onvifEvent: OnvifEvent,
    ) {
        try {
            val event = mapOnvifEventToEvent(cameraId, cameraName, onvifEvent)
            eventRepository.addEvent(event).fold(
                onSuccess = { savedEvent ->
                    logger.debug { "Saved ONVIF event: ${onvifEvent.topic} -> ${savedEvent.type} (severity: ${savedEvent.severity})" }
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to save ONVIF event: ${onvifEvent.topic} to repository" }
                },
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to process ONVIF event: ${onvifEvent.topic}" }
        }
    }

    private fun mapOnvifEventToEvent(
        cameraId: String,
        cameraName: String,
        onvifEvent: OnvifEvent,
    ): Event {
        val eventType = mapOnvifTopicToEventType(onvifEvent.topic)
        val severity = mapOnvifTopicToSeverity(onvifEvent.topic)
        val description = buildDescription(onvifEvent)
        val metadata = buildMetadata(onvifEvent)
        return Event(
            id = newRandomId(),
            cameraId = cameraId,
            cameraName = cameraName,
            type = eventType,
            severity = severity,
            timestamp = onvifEvent.timestamp,
            description = description,
            metadata = metadata,
            acknowledged = false,
            acknowledgedAt = null,
            acknowledgedBy = null,
            thumbnailUrl = null,
            videoUrl = null,
        )
    }

    internal fun mapOnvifEventToEventForTesting(
        cameraId: String,
        cameraName: String,
        onvifEvent: OnvifEvent,
    ): Event = mapOnvifEventToEvent(cameraId, cameraName, onvifEvent)

    private fun mapOnvifTopicToEventType(topic: String): EventType =
        when {
            OnvifEventType.isMotionDetection(topic) -> EventType.MOTION_DETECTION
            OnvifEventType.isIntrusionDetection(topic) -> EventType.OBJECT_DETECTION
            topic.contains("Face", ignoreCase = true) -> EventType.FACE_DETECTION
            topic.contains("LicensePlate", ignoreCase = true) || topic.contains("LPR", ignoreCase = true) -> EventType.LICENSE_PLATE_RECOGNITION
            topic.contains("VideoSourceLost", ignoreCase = true) -> EventType.CAMERA_OFFLINE
            topic.contains("VideoSourceRecovered", ignoreCase = true) -> EventType.CAMERA_ONLINE
            topic.contains(
                "Recording",
                ignoreCase = true,
            ) ->
                if (topic.contains(
                        "Start",
                        ignoreCase = true,
                    )
                ) {
                    EventType.RECORDING_STARTED
                } else {
                    EventType.RECORDING_STOPPED
                }
            OnvifEventType.isStorageFull(topic) -> EventType.STORAGE_FULL
            OnvifEventType.isHardwareFailure(topic) -> EventType.SYSTEM_ERROR
            OnvifEventType.isSystemError(topic) -> EventType.SYSTEM_ERROR
            OnvifEventType.isDeviceIoPortState(topic) -> EventType.OTHER
            OnvifEventType.isVideoSourceConfiguration(topic) -> EventType.OTHER
            OnvifEventType.isSystemDateTimeChanged(topic) -> EventType.OTHER
            OnvifEventType.isAnalyticsStream(topic) -> EventType.OTHER
            else -> EventType.OTHER
        }

    private fun mapOnvifTopicToSeverity(topic: String): EventSeverity =
        when {
            OnvifEventType.isMotionDetection(topic) -> EventSeverity.INFO
            OnvifEventType.isLineDetectorCrossed(topic) -> EventSeverity.WARNING
            OnvifEventType.isAlarm(topic) -> EventSeverity.CRITICAL
            topic.contains("Error", ignoreCase = true) || topic.contains("Failed", ignoreCase = true) -> EventSeverity.ERROR
            topic.contains("Warning", ignoreCase = true) || topic.contains("Tamper", ignoreCase = true) -> EventSeverity.WARNING
            OnvifEventType.isVideoSourceLost(topic) || topic.contains("Lost", ignoreCase = true) ||
                topic.contains(
                    "Offline",
                    ignoreCase = true,
                )
            -> EventSeverity.WARNING
            OnvifEventType.isDeviceIoPortState(topic) -> EventSeverity.WARNING
            OnvifEventType.isStorageFull(topic) -> EventSeverity.WARNING
            OnvifEventType.isHardwareFailure(topic) -> EventSeverity.WARNING
            OnvifEventType.isVideoSourceConfiguration(topic) || OnvifEventType.isSystemDateTimeChanged(topic) ||
                OnvifEventType.isAnalyticsStream(
                    topic,
                )
            -> EventSeverity.INFO
            else -> EventSeverity.INFO
        }

    private fun buildDescription(onvifEvent: OnvifEvent): String {
        val topicName = onvifEvent.topic.split("/").lastOrNull() ?: onvifEvent.topic
        val msg: String? = onvifEvent.message
        return if (msg != null && msg.isNotBlank()) "$topicName: $msg" else topicName
    }

    private fun buildMetadata(onvifEvent: OnvifEvent): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        metadata["onvif_topic"] = onvifEvent.topic
        onvifEvent.source?.let { metadata["onvif_source"] = it }
        metadata.putAll(onvifEvent.properties)
        onvifEvent.data?.let { metadata["onvif_data"] = it }
        return metadata
    }

    private suspend fun renewSubscriptionIfNeeded(
        cameraId: String,
        cameraName: String,
        oldSubscriptionId: String,
    ) {
        try {
            logger.warn { "Subscription renewal requires camera URL and credentials. Manual restart of monitoring required." }
            stopMonitoring(cameraId)
        } catch (e: Exception) {
            logger.error(e) { "Failed to renew subscription for camera: $cameraId" }
        }
    }

    private suspend fun checkAndRenewSubscription(
        cameraId: String,
        subscription: OnvifEventSubscription,
    ) {
        try {
            val currentTime = nowMillis()
            val timeUntilExpiry = subscription.expirationTime - currentTime
            if (timeUntilExpiry > 0 && timeUntilExpiry < 5 * 60 * 1000) {
                logger.info { "Subscription for camera $cameraId expires soon, renewing..." }
                eventService.renewSubscription(subscriptionId = subscription.id, renewalTime = 3600).fold(
                    onSuccess = { renewedSubscription ->
                        activeSubscriptions[cameraId] = renewedSubscription
                        logger.info { "Subscription renewed successfully for camera: $cameraId" }
                    },
                    onFailure = {
                            error ->
                        logger.warn(error) { "Failed to renew subscription for camera: $cameraId" }
                    },
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error checking subscription expiry for camera: $cameraId" }
        }
    }
}
