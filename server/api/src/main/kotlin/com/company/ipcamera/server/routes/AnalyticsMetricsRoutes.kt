package com.company.ipcamera.server.routes

import com.company.ipcamera.server.service.analytics.AnalyticsProductionMonitor
import com.company.ipcamera.server.service.VideoAnalyticsService
import com.company.ipcamera.server.service.analytics.CameraMetricsReport
import com.company.ipcamera.server.service.analytics.GlobalMetricsReport
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json

/**
 * REST Routes для Analytics Monitoring
 *
 * Endpoints:
 * - GET /api/v1/analytics/metrics - Глобальные метрики
 * - GET /api/v1/analytics/metrics?cameraId={id} - Метрики по камере
 * - GET /api/v1/analytics/health?cameraId={id} - Health check
 * - GET /api/v1/analytics/status?cameraId={id} - Статус детекторов
 * - GET /api/v1/analytics/recommendations?cameraId={id} - Рекомендации
 */
fun Route.analyticsMetricsRoutes(
    videoAnalyticsService: VideoAnalyticsService,
    productionMonitor: AnalyticsProductionMonitor
) {
    route("/api/v1/analytics") {
        // Глобальные метрики
        get("/metrics") {
            val globalMetrics = productionMonitor.getGlobalMetrics()
            call.respond(HttpStatusCode.OK, globalMetrics.toMetricsResponse())
        }

        // Метрики по камере
        get("/metrics") {
            val cameraId = call.request.queryParameters["cameraId"]
            
            if (cameraId.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, buildJsonObject {
                    put("error", "cameraId parameter is required")
                    put("hint", "Use: GET /api/v1/analytics/metrics?cameraId={id}")
                })
                return@get
            }

            val cameraMetrics = productionMonitor.getCameraMetrics(cameraId)
            
            if (cameraMetrics == null) {
                call.respond(HttpStatusCode.NotFound, buildJsonObject {
                    put("error", "Camera not found or analytics not running")
                    put("cameraId", cameraId)
                })
                return@get
            }

            call.respond(HttpStatusCode.OK, cameraMetrics.toCameraMetricsResponse())
        }

        // Health check
        get("/health") {
            val cameraId = call.request.queryParameters["cameraId"]
            
            if (cameraId.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, buildJsonObject {
                    put("error", "cameraId parameter is required")
                })
                return@get
            }

            val isHealthy = productionMonitor.isPipelineHealthy(cameraId)
            val recommendations = productionMonitor.getOptimizationRecommendations(cameraId)

            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("cameraId", cameraId)
                put("healthy", isHealthy)
                put("recommendations", JsonArray(recommendations.map { JsonPrimitive(it) }))
            })
        }

        // Статус детекторов
        get("/status") {
            val cameraId = call.request.queryParameters["cameraId"]
            
            if (cameraId.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, buildJsonObject {
                    put("error", "cameraId parameter is required")
                })
                return@get
            }

            val status = videoAnalyticsService.getAnalyticsStatus(cameraId)
            
            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("cameraId", cameraId)
                put("isRunning", status.isRunning)
                put("activeDetectors", JsonArray(status.activeDetectors.map { JsonPrimitive(it) }))
            })
        }

        // Статистика аналитики
        get("/stats") {
            val cameraId = call.request.queryParameters["cameraId"]
            
            if (cameraId.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, buildJsonObject {
                    put("error", "cameraId parameter is required")
                })
                return@get
            }

            val stats = videoAnalyticsService.getAnalyticsStats(cameraId)
            
            if (stats == null) {
                call.respond(HttpStatusCode.NotFound, buildJsonObject {
                    put("error", "Analytics not found for camera")
                    put("cameraId", cameraId)
                })
                return@get
            }

            call.respond(HttpStatusCode.OK, stats.toStatsResponse())
        }

        // Рекомендации по оптимизации
        get("/recommendations") {
            val cameraId = call.request.queryParameters["cameraId"]
            
            if (cameraId.isNullOrEmpty()) {
                call.respond(HttpStatusCode.BadRequest, buildJsonObject {
                    put("error", "cameraId parameter is required")
                })
                return@get
            }

            val recommendations = productionMonitor.getOptimizationRecommendations(cameraId)

            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("cameraId", cameraId)
                put("recommendations", JsonArray(recommendations.map { JsonPrimitive(it) }))
            })
        }

        // Список всех камер
        get("/cameras") {
            val allMetrics = productionMonitor.getAllCamerasStatus()
            
            val camerasJson = allMetrics.map { metric ->
                buildJsonObject {
                    put("cameraId", metric.cameraId)
                    put("cameraName", metric.cameraName)
                    put("isRunning", metric.isRunning)
                    put("totalFramesProcessed", metric.totalFramesProcessed)
                    put("totalErrors", metric.totalErrors)
                    put("currentFps", metric.currentFps)
                }
            }
            
            call.respond(HttpStatusCode.OK, buildJsonObject {
                put("totalCameras", allMetrics.size)
                put("activeCameras", allMetrics.count { it.isRunning })
                put("cameras", JsonArray(camerasJson))
            })
        }
    }
}

