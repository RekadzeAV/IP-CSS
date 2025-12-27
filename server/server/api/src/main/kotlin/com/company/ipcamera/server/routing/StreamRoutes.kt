package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.service.ScreenshotService
import com.company.ipcamera.server.service.VideoStreamService
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для работы с видеопотоками
 * Все маршруты требуют JWT аутентификации
 */
fun Route.streamRoutes() {
    val videoStreamService: VideoStreamService by inject()
    val screenshotService: ScreenshotService by inject()
    
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
                    
                    val status = StreamStatusDto(
                        active = isActive,
                        streamId = streamId,
                        hlsUrl = if (isActive) videoStreamService.getHlsUrl(cameraId) else null,
                        rtspUrl = null // RTSP URL будет доступен через отдельный endpoint
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
            
            // GET /api/v1/cameras/{id}/stream/hls/playlist.m3u8 - HLS плейлист
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
                    }
                    
                    // Получаем путь к HLS плейлисту
                    val playlistPath = videoStreamService.getHlsPlaylistPath(cameraId)
                    
                    if (playlistPath != null) {
                        val playlistFile = java.io.File(playlistPath)
                        if (playlistFile.exists()) {
                            // Читаем плейлист из файла
                            val playlistContent = playlistFile.readText()
                            
                            // Заменяем пути к сегментам на правильные URL
                            val streamId = videoStreamService.getStreamId(cameraId) ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                "Stream not found"
                            )
                            
                            // Заменяем локальные пути на URL
                            val modifiedPlaylist = playlistContent.replace(
                                Regex("segment_\\d+\\.ts"),
                                "/api/v1/cameras/streams/$streamId/hls/\\$0"
                            )
                            
                            call.respondText(
                                modifiedPlaylist,
                                ContentType("application", "vnd.apple.mpegurl"),
                                HttpStatusCode.OK
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                "HLS playlist not found"
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
                    val cameraRepository: CameraRepository by inject()
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

