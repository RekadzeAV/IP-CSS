package com.company.ipcamera.server.routing

import com.company.ipcamera.server.security.SecurityLogger
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import kotlinx.serialization.json.*

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для безопасности
 *
 * Включает:
 * - CSP report endpoint
 * - Security monitoring endpoints
 */
fun Route.securityRoutes() {
    route("/security") {
        // POST /api/v1/security/csp-report - прием CSP violation reports
        post("/csp-report") {
            try {
                // CSP reports приходят в формате JSON с вложенным объектом "csp-report"
                val body = call.receiveText()
                val json = Json { ignoreUnknownKeys = true }

                // Парсим JSON вручную, так как структура может быть разной
                val reportJson = json.parseToJsonElement(body).jsonObject
                val cspReport = reportJson["csp-report"]?.jsonObject

                if (cspReport != null) {
                    val documentUri = cspReport["document-uri"]?.jsonPrimitive?.content
                    val violatedDirective = cspReport["violated-directive"]?.jsonPrimitive?.content
                    val blockedUri = cspReport["blocked-uri"]?.jsonPrimitive?.content
                    val sourceFile = cspReport["source-file"]?.jsonPrimitive?.content
                    val lineNumber = cspReport["line-number"]?.jsonPrimitive?.intOrNull

                    // Логируем нарушение CSP
                    logger.warn {
                        "CSP Violation Report: " +
                        "document-uri=$documentUri, " +
                        "violated-directive=$violatedDirective, " +
                        "blocked-uri=$blockedUri, " +
                        "source-file=$sourceFile, " +
                        "line-number=$lineNumber"
                    }

                    // Логируем через SecurityLogger
                    SecurityLogger.logSuspiciousActivity(
                        description = "CSP violation detected",
                        userId = null,
                        ipAddress = call.request.local.remoteHost,
                        details = mapOf(
                            "document_uri" to (documentUri ?: "unknown"),
                            "violated_directive" to (violatedDirective ?: "unknown"),
                            "blocked_uri" to (blockedUri ?: "unknown"),
                            "source_file" to (sourceFile ?: "unknown"),
                            "line_number" to (lineNumber?.toString() ?: "unknown")
                        )
                    )
                } else {
                    logger.warn { "CSP report received but csp-report field not found: $body" }
                }

                // Возвращаем 204 No Content (стандартный ответ для CSP reports)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: Exception) {
                logger.error(e) { "Error processing CSP report: ${e.message}" }
                // Все равно возвращаем 204, чтобы браузер не повторял отправку
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
