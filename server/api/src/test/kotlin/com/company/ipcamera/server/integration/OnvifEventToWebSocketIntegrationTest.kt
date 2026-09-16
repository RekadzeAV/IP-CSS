package com.company.ipcamera.server.integration

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.network.onvif.OnvifEventFilter
import com.company.ipcamera.core.network.onvif.OnvifEventProperties
import com.company.ipcamera.core.network.onvif.OnvifEventService
import com.company.ipcamera.core.network.onvif.OnvifEventSubscription
import com.company.ipcamera.core.network.onvif.SubscriptionStatus
import com.company.ipcamera.server.config.OnvifEventsConfig
import com.company.ipcamera.server.repository.ServerUserRepository
import com.company.ipcamera.server.repository.ServerUserRepositoryInMemory
import com.company.ipcamera.server.service.EventService
import com.company.ipcamera.server.service.OnvifEventSubscriptionService
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Упрощённый интеграционный тест ONVIF -> EventRepository.
 * WebSocket E2E проверяется отдельно, здесь основной фокус на пайплайне событий.
 */
class OnvifEventToWebSocketIntegrationTest {

    private val testCameraUrl = "http://192.168.1.100/onvif"

    private val onvifNotificationXml = """
        <wsnt:NotificationMessage>
            <wsnt:Topic Dialect="http://www.onvif.org/ver10/tev/topicExpression/ConcreteSet">tns1:VideoSource/MotionAlarm</wsnt:Topic>
            <wsnt:Message>Motion detected</wsnt:Message>
            <wsnt:Source>${testCameraUrl}</wsnt:Source>
            <wsnt:UtcTime>2024-01-15T12:00:00Z</wsnt:UtcTime>
        </wsnt:NotificationMessage>
    """.trimIndent()

    @Test
    fun `ONVIF mock event flows to EventRepository`() = testApplication {
        application {
            configureTestApp()
        }

        val triggerResponse = client.get("/api/v1/test/trigger-onvif")
        assertEquals(io.ktor.http.HttpStatusCode.OK, triggerResponse.status)

        val countResponse = client.get("/api/v1/test/events-count")
        assertEquals(io.ktor.http.HttpStatusCode.OK, countResponse.status)

        val countJson = Json.parseToJsonElement(countResponse.bodyAsText()).jsonObject
        val total = countJson["total"]!!.jsonPrimitive.content.toInt()
        assertTrue(total >= 1, "Expected at least one event in repository, got $total")
    }

