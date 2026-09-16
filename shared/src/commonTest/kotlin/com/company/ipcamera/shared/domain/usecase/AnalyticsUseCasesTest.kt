package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.AnalyticsSettings
import com.company.ipcamera.shared.domain.model.BoundingBox
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.CameraSettings
import com.company.ipcamera.shared.domain.model.DetectedFace
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.DetectionZone
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.model.RecognizedLicensePlate
import com.company.ipcamera.shared.domain.model.StoredLicensePlate
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.FaceDetectionResult
import com.company.ipcamera.shared.domain.service.LicensePlateRecognitionResult
import com.company.ipcamera.shared.domain.service.MotionDetectionResult
import com.company.ipcamera.shared.domain.service.ObjectDetectionResult
import com.company.ipcamera.shared.domain.service.TrackInfo
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Юнит-тесты аналитических Use Cases с подменой [AnalyticsService] и репозиториев.
 * Не требуют нативных библиотек.
 */
class AnalyticsUseCasesTest {
    private fun testCamera(): Camera =
        Camera(
            id = "cam-1",
            name = "Test",
            url = "rtsp://x",
            resolution = Resolution(4, 4),
            status = CameraStatus.ONLINE,
            settings =
                CameraSettings(
                    analytics =
                        AnalyticsSettings(
                            motionMinArea = 10,
                            objectDetection = true,
                            faceRecognition = true,
                            anprEnabled = true,
                        ),
                ),
        )

    /** RGB24 кадр 4×4 — согласован с [testCamera].resolution */
    private fun frameRgb(): ByteArray = ByteArray(4 * 4 * 3) { 1 }

    private class FakeAnalyticsService : AnalyticsService {
        var motion: MotionDetectionResult = MotionDetectionResult(detected = false, confidence = 0f)
        var objects: ObjectDetectionResult = ObjectDetectionResult(emptyList())
        var faces: FaceDetectionResult = FaceDetectionResult(emptyList())
        var plates: LicensePlateRecognitionResult = LicensePlateRecognitionResult(emptyList())
        var tracks: List<TrackInfo> = emptyList()
        var compareReturn: Float = 1f
        var throwOnMotion: Boolean = false

        override suspend fun detectMotion(
            camera: Camera,
            frameData: ByteArray,
            previousFrameData: ByteArray?,
            zones: List<DetectionZone>,
            threshold: Float,
            minArea: Int?,
        ): MotionDetectionResult {
            if (throwOnMotion) error("native failed")
            return motion
        }

        override suspend fun detectObjects(
            camera: Camera,
            frameData: ByteArray,
            objectTypes: List<String>,
            minConfidence: Float,
        ): ObjectDetectionResult = objects

        override suspend fun detectFaces(
            camera: Camera,
            frameData: ByteArray,
            minConfidence: Float,
            includeLandmarks: Boolean,
            includeEmbeddings: Boolean,
        ): FaceDetectionResult = faces

        override suspend fun recognizeLicensePlates(
            camera: Camera,
            frameData: ByteArray,
            minConfidence: Float,
            country: String?,
        ): LicensePlateRecognitionResult = plates

        override suspend fun trackObjects(
            camera: Camera,
            frameData: ByteArray,
        ): List<TrackInfo> = tracks

        override suspend fun compareFaces(
            embedding1: FloatArray,
            embedding2: FloatArray,
        ): Float = compareReturn
    }

    private class CapturingEventRepository : EventRepository {
        val added = mutableListOf<Event>()

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
            added.add(event)
            return Result.success(event)
        }