// ============================================================================
// Data classes для API ответов
// ============================================================================

@Serializable
data class GlobalMetricsResponse(
    val totalCameras: Int,
    val activeCameras: Int,
    val totalFramesProcessed: Long,
    val totalFramesSkipped: Long,
    val totalErrors: Long,
    val totalMotionDetections: Long,
    val totalObjectDetections: Long,
    val totalFaceDetections: Long,
    val totalAnprDetections: Long,
    val lastFrameProcessedAt: Long,
    val lastErrorAt: Long
)

@Serializable
data class CameraMetricsResponse(
    val cameraId: String,
    val cameraName: String,
    val frameSourceKind: String,
    val isRunning: Boolean,
    val totalFramesProcessed: Long,
    val totalFramesSkipped: Long,
    val totalErrors: Long,
    val averageProcessingTimeMs: Float,
    val currentFps: Float,
    val lastFrameProcessedAt: Long,
    val lastError: ErrorReportDto?,
    val errorHistory: List<ErrorReportDto>,
    val detectorStats: DetectorStatsDto?
)

@Serializable
data class CameraSummary(
    val cameraId: String,
    val cameraName: String,
    val isRunning: Boolean,
    val totalFramesProcessed: Long,
    val totalErrors: Long,
    val currentFps: Float
)

@Serializable
data class DetectorStatsDto(
    val motionDetections: Long,
    val objectDetections: Long,
    val faceDetections: Long,
    val anprDetections: Long,
    val lastMotionDetectionAt: Long,
    val lastObjectDetectionAt: Long,
    val lastFaceDetectionAt: Long,
    val lastAnprDetectionAt: Long
)

@Serializable
data class ErrorReportDto(
    val timestamp: Long,
    val errorType: String,
    val message: String
)

@Serializable
data class AnalyticsStatsResponse(
    val cameraId: String,
    val frameCount: Long,
    val isActive: Boolean,
    val motionDetectionsCount: Long,
    val objectDetectionsCount: Long,
    val faceDetectionsCount: Long,
    val licensePlateRecognitionsCount: Long,
    val lastProcessedTimestamp: Long?
)

// ============================================================================
// Extension functions для конвертации
// ============================================================================

fun GlobalMetricsReport.toMetricsResponse(): GlobalMetricsResponse {
    return GlobalMetricsResponse(
        totalCameras = totalCameras,
        activeCameras = activeCameras,
        totalFramesProcessed = totalFramesProcessed,
        totalFramesSkipped = totalFramesSkipped,
        totalErrors = totalErrors,
        totalMotionDetections = totalMotionDetections,
        totalObjectDetections = totalObjectDetections,
        totalFaceDetections = totalFaceDetections,
        totalAnprDetections = totalAnprDetections,
        lastFrameProcessedAt = lastFrameProcessedAt,
        lastErrorAt = lastErrorAt
    )
}

fun CameraMetricsReport.toCameraMetricsResponse(): CameraMetricsResponse {
    return CameraMetricsResponse(
        cameraId = cameraId,
        cameraName = cameraName,
        frameSourceKind = frameSourceKind,
        isRunning = isRunning,
        totalFramesProcessed = totalFramesProcessed,
        totalFramesSkipped = totalFramesSkipped,
        totalErrors = totalErrors,
        averageProcessingTimeMs = averageProcessingTimeMs,
        currentFps = currentFps,
        lastFrameProcessedAt = lastFrameProcessedAt,
        lastError = lastError?.let { e ->
            ErrorReportDto(e.timestamp, e.errorType, e.message)
        },
        errorHistory = errorHistory.map { e ->
            ErrorReportDto(e.timestamp, e.errorType, e.message)
        },
        detectorStats = detectorStats?.let { s ->
            DetectorStatsDto(
                motionDetections = s.motionDetections,
                objectDetections = s.objectDetections,
                faceDetections = s.faceDetections,
                anprDetections = s.anprDetections,
                lastMotionDetectionAt = s.lastMotionDetectionAt,
                lastObjectDetectionAt = s.lastObjectDetectionAt,
                lastFaceDetectionAt = s.lastFaceDetectionAt,
                lastAnprDetectionAt = s.lastAnprDetectionAt
            )
        }
    )
}

fun CameraMetricsReport.toCameraSummary(): CameraSummary {
    return CameraSummary(
        cameraId = cameraId,
        cameraName = cameraName,
        isRunning = isRunning,
        totalFramesProcessed = totalFramesProcessed,
        totalErrors = totalErrors,
        currentFps = currentFps
    )
}

fun com.company.ipcamera.server.service.VideoAnalyticsService.AnalyticsStats.toStatsResponse(): AnalyticsStatsResponse {
    return AnalyticsStatsResponse(
        cameraId = cameraId,
        frameCount = frameCount,
        isActive = isActive,
        motionDetectionsCount = motionDetectionsCount.toLong(),
        objectDetectionsCount = objectDetectionsCount.toLong(),
        faceDetectionsCount = faceDetectionsCount.toLong(),
        licensePlateRecognitionsCount = licensePlateRecognitionsCount.toLong(),
        lastProcessedTimestamp = lastProcessedTimestamp
    )
}
