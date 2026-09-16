package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты текущей (stub) реализации [NotificationService]:
 * проверяем, что отправка уведомления/события возвращает успешный результат и корректные данные.
 */
class NotificationServiceRoutingTest {

    private class TestNotificationRepository : NotificationRepository {
        var lastAdded: Notification? = null

        override suspend fun getNotifications(
            userId: String?,
            type: NotificationType?,
            priority: NotificationPriority?,
            read: Boolean?,
            page: Int,
            limit: Int
        ): PaginatedResult<Notification> = PaginatedResult(
            items = emptyList(), total = 0, page = page, limit = limit, hasMore = false
        )

        override suspend fun getNotificationById(id: String): Notification? = null

        override suspend fun addNotification(notification: Notification): Result<Notification> {
            lastAdded = notification
            return Result.success(notification)
        }

        override suspend fun markAsRead(id: String): Result<Notification> =
            Result.failure(UnsupportedOperationException("not needed in test"))

        override suspend fun markAsRead(ids: List<String>): Result<List<Notification>> =
            Result.failure(UnsupportedOperationException("not needed in test"))

        override suspend fun markAllAsRead(userId: String?): Result<Int> =
            Result.failure(UnsupportedOperationException("not needed in test"))

        override suspend fun deleteNotification(id: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("not needed in test"))

        override suspend fun getUnreadCount(userId: String?): Int = 0
    }

    @Test
    fun `sendNotification returns success notification`() = runBlocking {
        val repository = TestNotificationRepository()
        val service = NotificationService(repository)

        val result = service.sendNotification(
            title = "Test",
            message = "Hello",
            type = NotificationType.INFO,
            priority = NotificationPriority.NORMAL,
            userId = null,
            cameraId = "cam-1",
            eventId = null,
            recordingId = null,
            extras = mapOf("key" to "value")
        )

        assertTrue(result.isSuccess)
        val notification = result.getOrThrow()
        assertNotNull(notification.id)
        assertEquals("Test", notification.title)
        assertEquals("Hello", notification.message)
        assertEquals("cam-1", notification.cameraId)
    }

    @Test
    fun `sendEventNotification delegates to sendNotification`() = runBlocking {
        val service = NotificationService(TestNotificationRepository())

        val result = service.sendEventNotification(
            eventId = "evt-1",
            title = "Motion",
            message = "Motion detected",
            cameraId = "cam-1",
            priority = NotificationPriority.HIGH,
            userId = "u1"
        )

        assertTrue(result.isSuccess)
        val notification = result.getOrThrow()
        assertEquals("Motion", notification.title)
        assertEquals(NotificationType.EVENT, notification.type)
    }
}
