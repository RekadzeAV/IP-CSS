package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.middleware.validateRequest
import com.company.ipcamera.server.service.VideoAnalyticsService
import com.company.ipcamera.server.service.VideoStreamService
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.server.validation.WebhookQueryValidator
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
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
 * РњР°СЂС€СЂСѓС‚С‹ РґР»СЏ СѓРїСЂР°РІР»РµРЅРёСЏ AI-Р°РЅР°Р»РёС‚РёРєРѕР№
 * Р’СЃРµ РјР°СЂС€СЂСѓС‚С‹ С‚СЂРµР±СѓСЋС‚ JWT Р°СѓС‚РµРЅС‚РёС„РёРєР°С†РёРё
 */
fun Route.analyticsRoutes() {
    val videoAnalyticsService: VideoAnalyticsService? by inject()
    val cameraRepository: CameraRepository by inject()
    val licensePlateRepository: LicensePlateRepository by inject()
    val videoStreamService: VideoStreamService? by inject()

    authenticate("jwt-auth") {
        route("/cameras/{id}/analytics") {
            val cameraIdParam: (io.ktor.server.application.ApplicationCall) -> String? = { it.parameters["id"] }

            // GET /api/v1/cameras/{id}/analytics - РїРѕР»СѓС‡РёС‚СЊ РєРѕРЅС„РёРіСѓСЂР°С†РёСЋ Р°РЅР°Р»РёС‚РёРєРё
            get {
                requireRole(UserRole.VIEWER)
                val cameraId = cameraIdParam(call) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                try {
                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Camera not found"
                            )
                        )
                        return@get
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = camera.settings.analytics.toDto(),
                            message = "Analytics configuration retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics config for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics config: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/cameras/{id}/analytics - РЅР°СЃС‚СЂРѕР№РєР° Р°РЅР°Р»РёС‚РёРєРё
            post {
                requireRole(UserRole.OPERATOR)
                val cameraId = cameraIdParam(call) ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                try {
                    val request = call.receive<AnalyticsConfigDto>()

                    // Р’Р°Р»РёРґР°С†РёСЏ Р·Р°РїСЂРѕСЃР°
                    if (!validateRequest(request) { RequestValidator.validateAnalyticsConfig(it) }) {
                        return@post
                    }

                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Camera not found"
                            )
                        )
                        return@post
                    }

                    // РћР±РЅРѕРІР»СЏРµРј РєРѕРЅС„РёРіСѓСЂР°С†РёСЋ Р°РЅР°Р»РёС‚РёРєРё (merge СЃ С‚РµРєСѓС‰РµР№, С‡С‚РѕР±С‹ РЅРµ СЃР±СЂРѕСЃРёС‚СЊ РїРѕР»СЏ РІРЅРµ DTO)
                    val updatedCamera = camera.copy(
                        settings = camera.settings.copy(
                            analytics = request.toDomain(camera.settings.analytics)
                        )
                    )

                    val result = cameraRepository.updateCamera(updatedCamera)
                    result.fold(
                        onSuccess = { updated ->
                            // РћР±РЅРѕРІР»СЏРµРј РєРѕРЅС„РёРіСѓСЂР°С†РёСЋ РІ Р°РєС‚РёРІРЅРѕР№ Р°РЅР°Р»РёС‚РёРєРµ
                            videoAnalyticsService?.updateAnalyticsConfig(cameraId, updated)

                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = updated.settings.analytics.toDto(),
                                    message = "Analytics configuration updated successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error updating analytics config: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error updating analytics config for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error updating analytics config: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/{id}/analytics/results - РїРѕР»СѓС‡РёС‚СЊ СЂРµР·СѓР»СЊС‚Р°С‚С‹ Р°РЅР°Р»РёС‚РёРєРё
            get("/results") {
                requireRole(UserRole.VIEWER)
                val cameraId = cameraIdParam(call) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                if (videoAnalyticsService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics service is not available"
                        )
                    )
                }
                val vas = videoAnalyticsService!!

                try {
                    val from = call.request.queryParameters["from"]?.toLongOrNull() ?: 0
                    val to = call.request.queryParameters["to"]?.toLongOrNull() ?: Long.MAX_VALUE
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                    val results = vas.getAnalyticsResults(
                        cameraId = cameraId,
                        from = from,
                        to = to,
                        page = page,
                        limit = limit
                    )

                    val total = vas.countAnalyticsResults(cameraId, from, to)
                    val hasMore = (page * limit) < total

                    val resultsDto = results.map { result ->
                        AnalyticsResultDto(
                            cameraId = result.cameraId,
                            timestamp = result.timestamp,
                            motionDetection = result.motionDetection?.let { com.company.ipcamera.server.dto.MotionDetectionResultDto(it.detected, it.confidence, emptyList(), it.timestamp) },
                            objectDetection = result.objectDetection?.let { com.company.ipcamera.server.dto.ObjectDetectionResultDto(emptyList(), it.timestamp) },
                            faceDetection = result.faceDetection?.let { com.company.ipcamera.server.dto.FaceDetectionResultDto(emptyList(), it.timestamp) },
                            licensePlateRecognition = result.licensePlateRecognition?.let { com.company.ipcamera.server.dto.LicensePlateRecognitionResultDto(emptyList(), it.timestamp) }
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = PaginatedAnalyticsResultResponse(
                                items = resultsDto,
                                total = total.toInt(),
                                page = page,
                                limit = limit,
                                hasMore = hasMore
                            ),
                            message = "Analytics results retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics results for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics results: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/{id}/analytics/status вЂ” РІРєР»СЋС‡РµРЅР° Р»Рё Р°РЅР°Р»РёС‚РёРєР°, Р°РєС‚РёРІРЅС‹Рµ РґРµС‚РµРєС‚РѕСЂС‹ (Р¤Р°Р·Р° 7)
            get("/status") {
                requireRole(UserRole.VIEWER)
                val cameraId = cameraIdParam(call) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                if (videoAnalyticsService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics service is not available"
                        )
                    )
                }
                val vas = videoAnalyticsService!!

                try {
                    val status = vas.getAnalyticsStatus(cameraId)
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = AnalyticsStatusDto(
                                cameraId = status.cameraId,
                                isRunning = status.isRunning,
                                activeDetectors = status.activeDetectors
                            ),
                            message = "Analytics status retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics status for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics status: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/{id}/analytics/metrics вЂ” РјРµС‚СЂРёРєРё РїР°Р№РїР»Р°Р№РЅР°: РѕР±СЂР°Р±РѕС‚Р°РЅРЅС‹Рµ/РїСЂРѕРїСѓС‰РµРЅРЅС‹Рµ РєР°РґСЂС‹ (Р¤Р°Р·Р° 8)
            get("/metrics") {
                requireRole(UserRole.VIEWER)
                val cameraId = cameraIdParam(call) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(success = false, data = null, message = "Camera ID is required")
                )
                if (videoAnalyticsService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(success = false, data = null, message = "Analytics service is not available")
                    )
                }
                val vas = videoAnalyticsService!!
                try {
                    val metrics = vas.getAnalyticsMetrics(cameraId)
                    if (metrics != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = AnalyticsMetricsDto(
                                    cameraId = metrics.cameraId,
                                    processedFrames = metrics.processedFrames,
                                    skippedFrames = metrics.skippedFrames,
                                    frameSource = metrics.frameSource,
                                    lastError = metrics.lastError,
                                    lastErrorAt = metrics.lastErrorAt
                                ),
                                message = "Analytics metrics retrieved successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(success = false, data = null, message = "Analytics not running for this camera")
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics metrics for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(success = false, data = null, message = "Error getting analytics metrics: ${e.message}")
                    )
                }
            }

            // GET /api/v1/cameras/{id}/analytics/stats - РїРѕР»СѓС‡РёС‚СЊ СЃС‚Р°С‚РёСЃС‚РёРєСѓ Р°РЅР°Р»РёС‚РёРєРё
            get("/stats") {
                requireRole(UserRole.VIEWER)
                val cameraId = cameraIdParam(call) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                if (videoAnalyticsService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics service is not available"
                        )
                    )
                }
                val vas = videoAnalyticsService!!

                try {
                    val stats = vas.getAnalyticsStats(cameraId)

                    if (stats != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data =                             AnalyticsStatsDto(
                                    cameraId = stats.cameraId,
                                    frameCount = stats.frameCount,
                                    isActive = stats.isActive,
                                    motionDetectionsCount = stats.motionDetectionsCount.toLong(),
                                    objectDetectionsCount = stats.objectDetectionsCount.toLong(),
                                    faceDetectionsCount = stats.faceDetectionsCount.toLong(),
                                    licensePlateRecognitionsCount = stats.licensePlateRecognitionsCount.toLong(),
                                    lastProcessedTimestamp = stats.lastProcessedTimestamp
                                ),
                                message = "Analytics stats retrieved successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Analytics not running for this camera"
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics stats for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics stats: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/{id}/analytics/tracks вЂ” С‚РµРєСѓС‰РёРµ Р°РєС‚РёРІРЅС‹Рµ С‚СЂРµРєРё РїРѕ РєР°РјРµСЂРµ
            get("/tracks") {
                requireRole(UserRole.VIEWER)
                val cameraId = cameraIdParam(call) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                if (videoAnalyticsService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics service is not available"
                        )
                    )
                }
                val vas = videoAnalyticsService!!

                try {
                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Camera not found"
                            )
                        )
                        return@get
                    }

                    val tracks = vas.getCurrentTracks(cameraId)
                    val tracksDto = tracks.map { track ->
                        val trackInfo = track as com.company.ipcamera.shared.domain.service.TrackInfo
                        trackInfo.toDto()
                    }
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = tracksDto,
                            message = "Tracks retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting tracks for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting tracks: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/{id}/analytics/license-plates вЂ” СЃРѕС…СЂР°РЅС‘РЅРЅС‹Рµ СЂР°СЃРїРѕР·РЅР°РЅРЅС‹Рµ РЅРѕРјРµСЂР° (ANPR РёР· Р‘Р”)
            get("/license-plates") {
                requireRole(UserRole.VIEWER)
                val cameraId = cameraIdParam(call) ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                try {
                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Camera not found"
                            )
                        )
                        return@get
                    }

                    val from = call.request.queryParameters["from"]?.toLongOrNull()
                    val to = call.request.queryParameters["to"]?.toLongOrNull()
                    val plateNumber = call.request.queryParameters["plateNumber"]?.takeIf { it.isNotBlank() }
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100

                    val plates = when {
                        plateNumber != null -> licensePlateRepository.getByPlateNumber(plateNumber, limit)
                            .filter { it.cameraId == cameraId }
                        from != null && to != null -> licensePlateRepository.getByCameraIdAndDateRange(cameraId, from, to)
                        else -> licensePlateRepository.getByCameraId(cameraId, limit)
                    }

                    val platesDto = plates.map { plate ->
                        StoredLicensePlateDto(
                            id = plate.id,
                            cameraId = plate.cameraId,
                            timestamp = plate.timestamp,
                            plateNumber = plate.plateNumber,
                            confidence = plate.confidence,
                            country = plate.country,
                            bboxX = plate.bboxX,
                            bboxY = plate.bboxY,
                            bboxWidth = plate.bboxWidth,
                            bboxHeight = plate.bboxHeight,
                            createdAt = plate.createdAt
                        )
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = platesDto,
                            message = "License plates retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting license plates for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting license plates: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/cameras/{id}/analytics/start - Р·Р°РїСѓСЃРє Р°РЅР°Р»РёС‚РёРєРё
            post("/start") {
                requireRole(UserRole.OPERATOR)
                val cameraId = cameraIdParam(call) ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                if (videoAnalyticsService == null || videoStreamService == null) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics or stream service is not available"
                        )
                    )
                }
                val vas = videoAnalyticsService!!
                val vss = videoStreamService!!

                try {
                    val request = runCatching { call.receive<AnalyticsStartRequestDto>() }
                        .getOrDefault(AnalyticsStartRequestDto())
                    val camera = cameraRepository.getCameraById(cameraId)
                    if (camera == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Camera not found"
                            )
                        )
                        return@post
                    }

                    if (request.restartStream && vss.isStreamActive(cameraId)) {
                        vss.stopStream(cameraId).onFailure { error ->
                            logger.warn(error) {
                                "Failed to stop stream before restart for camera: $cameraId. Continue with start attempt."
                            }
                        }
                    }

                    val streamResult = vss.startStream(cameraId)
                    streamResult.fold(
                        onSuccess = { streamId ->
                            val status = vas.getAnalyticsStatus(cameraId)
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = AnalyticsStartResponseDto(
                                        streamId = streamId,
                                        analyticsStatus = AnalyticsStatusDto(
                                            cameraId = status.cameraId,
                                            isRunning = status.isRunning,
                                            activeDetectors = status.activeDetectors
                                        )
                                    ),
                                    message = "Analytics started successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Failed to start video stream for analytics: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error starting analytics for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error starting analytics: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/cameras/{id}/analytics/stop - РѕСЃС‚Р°РЅРѕРІРєР° Р°РЅР°Р»РёС‚РёРєРё
            post("/stop") {
                requireRole(UserRole.OPERATOR)
                val cameraId = cameraIdParam(call) ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                if (videoAnalyticsService == null || videoStreamService == null) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics or stream service is not available"
                        )
                    )
                }
                val vas = videoAnalyticsService!!
                val vss = videoStreamService!!

                try {
                    val request = runCatching { call.receive<AnalyticsStopRequestDto>() }
                        .getOrDefault(AnalyticsStopRequestDto())

                    if (request.keepStreamAlive) {
                        vas.stopAnalytics(cameraId)
                    } else {
                        vss.stopStream(cameraId).getOrElse { error ->
                            throw IllegalStateException("Failed to stop stream: ${error.message}", error)
                        }
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = AnalyticsStopResponseDto(
                                cameraId = cameraId,
                                streamActive = vss.isStreamActive(cameraId),
                                analyticsRunning = vas.getAnalyticsStatus(cameraId).isRunning
                            ),
                            message = "Analytics stopped successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error stopping analytics for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error stopping analytics: ${e.message}"
                        )
                    )
                }
            }

            // DELETE /api/v1/cameras/{id}/analytics/results - РѕС‡РёСЃС‚РєР° СЂРµР·СѓР»СЊС‚Р°С‚РѕРІ Р°РЅР°Р»РёС‚РёРєРё
            delete("/results") {
                requireRole(UserRole.OPERATOR)
                val cameraId = cameraIdParam(call) ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Camera ID is required"
                    )
                )

                if (videoAnalyticsService == null) {
                    return@delete call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics service is not available"
                        )
                    )
                }
                val vas = videoAnalyticsService!!

                try {
                    vas.clearAnalyticsResults(cameraId)

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse<String>(
                            success = true,
                            data = null,
                            message = "Analytics results cleared successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error clearing analytics results for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error clearing analytics results: ${e.message}"
                        )
                    )
                }
            }
        }

        // GET /api/v1/analytics/summary вЂ” СЃРІРѕРґРєР° РїРѕ Р°РЅР°Р»РёС‚РёРєРµ РїРѕ РІСЃРµРј РєР°РјРµСЂР°Рј (СЃС‚Р°С‚СѓСЃ, РґРµС‚РµРєС‚РѕСЂС‹)
        route("/analytics") {
            get("summary") {
                requireRole(UserRole.VIEWER)
                try {
                    val cameras = cameraRepository.getCameras()
                    val summary = cameras.map { camera ->
                        val vasSummary = videoAnalyticsService
                        if (vasSummary != null) {
                            val status = vasSummary.getAnalyticsStatus(camera.id)
                            mapOf(
                                "cameraId" to camera.id,
                                "cameraName" to camera.name,
                                "isRunning" to status.isRunning,
                                "activeDetectors" to status.activeDetectors
                            )
                        } else {
                            mapOf(
                                "cameraId" to camera.id,
                                "cameraName" to camera.name,
                                "isRunning" to false,
                                "activeDetectors" to emptyList<String>()
                            )
                        }
                    }
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = summary,
                            message = "Analytics summary retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics summary" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics summary: ${e.message}"
                        )
                    )
                }
            }
        }

        // GET /api/v1/analytics/events - РїРѕР»СѓС‡РёС‚СЊ СЃРѕР±С‹С‚РёСЏ Р°РЅР°Р»РёС‚РёРєРё (С‡РµСЂРµР· EventRepository)
        route("/analytics/events") {
            get {
                requireRole(UserRole.VIEWER)
                // РСЃРїРѕР»СЊР·СѓРµРј СЃСѓС‰РµСЃС‚РІСѓСЋС‰РёР№ endpoint РґР»СЏ СЃРѕР±С‹С‚РёР№ СЃ С„РёР»СЊС‚СЂР°С†РёРµР№ РїРѕ С‚РёРїР°Рј Р°РЅР°Р»РёС‚РёРєРё
                // РџРµСЂРµРЅР°РїСЂР°РІР»СЏРµРј РЅР° /api/v1/events СЃ С„РёР»СЊС‚СЂР°РјРё
                call.respond(
                    HttpStatusCode.MovedPermanently,
                    ApiResponse<String>(
                        success = false,
                        data = null,
                        message = "Use /api/v1/events?type=MOTION_DETECTION or /api/v1/events?type=OBJECT_DETECTION"
                    )
                )
            }
        }

        // ========== ONVIF Analytics Engines Routes ==========
        route("/analytics/engines") {
            val analyticsEngineService: com.company.ipcamera.server.service.AnalyticsEngineService? by inject()

            // GET /api/v1/analytics/engines?cameraId={cameraId} - РїРѕР»СѓС‡РёС‚СЊ РІСЃРµ РґРІРёР¶РєРё РґР»СЏ РєР°РјРµСЂС‹
            get {
                requireRole(UserRole.VIEWER)
                val cameraId = call.request.queryParameters["cameraId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                if (analyticsEngineService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Engine Service is not available"
                        )
                    )
                }
                val aes = analyticsEngineService!!

                try {
                    val result = aes.getAnalyticsEngines(cameraId)
                    result.fold(
                        onSuccess = { engines ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = engines.map { it.toDto() },
                                    message = "Analytics engines retrieved successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error getting analytics engines: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics engines for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics engines: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/analytics/engines?cameraId={cameraId} - СЃРѕР·РґР°С‚СЊ РґРІРёР¶РѕРє
            post {
                requireRole(UserRole.OPERATOR)
                val cameraId = call.request.queryParameters["cameraId"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                if (analyticsEngineService == null) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Engine Service is not available"
                        )
                    )
                }
                val aes = analyticsEngineService!!

                try {
                    val request = call.receive<com.company.ipcamera.server.dto.CreateAnalyticsEngineRequest>()

                    val result = aes.createAnalyticsEngine(
                        cameraId = cameraId,
                        configuration = request.configuration.toDomain()
                    )

                    result.fold(
                        onSuccess = { engine ->
                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    data = engine.toDto(),
                                    message = "Analytics engine created successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error creating analytics engine: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error creating analytics engine for camera: $cameraId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error creating analytics engine: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/analytics/engines/{engineToken}?cameraId={cameraId} - РїРѕР»СѓС‡РёС‚СЊ РґРІРёР¶РѕРє
            get("{engineToken}") {
                requireRole(UserRole.VIEWER)
                val engineToken = call.parameters["engineToken"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Engine token is required"
                        )
                    )

                val cameraId = call.request.queryParameters["cameraId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                if (analyticsEngineService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Engine Service is not available"
                        )
                    )
                }
                val aes = analyticsEngineService!!

                try {
                    val result = aes.getAnalyticsEngine(cameraId, engineToken)
                    result.fold(
                        onSuccess = { engine ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = engine.toDto(),
                                    message = "Analytics engine retrieved successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Analytics engine not found: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics engine: $engineToken" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics engine: ${e.message}"
                        )
                    )
                }
            }

            // PUT /api/v1/analytics/engines/{engineToken}?cameraId={cameraId} - РѕР±РЅРѕРІРёС‚СЊ РґРІРёР¶РѕРє
            put("{engineToken}") {
                requireRole(UserRole.OPERATOR)
                val engineToken = call.parameters["engineToken"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Engine token is required"
                        )
                    )

                val cameraId = call.request.queryParameters["cameraId"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                if (analyticsEngineService == null) {
                    return@put call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Engine Service is not available"
                        )
                    )
                }
                val aes = analyticsEngineService!!

                try {
                    val request = call.receive<com.company.ipcamera.server.dto.UpdateAnalyticsEngineRequest>()

                    val result = aes.updateAnalyticsEngine(
                        cameraId = cameraId,
                        engineToken = engineToken,
                        configuration = request.configuration.toDomain()
                    )

                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<String>(
                                    success = true,
                                    data = null,
                                    message = "Analytics engine updated successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error updating analytics engine: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error updating analytics engine: $engineToken" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error updating analytics engine: ${e.message}"
                        )
                    )
                }
            }

            // DELETE /api/v1/analytics/engines/{engineToken}?cameraId={cameraId} - СѓРґР°Р»РёС‚СЊ РґРІРёР¶РѕРє
            delete("{engineToken}") {
                requireRole(UserRole.OPERATOR)
                val engineToken = call.parameters["engineToken"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Engine token is required"
                        )
                    )

                val cameraId = call.request.queryParameters["cameraId"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                if (analyticsEngineService == null) {
                    return@delete call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Engine Service is not available"
                        )
                    )
                }
                val aes = analyticsEngineService!!

                try {
                    val result = aes.deleteAnalyticsEngine(cameraId, engineToken)
                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<String>(
                                    success = true,
                                    data = null,
                                    message = "Analytics engine deleted successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error deleting analytics engine: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error deleting analytics engine: $engineToken" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error deleting analytics engine: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/analytics/engines/{engineToken}/inputs?cameraId={cameraId} - РїРѕР»СѓС‡РёС‚СЊ РІС…РѕРґРЅС‹Рµ РґР°РЅРЅС‹Рµ
            get("{engineToken}/inputs") {
                requireRole(UserRole.VIEWER)
                val engineToken = call.parameters["engineToken"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Engine token is required"
                        )
                    )

                val cameraId = call.request.queryParameters["cameraId"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                if (analyticsEngineService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Engine Service is not available"
                        )
                    )
                }
                val aes = analyticsEngineService!!

                try {
                    val result = aes.getAnalyticsEngineInputs(cameraId, engineToken)
                    result.fold(
                        onSuccess = { inputs ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = inputs.map { it.toDto() },
                                    message = "Analytics engine inputs retrieved successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error getting inputs: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting inputs for engine: $engineToken" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting inputs: ${e.message}"
                        )
                    )
                }
            }
        }

        // ========== Analytics Rules Routes ==========
        route("/analytics/rules") {
            val analyticsRuleService: com.company.ipcamera.server.service.AnalyticsRuleService? by inject()
            val analyticsWebhookService: com.company.ipcamera.server.service.AnalyticsWebhookService? by inject()

            // GET /api/v1/analytics/rules - РїРѕР»СѓС‡РёС‚СЊ РІСЃРµ РїСЂР°РІРёР»Р°
            get {
                requireRole(UserRole.VIEWER)
                if (analyticsRuleService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!

                try {
                    val cameraId = call.request.queryParameters["cameraId"]
                    val result = if (cameraId != null) {
                        ars.getRulesByCameraId(cameraId)
                    } else {
                        ars.getAllRules()
                    }

                    result.fold(
                        onSuccess = { rules ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = rules.map { it.toDto() },
                                    message = "Analytics rules retrieved successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error getting analytics rules: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics rules" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics rules: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/analytics/rules/webhook-deliveries?ruleId={ruleId}&cameraId={cameraId}&limit={limit}
            get("webhook-deliveries") {
                requireRole(UserRole.VIEWER)
                if (analyticsWebhookService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Webhook Service is not available"
                        )
                    )
                }
                val aws = analyticsWebhookService!!
                try {
                    val ruleId = call.request.queryParameters["ruleId"]?.takeIf { it.isNotBlank() }
                    val cameraId = call.request.queryParameters["cameraId"]?.takeIf { it.isNotBlank() }
                    val success = WebhookQueryValidator.parseSuccess(call.request.queryParameters["success"])
                    val (fromTimestamp, toTimestamp) = WebhookQueryValidator.parseTimeRange(
                        call.request.queryParameters["from"],
                        call.request.queryParameters["to"]
                    )
                    val page = WebhookQueryValidator.parsePage(call.request.queryParameters["page"])
                    val limit = WebhookQueryValidator.parseLimit(call.request.queryParameters["limit"])
                    val logsPage = aws.getDeliveryLogsPage(
                        ruleId = ruleId,
                        cameraId = cameraId,
                        success = success,
                        fromTimestamp = fromTimestamp,
                        toTimestamp = toTimestamp,
                        page = page,
                        limit = limit
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = PaginatedWebhookDeliveryLogsResponseDto(
                                items = logsPage.items,
                                total = logsPage.total,
                                page = logsPage.page,
                                limit = logsPage.limit,
                                hasMore = logsPage.hasMore
                            ),
                            message = "Webhook delivery logs retrieved successfully"
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = e.message ?: "Invalid query parameters"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting webhook delivery logs" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting webhook delivery logs: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/analytics/rules/webhook-deliveries/stats?ruleId={ruleId}&cameraId={cameraId}
            get("webhook-deliveries/stats") {
                requireRole(UserRole.VIEWER)
                if (analyticsWebhookService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Webhook Service is not available"
                        )
                    )
                }
                val aws = analyticsWebhookService!!
                try {
                    val ruleId = call.request.queryParameters["ruleId"]?.takeIf { it.isNotBlank() }
                    val cameraId = call.request.queryParameters["cameraId"]?.takeIf { it.isNotBlank() }
                    val (fromTimestamp, toTimestamp) = WebhookQueryValidator.parseTimeRange(
                        call.request.queryParameters["from"],
                        call.request.queryParameters["to"]
                    )
                    val stats = aws.getDeliveryStats(
                        ruleId = ruleId,
                        cameraId = cameraId,
                        fromTimestamp = fromTimestamp,
                        toTimestamp = toTimestamp
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = WebhookDeliveryStatsResponseDto(
                                totalAttempts = stats.totalAttempts,
                                successfulAttempts = stats.successfulAttempts,
                                failedAttempts = stats.failedAttempts,
                                successRatePercent = stats.successRatePercent,
                                ruleId = ruleId,
                                cameraId = cameraId,
                                from = fromTimestamp,
                                to = toTimestamp
                            ),
                            message = "Webhook delivery stats retrieved successfully"
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = e.message ?: "Invalid query parameters"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting webhook delivery stats" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting webhook delivery stats: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/analytics/rules/webhook-deliveries/summary?ruleId={ruleId}&cameraId={cameraId}&from={from}&to={to}&limit={limit}
            get("webhook-deliveries/summary") {
                requireRole(UserRole.VIEWER)
                if (analyticsWebhookService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Webhook Service is not available"
                        )
                    )
                }
                val aws = analyticsWebhookService!!
                try {
                    val ruleId = call.request.queryParameters["ruleId"]?.takeIf { it.isNotBlank() }
                    val cameraId = call.request.queryParameters["cameraId"]?.takeIf { it.isNotBlank() }
                    val (fromTimestamp, toTimestamp) = WebhookQueryValidator.parseTimeRange(
                        call.request.queryParameters["from"],
                        call.request.queryParameters["to"]
                    )
                    val limit = WebhookQueryValidator.parseLimit(call.request.queryParameters["limit"])
                    val summary = aws.getDeliverySummary(
                        ruleId = ruleId,
                        cameraId = cameraId,
                        fromTimestamp = fromTimestamp,
                        toTimestamp = toTimestamp,
                        limit = limit
                    ).map {
                        WebhookDeliverySummaryEntryDto(
                            ruleId = it.ruleId,
                            cameraId = it.cameraId,
                            totalAttempts = it.totalAttempts,
                            successfulAttempts = it.successfulAttempts,
                            failedAttempts = it.failedAttempts,
                            successRatePercent = it.successRatePercent,
                            lastAttemptTimestamp = it.lastAttemptTimestamp
                        )
                    }
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = summary,
                            message = "Webhook delivery summary retrieved successfully"
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = e.message ?: "Invalid query parameters"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting webhook delivery summary" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting webhook delivery summary: ${e.message}"
                        )
                    )
                }
            }

            // DELETE /api/v1/analytics/rules/webhook-deliveries?ruleId={ruleId}&cameraId={cameraId}
            delete("webhook-deliveries") {
                requireRole(UserRole.OPERATOR)
                if (analyticsWebhookService == null) {
                    return@delete call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Webhook Service is not available"
                        )
                    )
                }
                val aws = analyticsWebhookService!!
                try {
                    val ruleId = call.request.queryParameters["ruleId"]?.takeIf { it.isNotBlank() }
                    val cameraId = call.request.queryParameters["cameraId"]?.takeIf { it.isNotBlank() }
                    val success = WebhookQueryValidator.parseSuccess(call.request.queryParameters["success"])
                    val (fromTimestamp, toTimestamp) = WebhookQueryValidator.parseTimeRange(
                        call.request.queryParameters["from"],
                        call.request.queryParameters["to"]
                    )
                    val removed = aws.clearDeliveryLogs(
                        ruleId = ruleId,
                        cameraId = cameraId,
                        success = success,
                        fromTimestamp = fromTimestamp,
                        toTimestamp = toTimestamp
                    )
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = WebhookDeliveryLogsClearResponseDto(
                                removed = removed,
                                ruleId = ruleId,
                                cameraId = cameraId,
                                success = success,
                                from = fromTimestamp,
                                to = toTimestamp
                            ),
                            message = "Webhook delivery logs cleared successfully"
                        )
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = e.message ?: "Invalid query parameters"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error clearing webhook delivery logs" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error clearing webhook delivery logs: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/analytics/rules - СЃРѕР·РґР°С‚СЊ РїСЂР°РІРёР»Рѕ
            post {
                requireRole(UserRole.OPERATOR)
                if (analyticsRuleService == null) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!

                try {
                    val request = call.receive<com.company.ipcamera.server.dto.CreateAnalyticsRuleRequest>()

                    // Р’Р°Р»РёРґР°С†РёСЏ
                    if (!validateRequest<com.company.ipcamera.server.dto.CreateAnalyticsRuleRequest>(request) { RequestValidator.validateAnalyticsRule(it) }) {
                        return@post
                    }

                    val rule = request.toDomain()
                    val result = ars.createRule(rule)

                    result.fold(
                        onSuccess = { createdRule ->
                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    data = createdRule.toDto(),
                                    message = "Analytics rule created successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error creating analytics rule: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error creating analytics rule" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error creating analytics rule: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/analytics/rules/{id} - РїРѕР»СѓС‡РёС‚СЊ РїСЂР°РІРёР»Рѕ
            get("{id}") {
                requireRole(UserRole.VIEWER)
                val ruleId = call.parameters["id"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )

                if (analyticsRuleService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!

                try {
                    val result = ars.getRuleById(ruleId)
                    result.fold(
                        onSuccess = { rule ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = rule.toDto(),
                                    message = "Analytics rule retrieved successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Analytics rule not found: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics rule: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics rule: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/analytics/rules/{id}/test-notification - С‚РµСЃС‚РѕРІР°СЏ РѕС‚РїСЂР°РІРєР° РїРѕ РєР°РЅР°Р»Р°Рј РїСЂР°РІРёР»Р°
            post("{id}/test-notification") {
                requireRole(UserRole.OPERATOR)
                val ruleId = call.parameters["id"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )
                if (analyticsRuleService == null) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!
                try {
                    val request = runCatching { call.receive<TestAnalyticsRuleNotificationRequestDto>() }
                        .getOrDefault(TestAnalyticsRuleNotificationRequestDto())
                    val result = ars.sendTestNotification(
                        ruleId = ruleId,
                        userId = request.userId,
                        cameraIdOverride = request.cameraId,
                        notifyInApp = request.notifyInApp,
                        notifyEmail = request.notifyEmail,
                        notifyTelegram = request.notifyTelegram
                    )
                    result.fold(
                        onSuccess = { test ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = TestAnalyticsRuleNotificationResponseDto(
                                        ruleId = test.ruleId,
                                        notificationId = test.notificationId,
                                        channels = test.channels
                                    ),
                                    message = "Test notification sent successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            val notFound = error is IllegalArgumentException && (error.message?.contains("Rule not found") == true)
                            call.respond(
                                if (notFound) HttpStatusCode.NotFound else HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = error.message ?: "Failed to send test notification"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error sending test notification for rule: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error sending test notification: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/analytics/rules/{id}/notification-policy - РїРѕР»РёС‚РёРєР° РєР°РЅР°Р»РѕРІ СѓРІРµРґРѕРјР»РµРЅРёР№ РїСЂР°РІРёР»Р°
            get("{id}/notification-policy") {
                requireRole(UserRole.VIEWER)
                val ruleId = call.parameters["id"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )
                if (analyticsRuleService == null) {
                    return@get call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!
                try {
                    ars.getNotificationPolicy(ruleId).fold(
                        onSuccess = { policy ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = policy.toDto(),
                                    message = "Analytics rule notification policy retrieved successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            val notFound = error is IllegalArgumentException && (error.message?.contains("Rule not found") == true)
                            call.respond(
                                if (notFound) HttpStatusCode.NotFound else HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = error.message ?: "Failed to get analytics rule notification policy"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting analytics rule notification policy: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error getting analytics rule notification policy: ${e.message}"
                        )
                    )
                }
            }

            // PUT /api/v1/analytics/rules/{id}/notification-policy - РѕР±РЅРѕРІР»РµРЅРёРµ РїРѕР»РёС‚РёРєРё РєР°РЅР°Р»РѕРІ СѓРІРµРґРѕРјР»РµРЅРёР№ РїСЂР°РІРёР»Р°
            put("{id}/notification-policy") {
                requireRole(UserRole.OPERATOR)
                val ruleId = call.parameters["id"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )
                if (analyticsRuleService == null) {
                    return@put call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!
                try {
                    val request = call.receive<UpdateAnalyticsRuleNotificationPolicyRequestDto>()
                    val resolvedNotificationType = request.notificationType
                        ?.takeIf { it.isNotBlank() }
                        ?.let { com.company.ipcamera.shared.domain.model.NotificationType.valueOf(it) }

                    ars.updateNotificationPolicy(
                        ruleId = ruleId,
                        sendNotification = request.sendNotification,
                        notifyInApp = request.notifyInApp,
                        notifyEmail = request.notifyEmail,
                        notifyTelegram = request.notifyTelegram,
                        notificationType = resolvedNotificationType
                    ).fold(
                        onSuccess = { policy ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = policy.toDto(),
                                    message = "Analytics rule notification policy updated successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            val notFound = error is IllegalArgumentException && (error.message?.contains("Rule not found") == true)
                            call.respond(
                                if (notFound) HttpStatusCode.NotFound else HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = error.message ?: "Failed to update analytics rule notification policy"
                                )
                            )
                        }
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = e.message ?: "Invalid notification policy request"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error updating analytics rule notification policy: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error updating analytics rule notification policy: ${e.message}"
                        )
                    )
                }
            }

            // PUT /api/v1/analytics/rules/{id} - РѕР±РЅРѕРІРёС‚СЊ РїСЂР°РІРёР»Рѕ
            put("{id}") {
                requireRole(UserRole.OPERATOR)
                val ruleId = call.parameters["id"]
                    ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )

                if (analyticsRuleService == null) {
                    return@put call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!

                try {
                    val request = call.receive<com.company.ipcamera.server.dto.UpdateAnalyticsRuleRequest>()

                    // РџРѕР»СѓС‡Р°РµРј СЃСѓС‰РµСЃС‚РІСѓСЋС‰РµРµ РїСЂР°РІРёР»Рѕ
                    val existingResult = ars.getRuleById(ruleId)
                    val existingRule = existingResult.getOrNull()
                        ?: return@put call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Analytics rule not found"
                            )
                        )

                    // РћР±РЅРѕРІР»СЏРµРј С‚РѕР»СЊРєРѕ СѓРєР°Р·Р°РЅРЅС‹Рµ РїРѕР»СЏ
                    val updatedRule = existingRule.copy(
                        name = request.name ?: existingRule.name,
                        description = request.description ?: existingRule.description,
                        cameraId = request.cameraId ?: existingRule.cameraId,
                        analyticsType = request.analyticsType?.let {
                            com.company.ipcamera.shared.domain.model.AnalyticsRuleType.valueOf(it)
                        } ?: existingRule.analyticsType,
                        conditions = request.conditions?.toDomain() ?: existingRule.conditions,
                        actions = request.actions?.toDomain() ?: existingRule.actions,
                        enabled = request.enabled ?: existingRule.enabled,
                        priority = request.priority ?: existingRule.priority
                    )

                    val result = ars.updateRule(updatedRule)
                    result.fold(
                        onSuccess = { rule ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = rule.toDto(),
                                    message = "Analytics rule updated successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error updating analytics rule: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error updating analytics rule: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error updating analytics rule: ${e.message}"
                        )
                    )
                }
            }

            // DELETE /api/v1/analytics/rules/{id} - СѓРґР°Р»РёС‚СЊ РїСЂР°РІРёР»Рѕ
            delete("{id}") {
                requireRole(UserRole.OPERATOR)
                val ruleId = call.parameters["id"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )

                if (analyticsRuleService == null) {
                    return@delete call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!

                try {
                    val result = ars.deleteRule(ruleId)
                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<String>(
                                    success = true,
                                    data = null,
                                    message = "Analytics rule deleted successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error deleting analytics rule: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error deleting analytics rule: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error deleting analytics rule: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/analytics/rules/{id}/enable - РІРєР»СЋС‡РёС‚СЊ РїСЂР°РІРёР»Рѕ
            post("{id}/enable") {
                requireRole(UserRole.OPERATOR)
                val ruleId = call.parameters["id"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )

                if (analyticsRuleService == null) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!

                try {
                    val result = ars.setRuleEnabled(ruleId, true)
                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<String>(
                                    success = true,
                                    data = null,
                                    message = "Analytics rule enabled successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error enabling analytics rule: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error enabling analytics rule: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error enabling analytics rule: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/analytics/rules/{id}/disable - РІС‹РєР»СЋС‡РёС‚СЊ РїСЂР°РІРёР»Рѕ
            post("{id}/disable") {
                requireRole(UserRole.OPERATOR)
                val ruleId = call.parameters["id"]
                    ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Rule ID is required"
                        )
                    )

                if (analyticsRuleService == null) {
                    return@post call.respond(
                        HttpStatusCode.ServiceUnavailable,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Analytics Rule Service is not available"
                        )
                    )
                }
                val ars = analyticsRuleService!!

                try {
                    val result = ars.setRuleEnabled(ruleId, false)
                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<String>(
                                    success = true,
                                    data = null,
                                    message = "Analytics rule disabled successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error disabling analytics rule: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error disabling analytics rule: $ruleId" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error disabling analytics rule: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}
