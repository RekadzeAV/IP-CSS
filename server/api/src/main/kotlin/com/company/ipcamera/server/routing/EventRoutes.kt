package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.dto.AcknowledgeEventsRequest
import com.company.ipcamera.server.service.ExportService
import com.company.ipcamera.server.service.ApiMetricsService
import com.company.ipcamera.shared.domain.repository.EventRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(
        this.entries
            .mapNotNull { (key, value) ->
                (key as? String)?.let { validKey -> validKey to value.toJsonElement() }
            }
            .toMap()
    )
    is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
    is Array<*> -> JsonArray(this.map { it.toJsonElement() })
    else -> JsonPrimitive(this.toString())
}

fun Route.eventRoutes() {
    val eventRepository: EventRepository by inject()
    val exportService: ExportService by inject()
    val apiMetricsService: ApiMetricsService by inject()

    authenticate("jwt-auth") {
        route("/events") {
            get { handleListEvents(eventRepository, call) }
            post("/acknowledge") { handleBulkAcknowledge(eventRepository, call) }
            get("/statistics") { handleGetStatistics(eventRepository, apiMetricsService, call) }
            get("/export/csv") { handleExportCsv(eventRepository, exportService, call) }
            get("/export/json") { handleExportJson(eventRepository, exportService, call) }
            route("/{id}") {
                get { handleGetEvent(eventRepository, call) }
                delete { handleDeleteEvent(eventRepository, call) }
                post("/acknowledge") { handleAcknowledgeEvent(eventRepository, call) }
            }
        }
    }
}

data class EventFilterParams(
    val cameraId: String?,
    val type: com.company.ipcamera.shared.domain.model.EventType?,
    val severity: com.company.ipcamera.shared.domain.model.EventSeverity?,
    val acknowledged: Boolean?,
    val startTime: Long?,
    val endTime: Long?
)

private suspend fun parseEventFilters(call: ApplicationCall): EventFilterParams {
    val eventTypeStr = call.request.queryParameters["type"]
    val severityStr = call.request.queryParameters["severity"]
    return EventFilterParams(
        cameraId = call.request.queryParameters["cameraId"],
        type = eventTypeStr?.let { try { com.company.ipcamera.shared.domain.model.EventType.valueOf(it.uppercase()) } catch (e: Exception) { null } },
        severity = severityStr?.let { try { com.company.ipcamera.shared.domain.model.EventSeverity.valueOf(it.uppercase()) } catch (e: Exception) { null } },
        acknowledged = call.request.queryParameters["acknowledged"]?.toBoolean(),
        startTime = call.request.queryParameters["startTime"]?.toLongOrNull(),
        endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
    )
}

private suspend fun handleListEvents(eventRepository: EventRepository, call: ApplicationCall) {
    try {
        val filters = parseEventFilters(call)
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val result = eventRepository.getEvents(
            type = filters.type, cameraId = filters.cameraId, severity = filters.severity,
            acknowledged = filters.acknowledged, startTime = filters.startTime, endTime = filters.endTime,
            page = page, limit = limit
        )
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.toDto(), message = "Events retrieved successfully"))
    } catch (e: Exception) {
        call.respondError("Error retrieving events: ${e.message}")
    }
}

