package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.requireRole
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.usecase.GetNotificationsUseCase
import com.company.ipcamera.shared.domain.usecase.MarkNotificationAsReadUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

/**
 * DTO для ответа с уведомлением
 */
@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val priority: String,
    val cameraId: String? = null,
    val eventId: String? = null,
    val recordingId: String? = null,
    val read: Boolean,
    val timestamp: Long,
    val extras: Map<String, String> = emptyMap()
)

/**
 * DTO для пагинированного ответа с уведомлениями
 */
@Serializable
data class PaginatedNotificationResponse(
    val items: List<NotificationDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Маршруты для уведомлений
 *
 * Все маршруты требуют JWT аутентификации
 */
fun Route.notificationRoutes() {
    val getNotificationsUseCase: GetNotificationsUseCase by inject()
    val markNotificationAsReadUseCase: MarkNotificationAsReadUseCase by inject()

    authenticate("jwt-auth") {
        route("/notifications") {
            // GET /api/v1/notifications - список уведомлений
            get {
                try {
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.payload?.subject

                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                    val typeStr = call.request.queryParameters["type"]
                    val priorityStr = call.request.queryParameters["priority"]
                    val readStr = call.request.queryParameters["read"]

                    val type = typeStr?.let {
                        try { NotificationType.valueOf(it.uppercase()) }
                        catch (e: Exception) { null }
                    }
                    val priority = priorityStr?.let {
                        try { NotificationPriority.valueOf(it.uppercase()) }
                        catch (e: Exception) { null }
                    }
                    val read = readStr?.let {
                        when (it.lowercase()) {
                            "true" -> true
                            "false" -> false
                            else -> null
                        }
                    }

                    val result = getNotificationsUseCase(
                        userId = userId,
                        type = type,
                        priority = priority,
                        read = read,
                        page = page,
                        limit = limit
                    )

                    val notificationsDto = result.items.map { notification ->
                        NotificationDto(
                            id = notification.id,
                            title = notification.title,
                            message = notification.message,
                            type = notification.type.name,
                            priority = notification.priority.name,
                            cameraId = notification.cameraId,
                            eventId = notification.eventId,
                            recordingId = notification.recordingId,
                            read = notification.read,
                            timestamp = notification.timestamp,
                            extras = notification.extras
                        )
                    }

                    val response = PaginatedNotificationResponse(
                        items = notificationsDto,
                        total = result.total,
                        page = result.page,
                        limit = result.limit,
                        hasMore = result.hasMore
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = response,
                            message = "Notifications retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving notifications" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<PaginatedNotificationResponse>(
                            success = false,
                            data = null,
                            message = "Error retrieving notifications: ${e.message}"
                        )
                    )
                }
            }

            route("/{id}") {
                // POST /api/v1/notifications/{id}/read - отметить как прочитанное
                post("/read") {
                    try {
                        val id = call.parameters["id"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<NotificationDto>(
                                success = false,
                                data = null,
                                message = "Notification ID is required"
                            )
                        )

                        val result = markNotificationAsReadUseCase(id)

                        if (result.isSuccess) {
                            val notification = result.getOrThrow()
                            val notificationDto = NotificationDto(
                                id = notification.id,
                                title = notification.title,
                                message = notification.message,
                                type = notification.type.name,
                                priority = notification.priority.name,
                                cameraId = notification.cameraId,
                                eventId = notification.eventId,
                                recordingId = notification.recordingId,
                                read = notification.read,
                                timestamp = notification.timestamp,
                                extras = notification.extras
                            )

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = notificationDto,
                                    message = "Notification marked as read"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<NotificationDto>(
                                    success = false,
                                    data = null,
                                    message = result.exceptionOrNull()?.message ?: "Notification not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error marking notification as read" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<NotificationDto>(
                                success = false,
                                data = null,
                                message = "Error marking notification as read: ${e.message}"
                            )
                        )
                    }
                }
            }

            // POST /api/v1/notifications/read - массовое подтверждение уведомлений
            post("/read") {
                try {
                    val request = call.receive<MarkNotificationsAsReadRequest>()

                    if (request.ids.isEmpty()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<List<NotificationDto>>(
                                success = false,
                                data = null,
                                message = "IDs list cannot be empty"
                            )
                        )
                    }

                    val result = markNotificationAsReadUseCase(request.ids)

                    if (result.isSuccess) {
                        val notifications = result.getOrThrow()
                        val notificationsDto = notifications.map { notification ->
                            NotificationDto(
                                id = notification.id,
                                title = notification.title,
                                message = notification.message,
                                type = notification.type.name,
                                priority = notification.priority.name,
                                cameraId = notification.cameraId,
                                eventId = notification.eventId,
                                recordingId = notification.recordingId,
                                read = notification.read,
                                timestamp = notification.timestamp,
                                extras = notification.extras
                            )
                        }

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = notificationsDto,
                                message = "Notifications marked as read"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<List<NotificationDto>>(
                                success = false,
                                data = null,
                                message = result.exceptionOrNull()?.message ?: "Failed to mark notifications as read"
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error marking notifications as read" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<NotificationDto>>(
                            success = false,
                            data = null,
                            message = "Error marking notifications as read: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}

@Serializable
data class MarkNotificationsAsReadRequest(
    val ids: List<String>
)