    private fun Application.configureTestApp() {
        stopKoin()
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Koin) {
            modules(onvifIntegrationTestModule())
        }
        routing {
            get("/api/v1/test/trigger-onvif") {
                val koin = GlobalContext.get()
                val cameraRepository = koin.get<CameraRepository>()
                val subscriptionService = koin.get<OnvifEventSubscriptionService>()

                val cameras = cameraRepository.getCameras()
                var cameraId = cameras.firstOrNull()?.id
                if (cameraId == null) {
                    val cam = Camera(
                        id = "test-cam-1",
                        name = "Test Camera",
                        url = testCameraUrl,
                        username = "admin",
                        password = "secret",
                        status = CameraStatus.ONLINE
                    )
                    cameraRepository.addCamera(cam)
                    cameraId = cam.id
                }
                subscriptionService.subscribeToCameraEvents(cameraId, usePullPoint = false)
                val result = subscriptionService.handleIncomingEvent(onvifNotificationXml, "sub-1")
                assertTrue(result.isSuccess, "handleIncomingEvent should succeed: ${result.exceptionOrNull()}")
                call.respond("ok")
            }

            get("/api/v1/test/events-count") {
                val koin = GlobalContext.get()
                val eventRepository = koin.get<EventRepository>()
                val result = eventRepository.getEvents(null, null, null, null, null, null, 1, 100)
                call.respond(mapOf("total" to result.total))
            }
        }
    }

    private fun onvifIntegrationTestModule() = module {
        single<ServerUserRepository> { ServerUserRepositoryInMemory() }

        single<CameraRepository> {
            object : CameraRepository {
                private val list = mutableListOf<Camera>()
                override suspend fun getCameras(): List<Camera> = list.toList()
                override suspend fun getCameraById(id: String): Camera? = list.find { it.id == id }
                override suspend fun addCamera(camera: Camera): Result<Camera> {
                    list.add(camera)
                    return Result.success(camera)
                }
                override suspend fun updateCamera(camera: Camera): Result<Camera> {
                    val i = list.indexOfFirst { it.id == camera.id }
                    if (i < 0) return Result.failure(NoSuchElementException(camera.id))
                    list[i] = camera
                    return Result.success(camera)
                }
                override suspend fun removeCamera(id: String): Result<Unit> {
                    list.removeIf { it.id == id }
                    return Result.success(Unit)
                }
                override suspend fun discoverCameras(forceRefresh: Boolean): List<com.company.ipcamera.shared.domain.repository.DiscoveredCamera> = emptyList()
                override suspend fun discoverCamerasWithProgress(
                    forceRefresh: Boolean,
                    config: com.company.ipcamera.shared.domain.repository.DiscoveryConfig
                ): kotlinx.coroutines.flow.Flow<com.company.ipcamera.shared.domain.repository.DiscoveryProgress> = kotlinx.coroutines.flow.emptyFlow()
                override suspend fun testConnection(camera: Camera): com.company.ipcamera.shared.domain.repository.ConnectionTestResult =
                    com.company.ipcamera.shared.domain.repository.ConnectionTestResult.Failure("", com.company.ipcamera.shared.domain.repository.ErrorCode.UNKNOWN)
                override suspend fun getCameraStatus(id: String): CameraStatus = CameraStatus.ONLINE
            }
        }

        single<EventRepository> {
            object : EventRepository {
                private val events = mutableListOf<Event>()
                override suspend fun getEvents(
                    type: com.company.ipcamera.shared.domain.model.EventType?,
                    cameraId: String?,
                    severity: com.company.ipcamera.shared.domain.model.EventSeverity?,
                    acknowledged: Boolean?,
                    startTime: Long?,
                    endTime: Long?,
                    page: Int,
                    limit: Int
                ): PaginatedResult<Event> {
                    val filtered = events.filter { e ->
                        (type == null || e.type == type) &&
                            (cameraId == null || e.cameraId == cameraId) &&
                            (severity == null || e.severity == severity)
                    }
                    val total = filtered.size
                    val start = (page - 1) * limit
                    val items = filtered.drop(start).take(limit)
                    return PaginatedResult(items, total, page, limit, (start + items.size) < total)
                }

                override suspend fun getEventById(id: String): Event? = events.find { it.id == id }
                override suspend fun addEvent(event: Event): Result<Event> {
                    events.add(event)
                    return Result.success(event)
                }
                override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)
                override suspend fun acknowledgeEvent(id: String, userId: String): Result<Event> =
                    Result.failure(NotImplementedError())
                override suspend fun acknowledgeEvents(ids: List<String>, userId: String): Result<List<Event>> =
                    Result.failure(NotImplementedError())
                override suspend fun deleteEvent(id: String): Result<Unit> = Result.success(Unit)
                override suspend fun getEventStatistics(
                    cameraId: String?,
                    startTime: Long?,
                    endTime: Long?
                ): Result<Map<String, Any>> = Result.success(emptyMap())
            }
        }

        single<EventService> {
            EventService(eventRepository = get(), notificationService = null)
        }

        single<OnvifEventService> {
            object : OnvifEventService {
                override suspend fun subscribeToEvents(
                    cameraUrl: String,
                    username: String?,
                    password: String?,
                    filter: OnvifEventFilter?,
                    notificationConsumerUrl: String?,
                    subscriptionTime: Long
                ): Result<OnvifEventSubscription> = Result.success(
                    OnvifEventSubscription(
                        id = "sub-1",
                        cameraUrl = cameraUrl,
                        notificationConsumerUrl = "http://localhost/events",
                        subscriptionReference = "http://localhost/sub",
                        expirationTime = System.currentTimeMillis() + 3600_000,
                        createdAt = System.currentTimeMillis(),
                        status = SubscriptionStatus.ACTIVE
                    )
                )
                override suspend fun unsubscribe(subscriptionId: String): Result<Unit> = Result.success(Unit)
                override suspend fun renewSubscription(
                    subscriptionId: String,
                    renewalTime: Long
                ): Result<OnvifEventSubscription> = Result.success(
                    OnvifEventSubscription(
                        id = subscriptionId,
                        cameraUrl = testCameraUrl,
                        notificationConsumerUrl = "",
                        subscriptionReference = "",
                        expirationTime = System.currentTimeMillis() + 3600_000,
                        createdAt = System.currentTimeMillis(),
                        status = SubscriptionStatus.ACTIVE
                    )
                )
                override suspend fun getEventProperties(
                    cameraUrl: String,
                    username: String?,
                    password: String?
                ): Result<OnvifEventProperties> = Result.success(
                    OnvifEventProperties(
                        eventServiceUrl = "$cameraUrl/events",
                        supportedTopics = emptyList(),
                        supportsPullPoint = true,
                        supportsPush = false
                    )
                )
                override suspend fun getSubscription(subscriptionId: String): OnvifEventSubscription? = null
                override suspend fun getSubscriptionsForCamera(cameraUrl: String): List<OnvifEventSubscription> = emptyList()
                override suspend fun unsubscribeAll(cameraUrl: String): Result<Unit> = Result.success(Unit)
                override suspend fun createPullPointSubscription(
                    cameraUrl: String,
                    username: String?,
                    password: String?,
                    filter: OnvifEventFilter?,
                    subscriptionTime: Long
                ): Result<OnvifEventSubscription> = subscribeToEvents(cameraUrl, username, password, filter, null, subscriptionTime)
                override suspend fun pullMessages(
                    subscriptionId: String,
                    timeout: Long,
                    maxMessages: Int
                ): Result<List<com.company.ipcamera.core.network.onvif.OnvifEvent>> = Result.success(emptyList())
                override suspend fun setSynchronizationPoint(subscriptionId: String): Result<Unit> = Result.success(Unit)
            }
        }

        single<OnvifEventsConfig> { OnvifEventsConfig(enabled = true, usePullPointByDefault = false) }

        single<OnvifEventSubscriptionService> {
            OnvifEventSubscriptionService(
                onvifEventService = get(),
                eventService = get(),
                cameraRepository = get(),
                config = get()
            )
        }
    }
}
