package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.onvif.OnvifEventSubscription
import com.company.ipcamera.core.network.onvif.OnvifEventType
import com.company.ipcamera.core.network.onvif.SubscriptionStatus
import com.company.ipcamera.server.config.OnvifEventsConfig
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Юнит-тесты подписки/отписки OnvifEventSubscriptionService по cameraId.
 */
class OnvifEventSubscriptionServiceTest {

    private lateinit var onvifEventService: com.company.ipcamera.core.network.onvif.OnvifEventService
    private lateinit var eventService: EventService
    private lateinit var cameraRepository: CameraRepository
    private lateinit var config: OnvifEventsConfig
    private lateinit var subscriptionService: OnvifEventSubscriptionService

    private val cameraId = "cam-1"
    private val camera = Camera(
        id = cameraId,
        name = "Test Camera",
        url = "http://192.168.1.100",
        username = "admin",
        password = "secret"
    )

    private fun subscription(subId: String = "sub-123") = OnvifEventSubscription(
        id = subId,
        cameraUrl = camera.url,
        notificationConsumerUrl = "http://localhost/events",
        subscriptionReference = "http://192.168.1.100/sub",
        expirationTime = System.currentTimeMillis() + 3600_000,
        createdAt = System.currentTimeMillis(),
        status = SubscriptionStatus.ACTIVE
    )

    @BeforeEach
    fun setUp() {
        onvifEventService = mockk(relaxed = true)
        eventService = mockk(relaxed = true)
        cameraRepository = mockk(relaxed = true)
        config = OnvifEventsConfig(enabled = true, usePullPointByDefault = false)
        subscriptionService = OnvifEventSubscriptionService(
            onvifEventService = onvifEventService,
            eventService = eventService,
            cameraRepository = cameraRepository,
            config = config
        )
    }

    @Test
    fun `supportsOnvifEvents returns true for http url`() {
        assertTrue(subscriptionService.supportsOnvifEvents(camera))
    }

    @Test
    fun `supportsOnvifEvents returns true for https url`() {
        val httpsCamera = camera.copy(url = "https://192.168.1.100")
        assertTrue(subscriptionService.supportsOnvifEvents(httpsCamera))
    }

    @Test
    fun `supportsOnvifEvents returns false for rtsp url`() {
        val rtspCamera = camera.copy(url = "rtsp://192.168.1.100")
        assertTrue(!subscriptionService.supportsOnvifEvents(rtspCamera))
    }

    @Test
    fun `subscribeToCameraEvents returns failure when camera not found`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns null

        val result = subscriptionService.subscribeToCameraEvents(cameraId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `subscribeToCameraEvents success stores subscription and returns success`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        val sub = subscription()
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returns Result.success(sub)

        val result = subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)

        assertTrue(result.isSuccess)
        coVerify {
            onvifEventService.subscribeToEvents(
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = 3600L
            )
        }
        coVerify(exactly = 0) {
            onvifEventService.createPullPointSubscription(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `subscribeToCameraEvents when OnvifEventService fails returns failure`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returns Result.failure(RuntimeException("Camera unreachable"))

        val result = subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)

        assertTrue(result.isFailure)
    }

    @Test
    fun `subscribeToCameraEvents with usePullPoint true calls createPullPointSubscription`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        val sub = subscription("pull-sub-1")
        coEvery {
            onvifEventService.createPullPointSubscription(
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                filter = any(),
                subscriptionTime = any()
            )
        } returns Result.success(sub)

