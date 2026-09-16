package com.company.ipcamera.server.service.analytics

import com.company.ipcamera.server.repository.ObjectDetectionConfigRepository
import com.company.ipcamera.server.repository.ObjectDetectionEventRepository
import com.company.ipcamera.server.service.MotionSnapshotService
import com.company.ipcamera.server.service.MotionNotificationService
import com.company.ipcamera.server.service.ObjectDetectionService
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Graceful degradation детектора объектов: без ffmpeg (frameSource=null)
 * [ObjectDetectionService.startDetection] возвращает failure с диагностикой;
 * статус не искажается.
 */
class ObjectDetectionServiceGracefulTest {

    private val configRepo = mockk<ObjectDetectionConfigRepository>(relaxed = true)
    private val eventRepo = mockk<ObjectDetectionEventRepository>(relaxed = true)
    private val snapshots = mockk<MotionSnapshotService>(relaxed = true)
    private val notifications = mockk<MotionNotificationService>(relaxed = true)
    private val cameras = mockk<CameraRepository>(relaxed = true)

    @Test
    fun `startDetection fails gracefully without frame source`() = runTest {
        val service = ObjectDetectionService(
            configRepository = configRepo,
            eventRepository = eventRepo,
            frameSource = null,
            snapshotService = snapshots,
            notificationService = notifications,
            cameraRepository = cameras,
        )
        val result = service.startDetection("cam-1", "rtsp://example/stream")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("unavailable"))
        assertFalse(service.getDetectorStatus("cam-1")!!.isRunning)
    }
}
