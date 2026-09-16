package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.StoredLicensePlate
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportServiceImplTest {

    private class FakeEventRepository(
        private val events: List<Event>
    ) : EventRepository {
        override suspend fun getEvents(
            type: EventType?, cameraId: String?, severity: EventSeverity?,
            acknowledged: Boolean?, startTime: Long?, endTime: Long?,
            page: Int, limit: Int
        ): PaginatedResult<Event> =
            PaginatedResult(items = events, total = events.size, page = page, limit = limit, hasMore = false)

        override suspend fun getEventById(id: String): Event? = events.find { it.id == id }
        override suspend fun addEvent(event: Event): Result<Event> = Result.success(event)
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

    private class FakeRecordingRepository(
        private val recordings: List<Recording>
    ) : RecordingRepository {
        override suspend fun getRecordings(
            cameraId: String?, startTime: Long?, endTime: Long?,
            page: Int, limit: Int
        ): PaginatedResult<Recording> =
            PaginatedResult(items = recordings, total = recordings.size, page = page, limit = limit, hasMore = false)

        override suspend fun getRecordingById(id: String): Recording? = recordings.find { it.id == id }
        override suspend fun addRecording(recording: Recording): Result<Recording> = Result.success(recording)
        override suspend fun updateRecording(recording: Recording): Result<Recording> = Result.success(recording)
        override suspend fun deleteRecording(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun getDownloadUrl(id: String): Result<String> = Result.failure(UnsupportedOperationException())
        override suspend fun exportRecording(id: String, format: String, quality: String): Result<String> =
            Result.failure(UnsupportedOperationException())
    }

    private class FakeLicensePlateRepository(
        private val plates: List<StoredLicensePlate>
    ) : LicensePlateRepository {
        override suspend fun insert(plate: StoredLicensePlate): Result<Unit> = Result.success(Unit)
        override suspend fun getByCameraId(cameraId: String, limit: Int): List<StoredLicensePlate> =
            plates.filter { it.cameraId == cameraId }

        override suspend fun getByCameraIdAndDateRange(
            cameraId: String, fromTimestamp: Long, toTimestamp: Long
        ): List<StoredLicensePlate> =
            plates.filter { it.cameraId == cameraId && it.timestamp in fromTimestamp..toTimestamp }

        override suspend fun getByPlateNumber(plateNumber: String, limit: Int): List<StoredLicensePlate> =
            plates.filter { it.plateNumber == plateNumber }

        override suspend fun deleteOlderThan(timestamp: Long): Result<Int> = Result.success(0)
    }

    // ---- Фикстуры ----

    private fun event(
        id: String, cameraId: String = "cam-1", type: EventType = EventType.MOTION_DETECTION,
        severity: EventSeverity = EventSeverity.WARNING, acknowledged: Boolean = false,
        description: String? = null
    ) = Event(
        id = id, cameraId = cameraId, cameraName = null, type = type, severity = severity,
        timestamp = 1700000000000L, description = description, metadata = emptyMap(),
        acknowledged = acknowledged
    )

    private fun recording(
        id: String, status: RecordingStatus = RecordingStatus.COMPLETED, fileSize: Long? = 1024L
    ) = Recording(
        id = id, cameraId = "cam-1", startTime = 1700000000000L,
        endTime = 1700000060000L, duration = 60000L, fileSize = fileSize,
        format = RecordingFormat.MP4, quality = Quality.HIGH, status = status
    )

    private fun plate(
        id: String, plateNumber: String, confidence: Float, ts: Long = 1700000000000L,
        cameraId: String = "cam-1", country: String? = "RU"
    ) = StoredLicensePlate(
        id = id, cameraId = cameraId, timestamp = ts, plateNumber = plateNumber,
        confidence = confidence, country = country,
        bboxX = 10, bboxY = 20, bboxWidth = 100, bboxHeight = 40, createdAt = ts
    )

    private fun serviceWith(
        events: List<Event> = emptyList(),
        recordings: List<Recording> = emptyList(),
        plates: List<StoredLicensePlate> = emptyList()
    ): ReportServiceImpl = ReportServiceImpl(
        FakeEventRepository(events),
        FakeRecordingRepository(recordings),
        FakeLicensePlateRepository(plates)
    )

    @Test
    fun `events summary contains header rows and counts`() = runBlocking {
        val service = serviceWith(
            events = listOf(
                event("e1", acknowledged = true),
                event("e2", type = EventType.SYSTEM_ERROR, severity = EventSeverity.CRITICAL),
                event("e3", type = EventType.MOTION_DETECTION)
            )
        )
        val csv = String(
            service.generateReport(ReportService.ReportType.EVENTS_SUMMARY, ReportService.ExportFormat.CSV).getOrThrow()
        )

        assertTrue(csv.contains("# report,EVENTS_SUMMARY"))
        assertTrue(csv.contains("# summary,total_events,3"))
        assertTrue(csv.contains("# summary,acknowledged_events,1"))
        assertTrue(csv.contains("# summary,critical_events,1"))
        assertTrue(csv.contains("id,camera_id,type,severity,timestamp,acknowledged,description"))
        assertTrue(csv.contains("e2"))
        assertTrue(csv.contains("SYSTEM_ERROR"))
    }

    @Test
    fun `events summary respects camera filter in metadata`() = runBlocking {
        val service = serviceWith(events = listOf(event("e1", cameraId = "cam-9")))
        val csv = String(
            service.generateReport(
                ReportService.ReportType.EVENTS_SUMMARY, ReportService.ExportFormat.CSV,
                cameraId = "cam-9"
            ).getOrThrow()
        )
        assertTrue(csv.contains("# camera_id,cam-9"))
    }

    @Test
    fun `csv escapes commas via quotes`() = runBlocking {
        val service = serviceWith(events = listOf(event("e1", description = "a,b")))
        val csv = String(
            service.generateReport(ReportService.ReportType.EVENTS_SUMMARY, ReportService.ExportFormat.CSV).getOrThrow()
        )
        assertTrue(csv.contains("\"a,b\""))
    }

    @Test
    fun `recordings summary aggregates size and status`() = runBlocking {
        val service = serviceWith(
            recordings = listOf(
                recording("r1"),
                recording("r2", status = RecordingStatus.ACTIVE, fileSize = null),
                recording("r3", fileSize = 2048L)
            )
        )
        val csv = String(
            service.generateReport(
                ReportService.ReportType.RECORDINGS_SUMMARY, ReportService.ExportFormat.CSV
            ).getOrThrow()
        )
        assertTrue(csv.contains("# summary,total_recordings,3"))
        assertTrue(csv.contains("# summary,active_recordings,1"))
        assertTrue(csv.contains("# summary,completed_recordings,2"))
        assertTrue(csv.contains("# summary,total_size_bytes,3072"))
        assertTrue(csv.contains("id,camera_id,start_time,end_time,duration,status,format,quality,file_size"))
    }

    @Test
    fun `license plates summary requires cameraId`() = runBlocking {
        val service = serviceWith(plates = listOf(plate("p1", "A123BC", 0.9f)))
        val result = service.generateReport(
            ReportService.ReportType.LICENSE_PLATES_SUMMARY, ReportService.ExportFormat.CSV,
            cameraId = null
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `license plates summary counts unique plates`() = runBlocking {
        val service = serviceWith(
            plates = listOf(
                plate("p1", "A123BC", 0.8f),
                plate("p2", "A123BC", 0.6f),
                plate("p3", "X999YY", 1.0f)
            )
        )
        val csv = String(
            service.generateReport(
                ReportService.ReportType.LICENSE_PLATES_SUMMARY, ReportService.ExportFormat.CSV,
                cameraId = "cam-1"
            ).getOrThrow()
        )
        assertTrue(csv.contains("# summary,total_plates,3"))
        assertTrue(csv.contains("# summary,unique_plates,2"))
        assertTrue(csv.contains("A123BC"))
    }

    @Test
    fun `license plates uses date range when both bounds given`() = runBlocking {
        val repoPlates = listOf(
            plate("p-in", "IN0001", 0.9f, ts = 1500L),
            plate("p-out", "OUT001", 0.5f, ts = 5000L)
        )
        val service = ReportServiceImpl(
            FakeEventRepository(emptyList()),
            FakeRecordingRepository(emptyList()),
            FakeLicensePlateRepository(repoPlates)
        )
        val csv = String(
            service.generateReport(
                ReportService.ReportType.LICENSE_PLATES_SUMMARY, ReportService.ExportFormat.CSV,
                cameraId = "cam-1", fromTimestamp = 1000L, toTimestamp = 2000L
            ).getOrThrow()
        )
        assertTrue(csv.contains("IN0001"))
        assertFalse(csv.contains("OUT001"))
    }

    @Test
    fun `dashboard combines events and recordings sections`() = runBlocking {
        val service = serviceWith(events = listOf(event("e1")), recordings = listOf(recording("r1")))
        val csv = String(
            service.generateReport(ReportService.ReportType.ANALYTICS_DASHBOARD, ReportService.ExportFormat.CSV).getOrThrow()
        )
        assertTrue(csv.contains("# report,ANALYTICS_DASHBOARD"))
        assertTrue(csv.contains("metric,value"))
        assertTrue(csv.contains("events_total,1"))
        assertTrue(csv.contains("recordings_total,1"))
        assertTrue(csv.contains("license_plates_total,0"))
    }

    @Test
    fun `pdf export starts with pdf magic and contains eof marker`() = runBlocking {
        val service = serviceWith(events = listOf(event("e1")))
        val bytes = service.generateReport(
            ReportService.ReportType.EVENTS_SUMMARY, ReportService.ExportFormat.PDF
        ).getOrThrow()
        val text = String(bytes)
        assertTrue(text.startsWith("%PDF-1.4"))
        assertTrue(text.contains("%%EOF"))
    }

    @Test
    fun `empty dataset still produces valid csv`() = runBlocking {
        val service = serviceWith()
        val csv = String(
            service.generateReport(ReportService.ReportType.EVENTS_SUMMARY, ReportService.ExportFormat.CSV).getOrThrow()
        )
        assertTrue(csv.contains("# summary,total_events,0"))
    }

    @Test
    fun `file extension mapping`() {
        val service = serviceWith()
        assertEquals("csv", service.fileExtension(ReportService.ExportFormat.CSV))
        assertEquals("pdf", service.fileExtension(ReportService.ExportFormat.PDF))
    }
}