private suspend fun handleGetEvent(eventRepository: EventRepository, call: ApplicationCall) {
    try {
        val id = call.parameters["id"] ?: run { call.respond(HttpStatusCode.BadRequest, ApiResponse<EventDto>(success = false, data = null, message = "Event ID is required")); return }
        val event = eventRepository.getEventById(id)
        if (event != null) {
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = event.toDto(), message = "Event retrieved successfully"))
        } else {
            call.respond(HttpStatusCode.NotFound, ApiResponse<EventDto>(success = false, data = null, message = "Event not found"))
        }
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleDeleteEvent(eventRepository: EventRepository, call: ApplicationCall) {
    try {
        val id = call.parameters["id"] ?: run { call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "Event ID is required")); return }
        val result = eventRepository.deleteEvent(id)
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "Event deleted successfully")) },
            onFailure = { call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "Error deleting event: ${it.message}")) }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleAcknowledgeEvent(eventRepository: EventRepository, call: ApplicationCall) {
    try {
        val id = call.parameters["id"] ?: run { call.respond(HttpStatusCode.BadRequest, ApiResponse<EventDto>(success = false, data = null, message = "Event ID is required")); return }
        val result = eventRepository.acknowledgeEvent(id, "system")
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = it.toDto(), message = "Event acknowledged successfully")) },
            onFailure = { call.respond(HttpStatusCode.BadRequest, ApiResponse<EventDto>(success = false, data = null, message = "Error acknowledging event: ${it.message}")) }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleBulkAcknowledge(eventRepository: EventRepository, call: ApplicationCall) {
    try {
        val request = call.receive<AcknowledgeEventsRequest>()
        val result = eventRepository.acknowledgeEvents(request.ids, "system")
        result.fold(
            onSuccess = { call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = it.map { it.toDto() }, message = "Events acknowledged successfully")) },
            onFailure = { call.respond(HttpStatusCode.BadRequest, ApiResponse<List<EventDto>>(success = false, data = null, message = "Error acknowledging events: ${it.message}")) }
        )
    } catch (e: Exception) {
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleGetStatistics(eventRepository: EventRepository, apiMetricsService: ApiMetricsService, call: ApplicationCall) {
    try {
        val cameraId = call.request.queryParameters["cameraId"]
        val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
        val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
        val result = eventRepository.getEventStatistics(cameraId, startTime, endTime)
        result.fold(
            onSuccess = {
                apiMetricsService.markEventStatisticsSuccess()
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.getOrElse { null }?.toJsonElement(), message = "Event statistics retrieved successfully"))
            },
            onFailure = {
                apiMetricsService.markEventStatisticsFailure()
                call.respond(HttpStatusCode.ServiceUnavailable, ApiResponse<JsonElement>(success = false, data = null, message = "Error getting event statistics: ${it.message}"))
            }
        )
    } catch (e: Exception) {
        apiMetricsService.markEventStatisticsFailure()
        call.respondError("Internal server error: ${e.message}")
    }
}

private suspend fun handleExportCsv(eventRepository: EventRepository, exportService: ExportService, call: ApplicationCall) {
    try {
        exportCsvOrJson(eventRepository, exportService, call, "csv", ContentType.Text.CSV)
    } catch (e: Exception) {
        call.respondError("Error exporting events to CSV: ${e.message}")
    }
}

private suspend fun handleExportJson(eventRepository: EventRepository, exportService: ExportService, call: ApplicationCall) {
    try {
        exportCsvOrJson(eventRepository, exportService, call, "json", ContentType.Application.Json)
    } catch (e: Exception) {
        call.respondError("Error exporting events to JSON: ${e.message}")
    }
}

private suspend fun exportCsvOrJson(
    eventRepository: EventRepository,
    exportService: ExportService,
    call: ApplicationCall,
    format: String,
    contentType: io.ktor.http.ContentType
) {
    val filters = parseEventFilters(call)
    val result = eventRepository.getEvents(
        type = filters.type, cameraId = filters.cameraId, severity = filters.severity,
        acknowledged = filters.acknowledged, startTime = filters.startTime, endTime = filters.endTime,
        page = 1, limit = Int.MAX_VALUE
    )
    val content = if (format == "csv") exportService.exportEventsToCsv(result.items) else exportService.exportEventsToJson(result.items)
    val extension = if (format == "csv") "csv" else "json"
    call.response.headers.append("Content-Disposition", "attachment; filename=\"events_${System.currentTimeMillis()}.$extension\"")
    call.response.headers.append("Content-Type", "$contentType; charset=utf-8")
    call.respondText(content, contentType)
}

private suspend fun ApplicationCall.respondError(message: String) {
    respond(HttpStatusCode.InternalServerError, ApiResponse<String>(success = false, data = null, message = message))
}