        override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)

        override suspend fun acknowledgeEvent(
            id: String,
            userId: String,
        ): Result<Event> = Result.failure(UnsupportedOperationException())

        override suspend fun acknowledgeEvents(
            ids: List<String>,
            userId: String,
        ): Result<List<Event>> = Result.success(emptyList())

        override suspend fun deleteEvent(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun getEventStatistics(
            cameraId: String?,
            startTime: Long?,
            endTime: Long?,
        ): Result<Map<String, Any>> = Result.success(emptyMap())
    }

    private class CapturingLicensePlateRepository : LicensePlateRepository {
        val inserted = mutableListOf<StoredLicensePlate>()

        override suspend fun insert(plate: StoredLicensePlate): Result<Unit> {
            inserted.add(plate)
            return Result.success(Unit)
        }

        override suspend fun getByCameraId(
            cameraId: String,
            limit: Int,
        ): List<StoredLicensePlate> = emptyList()

        override suspend fun getByCameraIdAndDateRange(
            cameraId: String,
            fromTimestamp: Long,
            toTimestamp: Long,
        ): List<StoredLicensePlate> = emptyList()

        override suspend fun getByPlateNumber(
            plateNumber: String,
            limit: Int,
        ): List<StoredLicensePlate> = emptyList()

        override suspend fun deleteOlderThan(timestamp: Long): Result<Int> = Result.success(0)
    }

    @Test
    fun detectMotion_blankCameraId_fails() =
        runTest {
            val uc = DetectMotionUseCase(FakeAnalyticsService())
            val cam = testCamera().copy(id = "  ")
            val r = uc(cam, frameRgb())
            assertTrue(r.isFailure)
        }

    @Test
    fun detectMotion_emptyFrame_fails() =
        runTest {
            val uc = DetectMotionUseCase(FakeAnalyticsService())
            val r = uc(testCamera(), ByteArray(0))
            assertTrue(r.isFailure)
        }

    @Test
    fun detectMotion_invalidThreshold_fails() =
        runTest {
            val uc = DetectMotionUseCase(FakeAnalyticsService())
            val r = uc(testCamera(), frameRgb(), threshold = 1.5f)
            assertTrue(r.isFailure)
        }

    @Test
    fun detectMotion_whenDetected_createsMotionEvent() =
        runTest {
            val fake =
                FakeAnalyticsService().apply {
                    motion = MotionDetectionResult(detected = true, confidence = 0.8f)
                }
            val events = CapturingEventRepository()
            val uc = DetectMotionUseCase(fake, events)
            val r = uc(testCamera(), frameRgb(), createEvent = true)
            assertTrue(r.isSuccess)
            assertEquals(1, events.added.size)
            assertEquals(EventType.MOTION_DETECTION, events.added[0].type)
        }

    @Test
    fun detectMotion_analyticsThrows_fails() =
        runTest {
            val fake = FakeAnalyticsService().apply { throwOnMotion = true }
            val uc = DetectMotionUseCase(fake)
            val r = uc(testCamera(), frameRgb())
            assertTrue(r.isFailure)
            assertIs<RuntimeException>(r.exceptionOrNull())
        }

    @Test
    fun detectObjects_invalidType_fails() =
        runTest {
            val uc = DetectObjectsUseCase(FakeAnalyticsService())
            val r = uc(testCamera(), frameRgb(), objectTypes = listOf("spaceship"))
            assertTrue(r.isFailure)
        }

    @Test
    fun detectObjects_whenObjectsFound_createsEvent() =
        runTest {
            val fake =
                FakeAnalyticsService().apply {
                    objects =
                        ObjectDetectionResult(
                            listOf(DetectedObject("person", 0.9f, BoundingBox(0, 0, 1, 2))),
                        )
                }
            val events = CapturingEventRepository()
            val uc = DetectObjectsUseCase(fake, events)
            val r = uc(testCamera(), frameRgb())
            assertTrue(r.isSuccess)
            assertEquals(1, events.added.size)
            assertEquals(EventType.OBJECT_DETECTION, events.added[0].type)
        }

    @Test
    fun trackObjects_newDetection_emitsEnteredEvent() =
        runTest {
            val events = CapturingEventRepository()
            val uc = TrackObjectsUseCase(events)
            val obj = DetectedObject("person", 0.9f, BoundingBox(0, 0, 10, 10))
            val r = uc(testCamera(), listOf(obj), frameTimestamp = 1000L, maxLostFrames = 2, createEvents = true)
            assertTrue(r.isSuccess)
            assertEquals(1, r.getOrNull()!!.trackedObjects.size)
            assertEquals(1, events.added.size)
            assertTrue(events.added[0].metadata["event"] == "object_entered")
        }

    @Test
    fun trackObjects_afterMaxLost_emitsLostAndRemovesTrack() =
        runTest {
            val events = CapturingEventRepository()
            val uc = TrackObjectsUseCase(events)
            val cam = testCamera()
            val box = BoundingBox(0, 0, 10, 10)
            uc(
                cam,
                listOf(DetectedObject("person", 0.9f, box)),
                frameTimestamp = 1L,
                maxLostFrames = 1,
                createEvents = true,
            )
            events.added.clear()
            uc(cam, emptyList(), frameTimestamp = 2L, maxLostFrames = 1, createEvents = true)
            uc(cam, emptyList(), frameTimestamp = 3L, maxLostFrames = 1, createEvents = true)
            assertTrue(events.added.any { it.metadata["event"] == "object_lost" })
            uc.clearTracks()
        }

    @Test
    fun detectFaces_whenFacesFound_createsEvent() =
        runTest {
            val fake =
                FakeAnalyticsService().apply {
                    faces =
                        FaceDetectionResult(
                            listOf(DetectedFace(BoundingBox(1, 1, 2, 2), 0.95f, landmarks = null, embedding = null)),
                        )
                }
            val events = CapturingEventRepository()
            val uc = DetectFacesUseCase(fake, events)
            val r = uc(testCamera(), frameRgb())
            assertTrue(r.isSuccess)
            assertEquals(1, events.added.size)
            assertEquals(EventType.FACE_DETECTION, events.added[0].type)
        }

    @Test
    fun recognizeLicensePlate_storesAndCreatesEvent() =
        runTest {
            val fake =
                FakeAnalyticsService().apply {
                    plates =
                        LicensePlateRecognitionResult(
                            listOf(
                                RecognizedLicensePlate("AB123C", 0.9f, "RUS", BoundingBox(0, 0, 4, 4)),
                            ),
                        )
                }
            val events = CapturingEventRepository()
            val platesRepo = CapturingLicensePlateRepository()
            val uc = RecognizeLicensePlateUseCase(fake, events, platesRepo)
            val r = uc(testCamera(), frameRgb(), country = "RUS")
            assertTrue(r.isSuccess)
            assertEquals(1, platesRepo.inserted.size)
            assertEquals("AB123C", platesRepo.inserted[0].plateNumber)
            assertEquals(1, events.added.size)
            assertEquals(EventType.LICENSE_PLATE_RECOGNITION, events.added[0].type)
        }

    @Test
    fun analyzeVideo_respectsCameraAnalyticsFlags() =
        runTest {
            val fake =
                FakeAnalyticsService().apply {
                    motion = MotionDetectionResult(true, 0.7f)
                    objects =
                        ObjectDetectionResult(
                            listOf(DetectedObject("person", 0.8f, BoundingBox(0, 0, 1, 1))),
                        )
                }
            val cam =
                testCamera().copy(
                    settings =
                        CameraSettings(
                            analytics =
                                AnalyticsSettings(
                                    motionDetection = true,
                                    objectDetection = true,
                                    faceRecognition = false,
                                    anprEnabled = false,
                                ),
                        ),
                )
            val uc =
                AnalyzeVideoUseCase(
                    DetectMotionUseCase(fake, null),
                    DetectObjectsUseCase(fake, null),
                    DetectFacesUseCase(fake, null),
                    RecognizeLicensePlateUseCase(fake, null, null),
                    eventRepository = null,
                    notificationService = null,
                )
            val r = uc(cam, frameRgb())
            assertTrue(r.isSuccess)
            val vr = r.getOrNull()!!
            assertTrue(vr.motionDetected)
            assertEquals(1, vr.objectsDetected.size)
            assertTrue(vr.facesDetected.isEmpty())
            assertTrue(vr.licensePlatesDetected.isEmpty())
            assertEquals(2, vr.events.size)
        }
}
