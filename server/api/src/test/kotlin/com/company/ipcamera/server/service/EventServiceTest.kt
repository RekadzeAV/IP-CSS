package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.service.NotificationService
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventServiceTest {

    /** In-memory репозиторий событий для тестов */
    private class FakeEventRepository : EventRepository {
        val storage = ConcurrentHashMap<String, Event>()
        var failNextAdd = false

        override suspend fun getEvents(
            type: EventType?, cameraId: String?, severity: EventSeverity?,
            acknowledged: Boolean?, startTime: Long?, endTime: Long?,
            page: Int, limit: Int
        ): PaginatedResult<Event> {
            var list = storage.values.toList()
            type?.let { list = list.filter { it.type == type } }
            cameraId?.let { list = list.filter { it.cameraId == cameraId } }
            severity?.let { list = list.filter { it.severity == severity } }
            acknowledged?.let { list = list.filter { it.acknowledged == acknowledged } }
            return PaginatedResult(items = list, total = list.size, page = page, limit = limit, hasMore = false)
        }

        override suspend fun getEventById(id: String): Event? = storage[id]

        override suspend fun addEvent(event: Event): Result<Event> {
            if (failNextAdd) return Result.failure(IllegalStateException("repo failure"))
            storage[event.id] = event
            return Result.success(event)
        }

        override suspend fun updateEvent(event: Event): Result<Event> {
            storage[event.id] = event
            return Result.success(event)
        }

        override suspend fun acknowledgeEvent(id: String, userId: String): Result<Event> {
            val existing = storage[id] ?: return Result.failure(NoSuchElementException(id))
            val updated = existing.copy(
                acknowledged = true,
                acknowledgedAt = System.currentTimeMillis(),
                acknowledgedBy = userId
            )
            storage[id] = updated
            return Result.success(updated)
        }

        override suspend fun acknowledgeEvents(ids: List<String>, userId: String): Result<List<Event>> {
            if (ids.any { !storage.containsKey(it) }) {
                return Result.failure(NoSuchElementException("missing id"))
            }
            val updated = ids.map { acknowledgeEvent(it, userId).getOrThrow() }
            return Result.success(updated)
        }

        override suspend fun deleteEvent(id: String): Result<Unit> {
            if (storage.remove(id) == null) return Result.failure(NoSuchElementException(id))
            return Result.success(Unit)
        }

        override suspend fun getEventStatistics(
            cameraId: String?, startTime: Long?, endTime: Long?
        ): Result<Map<String, Any>> = Result.success(mapOf("total" to storage.size))
    }

    /** Записывающий NotificationService */
    private class RecordingNotificationService : NotificationService {
        data class Call(val eventId: String, val title: String, val message: String, val priority: NotificationPriority)

        val calls = mutableListOf<Call>()

        override suspend fun sendNotification(
            title: String, message: String, type: NotificationType,
            priority: NotificationPriority, userId: String?, cameraId: String?,
            eventId: String?, recordingId: String?, extras: Map<String, String>
        ): Result<Notification> = Result.failure(UnsupportedOperationException())

        override suspend fun sendEventNotification(
            eventId: String, title: String, message: String,
            cameraId: String?, priority: NotificationPriority, userId: String?
        ): Result<Notification> {
            calls.add(Call(eventId, title, message, priority))
            return Result.failure(UnsupportedOperationException())
        }
    }

    private val repo = FakeEventRepository()

    @Test
    fun `createEvent stores event with given fields`() = runBlocking {
        val service = EventService(repo)
        val result = service.createEvent(
            cameraId = "cam-1", cameraName = "Front",
            type = EventType.MOTION_DETECTION, severity = EventSeverity.WARNING,
            description = "move!", metadata = mapOf("zone" to "1"),
            thumbnailUrl = "http://t/1.jpg", videoUrl = "http://v/1.mp4"
        )
        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertEquals("cam-1", event.cameraId)
        assertEquals(EventType.MOTION_DETECTION, event.type)
        assertEquals(false, event.acknowledged)
        assertNotNull(event.id)
        assertEquals(event.id, repo.storage[event.id]!!.id)
    }

    @Test
    fun `createEvent propagates repository failure`() = runBlocking {
        val service = EventService(repo)
        repo.failNextAdd = true
        val result = service.createEvent(cameraId = "c", type = EventType.SYSTEM_ERROR)
        assertTrue(result.isFailure)
    }

    @Test
    fun `createMotionDetectionEvent defaults`() = runBlocking {
        val service = EventService(repo)
        val e = service.createMotionDetectionEvent("cam-1").getOrThrow()
        assertEquals(EventType.MOTION_DETECTION, e.type)
        assertEquals(EventSeverity.WARNING, e.severity)
        assertEquals("Обнаружено движение", e.description)
    }

    @Test
    fun `createObjectDetectionEvent enriches metadata`() = runBlocking {
        val service = EventService(repo)
        val e = service.createObjectDetectionEvent("cam-1", objectType = "person", confidence = 0.91f).getOrThrow()
        assertEquals(EventType.OBJECT_DETECTION, e.type)
        assertEquals("person", e.metadata["objectType"])
        assertEquals("0.91", e.metadata["confidence"])
        assertEquals("Обнаружен объект: person", e.description)
    }

    @Test
    fun `createCameraOfflineEvent is error severity`() = runBlocking {
        val service = EventService(repo)
        val e = service.createCameraOfflineEvent("cam-2").getOrThrow()
        assertEquals(EventType.CAMERA_OFFLINE, e.type)
        assertEquals(EventSeverity.ERROR, e.severity)
        assertEquals("Камера недоступна", e.description)
    }

    @Test
    fun `createCameraOnlineEvent is info severity`() = runBlocking {
        val service = EventService(repo)
        val e = service.createCameraOnlineEvent("cam-2").getOrThrow()
        assertEquals(EventType.CAMERA_ONLINE, e.type)
        assertEquals(EventSeverity.INFO, e.severity)
    }

    @Test
    fun `createRecordingStartedEvent carries recordingId in metadata`() = runBlocking {
        val service = EventService(repo)
        val withId = service.createRecordingStartedEvent("c", recordingId = "rec-9").getOrThrow()
        assertEquals("rec-9", withId.metadata["recordingId"])

        val withoutId = service.createRecordingStartedEvent("c").getOrThrow()
        assertTrue(withoutId.metadata.isEmpty())
    }

    @Test
    fun `createRecordingStoppedEvent defaults`() = runBlocking {
        val service = EventService(repo)
        val e = service.createRecordingStoppedEvent("c", recordingId = "rec-1").getOrThrow()
        assertEquals(EventType.RECORDING_STOPPED, e.type)
        assertEquals(EventSeverity.INFO, e.severity)
        assertEquals("Запись остановлена", e.description)
    }

    @Test
    fun `createStorageFullEvent falls back to system camera and critical severity`() = runBlocking {
        val service = EventService(repo)
        val e = service.createStorageFullEvent().getOrThrow()
        assertEquals("system", e.cameraId)
        assertEquals(EventType.STORAGE_FULL, e.type)
        assertEquals(EventSeverity.CRITICAL, e.severity)
    }

    @Test
    fun `createSystemErrorEvent uses system fallback and passes metadata`() = runBlocking {
        val service = EventService(repo)
        val e = service.createSystemErrorEvent("boom", metadata = mapOf("src" to "unit-test")).getOrThrow()
        assertEquals("system", e.cameraId)
        assertEquals(EventType.SYSTEM_ERROR, e.type)
        assertEquals("boom", e.description)
        assertEquals("unit-test", e.metadata["src"])
    }

    @Test
    fun `acknowledgeEvent marks event acknowledged by user`() = runBlocking {
        val service = EventService(repo)
        val created = service.createEvent(cameraId = "c", type = EventType.MOTION_DETECTION).getOrThrow()
        val acked = service.acknowledgeEvent(created.id, "admin").getOrThrow()
        assertTrue(acked.acknowledged)
        assertEquals("admin", acked.acknowledgedBy)
    }

    @Test
    fun `acknowledgeEvents returns all updated events`() = runBlocking {
        val service = EventService(repo)
        val ids = (1..3).map { service.createEvent(cameraId = "c", type = EventType.MOTION_DETECTION).getOrThrow().id }
        val res = service.acknowledgeEvents(ids, "operator")
        assertTrue(res.isSuccess)
        assertTrue(res.getOrThrow().all { it.acknowledged })
    }

    @Test
    fun `deleteEvent removes from repository`() = runBlocking {
        val service = EventService(repo)
        val created = service.createEvent(cameraId = "c", type = EventType.MOTION_DETECTION).getOrThrow()
        assertTrue(service.deleteEvent(created.id).isSuccess)
        assertTrue(service.deleteEvent(created.id).isFailure)
    }

    @Test
    fun `notification sent for critical and error events only`() = runBlocking {
        val notifier = RecordingNotificationService()
        val service = EventService(repo, notifier)

        service.createStorageFullEvent()          // CRITICAL -> notify (URGENT)
        service.createSystemErrorEvent("err")     // CRITICAL -> notify (URGENT)
        service.createCameraOfflineEvent("c")     // ERROR    -> notify (HIGH)
        service.createMotionDetectionEvent("c")   // WARNING  -> нет
        service.createCameraOnlineEvent("c")      // INFO     -> нет

        // Уведомления отправляются асинхронно (scope.launch на Dispatchers.Default) — дождёмся доставки.
        // Deadline увеличен, чтобы исключить флаки-падения под нагрузкой CI.
        val deadline = System.currentTimeMillis() + 10_000
        while (notifier.calls.size < 3 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20)
        }

        assertEquals(3, notifier.calls.size)
        assertTrue(notifier.calls.all { it.priority == NotificationPriority.URGENT || it.priority == NotificationPriority.HIGH })
        assertTrue(notifier.calls.any { it.title == "Камера недоступна" })
    }

    @Test
    fun `no notification when notification service absent`() = runBlocking {
        val service = EventService(repo, null)
        val e = service.createStorageFullEvent()
        assertTrue(e.isSuccess)
    }
}