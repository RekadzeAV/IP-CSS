package com.company.ipcamera.server.routing

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.middleware.validateRequest
import com.company.ipcamera.server.service.ExportService
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.HlsGeneratorService
import com.company.ipcamera.server.service.SignedUrlService
import com.company.ipcamera.server.service.StreamQuality
import com.company.ipcamera.server.service.VideoRecordingService
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.PaginatedResult
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
import kotlinx.serialization.json.*
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
    val hlsGeneratorService: HlsGeneratorService by inject()
    val ffmpegService: FfmpegService by inject()
    val signedUrlService: SignedUrlService by inject()
    val exportService: ExportService by inject()

    authenticate("jwt-auth") {
        route("/recordings") {
            // GET /api/v1/recordings - список записей с фильтрацией и пагинацией
            // Минимум VIEWER для просмотра записей
            // Поддерживает расширенную фильтрацию: cameraId, startTime, endTime, status, format, quality
            get {
                requireRole(UserRole.VIEWER)
                try {
                    val cameraId = call.request.queryParameters["cameraId"]
                    val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                    val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
                    val statusStr = call.request.queryParameters["status"]
                    val status = statusStr?.let {
                        try {
                            RecordingStatus.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val formatStr = call.request.queryParameters["format"]
                    val format = formatStr?.let {
                        try {
                            RecordingFormat.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val qualityStr = call.request.queryParameters["quality"]
                    val quality = qualityStr?.let {
                        try {
                            Quality.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

                    // Получаем все записи с базовой фильтрацией
                    var result = recordingRepository.getRecordings(
                        cameraId = cameraId,
                        startTime = startTime,
                        endTime = endTime,
                        page = 1,
                        limit = Int.MAX_VALUE // Получаем все для дополнительной фильтрации
                    )

                    // Применяем расширенную фильтрацию
                    var filteredItems = result.items
                    if (status != null) {
                        filteredItems = filteredItems.filter { it.status == status }
                    }
                    if (format != null) {
                        filteredItems = filteredItems.filter { it.format == format }
                    }
                    if (quality != null) {
                        filteredItems = filteredItems.filter { it.quality == quality }
                    }

                    // Применяем пагинацию
                    val total = filteredItems.size
                    val offset = (page - 1) * limit
                    val paginatedItems = filteredItems.drop(offset).take(limit)
                    val hasMore = offset + limit < total

                    val paginatedResult = PaginatedResult(
                        items = paginatedItems,
                        total = total,
                        page = page,
                        limit = limit,
                        hasMore = hasMore
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = paginatedResult.toDto(),
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

                // GET /api/v1/recordings/{id}/passport - паспорт записи с фактическими параметрами файла
                // Минимум VIEWER для просмотра метаданных записи
                get("/passport") {
                    requireRole(UserRole.VIEWER)
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<RecordingPassportDto>(
                                success = false,
                                data = null,
                                message = "Recording ID is required"
                            )
                        )

                        val recording = recordingRepository.getRecordingById(id)
                            ?: return@get call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<RecordingPassportDto>(
                                    success = false,
                                    data = null,
                                    message = "Recording not found"
                                )
                            )

                        val videoInfo = recording.filePath
                            ?.let { File(it) }
                            ?.takeIf { it.exists() }
                            ?.let { ffmpegService.getVideoInfo(it) }

                        val passport = RecordingPassportDto(
                            recordingId = recording.id,
                            cameraId = recording.cameraId,
                            declaredFormat = recording.format.name,
                            declaredQuality = recording.quality.name,
                            status = recording.status.name,
                            declaredCodec = recording.codec,
                            actualCodec = videoInfo?.get("codec") as? String,
                            width = videoInfo?.get("width") as? Int,
                            height = videoInfo?.get("height") as? Int,
                            durationSeconds = videoInfo?.get("duration")?.let { "${it}s" },
                            bitrate = videoInfo?.get("bitrate")?.toString(),
                            fileSizeBytes = recording.fileSize
                        )

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = passport,
                                message = "Recording passport retrieved successfully"
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error retrieving recording passport" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<RecordingPassportDto>(
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

                        // Генерируем подписанный URL для скачивания (действителен 1 час)
                        val signedUrl = signedUrlService.generateSignedUrl(id, expirationSeconds = 3600L)

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = signedUrl,
                                message = "Download URL retrieved successfully"
                            )
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

                // GET /api/v1/recordings/{id}/download/file - скачать файл записи (требует подписанный URL)
                get("/download/file") {
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Recording ID is required"
                        )

                        // Получаем параметры подписи из query
                        val signature = call.request.queryParameters["sig"]
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Signature is required"
                            )

                        val expiresStr = call.request.queryParameters["expires"]
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Expires parameter is required"
                            )

                        val expires = expiresStr.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Invalid expires parameter"
                            )

                        // Проверяем подпись
                        if (!signedUrlService.verifySignedUrl(id, signature, expires)) {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                "Invalid or expired signature"
                            )
                            return@get
                        }

                        // Получаем запись
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

                        val file = File(recording.filePath)
                        if (!file.exists()) {
                            return@get call.respond(
                                HttpStatusCode.NotFound,
                                "Recording file not found"
                            )
                        }

                        // Отправляем файл
                        call.respondFile(file)
                    } catch (e: Exception) {
                        logger.error(e) { "Error downloading recording file" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "Internal server error: ${e.message}"
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

                            val recordingFilePath = recording.filePath ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Recording file not available"
                            )

                            // Генерируем HLS из записи, если еще не сгенерирован
                            val playlistPath = hlsGeneratorService.startHlsFromRecording(
                                recordingId = id,
                                videoFilePath = recordingFilePath,
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

                            // Устанавливаем правильные заголовки для HLS плейлиста
                            call.response.headers.append("Cache-Control", "no-cache, no-store, must-revalidate")
                            call.response.headers.append("Access-Control-Allow-Origin", "*")
                            call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")

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
                    // Один параметр пути: смешанный литерал `segment_{n}.ts` в Ktor 2.3 не заполняет `segmentNumber` в call.parameters.
                    get("/{segmentFile}") {
                        requireRole(UserRole.VIEWER)
                        try {
                            val id = call.parameters["id"] ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Recording ID is required"
                            )

                            val segmentFileName = call.parameters["segmentFile"] ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Segment file name is required"
                            )
                            val segmentMatch = Regex("^segment_(\\d+)\\.ts$").matchEntire(segmentFileName)
                                ?: return@get call.respond(
                                    HttpStatusCode.BadRequest,
                                    "Invalid segment file name"
                                )
                            val segmentNumber = segmentMatch.groupValues[1]

                            val segmentPath = Paths.get(
                                "streams/hls/recordings",
                                id,
                                "segment_${segmentNumber.padStart(3, '0')}.ts"
                            )

                            val segmentFile = segmentPath.toFile()
                            if (segmentFile.exists() && segmentFile.isFile) {
                                // Устанавливаем правильные заголовки для сегментов
                                call.response.headers.append("Cache-Control", "public, max-age=3600")
                                call.response.headers.append("Access-Control-Allow-Origin", "*")
                                call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")
                                call.response.headers.append("Content-Type", "video/mp2t")

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

                        // Парсим качество
                        val quality = try {
                            Quality.valueOf(request.quality?.uppercase() ?: "MEDIUM")
                        } catch (e: Exception) {
                            Quality.MEDIUM
                        }

                        // Валидация и конвертация времени обрезки (миллисекунды -> секунды)
                        val startTimeSeconds = request.startTime?.let {
                            if (it < 0) null else it / 1000L // Конвертируем миллисекунды в секунды
                        }
                        val endTimeSeconds = request.endTime?.let {
                            val endSeconds = it / 1000L // Конвертируем миллисекунды в секунды
                            if (endSeconds <= (startTimeSeconds ?: 0)) null else endSeconds
                        }

                        // Создаем путь для экспортированного файла
                        val exportDir = File("exports")
                        exportDir.mkdirs()
                        val exportFileName = "${id}_export_${System.currentTimeMillis()}.${outputFormat.name.lowercase()}"
                        val outputFile = File(exportDir, exportFileName)

                        // Экспортируем видео с параметрами качества, времени и формата
                        val success = ffmpegService.exportVideo(
                            inputFile = inputFile,
                            outputFile = outputFile,
                            format = outputFormat,
                            quality = quality,
                            startTime = startTimeSeconds?.toDouble(),
                            endTime = endTimeSeconds?.toDouble(),
                            useH265 = request.useH265 ?: false
                        )

                        if (success && outputFile.exists()) {
                            // Генерируем подписанный URL для экспортированного файла (действителен 24 часа)
                            val exportUrl = signedUrlService.generateExportSignedUrl(
                                recordingId = id,
                                fileName = exportFileName,
                                expirationSeconds = 86400L // 24 часа
                            )
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

                // GET /api/v1/recordings/{id}/export/download - скачать экспортированный файл (требует подписанный URL)
                get("/export/download") {
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

                        // Получаем параметры подписи из query
                        val signature = call.request.queryParameters["sig"]
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Signature is required"
                            )

                        val expiresStr = call.request.queryParameters["expires"]
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Expires parameter is required"
                            )

                        val expires = expiresStr.toLongOrNull()
                            ?: return@get call.respond(
                                HttpStatusCode.BadRequest,
                                "Invalid expires parameter"
                            )

                        // Проверяем подпись
                        if (!signedUrlService.verifyExportSignedUrl(id, fileName, signature, expires)) {
                            call.respond(
                                HttpStatusCode.Forbidden,
                                "Invalid or expired signature"
                            )
                            return@get
                        }

                        val exportFile = File("exports", fileName)
                        if (exportFile.exists() && exportFile.isFile) {
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
                        cameraId = request.cameraId,
                        duration = request.duration,
                        format = request.format?.lowercase() ?: "mp4"
                    )

                    result.fold(
                        onSuccess = {
                            val recording = recordingRepository.getRecordingById(request.cameraId + "_latest")
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = StartRecordingResponse(
                                        recordingId = recording?.id ?: "",
                                        cameraId = request.cameraId,
                                        startTime = recording?.startTime ?: System.currentTimeMillis(),
                                        estimatedEndTime = if (request.duration != null) {
                                            (recording?.startTime ?: System.currentTimeMillis()) + request.duration
                                        } else null
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
                        onSuccess = {
                            val recording = recordingRepository.getRecordingById(cameraId + "_latest")
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = recording?.toDto() ?: RecordingDto(
                                        id = "",
                                        cameraId = cameraId,
                                        startTime = System.currentTimeMillis(),
                                        duration = 0,
                                        format = "mp4",
                                        quality = "HIGH",
                                        status = "COMPLETED",
                                        createdAt = System.currentTimeMillis()
                                    ),
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

                    // Pause recording is not supported in simplified VideoRecordingService
                    val recording = recordingRepository.getRecordingById(cameraId + "_latest")
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = recording?.toDto() ?: RecordingDto(
                                id = "",
                                cameraId = cameraId,
                                startTime = System.currentTimeMillis(),
                                duration = 0,
                                format = "mp4",
                                quality = "HIGH",
                                status = "PAUSED",
                                createdAt = System.currentTimeMillis()
                            ),
                            message = "Recording pause is not supported - use stop instead"
                        )
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

                    // Resume recording is not supported in simplified VideoRecordingService
                    val recording = recordingRepository.getRecordingById(cameraId + "_latest")
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = recording?.toDto() ?: RecordingDto(
                                id = "",
                                cameraId = cameraId,
                                startTime = System.currentTimeMillis(),
                                duration = 0,
                                format = "mp4",
                                quality = "HIGH",
                                status = "RECORDING",
                                createdAt = System.currentTimeMillis()
                            ),
                            message = "Recording resume is not supported - use start instead"
                        )
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

            // POST /api/v1/recordings/bulk/delete - массовое удаление записей
            // Минимум OPERATOR для массового удаления записей
            post("/bulk/delete") {
                requireRole(UserRole.OPERATOR)
                try {
                    val request = call.receive<BulkDeleteRecordingsRequest>()

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateBulkDeleteRecordingsRequest(it) }) {
                        return@post
                    }

                    val failedIds = mutableListOf<String>()
                    var deletedCount = 0

                    // Удаляем каждую запись
                    for (id in request.ids) {
                        val result = recordingRepository.deleteRecording(id)
                        result.fold(
                            onSuccess = {
                                deletedCount++
                            },
                            onFailure = { error ->
                                logger.warn(error) { "Failed to delete recording: $id" }
                                failedIds.add(id)
                            }
                        )
                    }

                    // Отправляем WebSocket событие о массовом удалении
                    try {
                        WebSocketManager.broadcastEvent(
                            WebSocketChannel.RECORDINGS,
                            "recordings_bulk_deleted",
                            buildJsonObject {
                                put("deletedCount", deletedCount)
                                put("failedCount", failedIds.size)
                                put("failedIds", JsonArray(failedIds.map { JsonPrimitive(it) }))
                                put("timestamp", System.currentTimeMillis())
                            }
                        )
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to send WebSocket event for bulk recording deletion" }
                    }

                    val response = BulkDeleteRecordingsResponse(
                        deletedCount = deletedCount,
                        failedCount = failedIds.size,
                        failedIds = failedIds
                    )

                    val statusCode = if (failedIds.isEmpty()) HttpStatusCode.OK else HttpStatusCode.PartialContent
                    call.respond(
                        statusCode,
                        ApiResponse(
                            success = failedIds.isEmpty(),
                            data = response,
                            message = if (failedIds.isEmpty()) {
                                "All recordings deleted successfully"
                            } else {
                                "Some recordings could not be deleted"
                            }
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error bulk deleting recordings" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<BulkDeleteRecordingsResponse>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/recordings/bulk/export - массовый экспорт записей
            // Минимум OPERATOR для массового экспорта записей
            post("/bulk/export") {
                requireRole(UserRole.OPERATOR)
                try {
                    val request = call.receive<BulkExportRecordingsRequest>()

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateBulkExportRecordingsRequest(it) }) {
                        return@post
                    }

                    val failedIds = mutableListOf<String>()
                    val exportUrls = mutableListOf<String>()
                    var exportedCount = 0

                    // Экспортируем каждую запись
                    for (id in request.ids) {
                        val recording = recordingRepository.getRecordingById(id)
                        if (recording == null) {
                            failedIds.add(id)
                            continue
                        }

                        if (recording.filePath == null) {
                            failedIds.add(id)
                            continue
                        }

                        val inputFile = File(recording.filePath)
                        if (!inputFile.exists()) {
                            failedIds.add(id)
                            continue
                        }

                        try {
                            // Парсим формат
                            val outputFormat = try {
                                RecordingFormat.valueOf(request.format?.uppercase() ?: "MP4")
                            } catch (e: Exception) {
                                RecordingFormat.MP4
                            }

                            // Парсим качество
                            val quality = try {
                                Quality.valueOf(request.quality?.uppercase() ?: "MEDIUM")
                            } catch (e: Exception) {
                                Quality.MEDIUM
                            }

                            // Валидация и конвертация времени обрезки (миллисекунды -> секунды)
                            val startTimeSeconds = request.startTime?.let {
                                if (it < 0) null else it / 1000L // Конвертируем миллисекунды в секунды
                            }
                            val endTimeSeconds = request.endTime?.let {
                                val endSeconds = it / 1000L // Конвертируем миллисекунды в секунды
                                if (endSeconds <= (startTimeSeconds ?: 0)) null else endSeconds
                            }

                            // Создаем путь для экспортированного файла
                            val exportDir = File("exports")
                            exportDir.mkdirs()
                            val exportFileName = "${id}_export_${System.currentTimeMillis()}.${outputFormat.name.lowercase()}"
                            val outputFile = File(exportDir, exportFileName)

                            // Экспортируем видео с параметрами качества, времени и формата
                            val success = ffmpegService.exportVideo(
                                inputFile = inputFile,
                                outputFile = outputFile,
                                format = outputFormat,
                                quality = quality,
                                startTime = startTimeSeconds?.toDouble(),
                                endTime = endTimeSeconds?.toDouble(),
                                useH265 = request.useH265 ?: false
                            )

                            if (success && outputFile.exists()) {
                                // Генерируем подписанный URL для экспортированного файла (действителен 24 часа)
                                val exportUrl = signedUrlService.generateExportSignedUrl(
                                    recordingId = id,
                                    fileName = exportFileName,
                                    expirationSeconds = 86400L // 24 часа
                                )
                                exportUrls.add(exportUrl)
                                exportedCount++
                            } else {
                                logger.warn { "Failed to export recording: $id" }
                                failedIds.add(id)
                            }
                        } catch (e: Exception) {
                            logger.warn(e) { "Error exporting recording: $id" }
                            failedIds.add(id)
                        }
                    }

                    // Отправляем WebSocket событие о массовом экспорте
                    try {
                        WebSocketManager.broadcastEvent(
                            WebSocketChannel.RECORDINGS,
                            "recordings_bulk_exported",
                            buildJsonObject {
                                put("exportedCount", exportedCount)
                                put("failedCount", failedIds.size)
                                put("failedIds", JsonArray(failedIds.map { JsonPrimitive(it) }))
                                put("timestamp", System.currentTimeMillis())
                            }
                        )
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to send WebSocket event for bulk recording export" }
                    }

                    val response = BulkExportRecordingsResponse(
                        exportedCount = exportedCount,
                        failedCount = failedIds.size,
                        failedIds = failedIds,
                        exportUrls = exportUrls
                    )

                    val statusCode = if (failedIds.isEmpty()) HttpStatusCode.OK else HttpStatusCode.PartialContent
                    call.respond(
                        statusCode,
                        ApiResponse(
                            success = failedIds.isEmpty(),
                            data = response,
                            message = if (failedIds.isEmpty()) {
                                "All recordings exported successfully"
                            } else {
                                "Some recordings could not be exported"
                            }
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error bulk exporting recordings" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<BulkExportRecordingsResponse>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/recordings/export/csv - экспорт записей в CSV формат
            // Минимум VIEWER для экспорта записей
            get("/export/csv") {
                requireRole(UserRole.VIEWER)
                try {
                    val cameraId = call.request.queryParameters["cameraId"]
                    val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                    val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
                    val statusStr = call.request.queryParameters["status"]
                    val status = statusStr?.let {
                        try {
                            RecordingStatus.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val formatStr = call.request.queryParameters["format"]
                    val format = formatStr?.let {
                        try {
                            RecordingFormat.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val qualityStr = call.request.queryParameters["quality"]
                    val quality = qualityStr?.let {
                        try {
                            Quality.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }

                    // Получаем все записи с фильтрацией (без пагинации для экспорта)
                    var result = recordingRepository.getRecordings(
                        cameraId = cameraId,
                        startTime = startTime,
                        endTime = endTime,
                        page = 1,
                        limit = Int.MAX_VALUE
                    )

                    // Применяем расширенную фильтрацию
                    var filteredItems = result.items
                    if (status != null) {
                        filteredItems = filteredItems.filter { it.status == status }
                    }
                    if (format != null) {
                        filteredItems = filteredItems.filter { it.format == format }
                    }
                    if (quality != null) {
                        filteredItems = filteredItems.filter { it.quality == quality }
                    }

                    // Экспортируем в CSV
                    val csvContent = exportService.exportRecordingsToCsv(filteredItems)

                    // Устанавливаем заголовки для скачивания файла
                    call.response.headers.append("Content-Disposition", "attachment; filename=\"recordings_${System.currentTimeMillis()}.csv\"")
                    call.response.headers.append("Content-Type", "text/csv; charset=utf-8")

                    call.respondText(csvContent, ContentType.Text.CSV)
                } catch (e: Exception) {
                    logger.error(e) { "Error exporting recordings to CSV" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error exporting recordings to CSV: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/recordings/export/json - экспорт записей в JSON формат
            // Минимум VIEWER для экспорта записей
            get("/export/json") {
                requireRole(UserRole.VIEWER)
                try {
                    val cameraId = call.request.queryParameters["cameraId"]
                    val startTime = call.request.queryParameters["startTime"]?.toLongOrNull()
                    val endTime = call.request.queryParameters["endTime"]?.toLongOrNull()
                    val statusStr = call.request.queryParameters["status"]
                    val status = statusStr?.let {
                        try {
                            RecordingStatus.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val formatStr = call.request.queryParameters["format"]
                    val format = formatStr?.let {
                        try {
                            RecordingFormat.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }
                    val qualityStr = call.request.queryParameters["quality"]
                    val quality = qualityStr?.let {
                        try {
                            Quality.valueOf(it.uppercase())
                        } catch (e: Exception) { null }
                    }

                    // Получаем все записи с фильтрацией (без пагинации для экспорта)
                    var result = recordingRepository.getRecordings(
                        cameraId = cameraId,
                        startTime = startTime,
                        endTime = endTime,
                        page = 1,
                        limit = Int.MAX_VALUE
                    )

                    // Применяем расширенную фильтрацию
                    var filteredItems = result.items
                    if (status != null) {
                        filteredItems = filteredItems.filter { it.status == status }
                    }
                    if (format != null) {
                        filteredItems = filteredItems.filter { it.format == format }
                    }
                    if (quality != null) {
                        filteredItems = filteredItems.filter { it.quality == quality }
                    }

                    // Экспортируем в JSON
                    val jsonContent = exportService.exportRecordingsToJson(filteredItems)

                    // Устанавливаем заголовки для скачивания файла
                    call.response.headers.append("Content-Disposition", "attachment; filename=\"recordings_${System.currentTimeMillis()}.json\"")
                    call.response.headers.append("Content-Type", "application/json; charset=utf-8")

                    call.respondText(jsonContent, ContentType.Application.Json)
                } catch (e: Exception) {
                    logger.error(e) { "Error exporting recordings to JSON" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error exporting recordings to JSON: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}

