package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Use case для получения списка уведомлений
 */
class GetNotificationsUseCase(
    private val notificationRepository: NotificationRepository
) {
    /**
     * Получить список уведомлений с фильтрацией и пагинацией
     *
     * @param userId ID пользователя (если null, возвращаются все уведомления)
     * @param type Тип уведомления для фильтрации
     * @param priority Приоритет уведомления для фильтрации
     * @param read Статус прочтения для фильтрации (true - прочитанные, false - непрочитанные, null - все)
     * @param page Номер страницы (начинается с 1)
     * @param limit Количество элементов на странице (максимум 100)
     */
    suspend operator fun invoke(
        userId: String? = null,
        type: NotificationType? = null,
        priority: NotificationPriority? = null,
        read: Boolean? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResult<Notification> {
        val validPage = if (page < 1) 1 else page
        val validLimit = when {
            limit <= 0 -> 20
            limit > 100 -> 100
            else -> limit
        }

        return notificationRepository.getNotifications(
            userId = userId?.takeIf { it.isNotBlank() },
            type = type,
            priority = priority,
            read = read,
            page = validPage,
            limit = validLimit
        )
    }
}

