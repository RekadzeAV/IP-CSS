package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.network.onvif.OnvifEvent
import com.company.ipcamera.core.network.onvif.OnvifEventFilter
import com.company.ipcamera.core.network.onvif.OnvifEventProperties
import com.company.ipcamera.core.network.onvif.OnvifEventService
import com.company.ipcamera.core.network.onvif.OnvifEventSubscription
import com.company.ipcamera.core.network.onvif.SubscriptionStatus
import com.company.ipcamera.shared.domain.model.Event
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnvifEventIntegrationLifecycleTest {
    private data class Fixture(
        val fakeOnvif: FakeOnvifEventService,
        val service: OnvifEventIntegrationService,
    )

    private fun TestScope.createFixture(): Fixture {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fakeOnvif = FakeOnvifEventService()
        val service =
            OnvifEventIntegrationService(
                eventService = fakeOnvif,
                eventRepository = FakeEventRepository(),
                scope = backgroundScope,
                workerDispatcher = dispatcher,
            )
        return Fixture(fakeOnvif = fakeOnvif, service = service)
    }

    @Test
    fun `startMonitoring success then isMonitoring and getMonitoredCameras`() =
        runTest {
            val fixture = createFixture()
            fixture.fakeOnvif.createPullPointResult = Result.success(FakeOnvifEventService.subscription("sub-1"))

            val result =
                fixture.service.startMonitoring(
                    cameraId = "cam-1",
                    cameraName = "Camera 1",
                    cameraUrl = "http://192.168.1.100",
                    username = "admin",
                    password = "secret",
                    pullInterval = 60_000,
                )

            assertTrue(result.isSuccess)
            advanceTimeBy(1)
            assertTrue(fixture.service.isMonitoring("cam-1"))
            assertEquals(listOf("cam-1"), fixture.service.getMonitoredCameras())
            fixture.service.stopAll()
        }

    @Test
    fun `startMonitoring when already started returns success without second subscription`() =
        runTest {
            val fixture = createFixture()
            fixture.fakeOnvif.createPullPointResult = Result.success(FakeOnvifEventService.subscription("sub-1"))

            fixture.service.startMonitoring("cam-1", "Cam", "http://192.168.1.100", pullInterval = 60_000)
            advanceTimeBy(1)
            val second = fixture.service.startMonitoring("cam-1", "Cam", "http://192.168.1.100")

            assertTrue(second.isSuccess)
            assertEquals(1, fixture.fakeOnvif.createPullPointCallCount)
            fixture.service.stopAll()
        }

    @Test
    fun `startMonitoring when createPullPointSubscription fails returns failure`() =
        runTest {
            val fixture = createFixture()
            fixture.fakeOnvif.createPullPointResult = Result.failure(RuntimeException("Camera unreachable"))

            val result = fixture.service.startMonitoring("cam-1", "Cam", "http://192.168.1.100")

            assertTrue(result.isFailure)
            assertFalse(fixture.service.isMonitoring("cam-1"))
            fixture.service.stopAll()
        }

    @Test
    fun `stopMonitoring removes subscription and calls unsubscribe`() =
        runTest {
            val fixture = createFixture()
            fixture.fakeOnvif.createPullPointResult = Result.success(FakeOnvifEventService.subscription("sub-1"))
            fixture.service.startMonitoring("cam-1", "Cam", "http://192.168.1.100", pullInterval = 60_000)
            advanceTimeBy(1)

            val result = fixture.service.stopMonitoring("cam-1")

            assertTrue(result.isSuccess)
            assertFalse(fixture.service.isMonitoring("cam-1"))
            assertEquals(1, fixture.fakeOnvif.unsubscribeCallCount)
            assertEquals("sub-1", fixture.fakeOnvif.lastUnsubscribeId)
            fixture.service.stopAll()
        }

    @Test
    fun `stopAll clears all subscriptions and unsubscribes`() =
        runTest {
            val fixture = createFixture()
            fixture.fakeOnvif.createPullPointResult = Result.success(FakeOnvifEventService.subscription("sub-1"))
            fixture.service.startMonitoring("cam-1", "Cam1", "http://192.168.1.100", pullInterval = 60_000)
            advanceTimeBy(1)
            fixture.fakeOnvif.createPullPointResult = Result.success(FakeOnvifEventService.subscription("sub-2"))
            fixture.service.startMonitoring("cam-2", "Cam2", "http://192.168.1.101", pullInterval = 60_000)
            advanceTimeBy(1)

            val result = fixture.service.stopAll()

            assertTrue(result.isSuccess)
            assertFalse(fixture.service.isMonitoring("cam-1"))
            assertFalse(fixture.service.isMonitoring("cam-2"))
            assertEquals(2, fixture.fakeOnvif.unsubscribeCallCount)
        }

    private class FakeOnvifEventService : OnvifEventService {
        var createPullPointResult: Result<OnvifEventSubscription> = Result.success(subscription("default"))
        var createPullPointCallCount = 0
        var unsubscribeCallCount = 0
        var lastUnsubscribeId: String? = null
        var pullMessagesResult: Result<List<OnvifEvent>> = Result.success(emptyList())

        override suspend fun subscribeToEvents(
            cameraUrl: String,
            username: String?,
            password: String?,
            filter: OnvifEventFilter?,
            notificationConsumerUrl: String?,
            subscriptionTime: Long,
        ): Result<OnvifEventSubscription> = Result.success(subscription("sub"))

        override suspend fun unsubscribe(subscriptionId: String): Result<Unit> {
            unsubscribeCallCount++
            lastUnsubscribeId = subscriptionId
            return Result.success(Unit)
        }

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
        ): Result<OnvifEventSubscription> {
            createPullPointCallCount++
            return createPullPointResult
        }

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
        override suspend fun getEvents(
            type: com.company.ipcamera.shared.domain.model.EventType?,
            cameraId: String?,
            severity: com.company.ipcamera.shared.domain.model.EventSeverity?,
            acknowledged: Boolean?,
            startTime: Long?,
            endTime: Long?,
            page: Int,
            limit: Int,
        ): PaginatedResult<Event> = PaginatedResult(emptyList(), 0, page, limit, false)

        override suspend fun getEventById(id: String): Event? = null

        override suspend fun addEvent(event: Event): Result<Event> = Result.success(event)

        override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)

        override suspend fun acknowledgeEvent(
            id: String,
            userId: String,
        ): Result<Event> =
            Result.failure(
                NotImplementedError(),
            )

        override suspend fun acknowledgeEvents(
            ids: List<String>,
            userId: String,
        ): Result<List<Event>> =
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
