package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAdmin
import com.company.ipcamera.server.security.SecurityAlert
import com.company.ipcamera.server.security.SecurityAlertRepository
import com.company.ipcamera.server.security.SecurityAlertType
import com.company.ipcamera.server.security.SecurityDashboardSummary
import com.company.ipcamera.server.security.SecurityMonitoringService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

@Serializable
data class SecurityDashboardDto(
    val eventCountByType: Map<String, Int>,
    val recentEvents: List<AuditEventDto>,
    val activeAlerts: List<SecurityAlertDto>,
    val activeAlertCount: Int,
    val from: Long,
    val to: Long
)

@Serializable
data class SecurityAlertDto(
    val id: String,
    val type: String,
    val severity: String,
    val title: String,
    val description: String,
    val createdAt: Long,
    val resolvedAt: Long?,
    val metadata: Map<String, String>
)

/**
 * Маршруты мониторинга безопасности (4.3.3.2): дашборд и алерты.
 */
fun Route.securityMonitoringRoutes() {
    val monitoringService: SecurityMonitoringService by inject()
    val alertRepository: SecurityAlertRepository by inject()

    authenticate("jwt-auth") {
        route("/security") {
            get("/dashboard") {
                requireAdmin()
                try {
                    val from = call.request.queryParameters["from"]?.toLongOrNull()
                    val to = call.request.queryParameters["to"]?.toLongOrNull()
                    val summary = monitoringService.getDashboardSummary(fromTimestamp = from, toTimestamp = to)
                    val dto = SecurityDashboardDto(
                        eventCountByType = summary.eventCountByType.mapKeys { it.key.name },
                        recentEvents = summary.recentEvents.map { e ->
                            AuditEventDto(
                                type = e.type.name,
                                severity = e.severity.name,
                                userId = e.userId,
                                username = e.username,
                                ipAddress = e.ipAddress,
                                userAgent = e.userAgent,
                                details = e.details.mapValues { (_, v) -> v?.toString() ?: "" },
                                timestamp = e.timestamp
                            )
                        },
                        activeAlerts = summary.activeAlerts.map { a -> toAlertDto(a) },
                        activeAlertCount = summary.activeAlertCount,
                        from = summary.from,
                        to = summary.to
                    )
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = dto, message = "Dashboard"))
                } catch (e: Exception) {
                    logger.error(e) { "Error fetching security dashboard" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<SecurityDashboardDto>(success = false, data = null, message = e.message ?: "Error fetching security dashboard"))
                }
            }

            get("/alerts") {
                requireAdmin()
                try {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceIn(0, Int.MAX_VALUE) ?: 0
                    val resolved = call.request.queryParameters["resolved"]?.let { it.toBooleanStrictOrNull() }
                    val typeStr = call.request.queryParameters["type"]
                    val type = typeStr?.let { runCatching { SecurityAlertType.valueOf(it) }.getOrNull() }
                    val from = call.request.queryParameters["from"]?.toLongOrNull()
                    val to = call.request.queryParameters["to"]?.toLongOrNull()
                    val alerts = alertRepository.list(limit = limit, offset = offset, resolved = resolved, type = type, fromTimestamp = from, toTimestamp = to)
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = alerts.map { toAlertDto(it) }, message = "Alerts"))
                } catch (e: Exception) {
                    logger.error(e) { "Error fetching alerts" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<List<SecurityAlertDto>>(success = false, data = null, message = e.message ?: "Error fetching alerts"))
                }
            }

            post("/alerts/{id}/resolve") {
                requireAdmin()
                try {
                    val id = call.parameters["id"] ?: run {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, data = null, message = "Alert id required"))
                        return@post
                    }
                    val ok = alertRepository.resolve(id)
                    if (!ok) {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, data = null, message = "Alert not found"))
                        return@post
                    }
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = null, message = "Alert resolved"))
                } catch (e: Exception) {
                    logger.error(e) { "Error resolving alert" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, data = null, message = e.message ?: "Error resolving alert"))
                }
            }
        }
    }
}

private fun toAlertDto(a: SecurityAlert) = SecurityAlertDto(
    id = a.id,
    type = a.type.name,
    severity = a.severity.name,
    title = a.title,
    description = a.description ?: "",
    createdAt = a.createdAt,
    resolvedAt = a.resolvedAt,
    metadata = a.metadata
)
