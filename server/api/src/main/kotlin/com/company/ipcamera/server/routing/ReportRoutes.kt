package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.ApiResponse
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.service.ReportService
import com.company.ipcamera.shared.domain.model.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Маршруты отчётов (блок 8.4). Заглушка: генерация и скачивание отчётов (CSV, PDF).
 */
fun Route.reportRoutes() {
    val reportService: ReportService by inject()

    authenticate("jwt-auth") {
        route("/reports") {
            get {
                requireRole(UserRole.VIEWER)
                val type = call.request.queryParameters["type"] ?: "EVENTS_SUMMARY"
                val format = call.request.queryParameters["format"] ?: "CSV"
                val cameraId = call.request.queryParameters["cameraId"]
                val from = call.request.queryParameters["from"]?.toLongOrNull()
                val to = call.request.queryParameters["to"]?.toLongOrNull()

                val reportType = try {
                    ReportService.ReportType.valueOf(type)
                } catch (_: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(success = false, data = null, message = "Invalid report type")
                    )
                    return@get
                }
                val exportFormat = try {
                    ReportService.ExportFormat.valueOf(format)
                } catch (_: Exception) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(success = false, data = null, message = "Invalid format; use CSV or PDF")
                    )
                    return@get
                }
                if (from != null && to != null && from > to) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(success = false, data = null, message = "'from' must be <= 'to'")
                    )
                    return@get
                }
                if (
                    reportType == ReportService.ReportType.LICENSE_PLATES_SUMMARY &&
                    cameraId.isNullOrBlank()
                ) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "cameraId is required for LICENSE_PLATES_SUMMARY"
                        )
                    )
                    return@get
                }

                val result = reportService.generateReport(
                    type = reportType,
                    format = exportFormat,
                    cameraId = cameraId?.takeIf { it.isNotBlank() },
                    fromTimestamp = from,
                    toTimestamp = to
                )
                result.fold(
                    onSuccess = { bytes ->
                        call.response.header(
                            HttpHeaders.ContentDisposition,
                            "attachment; filename=\"report-${reportType.name.lowercase()}.${reportService.fileExtension(exportFormat)}\""
                        )
                        call.respondBytes(
                            bytes,
                            when (exportFormat) {
                                ReportService.ExportFormat.CSV -> ContentType.Text.CSV
                                ReportService.ExportFormat.PDF -> ContentType.Application.Pdf
                            }
                        )
                    },
                    onFailure = {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<String>(success = false, data = null, message = it.message ?: "Report generation failed")
                        )
                    }
                )
            }
        }
    }
}
