package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.middleware.validateRequest
import com.company.ipcamera.server.validation.RequestValidator
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
    val pushTokenService: com.company.ipcamera.server.service.PushTokenService by inject()

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

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateMarkNotificationsAsReadRequest(it) }) {
                        return@post
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

            // GET /api/v1/notifications/push-tokens - список push токенов текущего пользователя
            get("/push-tokens") {
                try {
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.payload?.subject
                        ?: return@get call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "User is not authenticated"
                            )
                        )

                    val items = pushTokenService.list(userId).map {
                        PushTokenDto(
                            token = it.token,
                            platform = it.platform,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    }
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = items,
                            message = "Push tokens retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting push tokens" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting push tokens: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/notifications/push-tokens - регистрация/обновление push токена
            post("/push-tokens") {
                try {
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.payload?.subject
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "User is not authenticated"
                            )
                        )
                    val request = call.receive<RegisterPushTokenRequest>()
                    if (request.token.isBlank()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Push token is required"
                            )
                        )
                    }
                    val entry = pushTokenService.register(
                        userId = userId,
                        token = request.token,
                        platform = request.platform
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = PushTokenDto(
                                token = entry.token,
                                platform = entry.platform,
                                createdAt = entry.createdAt,
                                updatedAt = entry.updatedAt
                            ),
                            message = "Push token registered successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error registering push token" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error registering push token: ${e.message}"
                        )
                    )
                }
            }

            // DELETE /api/v1/notifications/push-tokens/{token} - отзыв push токена текущего пользователя
            delete("/push-tokens/{token}") {
                try {
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.payload?.subject
                        ?: return@delete call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "User is not authenticated"
                            )
                        )
                    val token = call.parameters["token"]
                        ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Push token path parameter is required"
                            )
                        )
                    val removed = pushTokenService.revoke(userId, token)
                    call.respond(
                        if (removed) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        ApiResponse<String>(
                            success = removed,
                            data = null,
                            message = if (removed) "Push token revoked successfully" else "Push token not found"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error revoking push token" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error revoking push token: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/notifications/push-tokens/revoke-invalid - пакетный отзыв невалидных токенов
            post("/push-tokens/revoke-invalid") {
                try {
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.payload?.subject
                        ?: return@post call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "User is not authenticated"
                            )
                        )
                    val request = call.receive<RevokeInvalidPushTokensRequest>()
                    if (request.tokens.isEmpty()) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "At least one push token is required"
                            )
                        )
                    }
                    val removed = pushTokenService.revokeMany(userId, request.tokens)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = RevokeInvalidPushTokensResponse(removed = removed),
                            message = "Invalid push tokens revoked"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error revoking invalid push tokens" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error revoking invalid push tokens: ${e.message}"
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

@Serializable
data class RegisterPushTokenRequest(
    val token: String,
    val platform: String = "unknown"
)

@Serializable
data class PushTokenDto(
    val token: String,
    val platform: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class RevokeInvalidPushTokensRequest(
    val tokens: List<String>
)

@Serializable
data class RevokeInvalidPushTokensResponse(
    val removed: Int
)



