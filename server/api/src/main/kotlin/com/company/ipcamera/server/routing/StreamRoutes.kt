package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.validateRequest
import com.company.ipcamera.server.service.ScreenshotService
import com.company.ipcamera.server.service.StreamQuality
import com.company.ipcamera.server.service.allStreamQualityApiValues
import com.company.ipcamera.server.service.VideoStreamService
import com.company.ipcamera.server.service.WebRtcService
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.nio.file.Paths
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import org.koin.core.parameter.parametersOf

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для работы с видеопотоками
 * Все маршруты требуют JWT аутентификации
 */
fun Route.streamRoutes() {
    val videoStreamService: VideoStreamService by inject()
    val screenshotService: ScreenshotService by inject()
    val webRtcService: WebRtcService by inject()

    // HlsGeneratorService может быть не доступен, если FFmpeg не установлен
    val hlsGeneratorService: com.company.ipcamera.server.service.HlsGeneratorService? = try {
        org.koin.core.context.GlobalContext.get().getOrNull<com.company.ipcamera.server.service.HlsGeneratorService>()
    } catch (e: Exception) {
        null
    }

    authenticate("jwt-auth") {
        route("/cameras/{id}/stream") {
            // GET /api/v1/cameras/{id}/stream/start - начать трансляцию
            post("/start") {
                try {
                    val cameraId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                    val result = videoStreamService.startStream(cameraId)

                    result.fold(
                        onSuccess = { streamId ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = streamId,
                                    message = "Stream started successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error starting stream for camera: $cameraId" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Error starting stream: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error in stream start endpoint" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/cameras/{id}/stream/stop - остановить трансляцию
            post("/stop") {
                try {
                    val cameraId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                    val result = videoStreamService.stopStream(cameraId)

                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<Unit>(
                                    success = true,
                                    data = null,
                                    message = "Stream stopped successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error stopping stream for camera: $cameraId" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "Error stopping stream: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error in stream stop endpoint" }
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

            // GET /api/v1/cameras/{id}/stream/status - получить статус трансляции
            get("/status") {
                try {
                    val cameraId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<StreamStatusDto>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                    val isActive = videoStreamService.isStreamActive(cameraId)
                    val streamId = videoStreamService.getStreamId(cameraId)
                    val rtspDiagnostics = videoStreamService.getRtspDiagnostics(cameraId)

                    // Получаем RTSP URL для прямой трансляции (для мобильных приложений)
                    val rtspUrl = videoStreamService.getRtspUrl(cameraId)

                    val status = StreamStatusDto(
                        active = isActive,
                        streamId = streamId,
                        hlsUrl = if (isActive) videoStreamService.getHlsUrl(cameraId) else null,
                        rtspUrl = rtspUrl, // RTSP URL для прямой трансляции (ExoPlayer)
                        rtspDiagnostics = rtspDiagnostics?.let { diagnostics ->
                            RtspRuntimeDiagnosticsDto(
                                connectAttempts = diagnostics.connectAttempts,
                                connectSuccesses = diagnostics.connectSuccesses,
                                connectFailures = diagnostics.connectFailures,
                                reconnectAttempts = diagnostics.reconnectAttempts,
                                reconnectSuccesses = diagnostics.reconnectSuccesses,
                                reconnectFailures = diagnostics.reconnectFailures,
                                consecutiveFailures = diagnostics.consecutiveFailures,
                                lastError = diagnostics.lastError,
                                lastErrorAt = diagnostics.lastErrorAt,
                                lastConnectedAt = diagnostics.lastConnectedAt,
                                lastDisconnectedAt = diagnostics.lastDisconnectedAt,
                                lastPlayingAt = diagnostics.lastPlayingAt ?: 0L,
                                lastFrameAt = diagnostics.lastFrameAt ?: 0L
                            )
                        }
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = status,
                            message = "Stream status retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error in stream status endpoint" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<StreamStatusDto>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/{id}/stream/hls/master.m3u8 - Master HLS плейлист (адаптивный битрейт)
            get("/hls/master.m3u8") {
                try {
                    val cameraId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Camera ID is required"
                    )

                    // Проверяем, активен ли стрим, если нет - запускаем
                    if (!videoStreamService.isStreamActive(cameraId)) {
                        val startResult = videoStreamService.startStream(cameraId)
                        if (startResult.isFailure) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to start stream: ${startResult.exceptionOrNull()?.message}"
                            )
                            return@get
                        }
                        // Ждем немного для генерации первых сегментов
                        kotlinx.coroutines.delay(5000)
                    }

                    // Получаем путь к master playlist
                    val streamId = videoStreamService.getStreamId(cameraId)

                    if (streamId != null && hlsGeneratorService != null) {
                        val masterPlaylistPath = hlsGeneratorService.getMasterPlaylistPath(streamId)
                        val masterPlaylistFile = java.io.File(masterPlaylistPath)

                        if (masterPlaylistFile.exists()) {
                            var playlistContent = masterPlaylistFile.readText()

                            // Заменяем пути к вариантам на правильные URL
                            playlistContent = playlistContent.replace(
                                Regex("/api/v1/cameras/streams/([^/]+)/hls/([^/]+)/playlist\\.m3u8"),
                                "/api/v1/cameras/$cameraId/stream/hls/\$2/playlist.m3u8"
                            )

                            // Устанавливаем правильные заголовки для HLS
                            call.response.headers.append("Cache-Control", "no-cache, no-store, must-revalidate")
                            call.response.headers.append("Access-Control-Allow-Origin", "*")
                            call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")

                            call.respondText(
                                playlistContent,
                                ContentType("application", "vnd.apple.mpegurl"),
                                HttpStatusCode.OK
                            )
                        } else {
                            // Fallback на обычный плейлист
                            call.respond(
                                HttpStatusCode.NotFound,
                                "Master playlist not found. Stream may still be initializing."
                            )
                        }
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            "Adaptive HLS generation not available"
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error serving master HLS playlist" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error serving master playlist: ${e.message}"
                    )
                }
            }

            // GET /api/v1/cameras/{id}/stream/hls/playlist.m3u8 - HLS плейлист (одиночное качество)
            get("/hls/playlist.m3u8") {
                try {
                    val cameraId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Camera ID is required"
                    )

                    // Проверяем, активен ли стрим, если нет - запускаем
                    if (!videoStreamService.isStreamActive(cameraId)) {
                        val startResult = videoStreamService.startStream(cameraId)
                        if (startResult.isFailure) {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Failed to start stream: ${startResult.exceptionOrNull()?.message}"
                            )
                            return@get
                        }
                        // Ждем немного для генерации первого сегмента
                        kotlinx.coroutines.delay(2000)
                    }

                    // Получаем путь к HLS плейлисту
                    val playlistPath = videoStreamService.getHlsPlaylistPath(cameraId)

                    if (playlistPath != null) {
                        val playlistFile = File(playlistPath)
                        if (playlistFile.exists()) {
                            // Читаем плейлист из файла
                            var playlistContent = playlistFile.readText()

                            // Заменяем пути к сегментам на правильные URL
                            // Формат: segment_001.ts -> /api/v1/cameras/{cameraId}/stream/hls/segment_001.ts
                            playlistContent = playlistContent.replace(
                                Regex("(segment_\\d+\\.ts)"),
                                "/api/v1/cameras/$cameraId/stream/hls/\$1"
                            )

                            // Устанавливаем правильные заголовки для HLS
                            call.response.headers.append("Cache-Control", "no-cache, no-store, must-revalidate")
                            call.response.headers.append("Access-Control-Allow-Origin", "*")
                            call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")

                            call.respondText(
                                playlistContent,
                                ContentType("application", "vnd.apple.mpegurl"),
                                HttpStatusCode.OK
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                "HLS playlist not found. Stream may still be initializing."
                            )
                        }
                    } else {
                        call.respond(
                            HttpStatusCode.ServiceUnavailable,
                            "HLS generation not available"
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error serving HLS playlist" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error serving playlist: ${e.message}"
                    )
                }
            }

            // GET /api/v1/cameras/{id}/stream/hls/{quality}/playlist.m3u8 - HLS плейлист для конкретного качества
            get("/hls/{quality}/playlist.m3u8") {
                try {
                    val cameraId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Camera ID is required"
                    )

                    val quality = call.parameters["quality"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Quality is required"
                    )

                    val validQualities = allStreamQualityApiValues()
                    if (!validQualities.contains(quality.lowercase())) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Invalid quality. Use: ${validQualities.joinToString(", ")}"
                        )
                        return@get
                    }

                    val streamId = videoStreamService.getStreamId(cameraId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            "Stream not found"
                        )

                    val variantPlaylistPath = Paths.get("streams/hls", streamId, quality.lowercase(), "playlist.m3u8")
                    val variantPlaylistFile = variantPlaylistPath.toFile()

                    if (variantPlaylistFile.exists()) {
                        var playlistContent = variantPlaylistFile.readText()

                        // Заменяем пути к сегментам на правильные URL
                        playlistContent = playlistContent.replace(
                            Regex("(segment_\\d+\\.ts)"),
                            "/api/v1/cameras/$cameraId/stream/hls/$quality/\$1"
                        )

                        call.response.headers.append("Cache-Control", "no-cache, no-store, must-revalidate")
                        call.response.headers.append("Access-Control-Allow-Origin", "*")
                        call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")

                        call.respondText(
                            playlistContent,
                            ContentType("application", "vnd.apple.mpegurl"),
                            HttpStatusCode.OK
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            "Variant playlist not found for quality: $quality"
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error serving variant HLS playlist" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error serving variant playlist: ${e.message}"
                    )
                }
            }

            // GET /api/v1/cameras/{id}/stream/hls/{quality}/{segment}.ts - HLS сегменты для конкретного качества
            get("/hls/{quality}/{segment}") {
                try {
                    val cameraId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Camera ID is required"
                    )

                    val quality = call.parameters["quality"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Quality is required"
                    )

                    val segmentName = call.parameters["segment"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Segment name is required"
                    )

                    if (!segmentName.endsWith(".ts")) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Invalid segment format"
                        )
                        return@get
                    }

                    val streamId = videoStreamService.getStreamId(cameraId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            "Stream not found"
                        )

                    val segmentPath = Paths.get("streams/hls", streamId, quality.lowercase(), segmentName)
                    val segmentFile = segmentPath.toFile()

                    if (segmentFile.exists() && segmentFile.isFile) {
                        call.response.headers.append("Cache-Control", "public, max-age=3600")
                        call.response.headers.append("Access-Control-Allow-Origin", "*")
                        call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")
                        call.response.headers.append("Content-Type", "video/mp2t")

                        call.respondFile(segmentFile)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            "Segment not found: $segmentName"
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error serving variant HLS segment" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error serving segment: ${e.message}"
                    )
                }
            }

            // GET /api/v1/cameras/{id}/stream/hls/{segment}.ts - HLS сегменты (для обратной совместимости)
            get("/hls/{segment}") {
                try {
                    val cameraId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Camera ID is required"
                    )

                    val segmentName = call.parameters["segment"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Segment name is required"
                    )

                    // Проверяем, что это .ts файл
                    if (!segmentName.endsWith(".ts")) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "Invalid segment format"
                        )
                        return@get
                    }

                    // Получаем путь к HLS плейлисту для определения директории
                    val playlistPath = videoStreamService.getHlsPlaylistPath(cameraId)
                        ?: return@get call.respond(
                            HttpStatusCode.NotFound,
                            "Stream not found"
                        )

                    // Определяем директорию сегментов
                    val playlistFile = File(playlistPath)
                    val segmentDir = playlistFile.parentFile
                    val segmentFile = File(segmentDir, segmentName)

                    if (segmentFile.exists() && segmentFile.isFile) {
                        // Устанавливаем правильные заголовки для сегментов
                        call.response.headers.append("Cache-Control", "public, max-age=3600")
                        call.response.headers.append("Access-Control-Allow-Origin", "*")
                        call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")
                        call.response.headers.append("Content-Type", "video/mp2t")

                        call.respondFile(segmentFile)
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            "Segment not found: $segmentName"
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error serving HLS segment" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error serving segment: ${e.message}"
                    )
                }
            }


            // GET /api/v1/cameras/{id}/stream/rtsp - получить RTSP URL для прямой трансляции
            get("/rtsp") {
                try {
                    val cameraId = call.parameters["id"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<RtspStreamUrlDto>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                    val rtspUrl = videoStreamService.getRtspUrl(cameraId)

                    if (rtspUrl == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<RtspStreamUrlDto>(
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
                            data = RtspStreamUrlDto(rtspUrl = rtspUrl),
                            message = "RTSP URL retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error getting RTSP URL" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<RtspStreamUrlDto>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/cameras/{id}/stream/quality - изменить качество потока
            post("/quality") {
                try {
                    val cameraId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                    // Валидация camera ID
                    if (!validateRequest(cameraId) { RequestValidator.validateCameraId(it) }) {
                        return@post
                    }

                    val qualityStr = call.request.queryParameters["quality"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(
                            success = false,
                            data = null,
                            message = "Quality parameter is required (${allStreamQualityApiValues().joinToString(", ")})"
                        )
                    )

                    val request = com.company.ipcamera.server.dto.SetStreamQualityRequest(quality = qualityStr)

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateSetStreamQualityRequest(it) }) {
                        return@post
                    }

                    val quality = try {
                        StreamQuality.valueOf(qualityStr.uppercase())
                    } catch (e: Exception) {
                        return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Invalid quality value. Use: ${allStreamQualityApiValues().joinToString(", ")}"
                            )
                        )
                    }

                    val result = videoStreamService.setStreamQuality(cameraId, quality)

                    result.fold(
                        onSuccess = {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse<Unit>(
                                    success = true,
                                    data = null,
                                    message = "Stream quality changed to $qualityStr successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error changing stream quality for camera: $cameraId" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<Unit>(
                                    success = false,
                                    data = null,
                                    message = "Error changing stream quality: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error in stream quality change endpoint" }
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

            // POST /api/v1/cameras/{id}/stream/webrtc/offer - обработать WebRTC offer
            post("/webrtc/offer") {
                try {
                    val cameraId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Map<String, Any>>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                    // Валидация camera ID
                    if (!validateRequest(cameraId) { RequestValidator.validateCameraId(it) }) {
                        return@post
                    }

                    val request = call.receive<com.company.ipcamera.server.dto.WebRtcOfferRequest>()

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateWebRtcOfferRequest(it) }) {
                        return@post
                    }

                    val offerSdp = request.offer.sdp

                    // Обрабатываем WebRTC offer через WebRtcService (с интеграцией Janus)
                    val result = webRtcService.handleOffer(cameraId, offerSdp)

                    result.fold(
                        onSuccess = { answer ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = mapOf(
                                        "answer" to mapOf(
                                            "type" to "answer",
                                            "sdp" to answer.answer
                                        ),
                                        "iceCandidates" to answer.iceCandidates.map { candidate ->
                                            mapOf(
                                                "candidate" to candidate.candidate,
                                                "sdpMid" to candidate.sdpMid,
                                                "sdpMLineIndex" to candidate.sdpMLineIndex
                                            )
                                        }
                                    ),
                                    message = "WebRTC offer processed successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error processing WebRTC offer for camera: $cameraId" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<Map<String, Any>>(
                                    success = false,
                                    data = null,
                                    message = "Error processing WebRTC offer: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error in WebRTC offer endpoint" }
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

            // POST /api/v1/cameras/{id}/stream/screenshot - создать снимок кадра
            post("/screenshot") {
                try {
                    val cameraId = call.parameters["id"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )

                    // Получаем RTSP URL для камеры
                    val rtspUrl = videoStreamService.getRtspUrl(cameraId)

                    if (rtspUrl == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Camera not found or RTSP URL not available"
                            )
                        )
                        return@post
                    }

                    // Получаем камеру через репозиторий
                    val cameraRepository = org.koin.core.context.GlobalContext.get().get<CameraRepository>()
                    val camera = cameraRepository.getCameraById(cameraId)
                        ?: return@post call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Camera not found"
                            )
                        )

                    // Создаем снимок
                    val screenshotPath = screenshotService.captureFromRtsp(
                        rtspUrl = rtspUrl,
                        cameraId = cameraId,
                        username = camera.username,
                        password = camera.password
                    )

                    if (screenshotPath != null) {
                        val screenshotUrl = screenshotService.getScreenshotUrl(screenshotPath)
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = screenshotUrl,
                                message = "Screenshot captured successfully"
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Failed to capture screenshot"
                            )
                        )
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error capturing screenshot" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
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

