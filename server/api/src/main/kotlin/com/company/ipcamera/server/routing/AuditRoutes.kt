package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireAdmin
import com.company.ipcamera.server.security.AuditLogRepository
import com.company.ipcamera.server.security.SecurityEvent
import com.company.ipcamera.server.security.SecurityEventSeverity
import com.company.ipcamera.server.security.SecurityEventType
import com.company.ipcamera.server.security.SecurityLogger
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

/** Максимальное число клиентских событий в одном батче (защита от спама) */
const val MAX_CLIENT_EVENT_BATCH = 50

@Serializable
data class AuditEventDto(
    val type: String,
    val severity: String,
    val userId: String?,
    val username: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val details: Map<String, String>,
    val timestamp: Long
)

@Serializable
data class AuditIntegrityStatusDto(
    val valid: Boolean
)

@Serializable
data class ClientSecurityEventDto(
    val type: String,
    val severity: String,
    val details: Map<String, String> = emptyMap(),
    val timestamp: Long? = null
)

@Serializable
data class ClientSecurityEventBatchDto(
    val events: List<ClientSecurityEventDto>
)

@Serializable
data class ClientEventsAcceptedDto(
    val accepted: Int,
    val rejected: Int
)

/**
 * Маршруты аудита безопасности (4.3.3).
 * GET /api/v1/audit — только для администраторов.
 */
fun Route.auditRoutes() {
    val auditRepository: AuditLogRepository by inject()

    // === Приём клиентских событий безопасности (REMAINING_TASKS 2.6) ===
    // Без аутентификации: критические события (TLS/pinning-ошибки) могут возникать
    // до/без JWT. Защита от спама: лимит батча, валидация enum, обрезка деталей.
    // Запись ведётся только в audit-log (SecurityLogger), никаких данных не возвращается.
    post("/audit/client-events") {
        try {
            val request = call.receive<ClientSecurityEventBatchDto>()
            val ipAddress = call.request.local.remoteHost
            val userAgent = call.request.headers[HttpHeaders.UserAgent]
            var accepted = 0
            var rejected = 0

            request.events.take(MAX_CLIENT_EVENT_BATCH).forEach { event ->
                val type = runCatching { SecurityEventType.valueOf(event.type) }.getOrNull()
                val severity = runCatching { SecurityEventSeverity.valueOf(event.severity) }.getOrNull()
                if (type == null || severity == null) {
                    rejected++
                    return@forEach
                }
                SecurityLogger.log(
                    SecurityEvent(
                        type = type,
                        severity = severity,
                        userId = null,
                        username = null,
                        ipAddress = ipAddress,
                        userAgent = userAgent,
                        details = mapOf(
                            "source" to "client",
                            "client_timestamp" to (event.timestamp?.toString() ?: "unknown")
                        ) + event.details.entries.take(10)
                            .associate { it.key.take(50) to it.value.take(200) }
                    )
                )
                accepted++
            }
            rejected += request.events.size - accepted

            call.respond(
                HttpStatusCode.Accepted,
                ApiResponse(
                    success = true,
                    data = ClientEventsAcceptedDto(accepted = accepted, rejected = rejected),
                    message = "Client security events queued"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error receiving client security events" }
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<ClientEventsAcceptedDto>(
                    success = false,
                    data = null,
                    message = e.message ?: "Invalid client events payload"
                )
            )
        }
    }

    authenticate("jwt-auth") {
        route("/audit") {
            get {
                requireAdmin()
                try {
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
                    val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceIn(0, Int.MAX_VALUE) ?: 0
                    val typeStr = call.request.queryParameters["type"]
                    val type = typeStr?.let { runCatching { SecurityEventType.valueOf(it) }.getOrNull() }
                    val userId = call.request.queryParameters["userId"]
                    val fromTs = call.request.queryParameters["from"]?.toLongOrNull()
                    val toTs = call.request.queryParameters["to"]?.toLongOrNull()

                    val events = auditRepository.getRecent(limit = limit, offset = offset, type = type, userId = userId, fromTimestamp = fromTs, toTimestamp = toTs)
                    val dtos = events.map { e ->
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
                    }
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = dtos, message = "Audit log"))
                } catch (e: Exception) {
                    logger.error(e) { "Error fetching audit log" }
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<List<AuditEventDto>>(success = false, data = null, message = e.message ?: "Internal server error"))
                }
            }

            get("/integrity") {
                requireAdmin()
                try {
                    val valid = auditRepository.verifyIntegrityChain()
                    val status = if (valid) HttpStatusCode.OK else HttpStatusCode.Conflict
                    call.respond(
                        status,
                        ApiResponse(
                            success = valid,
                            data = AuditIntegrityStatusDto(valid = valid),
                            message = if (valid) "Audit integrity chain is valid" else "Audit integrity chain is broken"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error verifying audit integrity chain" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<AuditIntegrityStatusDto>(
                            success = false,
                            data = null,
                            message = e.message ?: "Internal server error"
                        )
                    )
                }
            }
        }
    }
}
