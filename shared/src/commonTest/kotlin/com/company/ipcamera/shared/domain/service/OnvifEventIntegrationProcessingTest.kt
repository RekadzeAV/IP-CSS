package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.network.onvif.OnvifEvent
import com.company.ipcamera.core.network.onvif.OnvifEventFilter
import com.company.ipcamera.core.network.onvif.OnvifEventProperties
import com.company.ipcamera.core.network.onvif.OnvifEventService
import com.company.ipcamera.core.network.onvif.OnvifEventSubscription
import com.company.ipcamera.core.network.onvif.SubscriptionStatus
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnvifEventIntegrationProcessingTest {
    private data class Fixture(
        val fakeOnvif: FakeOnvifEventService,
        val fakeEventRepo: FakeEventRepository,
        val service: OnvifEventIntegrationService,
    )

    private fun TestScope.createFixture(): Fixture {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fakeOnvif = FakeOnvifEventService()
        val fakeEventRepo = FakeEventRepository()
        val service =
            OnvifEventIntegrationService(
                eventService = fakeOnvif,
                eventRepository = fakeEventRepo,
                scope = backgroundScope,
                workerDispatcher = dispatcher,
            )
        return Fixture(fakeOnvif, fakeEventRepo, service)
    }

    @Test
    fun `received ONVIF event is mapped and saved to repository`() =
        runTest {
            val fixture = createFixture()
            fixture.fakeOnvif.createPullPointResult = Result.success(FakeOnvifEventService.subscription("sub-1"))
            fixture.fakeOnvif.pullMessagesResult =
                Result.success(
                    listOf(
                        OnvifEvent(
                            topic = "tns1:VideoSource/MotionAlarm",
                            timestamp = 12345L,
                            message = "Motion",
                            properties = mapOf("k" to "v"),
                        ),
                    ),
                )

            fixture.service.startMonitoring("cam-1", "Camera 1", "http://192.168.1.100", pullInterval = 60_000)
            advanceTimeBy(1)
            advanceTimeBy(1)
            advanceTimeBy(1)

            assertTrue(fixture.fakeEventRepo.addEventCalls.isNotEmpty())
            val saved = fixture.fakeEventRepo.addEventCalls.last().getOrNull()
            assertNotNull(saved)
            assertEquals("cam-1", saved.cameraId)
            assertEquals("Camera 1", saved.cameraName)
            assertEquals(EventType.MOTION_DETECTION, saved.type)
            assertEquals(EventSeverity.INFO, saved.severity)
            assertEquals(12345L, saved.timestamp)
            assertEquals("tns1:VideoSource/MotionAlarm", saved.metadata["onvif_topic"])
            fixture.service.stopAll()
        }

    @Test
    fun `pullMessages failure does not stop monitoring and stopMonitoring still works`() =
        runTest {
            val fixture = createFixture()
            fixture.fakeOnvif.createPullPointResult = Result.success(FakeOnvifEventService.subscription("sub-1"))
            fixture.fakeOnvif.pullMessagesResult = Result.failure(RuntimeException("Network timeout"))

            fixture.service.startMonitoring("cam-1", "Cam", "http://192.168.1.100", pullInterval = 60_000)
            advanceTimeBy(1)
            advanceTimeBy(1)
            advanceTimeBy(1)

            assertTrue(fixture.service.isMonitoring("cam-1"))
            val stopResult = fixture.service.stopMonitoring("cam-1")
            assertTrue(stopResult.isSuccess)
            assertFalse(fixture.service.isMonitoring("cam-1"))
            fixture.service.stopAll()
        }

    private class FakeOnvifEventService : OnvifEventService {
        var createPullPointResult: Result<OnvifEventSubscription> = Result.success(subscription("default"))
        var pullMessagesResult: Result<List<OnvifEvent>> = Result.success(emptyList())

        override suspend fun subscribeToEvents(
            cameraUrl: String,
            username: String?,
            password: String?,
            filter: OnvifEventFilter?,
            notificationConsumerUrl: String?,
            subscriptionTime: Long,
        ): Result<OnvifEventSubscription> = Result.success(subscription("sub"))

        override suspend fun unsubscribe(subscriptionId: String): Result<Unit> = Result.success(Unit)

        override suspend fun renewSubscription(
            subscriptionId: String,
            renewalTime: Long,
        ): Result<OnvifEventSubscription> = Result.success(subscription(subscriptionId))

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

        override suspend fun getSubscription(subscriptionId: String): OnvifEventSubscription? =
            subscription(subscriptionId)

        override suspend fun getSubscriptionsForCamera(cameraUrl: String): List<OnvifEventSubscription> = emptyList()

        override suspend fun unsubscribeAll(cameraUrl: String): Result<Unit> = Result.success(Unit)

        override suspend fun createPullPointSubscription(
            cameraUrl: String,
            username: String?,
            password: String?,
            filter: OnvifEventFilter?,
            subscriptionTime: Long,
        ): Result<OnvifEventSubscription> = createPullPointResult

        override suspend fun pullMessages(
            subscriptionId: String,
            timeout: Long,
            maxMessages: Int,
        ): Result<List<OnvifEvent>> {
            val current = pullMessagesResult
            pullMessagesResult = Result.success(emptyList())
            return current
        }

        override suspend fun setSynchronizationPoint(subscriptionId: String): Result<Unit> = Result.success(Unit)

        companion object {
            fun subscription(id: String) =
                OnvifEventSubscription(
                    id = id,
                    cameraUrl = "http://192.168.1.100",
                    notificationConsumerUrl = "http://localhost/events",
                    subscriptionReference = "http://192.168.1.100/sub",
                    expirationTime = System.currentTimeMillis() + 3600_000,
                    createdAt = System.currentTimeMillis(),
                    status = SubscriptionStatus.ACTIVE,
                )
        }
    }

    private class FakeEventRepository : EventRepository {
        val addEventCalls = mutableListOf<Result<Event>>()

        override suspend fun getEvents(
            type: EventType?,
            cameraId: String?,
            severity: EventSeverity?,
            acknowledged: Boolean?,
            startTime: Long?,
            endTime: Long?,
            page: Int,
            limit: Int,
        ): PaginatedResult<Event> = PaginatedResult(emptyList(), 0, page, limit, false)

        override suspend fun getEventById(id: String): Event? = null

        override suspend fun addEvent(event: Event): Result<Event> {
            addEventCalls.add(Result.success(event))
            return Result.success(event)
        }

        override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)

        override suspend fun acknowledgeEvent(
            id: String,
            userId: String,
        ): Result<Event> = Result.failure(NotImplementedError())

        override suspend fun acknowledgeEvents(
            ids: List<String>,
            userId: String,
        ): Result<List<Event>> = Result.failure(NotImplementedError())

        override suspend fun deleteEvent(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun getEventStatistics(
            cameraId: String?,
            startTime: Long?,
            endTime: Long?,
        ): Result<Map<String, Any>> = Result.success(emptyMap())
    }
}
