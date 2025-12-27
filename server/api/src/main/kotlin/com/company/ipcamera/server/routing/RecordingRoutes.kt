package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.HlsGeneratorService
import com.company.ipcamera.server.service.StreamQuality
import com.company.ipcamera.server.service.VideoRecordingService
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import java.io.File
import java.nio.file.Paths
import io.ktor.http.*
import io.ktor.http.content.*
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
    val hlsGeneratorService: HlsGeneratorService? by inject()
    val ffmpegService: FfmpegService by inject()

    authenticate("jwt-auth") {
        route("/recordings") {
            // GET /api/v1/recordings - список записей с фильтрацией и пагинацией
            // Минимум VIEWER для просмотра записей
            get {
                requireRole(UserRole.VIEWER)
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
                // Минимум VIEWER для просмотра записи
                get {
                    requireRole(UserRole.VIEWER)
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
                // Минимум OPERATOR для удаления записей
                delete {
                    requireRole(UserRole.OPERATOR)
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
                // Минимум VIEWER для скачивания записей
                get("/download") {
                    requireRole(UserRole.VIEWER)
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

                // GET /api/v1/recordings/{id}/hls/playlist.m3u8 - HLS плейлист для воспроизведения записи
                // Минимум VIEWER для воспроизведения записей
                route("/hls") {
                    get("/playlist.m3u8") {
                        requireRole(UserRole.VIEWER)
                        try {
                            val id = call.parameters["id"] ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Recording ID is required"
                            )

                            val recording = recordingRepository.getRecordingById(id)
                                ?: return@get call.respond(
                                    HttpStatusCode.NotFound,
                                    "Recording not found"
                                )

                            if (recording.filePath == null) {
                                return@get call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Recording file not available"
                                )
                            }

                            val videoFile = File(recording.filePath)
                            if (!videoFile.exists()) {
                                return@get call.respond(
                                    HttpStatusCode.NotFound,
                                    "Recording file not found"
                                )
                            }

                            // Получаем качество из query параметра
                            val qualityStr = call.request.queryParameters["quality"] ?: "medium"
                            val quality = try {
                                StreamQuality.valueOf(qualityStr.uppercase())
                            } catch (e: Exception) {
                                StreamQuality.MEDIUM
                            }

                            // Генерируем HLS из записи, если еще не сгенерирован
                            val hlsGenerator = hlsGeneratorService
                            if (hlsGenerator == null) {
                                return@get call.respond(
                                    HttpStatusCode.ServiceUnavailable,
                                    "HLS generation service not available"
                                )
                            }

                            val playlistPath = hlsGenerator.startHlsFromRecording(
                                recordingId = id,
                                videoFilePath = recording.filePath,
                                quality = quality
                            )

                            if (playlistPath == null) {
                                return@get call.respond(
                                    HttpStatusCode.InternalServerError,
                                    "Failed to generate HLS playlist"
                                )
                            }

                            val playlistFile = File(playlistPath)
                            if (!playlistFile.exists()) {
                                return@get call.respond(
                                    HttpStatusCode.NotFound,
                                    "HLS playlist not found"
                                )
                            }

                            var playlistContent = playlistFile.readText()

                            // Заменяем пути к сегментам на правильные URL
                            playlistContent = playlistContent.replace(
                                Regex("segment_\\d+\\.ts"),
                                "/api/v1/recordings/$id/hls/\\$0"
                            )

                            call.respondText(
                                playlistContent,
                                ContentType("application", "vnd.apple.mpegurl"),
                                HttpStatusCode.OK
                            )
                        } catch (e: Exception) {
                            logger.error(e) { "Error getting HLS playlist for recording" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Internal server error: ${e.message}"
                            )
                        }
                    }

                    // GET /api/v1/recordings/{id}/hls/segment_XXX.ts - HLS сегмент
                    get("/segment_{segmentNumber:\\d+}.ts") {
                        requireRole(UserRole.VIEWER)
                        try {
                            val id = call.parameters["id"] ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Recording ID is required"
                            )

                            val segmentNumber = call.parameters["segmentNumber"] ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Segment number is required"
                            )

                            val segmentPath = Paths.get(
                                "streams/hls/recordings",
                                id,
                                "segment_${segmentNumber.padStart(3, '0')}.ts"
                            )

                            val segmentFile = segmentPath.toFile()
                            if (segmentFile.exists()) {
                                call.respondFile(segmentFile)
                            } else {
                                call.respond(HttpStatusCode.NotFound, "Segment not found")
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "Error getting HLS segment" }
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                "Internal server error: ${e.message}"
                            )
                        }
                    }
                }

                // POST /api/v1/recordings/{id}/export - экспорт записи
                // Минимум OPERATOR для экспорта записей
                post("/export") {
                    requireRole(UserRole.OPERATOR)
                    try {
                        val id = call.parameters["id"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<String>(
                                success = false,
                                data = null,
                                message = "Recording ID is required"
                            )
                        )

                        // Получаем параметры экспорта из тела запроса или query параметров
                        val request = try {
                            call.receive<ExportRecordingRequest>()
                        } catch (e: Exception) {
                            // Fallback на query параметры для обратной совместимости
                            ExportRecordingRequest(
                                format = call.request.queryParameters["format"] ?: "mp4",
                                quality = call.request.queryParameters["quality"] ?: "medium",
                                startTime = call.request.queryParameters["startTime"]?.toLongOrNull(),
                                endTime = call.request.queryParameters["endTime"]?.toLongOrNull(),
                                useH265 = call.request.queryParameters["useH265"]?.toBoolean() ?: false
                            )
                        }

                        val recording = recordingRepository.getRecordingById(id)
                            ?: return@post call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Recording not found"
                                )
                            )

                        if (recording.filePath == null) {
                            return@post call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Recording file not available"
                                )
                            )
                        }

                        val inputFile = File(recording.filePath)
                        if (!inputFile.exists()) {
                            return@post call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Recording file not found"
                                )
                            )
                        }

                        // Парсим формат
                        val outputFormat = try {
                            RecordingFormat.valueOf(request.format?.uppercase() ?: "MP4")
                        } catch (e: Exception) {
                            RecordingFormat.MP4
                        }

                        // Создаем путь для экспортированного файла
                        val exportDir = File("exports")
                        exportDir.mkdirs()
                        val exportFileName = "${id}_export_${System.currentTimeMillis()}.${outputFormat.name.lowercase()}"
                        val outputFile = File(exportDir, exportFileName)

                        // Конвертируем видео используя FfmpegService
                        val success = ffmpegService.convertVideo(
                            inputFile = inputFile,
                            outputFile = outputFile,
                            outputFormat = outputFormat
                        )

                        if (success && outputFile.exists()) {
                            val exportUrl = "/api/v1/recordings/$id/export/download?file=$exportFileName"
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = exportUrl,
                                    message = "Recording exported successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ApiResponse<String>(
                                    success = false,
                                    data = null,
                                    message = "Failed to export recording"
                                )
                            )
                        }
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

                // GET /api/v1/recordings/{id}/export/download - скачать экспортированный файл
                get("/export/download") {
                    requireRole(UserRole.VIEWER)
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Recording ID is required"
                        )

                        val fileName = call.request.queryParameters["file"]
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "File name is required"
                            )

                        val exportFile = File("exports", fileName)
                        if (exportFile.exists()) {
                            call.respondFile(exportFile)
                        } else {
                            call.respond(HttpStatusCode.NotFound, "Export file not found")
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error downloading export file" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Internal server error: ${e.message}"
                        )
                    }
                }
            }

            // POST /api/v1/recordings/start - начать запись
            // Минимум OPERATOR для начала записи
            post("/start") {
                requireRole(UserRole.OPERATOR)
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
            // Минимум OPERATOR для остановки записи
            post("/stop/{cameraId}") {
                requireRole(UserRole.OPERATOR)
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
            // Минимум OPERATOR для приостановки записи
            post("/pause/{cameraId}") {
                requireRole(UserRole.OPERATOR)
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
            // Минимум OPERATOR для возобновления записи
            post("/resume/{cameraId}") {
                requireRole(UserRole.OPERATOR)
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

