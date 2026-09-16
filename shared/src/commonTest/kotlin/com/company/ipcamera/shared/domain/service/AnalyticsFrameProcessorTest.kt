@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.AnalyticsExecutionLocation
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
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.usecase.DetectFacesUseCase
import com.company.ipcamera.shared.domain.usecase.DetectMotionUseCase
import com.company.ipcamera.shared.domain.usecase.DetectObjectsUseCase
import com.company.ipcamera.shared.domain.usecase.RecognizeLicensePlateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalyticsFrameProcessorTest {
    private class FakeAnalytics : AnalyticsService {
        var motion = MotionDetectionResult(detected = true, confidence = 0.9f)
        var objects = ObjectDetectionResult(emptyList())
        var faces = FaceDetectionResult(emptyList())
        var plates = LicensePlateRecognitionResult(emptyList())

        override suspend fun detectMotion(
            camera: Camera,
            frameData: ByteArray,
            previousFrameData: ByteArray?,
            zones: List<DetectionZone>,
            threshold: Float,
            minArea: Int?,
        ): MotionDetectionResult = motion

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
        ): List<TrackInfo> = emptyList()

        override suspend fun compareFaces(
            embedding1: FloatArray,
            embedding2: FloatArray,
        ): Float = 0f
    }

    private class CapturingVsaasClient : VsaasAnalyticsIngestClient {
        var submits = 0

        override suspend fun submitFrame(
            cameraId: String,
            timestampMs: Long,
            width: Int,
            height: Int,
            frameData: ByteArray,
        ): Result<Unit> {
            submits++
            return Result.success(Unit)
        }
    }

    private class CapturingEvents : EventRepository {
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

    private fun camera(analytics: AnalyticsSettings): Camera =
        Camera(
            id = "c1",
            name = "C",
            url = "rtsp://x",
            resolution = Resolution(4, 4),
            status = CameraStatus.ONLINE,
            settings = CameraSettings(analytics = analytics),
        )

    private fun rgb4x4(): ByteArray = ByteArray(4 * 4 * 3) { 7 }

    @Test
    fun motion_secondFrameWithinThrottle_skipsSecondEvent() =
        runTest {
            val events = CapturingEvents()
            val fake = FakeAnalytics().apply { motion = MotionDetectionResult(detected = true, confidence = 0.9f) }
            val motionUc = DetectMotionUseCase(fake, events)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val processor =
                AnalyticsFrameProcessor(
                    camera =
                        camera(
                            AnalyticsSettings(
                                motionDetection = true,
                                motionEventCooldownMs = 60_000L,
                            ),
                        ),
                    detectMotionUseCase = motionUc,
                    detectObjectsUseCase = null,
                    motionThrottleMs = 60_000L,
                    scope = scope,
                )
            val f = rgb4x4()
            processor.processFrame(f, 4, 4)
            processor.processFrame(f, 4, 4)
            assertEquals(1, events.added.size)
            processor.dispose()
        }

    @Test
    fun objectDetection_whenEnabled_emitsOncePerObjectThrottleWindow() =
        runTest {
            val events = CapturingEvents()
            val fake =
                FakeAnalytics().apply {
                    motion = MotionDetectionResult(detected = false, confidence = 0f)
                    objects =
                        ObjectDetectionResult(
                            listOf(DetectedObject("person", 0.9f, BoundingBox(0, 0, 2, 2))),
                        )
                }
            val motionUc = DetectMotionUseCase(fake, events)
            val objectUc = DetectObjectsUseCase(fake, events)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val processor =
                AnalyticsFrameProcessor(
                    camera =
                        camera(
                            AnalyticsSettings(
                                motionDetection = true,
                                objectDetection = true,
                                motionEventCooldownMs = 60_000L,
                                objectTypes = listOf("person"),
                            ),
                        ),
                    detectMotionUseCase = motionUc,
                    detectObjectsUseCase = objectUc,
                    motionThrottleMs = 60_000L,
                    objectThrottleMs = 60_000L,
                    scope = scope,
                )
            val f = rgb4x4()
            processor.processFrame(f, 4, 4)
            processor.processFrame(f, 4, 4)
            val objectEvents = events.added.filter { it.type == EventType.OBJECT_DETECTION }
            assertEquals(1, objectEvents.size)
            processor.dispose()
        }

    @Test
    fun faceDetection_whenEnabled_throttled() =
        runTest {
            val events = CapturingEvents()
            val fake =
                FakeAnalytics().apply {
                    motion = MotionDetectionResult(detected = false, confidence = 0f)
                    faces =
                        FaceDetectionResult(
                            listOf(DetectedFace(BoundingBox(0, 0, 2, 2), 0.92f, landmarks = null, embedding = null)),
                        )
                }
            val motionUc = DetectMotionUseCase(fake, events)
            val faceUc = DetectFacesUseCase(fake, events)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val processor =
                AnalyticsFrameProcessor(
                    camera =
                        camera(
                            AnalyticsSettings(
                                motionDetection = true,
                                motionEventCooldownMs = 60_000L,
                                faceRecognition = true,
                                faceRecognitionConfidenceThreshold = 0.5f,
                            ),
                        ),
                    detectMotionUseCase = motionUc,
                    detectObjectsUseCase = null,
                    detectFacesUseCase = faceUc,
                    recognizeLicensePlateUseCase = null,
                    motionThrottleMs = 60_000L,
                    faceThrottleMs = 60_000L,
                    scope = scope,
                )
            val f = rgb4x4()
            processor.processFrame(f, 4, 4)
            processor.processFrame(f, 4, 4)
            assertEquals(1, events.added.count { it.type == EventType.FACE_DETECTION })
            processor.dispose()
        }

    @Test
    fun anpr_whenEnabled_throttled() =
        runTest {
            val events = CapturingEvents()
            val fake =
                FakeAnalytics().apply {
                    motion = MotionDetectionResult(detected = false, confidence = 0f)
                    plates =
                        LicensePlateRecognitionResult(
                            listOf(RecognizedLicensePlate("X777XX", 0.91f, "RUS", BoundingBox(0, 0, 4, 4))),
                        )
                }
            val motionUc = DetectMotionUseCase(fake, events)
            val plateUc = RecognizeLicensePlateUseCase(fake, events, null)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val processor =
                AnalyticsFrameProcessor(
                    camera =
                        camera(
                            AnalyticsSettings(
                                motionDetection = true,
                                motionEventCooldownMs = 60_000L,
                                anprEnabled = true,
                                anprConfidenceThreshold = 0.7f,
                            ),
                        ),
                    detectMotionUseCase = motionUc,
                    detectObjectsUseCase = null,
                    detectFacesUseCase = null,
                    recognizeLicensePlateUseCase = plateUc,
                    motionThrottleMs = 60_000L,
                    licensePlateThrottleMs = 60_000L,
                    scope = scope,
                )
            val f = rgb4x4()
            processor.processFrame(f, 4, 4)
            processor.processFrame(f, 4, 4)
            assertEquals(1, events.added.count { it.type == EventType.LICENSE_PLATE_RECOGNITION })
            processor.dispose()
        }

    @Test
    fun dispose_cancelsScope_noCrashOnSecondDispose() =
        runTest {
            val fake = FakeAnalytics()
            val motionUc = DetectMotionUseCase(fake, null)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val processor =
                AnalyticsFrameProcessor(
                    camera = camera(AnalyticsSettings(motionDetection = true)),
                    detectMotionUseCase = motionUc,
                    scope = scope,
                )
            processor.dispose()
            processor.dispose()
            assertFalse(scope.isActive)
        }

    @Test
    fun vsaas_executionLocation_routesFramesToIngest_andSkipsLocalEvents() =
        runTest {
            val events = CapturingEvents()
            val fake = FakeAnalytics().apply { motion = MotionDetectionResult(detected = true, confidence = 0.9f) }
            val motionUc = DetectMotionUseCase(fake, events)
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val vsaas = CapturingVsaasClient()
            val processor =
                AnalyticsFrameProcessor(
                    camera =
                        camera(
                            AnalyticsSettings(
                                motionDetection = true,
                                executionLocation = AnalyticsExecutionLocation.VSAAS,
                            ),
                        ),
                    detectMotionUseCase = motionUc,
                    vsaasIngestClient = vsaas,
                    scope = scope,
                )
            processor.processFrame(rgb4x4(), 4, 4)
            assertEquals(1, vsaas.submits)
            assertTrue(events.added.isEmpty())
            processor.dispose()
        }
}
