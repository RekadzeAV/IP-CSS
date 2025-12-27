package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.service.VideoRecordingService
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import org.koin.ktor.ext.inject

private val logger = KotlinLogging.logger {}

/**
 * Маршруты для управления записями
 * Все маршруты требуют JWT аутентификации
 */
fun Route.recordingRoutes() {
    val recordingRepository: RecordingRepository by inject()
    val videoRecordingService: VideoRecordingService by inject()
    val cameraRepository: CameraRepository by inject()
    
    authenticate("jwt-auth") {
        route("/recordings") {
            // GET /api/v1/recordings - список записей с фильтрацией и пагинацией
            get {
                try {
                    val cameraId = call.request.queryParameters["cameraId"]
                    val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                    val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                    
                    val result = recordingRepository.getRecordings(
                        cameraId = cameraId,
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
                            message = "Recordings retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error retrieving recordings" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<PaginatedRecordingResponse>(
                            success = false,
                            data = null,
                            message = "Error retrieving recordings: ${e.message}"
                        )
                    )
                }
            }
            
            route("/{id}") {
                // GET /api/v1/recordings/{id} - получение записи по ID
                get {
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<RecordingDto>(
                                success = false,
                                data = null,
                                message = "Recording ID is required"
                            )
                        )
                        
                        val recording = recordingRepository.getRecordingById(id)
                        if (recording != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = recording.toDto(),
                                    message = "Recording retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<RecordingDto>(
                                    success = false,
                                    data = null,
                                    message = "Recording not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error retrieving recording" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<RecordingDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
                
                // DELETE /api/v1/recordings/{id} - удаление записи
                delete {
                    try {
                        val id = call.parameters["id"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Recording ID is required"
                            )
                        )
                        
                        val result = recordingRepository.deleteRecording(id)
                        result.fold(
                            onSuccess = {
                                logger.info { "Recording deleted: $id" }
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse<Unit>(
                                        success = true,
                                        data = null,
                                        message = "Recording deleted successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error deleting recording: $id" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        data = null,
                                        message = "Error deleting recording: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error deleting recording" }
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
                
                // GET /api/v1/recordings/{id}/download - получение URL для скачивания
                get("/download") {
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Recording ID is required"
                            )
                        )
                        
                        val result = recordingRepository.getDownloadUrl(id)
                        result.fold(
                            onSuccess = { url ->
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        data = url,
                                        message = "Download URL retrieved successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error getting download URL for recording: $id" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        data = null,
                                        message = "Error getting download URL: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error getting download URL" }
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
                
                // POST /api/v1/recordings/{id}/export - экспорт записи
                post("/export") {
                    try {
                        val id = call.parameters["id"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Recording ID is required"
                            )
                        )
                        
                        // TODO: Получить параметры экспорта из тела запроса
                        val format = call.request.queryParameters["format"] ?: "mp4"
                        val quality = call.request.queryParameters["quality"] ?: "medium"
                        
                        val result = recordingRepository.exportRecording(id, format, quality)
                        result.fold(
                            onSuccess = { exportUrl ->
                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        data = exportUrl,
                                        message = "Recording exported successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                logger.error(error) { "Error exporting recording: $id" }
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<String>(
                                        success = false,
                                        data = null,
                                        message = "Error exporting recording: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error exporting recording" }
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
            
            // POST /api/v1/recordings/start - начать запись
            post("/start") {
                try {
                    val request = call.receive<StartRecordingRequest>()
                    
                    // Получаем камеру
                    val camera = cameraRepository.getCameraById(request.cameraId)
                        ?: return@post call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<StartRecordingResponse>(
                                success = false,
                                data = null,
                                message = "Camera not found: ${request.cameraId}"
                            )
                        )
                    
                    // Парсим формат и качество
                    val format = try {
                        RecordingFormat.valueOf(request.format?.uppercase() ?: "MP4")
                    } catch (e: Exception) {
                        RecordingFormat.MP4
                    }
                    
                    val quality = try {
                        Quality.valueOf(request.quality?.uppercase() ?: "HIGH")
                    } catch (e: Exception) {
                        Quality.HIGH
                    }
                    
                    // Начинаем запись
                    val result = videoRecordingService.startRecording(
                        camera = camera,
                        format = format,
                        quality = quality,
                        duration = request.duration
                    )
                    
                    result.fold(
                        onSuccess = { recording ->
                            val estimatedEndTime = if (request.duration != null) {
                                recording.startTime + request.duration
                            } else null
                            
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = StartRecordingResponse(
                                        recordingId = recording.id,
                                        cameraId = recording.cameraId,
                                        startTime = recording.startTime,
                                        estimatedEndTime = estimatedEndTime
                                    ),
                                    message = "Recording started successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error starting recording for camera: ${request.cameraId}" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<StartRecordingResponse>(
                                    success = false,
                                    data = null,
                                    message = "Error starting recording: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error starting recording" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<StartRecordingResponse>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
            
            // POST /api/v1/recordings/stop/{cameraId} - остановить запись
            post("/stop/{cameraId}") {
                try {
                    val cameraId = call.parameters["cameraId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<RecordingDto>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )
                    
                    val result = videoRecordingService.stopRecording(cameraId)
                    
                    result.fold(
                        onSuccess = { recording ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = recording.toDto(),
                                    message = "Recording stopped successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error stopping recording for camera: $cameraId" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<RecordingDto>(
                                    success = false,
                                    data = null,
                                    message = "Error stopping recording: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error stopping recording" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<RecordingDto>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
            
            // POST /api/v1/recordings/pause/{cameraId} - приостановить запись
            post("/pause/{cameraId}") {
                try {
                    val cameraId = call.parameters["cameraId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<RecordingDto>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )
                    
                    val result = videoRecordingService.pauseRecording(cameraId)
                    
                    result.fold(
                        onSuccess = { recording ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = recording.toDto(),
                                    message = "Recording paused successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error pausing recording for camera: $cameraId" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<RecordingDto>(
                                    success = false,
                                    data = null,
                                    message = "Error pausing recording: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error pausing recording" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<RecordingDto>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }
            
            // POST /api/v1/recordings/resume/{cameraId} - возобновить запись
            post("/resume/{cameraId}") {
                try {
                    val cameraId = call.parameters["cameraId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<RecordingDto>(
                            success = false,
                            data = null,
                            message = "Camera ID is required"
                        )
                    )
                    
                    val result = videoRecordingService.resumeRecording(cameraId)
                    
                    result.fold(
                        onSuccess = { recording ->
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = recording.toDto(),
                                    message = "Recording resumed successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            logger.error(error) { "Error resuming recording for camera: $cameraId" }
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<RecordingDto>(
                                    success = false,
                                    data = null,
                                    message = "Error resuming recording: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error resuming recording" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<RecordingDto>(
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