        val result = subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = true)

        assertTrue(result.isSuccess)
        coVerify {
            onvifEventService.createPullPointSubscription(
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                filter = any(),
                subscriptionTime = 3600L
            )
        }
    }

    @Test
    fun `subscribeToCameraEvents with usePullPoint true when createPullPointSubscription fails returns failure`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        coEvery {
            onvifEventService.createPullPointSubscription(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                subscriptionTime = any()
            )
        } returns Result.failure(RuntimeException("PullPoint not supported"))

        val result = subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = true)

        assertTrue(result.isFailure)
    }

    @Test
    fun `unsubscribeFromCameraEvents calls unsubscribe for each subscription of camera`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        val sub = subscription()
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returns Result.success(sub)
        coEvery { onvifEventService.unsubscribe(sub.id) } returns Result.success(Unit)

        subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
        val unsubResult = subscriptionService.unsubscribeFromCameraEvents(cameraId)

        assertTrue(unsubResult.isSuccess)
        coVerify { onvifEventService.unsubscribe(sub.id) }
    }

    @Test
    fun `unsubscribeFromCameraEvents when no subscriptions returns success`() = runBlocking {
        val result = subscriptionService.unsubscribeFromCameraEvents("non-existent-cam")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `unsubscribeFromCameraEvents when OnvifEventService unsubscribe fails returns failure`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        val sub = subscription("sub-fail")
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returns Result.success(sub)
        coEvery { onvifEventService.unsubscribe("sub-fail") } returns Result.failure(RuntimeException("Network error"))

        subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
        val unsubResult = subscriptionService.unsubscribeFromCameraEvents(cameraId)

        assertTrue(unsubResult.isFailure)
        assertEquals("Network error", unsubResult.exceptionOrNull()?.message)
    }

    @Test
    fun `unsubscribeFromCameraEvents removes all subscriptions created by repeated subscribe`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        val sub1 = subscription("sub-1")
        val sub2 = subscription("sub-2")
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returnsMany listOf(Result.success(sub1), Result.success(sub2))
        coEvery { onvifEventService.unsubscribe(any()) } returns Result.success(Unit)

        subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
        subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)

        val unsubResult = subscriptionService.unsubscribeFromCameraEvents(cameraId)
        assertTrue(unsubResult.isSuccess)
        coVerify(exactly = 1) { onvifEventService.unsubscribe("sub-1") }
        coVerify(exactly = 1) { onvifEventService.unsubscribe("sub-2") }
    }

    @Test
    fun `unsubscribe then resubscribe works for same camera`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        val sub1 = subscription("sub-restart-1")
        val sub2 = subscription("sub-restart-2")
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returnsMany listOf(Result.success(sub1), Result.success(sub2))
        coEvery { onvifEventService.unsubscribe("sub-restart-1") } returns Result.success(Unit)

        val firstSubscribe = subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
        assertTrue(firstSubscribe.isSuccess)
        val unsubscribe = subscriptionService.unsubscribeFromCameraEvents(cameraId)
        assertTrue(unsubscribe.isSuccess)
        val secondSubscribe = subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
        assertTrue(secondSubscribe.isSuccess)

        coVerify(exactly = 2) {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        }
    }

    @Test
    fun `subscribeToAllCamerasAtStartup when config disabled does not subscribe`() = runBlocking {
        val disabledConfig = OnvifEventsConfig(enabled = false)
        val serviceWithDisabled = OnvifEventSubscriptionService(
            onvifEventService = onvifEventService,
            eventService = eventService,
            cameraRepository = cameraRepository,
            config = disabledConfig
        )
        coEvery { cameraRepository.getCameras() } returns listOf(camera)

        serviceWithDisabled.subscribeToAllCamerasAtStartup()

        coVerify(exactly = 0) {
            onvifEventService.subscribeToEvents(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 0) {
            onvifEventService.createPullPointSubscription(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `subscribeToAllCamerasAtStartup subscribes only http and https cameras`() = runBlocking {
        val rtspCamera = camera.copy(id = "cam-2", url = "rtsp://192.168.1.101")
        coEvery { cameraRepository.getCameras() } returns listOf(camera, rtspCamera)
        val sub = subscription()
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returns Result.success(sub)

        subscriptionService.subscribeToAllCamerasAtStartup()

        coVerify(exactly = 1) {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        }
    }

    @Test
    fun `pull point timeout does not create events`() = runBlocking {
        val pullConfig = OnvifEventsConfig(
            enabled = true,
            usePullPointByDefault = true,
            pullIntervalMs = 50L,
            pullTimeoutMs = 100
        )
        val pullService = OnvifEventSubscriptionService(
            onvifEventService = onvifEventService,
            eventService = eventService,
            cameraRepository = cameraRepository,
            config = pullConfig
        )

        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        coEvery { cameraRepository.getCameras() } returns listOf(camera)

        val sub = subscription("pull-timeout-sub")
        coEvery {
            onvifEventService.createPullPointSubscription(
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                filter = any(),
                subscriptionTime = any()
            )
        } returns Result.success(sub)
        coEvery {
            onvifEventService.pullMessages(
                subscriptionId = sub.id,
                timeout = any(),
                maxMessages = any()
            )
        } returns Result.failure(RuntimeException("timeout while pulling messages"))

        val result = pullService.subscribeToCameraEvents(cameraId, usePullPoint = true)
        assertTrue(result.isSuccess)

        delay(180)

        coVerify(atLeast = 1) {
            onvifEventService.pullMessages(
                subscriptionId = sub.id,
                timeout = any(),
                maxMessages = any()
            )
        }
        coVerify(exactly = 0) {
            eventService.createEvent(any(), any(), any(), any(), any(), any(), any(), any())
        }

        pullService.stop()
    }

    @Test
    fun `pull point flow performs sync pull mapping and renew`() = runBlocking {
        val now = System.currentTimeMillis()
        val nearExpirySubscription = OnvifEventSubscription(
            id = "pull-renew-sub",
            cameraUrl = camera.url,
            notificationConsumerUrl = "http://localhost/events",
            subscriptionReference = "http://camera/pullpoint",
            expirationTime = now + 2 * 60 * 1000, // needsRenewal() -> true immediately
            createdAt = now,
            status = SubscriptionStatus.ACTIVE
        )
        val renewedSubscription = nearExpirySubscription.copy(expirationTime = now + 3600_000)
        val pullEvent = com.company.ipcamera.core.network.onvif.OnvifEvent(
            topic = OnvifEventType.MOTION_ALARM,
            timestamp = now,
            message = "Motion alarm"
        )

        val pullConfig = OnvifEventsConfig(
            enabled = true,
            usePullPointByDefault = true,
            pullIntervalMs = 50L,
            pullTimeoutMs = 100,
            renewalCheckIntervalMs = 50L
        )
        val pullService = OnvifEventSubscriptionService(
            onvifEventService = onvifEventService,
            eventService = eventService,
            cameraRepository = cameraRepository,
            config = pullConfig
        )

        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        coEvery { cameraRepository.getCameras() } returns listOf(camera)
        coEvery {
            onvifEventService.createPullPointSubscription(
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                filter = any(),
                subscriptionTime = any()
            )
        } returns Result.success(nearExpirySubscription)
        coEvery { onvifEventService.setSynchronizationPoint(nearExpirySubscription.id) } returns Result.success(Unit)
        coEvery {
            onvifEventService.pullMessages(
                subscriptionId = nearExpirySubscription.id,
                timeout = any(),
                maxMessages = any()
            )
        } returns Result.success(listOf(pullEvent))
        coEvery {
            onvifEventService.renewSubscription(
                subscriptionId = nearExpirySubscription.id,
                renewalTime = any()
            )
        } returns Result.success(renewedSubscription)

        val result = pullService.subscribeToCameraEvents(cameraId, usePullPoint = true)
        assertTrue(result.isSuccess)

        delay(220)

        coVerify(atLeast = 1) { onvifEventService.setSynchronizationPoint(nearExpirySubscription.id) }
        coVerify(atLeast = 1) {
            onvifEventService.pullMessages(
                subscriptionId = nearExpirySubscription.id,
                timeout = any(),
                maxMessages = any()
            )
        }
        coVerify(atLeast = 1) {
            onvifEventService.renewSubscription(
                subscriptionId = nearExpirySubscription.id,
                renewalTime = 3600L
            )
        }
        coVerify(atLeast = 1) {
            eventService.createEvent(
                cameraId = cameraId,
                cameraName = camera.name,
                type = EventType.MOTION_DETECTION,
                severity = EventSeverity.INFO,
                description = any(),
                metadata = any(),
                thumbnailUrl = any(),
                videoUrl = any()
            )
        }

        pullService.stop()
    }

    @Test
    fun `pull point retry after transient failure eventually processes event`() = runBlocking {
        val now = System.currentTimeMillis()
        val pullConfig = OnvifEventsConfig(
            enabled = true,
            usePullPointByDefault = true,
            pullIntervalMs = 40L,
            pullTimeoutMs = 100
        )
        val pullService = OnvifEventSubscriptionService(
            onvifEventService = onvifEventService,
            eventService = eventService,
            cameraRepository = cameraRepository,
            config = pullConfig
        )
        val sub = subscription("pull-retry-sub")
        val pullEvent = com.company.ipcamera.core.network.onvif.OnvifEvent(
            topic = OnvifEventType.MOTION_ALARM,
            timestamp = now,
            message = "Motion after transient error"
        )

        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        coEvery { cameraRepository.getCameras() } returns listOf(camera)
        coEvery {
            onvifEventService.createPullPointSubscription(
                cameraUrl = camera.url,
                username = camera.username,
                password = camera.password,
                filter = any(),
                subscriptionTime = any()
            )
        } returns Result.success(sub)
        coEvery { onvifEventService.setSynchronizationPoint(sub.id) } returns Result.success(Unit)
        coEvery {
            onvifEventService.pullMessages(
                subscriptionId = sub.id,
                timeout = any(),
                maxMessages = any()
            )
        } returnsMany listOf(
            Result.failure(RuntimeException("temporary network timeout")),
            Result.success(listOf(pullEvent))
        )

        val startResult = pullService.subscribeToCameraEvents(cameraId, usePullPoint = true)
        assertTrue(startResult.isSuccess)
        delay(180)

        coVerify(atLeast = 2) { onvifEventService.pullMessages(sub.id, any(), any()) }
        coVerify(atLeast = 1) {
            eventService.createEvent(
                cameraId = cameraId,
                cameraName = camera.name,
                type = EventType.MOTION_DETECTION,
                severity = EventSeverity.INFO,
                description = any(),
                metadata = any(),
                thumbnailUrl = any(),
                videoUrl = any()
            )
        }
        pullService.stop()
    }

    @Test
    fun `concurrent subscribe unsubscribe same camera does not crash`() = runBlocking {
        coEvery { cameraRepository.getCameraById(cameraId) } returns camera
        coEvery {
            onvifEventService.subscribeToEvents(
                cameraUrl = any(),
                username = any(),
                password = any(),
                filter = any(),
                notificationConsumerUrl = any(),
                subscriptionTime = any()
            )
        } returns Result.success(subscription("sub-race"))
        coEvery { onvifEventService.unsubscribe(any()) } returns Result.success(Unit)

        val jobs = List(5) { idx ->
            launch {
                if (idx % 2 == 0) {
                    subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
                } else {
                    subscriptionService.unsubscribeFromCameraEvents(cameraId)
                }
            }
        }
        jobs.forEach { it.join() }

        val finalResult = subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
        assertTrue(finalResult.isSuccess)
    }
}
