package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Use case для отметки уведомления как прочитанного
 */
class MarkNotificationAsReadUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Отметить одно уведомление как прочитанное
     *
     * @param id ID уведомления
     */
    suspend operator fun invoke(id: String): Result<Notification> {
        return try {
            require(id.isNotBlank()) { "Notification ID cannot be empty" }

            val result = notificationRepository.markAsRead(id)

            if (result.isSuccess) {
                logger.debug { "Notification marked as read: $id" }
            } else {
                logger.error { "Failed to mark notification as read: ${result.exceptionOrNull()?.message}" }
            }

            result
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid notification ID" }
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Error marking notification as read" }
            Result.failure(e)
        }
    }

    /**
     * Отметить несколько уведомлений как прочитанные
     *
     * @param ids Список ID уведомлений
     */
    suspend operator fun invoke(ids: List<String>): Result<List<Notification>> {
        return try {
            require(ids.isNotEmpty()) { "IDs list cannot be empty" }
            require(ids.all { it.isNotBlank() }) { "All notification IDs must be non-empty" }

            val result = notificationRepository.markAsRead(ids)

            if (result.isSuccess) {
                logger.debug { "Marked ${ids.size} notifications as read" }
            } else {
                logger.error { "Failed to mark notifications as read: ${result.exceptionOrNull()?.message}" }
            }

            result
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid notification IDs" }
            Result.failure(e)
        } catch (e: Exception) {
            logger.error(e) { "Error marking notifications as read" }
            Result.failure(e)
        }
    }
}

