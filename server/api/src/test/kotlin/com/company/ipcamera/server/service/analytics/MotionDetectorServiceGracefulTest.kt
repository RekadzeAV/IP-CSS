package com.company.ipcamera.server.service.analytics

import com.company.ipcamera.server.repository.MotionConfigRepository
import com.company.ipcamera.server.repository.MotionEventRepository
import com.company.ipcamera.server.service.MotionDetectorService
import com.company.ipcamera.server.service.MotionNotificationService
import com.company.ipcamera.server.service.MotionSnapshotService
import com.company.ipcamera.shared.domain.model.MotionConfig
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Graceful degradation детектора движения: без ffmpeg (frameSource=null)
 * [MotionDetectorService.startDetection] возвращает failure с диагностикой;
 * статус детектора не искажается (isRunning=false), повторные вызовы безопасны.
 */
class MotionDetectorServiceGracefulTest {

    private val configRepo = mockk<MotionConfigRepository>(relaxed = true)
    private val eventRepo = mockk<MotionEventRepository>(relaxed = true)
    private val notifications = mockk<MotionNotificationService>(relaxed = true)
    private val snapshots = mockk<MotionSnapshotService>(relaxed = true)
    private val cameras = mockk<CameraRepository>(relaxed = true)

    private fun config() = MotionConfig(
        id = "cfg-1", cameraId = "cam-1", enabled = true,
        sensitivity = 0.5, minArea = 0.01, zones = emptyList(),
        cooldownSeconds = 10, recordOnMotion = false, notifyOnMotion = true,
        createdAt = 0, updatedAt = 0,
    )

    @Test
    fun `startDetection fails gracefully without frame source`() = runTest {
        val service = MotionDetectorService(
            motionConfigRepository = configRepo,
            motionEventRepository = eventRepo,
            notificationService = notifications,
            snapshotService = snapshots,
            frameSource = null,
            cameraRepository = cameras,
        )
        val result = service.startDetection("cam-1", config())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("unavailable"))
        // Статус не искажается: детектор не числится запущенным.
        assertFalse(service.getDetectorStatus("cam-1")!!.isRunning)
        assertTrue(service.getAllDetectorStatuses().isEmpty())
    }
}
