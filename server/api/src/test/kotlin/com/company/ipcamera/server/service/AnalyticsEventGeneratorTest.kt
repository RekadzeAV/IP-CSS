package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamType
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.DetectedFace
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.model.BoundingBox
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.RecognizedLicensePlate
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.service.FaceDetectionResult
import com.company.ipcamera.shared.domain.service.LicensePlateRecognitionResult
import com.company.ipcamera.shared.domain.service.MotionDetectionResult
import com.company.ipcamera.shared.domain.service.ObjectDetectionResult
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnalyticsEventGeneratorTest {

    /** In-memory репозиторий, записывающий созданные события */
    private class FakeEventRepository : EventRepository {
        val events = mutableListOf<Event>()
        var failNextAdd = false

        override suspend fun getEvents(
            type: EventType?, cameraId: String?, severity: EventSeverity?,
            acknowledged: Boolean?, startTime: Long?, endTime: Long?,
            page: Int, limit: Int
        ): PaginatedResult<Event> =
            PaginatedResult(items = emptyList(), total = 0, page = 1, limit = limit, hasMore = false)

        override suspend fun getEventById(id: String): Event? = null
        override suspend fun addEvent(event: Event): Result<Event> {
            if (failNextAdd) return Result.failure(IllegalStateException("repo failure"))
            events.add(event)
            return Result.success(event)
        }

        override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)

        override suspend fun acknowledgeEvent(id: String, userId: String): Result<Event> =
            Result.failure(UnsupportedOperationException())

        override suspend fun acknowledgeEvents(ids: List<String>, userId: String): Result<List<Event>> =
            Result.failure(UnsupportedOperationException())

        override suspend fun deleteEvent(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun getEventStatistics(
            cameraId: String?, startTime: Long?, endTime: Long?
        ): Result<Map<String, Any>> = Result.success(emptyMap())
    }

    // ---- Фикстуры ----

    private fun camera(id: String = "cam-1") = Camera(
        id = id, name = "Front Door", url = "rtsp://cam/" + id
    )

    private fun frame(ts: Long = 1000L) = RtspFrame(
        data = ByteArray(0), timestamp = ts,
        streamType = RtspStreamType.VIDEO, width = 1920, height = 1080
    )

    private fun eventService(): EventService = EventService(repo)

    private fun generator(
        repo: FakeEventRepository = this.repo,
        cooldownPeriod: Long = 5000L
    ): AnalyticsEventGenerator = AnalyticsEventGenerator(
        eventService = EventService(repo),
        analyticsRuleService = null,
        cooldownPeriod = cooldownPeriod,
        aggregationWindow = 10000L
    )

    private val repo = FakeEventRepository()

    @Test
    fun `motion event ignored when not detected or low confidence`() = runBlocking {
        val g = generator()

        g.generateMotionEvent(camera(), frame(), MotionDetectionResult(detected = false, confidence = 0.1f, timestamp = 1))
        g.generateMotionEvent(camera(), frame(), MotionDetectionResult(detected = true, confidence = 0.29f, timestamp = 2))

        assertTrue(repo.events.isEmpty())
    }

    @Test
    fun `motion event created with severity and metadata`() = runBlocking {
        val g = generator()
        val zone = com.company.ipcamera.shared.domain.service.MotionZone(
            zone = DetectionZone(name = "Gate", polygon = listOf(listOf(0, 0), listOf(0, 1), listOf(1, 1), listOf(1, 0)), sensitivity = 80),
            intensity = 0.9f
        )
        val result = com.company.ipcamera.shared.domain.service.MotionDetectionResult(
            detected = true, confidence = 0.95f, zones = listOf(zone), timestamp = 7
        )

        val res = g.generateMotionEvent(camera(), frame(), result)

        assertTrue(res.isSuccess)
        assertEquals(1, repo.events.size)
        val e = repo.events[0]
        assertEquals(EventType.MOTION_DETECTION, e.type)
        assertEquals(EventSeverity.CRITICAL, e.severity) // zones + 0.9+
        assertEquals("Gate", e.metadata["zones"])
        assertEquals("1", e.metadata["zoneCount"])
    }

    @Test
    fun `motion severity maps by confidence`() = runBlocking {
        fun severityFor(conf: Float) = runBlocking {
            val r = FakeEventRepository()
            generator(r, cooldownPeriod = 1).generateMotionEvent(
                camera(), frame(),
                com.company.ipcamera.shared.domain.service.MotionDetectionResult(detected = true, confidence = conf, timestamp = 1)
            )
            r.events.firstOrNull()?.severity
        }

        // 0.0-0.79 -> INFO (без зон); >=0.8 ERROR; критичное требует зон
        assertEquals(EventSeverity.WARNING, severityFor(0.5f))
        assertEquals(EventSeverity.ERROR, severityFor(0.85f))
    }

    @Test
    fun `motion event suppressed by cooldown`() = runBlocking {
        val r = FakeEventRepository()
        val g = AnalyticsEventGenerator(eventService = EventService(r), analyticsRuleService = null, cooldownPeriod = 60_000L)

        g.generateMotionEvent(camera(), frame(), MotionDetectionResult(detected = true, confidence = 0.9f, timestamp = 1))
        g.generateMotionEvent(camera(), frame(), MotionDetectionResult(detected = true, confidence = 0.9f, timestamp = 2))

        assertEquals(1, r.events.size) // второе событие в cooldown пропущено
    }

    @Test
    fun `motion event propagates repository failure`() = runBlocking {
        val r = FakeEventRepository()
        r.failNextAdd = true
        val g = AnalyticsEventGenerator(eventService = EventService(r), analyticsRuleService = null, cooldownPeriod = 1)
        val res = g.generateMotionEvent(camera(), frame(), MotionDetectionResult(detected = true, confidence = 0.9f, timestamp = 1))
        assertTrue(res.isFailure)
    }
@Test
    fun `object detection ignored when no objects`() = runBlocking {
        val g = generator()
        g.generateObjectDetectionEvent(camera(), frame(), ObjectDetectionResult(objects = emptyList(), timestamp = 1))
        assertTrue(repo.events.isEmpty())
    }

    @Test
    fun `object event generated per filtered type`() = runBlocking {
        val g = generator()
        val result = ObjectDetectionResult(
            objects = listOf(
                DetectedObject(type = "person", confidence = 0.6f, boundingBox = BoundingBox(1, 2, 3, 4))
            ),
            timestamp = 1
        )
        g.generateObjectDetectionEvent(camera(), frame(), result)
        assertTrue(repo.events.isNotEmpty())
        val e = repo.events[0]
        assertEquals(EventType.OBJECT_DETECTION, e.type)
        assertEquals("person", e.metadata["objectType"])
        assertTrue(e.metadata["maxConfidence"]!!.isNotBlank())
    }

    @Test
    fun `object detection with low confidence objects is suppressed`() = runBlocking {
        val g = generator()
        val result = ObjectDetectionResult(
            objects = listOf(DetectedObject(type = "car", confidence = 0.2f, boundingBox = BoundingBox(0, 0, 1, 1))),
            timestamp = 1
        )
        g.generateObjectDetectionEvent(camera(), frame(), result)
        assertTrue(repo.events.isEmpty())
    }

    @Test
    fun `face detection creates event when confidence above threshold`() = runBlocking {
        val g = generator()
        val result = FaceDetectionResult(
            faces = listOf(DetectedFace(boundingBox = BoundingBox(0, 0, 10, 10), confidence = 0.8f)),
            timestamp = 1
        )
        g.generateFaceDetectionEvent(camera(), frame(), result)
        assertEquals(1, repo.events.size)
        assertEquals(EventType.FACE_DETECTION, repo.events[0].type)
    }

    @Test
    fun `face detection suppressed when low confidence`() = runBlocking {
        val g = generator()
        val result = FaceDetectionResult(
            faces = listOf(DetectedFace(boundingBox = BoundingBox(0, 0, 10, 10), confidence = 0.3f)),
            timestamp = 1
        )
        g.generateFaceDetectionEvent(camera(), frame(), result)
        assertTrue(repo.events.isEmpty())
    }

    @Test
    fun `license plate event created and enriches metadata`() = runBlocking {
        val g = generator()
        val result = LicensePlateRecognitionResult(
            plates = listOf(
                RecognizedLicensePlate(
                    plateNumber = "A123BC", confidence = 0.95f, country = "RU",
                    boundingBox = BoundingBox(1, 1, 50, 20)
                )
            ),
            timestamp = 1
        )
        g.generateLicensePlateEvent(camera(), frame(), result)
        assertEquals(1, repo.events.size)
        val e = repo.events[0]
        assertEquals(EventType.LICENSE_PLATE_RECOGNITION, e.type)
        assertEquals("A123BC", e.metadata["plateNumber"])
        assertEquals("RU", e.metadata["country"])
        assertEquals(EventSeverity.CRITICAL, e.severity) // confidence 0.95 -> CRITICAL
    }

    @Test
    fun `license plate below threshold is filtered`() = runBlocking {
        val g = generator()
        val result = LicensePlateRecognitionResult(
            plates = listOf(RecognizedLicensePlate(plateNumber = "X", confidence = 0.5f, boundingBox = BoundingBox(0, 0, 1, 1))),
            timestamp = 1
        )
        g.generateLicensePlateEvent(camera(), frame(), result)
        assertTrue(repo.events.isEmpty())
    }

    @Test
    fun `object detection creates event for confident result`() = runBlocking {
        val r = FakeEventRepository()
        val g = AnalyticsEventGenerator(eventService = EventService(r), analyticsRuleService = null, cooldownPeriod = 60_000L)
        val result = ObjectDetectionResult(
            objects = listOf(DetectedObject(type = "person", confidence = 0.9f, boundingBox = BoundingBox(0, 0, 1, 1))),
            timestamp = 1
        )
        g.generateObjectDetectionEvent(camera(), frame(), result)
        assertTrue(r.events.isNotEmpty())
    }

    @Test
    fun `cleanupOldCooldowns does not throw`() = runBlocking {
        val g = generator(cooldownPeriod = 1000L)
        g.generateMotionEvent(camera(), frame(), MotionDetectionResult(detected = true, confidence = 0.9f, timestamp = 1))
        g.cleanupOldCooldowns()
        assertNotNull(repo.events)
    }
}