package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.network.onvif.OnvifEvent
import com.company.ipcamera.core.network.onvif.OnvifEventFilter
import com.company.ipcamera.core.network.onvif.OnvifEventProperties
import com.company.ipcamera.core.network.onvif.OnvifEventService
import com.company.ipcamera.core.network.onvif.OnvifEventSubscription
import com.company.ipcamera.core.network.onvif.SubscriptionStatus
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlin.test.Test
import kotlin.test.assertEquals

class OnvifEventMappingTest {
    private val service =
        OnvifEventIntegrationService(
            eventService = NoopOnvifEventService(),
            eventRepository = NoopEventRepository(),
        )

    @Test
    fun `maps system error topic to SYSTEM_ERROR and ERROR`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:System/SystemError",
                        timestamp = 123L,
                        message = "Error happened",
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.SYSTEM_ERROR, mapped.type)
        assertEquals(EventSeverity.ERROR, mapped.severity)
    }

    @Test
    fun `maps video source recovered topic to CAMERA_ONLINE and INFO`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:Device/VideoSourceRecovered",
                        timestamp = 123L,
                        message = null,
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.CAMERA_ONLINE, mapped.type)
        assertEquals(EventSeverity.INFO, mapped.severity)
    }

    @Test
    fun `maps storage failure topic to STORAGE_FULL and WARNING`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:Device/StorageFailure",
                        timestamp = 123L,
                        message = "Storage full",
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.STORAGE_FULL, mapped.type)
        assertEquals(EventSeverity.WARNING, mapped.severity)
    }

    @Test
    fun `maps intrusion detector topic to OBJECT_DETECTION and CRITICAL`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:RuleEngine/IntrusionDetector",
                        timestamp = 100L,
                        message = "Intrusion",
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.OBJECT_DETECTION, mapped.type)
        assertEquals(EventSeverity.CRITICAL, mapped.severity)
    }

    @Test
    fun `maps line detector crossed topic to OBJECT_DETECTION and WARNING`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:RuleEngine/LineDetector/Crossed",
                        timestamp = 101L,
                        message = null,
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.OBJECT_DETECTION, mapped.type)
        assertEquals(EventSeverity.WARNING, mapped.severity)
    }

    @Test
    fun `maps recording started topic to RECORDING_STARTED and INFO`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:RecordingConfig/Recording/Started",
                        timestamp = 102L,
                        message = null,
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.RECORDING_STARTED, mapped.type)
        assertEquals(EventSeverity.INFO, mapped.severity)
    }

    @Test
    fun `maps recording stopped topic to RECORDING_STOPPED and INFO`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:Recording/Stopped",
                        timestamp = 103L,
                        message = null,
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.RECORDING_STOPPED, mapped.type)
        assertEquals(EventSeverity.INFO, mapped.severity)
    }

    @Test
    fun `maps hardware failure topic to SYSTEM_ERROR and WARNING`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = com.company.ipcamera.core.network.onvif.OnvifEventType.DEVICE_HARDWARE_FAILURE,
                        timestamp = 104L,
                        message = "Hardware failure",
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.SYSTEM_ERROR, mapped.type)
        assertEquals(EventSeverity.WARNING, mapped.severity)
    }

    @Test
    fun `maps system date time changed topic to OTHER and INFO`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = com.company.ipcamera.core.network.onvif.OnvifEventType.SYSTEM_DATE_TIME_CHANGED,
                        timestamp = 105L,
                        message = null,
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.OTHER, mapped.type)
        assertEquals(EventSeverity.INFO, mapped.severity)
    }

    @Test
    fun `maps io port state topic to OTHER and WARNING`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = com.company.ipcamera.core.network.onvif.OnvifEventType.DEVICE_IO_PORT_STATE,
                        timestamp = 106L,
                        message = null,
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.OTHER, mapped.type)
        assertEquals(EventSeverity.WARNING, mapped.severity)
    }

    @Test
    fun `maps unknown topic to OTHER and INFO`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:Custom/UnknownTopic",
                        timestamp = 107L,
                        message = "Custom",
                        properties = emptyMap(),
                    ),
            )

        assertEquals(EventType.OTHER, mapped.type)
        assertEquals(EventSeverity.INFO, mapped.severity)
    }

    @Test
    fun `maps description and metadata fields from ONVIF event`() {
        val mapped =
            service.mapOnvifEventToEventForTesting(
                cameraId = "cam-1",
                cameraName = "Cam 1",
                onvifEvent =
                    OnvifEvent(
                        topic = "tns1:VideoSource/MotionAlarm",
                        timestamp = 108L,
                        message = "Custom motion text",
                        properties = mapOf("State" to "true"),
                        source = "http://192.168.1.100/onvif",
                    ),
            )

        assertEquals("MotionAlarm: Custom motion text", mapped.description)
        assertEquals("tns1:VideoSource/MotionAlarm", mapped.metadata["onvif_topic"])
        assertEquals("http://192.168.1.100/onvif", mapped.metadata["onvif_source"])
        assertEquals("true", mapped.metadata["State"])
    }

    private class NoopOnvifEventService : OnvifEventService {
        override suspend fun subscribeToEvents(
            cameraUrl: String,
            username: String?,
            password: String?,
            filter: OnvifEventFilter?,
            notificationConsumerUrl: String?,
            subscriptionTime: Long,
        ): Result<OnvifEventSubscription> = Result.success(dummySub())

        override suspend fun unsubscribe(subscriptionId: String): Result<Unit> = Result.success(Unit)

        override suspend fun renewSubscription(
            subscriptionId: String,
            renewalTime: Long,
        ): Result<OnvifEventSubscription> = Result.success(dummySub())

        override suspend fun getEventProperties(
            cameraUrl: String,
            username: String?,
            password: String?,
        ): Result<OnvifEventProperties> =
            Result.success(
                OnvifEventProperties(
                    eventServiceUrl = "$cameraUrl/onvif/events",
                    supportedTopics = emptyList(),
                    supportsPullPoint = true,
                    supportsPush = false,
                ),
            )

        override suspend fun getSubscription(subscriptionId: String): OnvifEventSubscription? = dummySub()

        override suspend fun getSubscriptionsForCamera(cameraUrl: String): List<OnvifEventSubscription> = emptyList()

        override suspend fun unsubscribeAll(cameraUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun createPullPointSubscription(
            cameraUrl: String,
            username: String?,
            password: String?,
            filter: OnvifEventFilter?,
            subscriptionTime: Long,
        ): Result<OnvifEventSubscription> = Result.success(dummySub())

        override suspend fun pullMessages(
            subscriptionId: String,
            timeout: Long,
            maxMessages: Int,
        ): Result<List<OnvifEvent>> = Result.success(emptyList())

        override suspend fun setSynchronizationPoint(subscriptionId: String): Result<Unit> = Result.success(Unit)

        private fun dummySub() =
            OnvifEventSubscription(
                id = "sub",
                cameraUrl = "http://localhost",
                notificationConsumerUrl = "http://localhost/events",
                subscriptionReference = "http://localhost/sub",
                expirationTime = System.currentTimeMillis() + 60_000,
                createdAt = System.currentTimeMillis(),
                status = SubscriptionStatus.ACTIVE,
            )
    }

    private class NoopEventRepository : EventRepository {
        override suspend fun getEvents(
            type: EventType?,
            cameraId: String?,
            severity: EventSeverity?,
            acknowledged: Boolean?,
            startTime: Long?,
            endTime: Long?,
            page: Int,
            limit: Int,
        ): PaginatedResult<com.company.ipcamera.shared.domain.model.Event> =
            PaginatedResult(emptyList(), 0, page, limit, false)

        override suspend fun getEventById(id: String): com.company.ipcamera.shared.domain.model.Event? = null

        override suspend fun addEvent(
            event: com.company.ipcamera.shared.domain.model.Event,
        ): Result<com.company.ipcamera.shared.domain.model.Event> =
            Result.success(
                event,
            )

        override suspend fun updateEvent(
            event: com.company.ipcamera.shared.domain.model.Event,
        ): Result<com.company.ipcamera.shared.domain.model.Event> =
            Result.success(
                event,
            )

        override suspend fun acknowledgeEvent(
            id: String,
            userId: String,
        ): Result<com.company.ipcamera.shared.domain.model.Event> =
            Result.failure(
                NotImplementedError(),
            )

        override suspend fun acknowledgeEvents(
            ids: List<String>,
            userId: String,
        ): Result<List<com.company.ipcamera.shared.domain.model.Event>> =
            Result.failure(
                NotImplementedError(),
            )

        override suspend fun deleteEvent(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun getEventStatistics(
            cameraId: String?,
            startTime: Long?,
            endTime: Long?,
        ): Result<Map<String, Any>> = Result.success(emptyMap())
    }
}
