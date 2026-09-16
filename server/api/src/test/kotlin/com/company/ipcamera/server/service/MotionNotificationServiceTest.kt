package com.company.ipcamera.server.service

import com.company.ipcamera.server.repository.MotionEventRepository
import com.company.ipcamera.shared.domain.model.MotionEvent
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Юнит-тесты [MotionNotificationService]: маршрутизация уведомлений движения
 * в реальный [NotificationService] (ко 2 части итерации).
 */
class MotionNotificationServiceTest {

    private val snapshot = mockk<MotionSnapshotService>(relaxed = true)
    private val motionRepo = mockk<MotionEventRepository>(relaxed = true)

    private fun event(): MotionEvent = MotionEvent(
        id = "m-evt-1",
        cameraId = "cam-1",
        zoneId = "zone-A",
        timestamp = System.currentTimeMillis(),
        confidence = 0.87,
        area = 0.12,
        snapshotPath = null,
        recordingId = null,
    )

    @Test
    fun `sendNotification skips when NotificationService absent`() = runTest {
        val service = MotionNotificationService(motionRepo, snapshot, null)
        val result = service.sendNotification(event())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `sendNotification routes to NotificationService with HIGH priority`() = runTest {
        val notificationService = mockk<NotificationService>(relaxed = true)
        coEvery {
            notificationService.sendNotification(
                title = any(), message = any(), type = any(), priority = any(),
                userId = any(), cameraId = any(), eventId = any(), recordingId = any(),
                extras = any(),
            )
        } returns Result.success(
            Notification(
                id = "n-1", title = "Обнаружено движение", message = "m",
                type = NotificationType.EVENT, priority = NotificationPriority.HIGH,
                timestamp = System.currentTimeMillis(),
            )
        )

        val service = MotionNotificationService(motionRepo, snapshot, notificationService)
        val result = service.sendNotification(event())

        assertTrue(result.isSuccess)
        coVerify {
            notificationService.sendNotification(
                title = "Обнаружено движение",
                message = match { it.contains("cam-1") },
                type = NotificationType.EVENT,
                priority = NotificationPriority.HIGH,
                userId = null,
                cameraId = "cam-1",
                eventId = "m-evt-1",
                recordingId = null,
                extras = any(),
            )
        }
    }

    @Test
    fun `sendNotification propagates failure from NotificationService`() = runTest {
        val notificationService = mockk<NotificationService>(relaxed = true)
        coEvery {
            notificationService.sendNotification(
                title = any(), message = any(), type = any(), priority = any(),
                userId = any(), cameraId = any(), eventId = any(), recordingId = any(),
                extras = any(),
            )
        } returns Result.failure(RuntimeException("push down"))

        val service = MotionNotificationService(motionRepo, snapshot, notificationService)
        val result = service.sendNotification(event())

        assertFalse(result.isSuccess)
    }
}