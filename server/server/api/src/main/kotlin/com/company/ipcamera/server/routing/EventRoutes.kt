package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.dto.AcknowledgeEventsRequest
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.EventRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для управления событиями
 * Все маршруты требуют JWT аутентификации
 */
fun Route.eventRoutes() {
    val eventRepository: EventRepository by inject()

    authenticate("jwt-auth") {
        route("/events") {
            // GET /api/v1/events - список событий с фильтрацией и пагинацией
            // Минимум VIEWER для просмотра событий
            get {
                requireRole(UserRole.VIEWER)
                try {
                    val cameraId = call.request.queryParameters["cameraId"]
                    val eventTypeStr = call.request.queryParameters["type"]
                    val eventType = eventTypeStr?.let {
                        try {
                            com.company.ipcamera.shared.domain.model.EventType.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val severityStr = call.request.queryParameters["severity"]
                    val severity = severityStr?.let {
                        try {
                            com.company.ipcamera.shared.domain.model.EventSeverity.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val acknowledged = call.request.queryParameters["acknowledged"]?.toBoolean()
                    val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                    val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                    val result = eventRepository.getEvents(
                        type = eventType,
                        cameraId = cameraId,
                        severity = severity,
                        acknowledged = acknowledged,
                        startTime = startTime,
                        endTime = endTime,
                        page = page,
                        limit = limit
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = result.toDto(),
                            message = "Events retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving events" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<PaginatedEventResponse>(
                            success = false,
                            data = null,
                            message = "Error retrieving events: ${e.message}"
                        )
                    )
                }
            }

            route("/{id}") {
                // GET /api/v1/events/{id} - получение события по ID
                // Минимум VIEWER для просмотра события
                get {
                    requireRole(UserRole.VIEWER)
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<EventDto>(
                                success = false,
                                data = null,
                                message = "Event ID is required"
                            )
                        )

                        val event = eventRepository.getEventById(id)
                        if (event != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = event.toDto(),
                                    message = "Event retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<EventDto>(
                                    success = false,
                                    data = null,
                                    message = "Event not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error retrieving event" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<EventDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // DELETE /api/v1/events/{id} - удаление события
                // Минимум OPERATOR для удаления событий
                delete {
                    requireRole(UserRole.OPERATOR)
                    try {
                        val id = call.parameters["id"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Event ID is required"
                            )
                        )

                        val result = eventRepository.deleteEvent(id)
                        result.fold(
                            onSuccess = {
                                logger.info { "Event deleted: $id" }
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse<Unit>(
                                        success = true,
                                        data = null,
                                        message = "Event deleted successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error deleting event: $id" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        data = null,
                                        message = "Error deleting event: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error deleting event" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // POST /api/v1/events/{id}/acknowledge - подтверждение события
                // Минимум OPERATOR для подтверждения событий
                post("/acknowledge") {
                    requireRole(UserRole.OPERATOR)
                    try {
                        val id = call.parameters["id"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<EventDto>(
                                success = false,
                                data = null,
                                message = "Event ID is required"
                            )
                        )

                        // Получаем userId из JWT токена
                        val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                        val userId = principal?.payload?.subject ?: "unknown"

                        val result = eventRepository.acknowledgeEvent(id, userId)
                        result.fold(
                            onSuccess = { event ->
                                logger.info { "Event acknowledged: $id by user: $userId" }
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        data = event.toDto(),
                                        message = "Event acknowledged successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error acknowledging event: $id" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<EventDto>(
                                        success = false,
                                        data = null,
                                        message = "Error acknowledging event: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error acknowledging event" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<EventDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
            }

            // POST /api/v1/events/acknowledge - массовое подтверждение событий
            // Минимум OPERATOR для массового подтверждения
            post("/acknowledge") {
                requireRole(UserRole.OPERATOR)
                try {
                    val request = call.receive<AcknowledgeEventsRequest>()

                    if (request.ids.isEmpty()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<List<EventDto>>(
                                success = false,
                                data = null,
                                message = "Event IDs are required"
                            )
                        )
                        return@post
                    }

                    val ids = request.ids

                    // Получаем userId из JWT токена
                    val principal = call.principal<io.ktor.server.auth.jwt.JWTPrincipal>()
                    val userId = principal?.payload?.subject ?: "unknown"

                    val result = eventRepository.acknowledgeEvents(ids, userId)
                    result.fold(
                        onSuccess = { events ->
                            logger.info { "Events acknowledged: ${ids.size} events by user: $userId" }
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = events.map { it.toDto() },
                                    message = "Events acknowledged successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error acknowledging events" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<List<EventDto>>(
                                    success = false,
                                    data = null,
                                    message = "Error acknowledging events: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error acknowledging events" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<EventDto>>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/events/statistics - статистика событий
            // Минимум VIEWER для просмотра статистики
            get("/statistics") {
                requireRole(UserRole.VIEWER)
                try {
                    val cameraId = call.request.queryParameters["cameraId"]
                    val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                    val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()

                    val result = eventRepository.getEventStatistics(cameraId, startTime, endTime)
                    result.fold(
                        onSuccess = { statistics ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = statistics,
                                    message = "Event statistics retrieved successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error getting event statistics" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Map<String, Any>>(
                                    success = false,
                                    data = null,
                                    message = "Error getting event statistics: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting event statistics" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<Map<String, Any>>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
            }
        }
    }
}

