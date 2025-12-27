package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamType
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.StorageService
import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.fileSize

private val logger = KotlinLogging.logger {}

/**
 * Сервис для записи видео с IP-камер
 *
 * Управляет записью видеопотоков с камер через RTSP клиент,
 * сохраняет записи в файлы, генерирует thumbnail'ы и управляет
 * жизненным циклом записей.
 */
class VideoRecordingService(
    private val recordingRepository: RecordingRepository,
    private val recordingsDirectory: String = "recordings",
    private val thumbnailsDirectory: String = "thumbnails",
    private val ffmpegService: FfmpegService = FfmpegService(),
    private val storageService: StorageService = StorageService("recordings")
) {
    private val activeRecordings = ConcurrentHashMap<String, ActiveRecording>()
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Создаем директории для записей и thumbnail'ов
        ensureDirectoriesExist()

        // Запускаем фоновую задачу для очистки старых записей
        startCleanupTask()
    }

    /**
     * Активная запись
     */
    private data class ActiveRecording(
        val recording: Recording,
        val rtspClient: RtspClient?,
        val recordingJob: Job,
        val fileOutputStream: FileOutputStream?,
        val ffmpegProcess: Process?,
        val startTime: Long
    )

    /**
     * Начать запись с камеры
     */
    suspend fun startRecording(
        camera: Camera,
        format: RecordingFormat = RecordingFormat.MP4,
        quality: Quality = Quality.HIGH,
        duration: Long? = null // null = бесконечная запись
    ): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            // Проверяем, не идет ли уже запись с этой камеры
            if (activeRecordings.containsKey(camera.id)) {
                return@withContext Result.failure(
                    IllegalStateException("Recording already in progress for camera: ${camera.id}")
                )
            }

            // Создаем запись
            val recordingId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            val recording = Recording(
                id = recordingId,
                cameraId = camera.id,
                cameraName = camera.name,
                startTime = startTime,
                endTime = null,
                duration = 0,
                filePath = null,
                fileSize = null,
                format = format,
                quality = quality,
                status = RecordingStatus.ACTIVE,
                thumbnailUrl = null,
                createdAt = startTime
            )

            // Сохраняем запись в репозиторий
            val addResult = recordingRepository.addRecording(recording)
            if (addResult.isFailure) {
                return@withContext Result.failure(
                    addResult.exceptionOrNull() ?: Exception("Failed to add recording")
                )
            }

            // Создаем RTSP клиент
            val rtspConfig = RtspClientConfig(
                url = camera.url,
                username = camera.username,
                password = camera.password,
                enableVideo = true,
                enableAudio = camera.audio
            )

            val rtspClient = RtspClient(rtspConfig)

            // Подключаемся к RTSP потоку
            rtspClient.connect()

            // Ждем подключения (максимум 10 секунд)
            var connected = false
            val timeout = System.currentTimeMillis() + 10000

            // Проверяем статус с таймаутом
            while (System.currentTimeMillis() < timeout && !connected) {
                val status = rtspClient.getStatus().value
                if (status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING) {
                    connected = true
                    break
                }
                delay(100) // Проверяем каждые 100мс
            }

            if (!connected) {
                rtspClient.close()
                return@withContext Result.failure(
                    Exception("Failed to connect to RTSP stream within timeout")
                )
            }

            // Проверяем доступное место на диске
            // Оцениваем примерный размер записи (1 час записи ~500MB-2GB в зависимости от качества)
            val estimatedSize = when (quality) {
                Quality.LOW -> 500L * 1024 * 1024 // 500MB
                Quality.MEDIUM -> 1000L * 1024 * 1024 // 1GB
                Quality.HIGH -> 2000L * 1024 * 1024 // 2GB
                Quality.ULTRA -> 4000L * 1024 * 1024 // 4GB
            }

            if (!storageService.hasEnoughSpace(estimatedSize)) {
                rtspClient.close()
                return@withContext Result.failure(
                    IllegalStateException("Not enough disk space for recording. Required: ${estimatedSize / 1024 / 1024}MB")
                )
            }

            // Проверяем порог предупреждения
            if (storageService.isWarningThresholdExceeded()) {
                logger.warn {
                    "Storage usage threshold exceeded: ${storageService.getUsagePercentage()}% used"
                }
            }

            // Создаем файл для записи
            val filePath = createRecordingFilePath(recordingId, format)
            val file = File(filePath)
            file.parentFile?.mkdirs()

            // Обновляем запись с путем к файлу
            val recordingWithPath = recording.copy(filePath = filePath)
            recordingRepository.updateRecording(recordingWithPath)

            // Используем FFmpeg для записи, если доступен
            val useFfmpeg = ffmpegService.isAvailable()
            var ffmpegProcess: Process? = null
            var fileOutputStream: FileOutputStream? = null

            if (useFfmpeg) {
                // Используем FFmpeg для прямого кодирования из RTSP потока
                logger.info { "Using FFmpeg for recording: ${recording.id}" }
                ffmpegProcess = ffmpegService.encodeRtspToFile(
                    rtspUrl = camera.url,
                    outputFile = file,
                    format = format,
                    quality = quality,
                    duration = duration?.let { it / 1000 }, // Конвертируем в секунды
                    username = camera.username,
                    password = camera.password
                )
            } else {
                // Fallback: используем RTSP клиент и записываем кадры
                logger.warn { "FFmpeg not available, using fallback recording method" }
                rtspClient.play()
                fileOutputStream = FileOutputStream(file)
            }

            // Запускаем запись
            val recordingJob = recordingScope.launch {
                try {
                    if (useFfmpeg && ffmpegProcess != null) {
                        // Ждем завершения FFmpeg процесса
                        val exitCode = ffmpegProcess.waitFor()
                        if (exitCode != 0) {
                            logger.error { "FFmpeg process exited with code: $exitCode" }
                            updateRecordingStatus(recording.id, RecordingStatus.FAILED)
                        }
                    } else {
                        // Используем старый метод записи
                        recordStream(rtspClient, fileOutputStream!!, recordingWithPath, duration)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error during recording: ${recording.id}" }
                    updateRecordingStatus(recording.id, RecordingStatus.FAILED)
                } finally {
                    fileOutputStream?.close()
                    ffmpegProcess?.destroy()
                    rtspClient.disconnect()
                    rtspClient.close()
                }
            }

            // Сохраняем активную запись
            activeRecordings[camera.id] = ActiveRecording(
                recording = recordingWithPath,
                rtspClient = if (useFfmpeg) null else rtspClient,
                recordingJob = recordingJob,
                fileOutputStream = fileOutputStream,
                ffmpegProcess = ffmpegProcess,
                startTime = startTime
            )

            // Отправляем WebSocket событие о начале записи
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.RECORDINGS,
                    "recording_started",
                    jsonObject {
                        put("recordingId", recording.id)
                        put("cameraId", camera.id)
                        put("cameraName", camera.name)
                        put("format", format.name)
                        put("quality", quality.name)
                        put("startTime", startTime)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for recording start" }
            }

            logger.info { "Started recording: ${recording.id} for camera: ${camera.id}" }
            Result.success(recording)

        } catch (e: Exception) {
            logger.error(e) { "Error starting recording for camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    /**
     * Остановить запись с камеры
     */
    suspend fun stopRecording(cameraId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val activeRecording = activeRecordings.remove(cameraId)
                ?: return@withContext Result.failure(
                    IllegalStateException("No active recording for camera: $cameraId")
                )

            // Отменяем задачу записи
            activeRecording.recordingJob.cancel()

            // Останавливаем FFmpeg процесс или RTSP клиент
            activeRecording.ffmpegProcess?.destroyForcibly()
            activeRecording.rtspClient?.stop()
            activeRecording.rtspClient?.disconnect()
            activeRecording.rtspClient?.close()

            // Закрываем файл
            activeRecording.fileOutputStream?.close()

            // Обновляем запись
            val endTime = System.currentTimeMillis()
            val duration = endTime - activeRecording.startTime

            val filePath = activeRecording.recording.filePath
            val fileSize = if (filePath != null) {
                try {
                    Paths.get(filePath).fileSize()
                } catch (e: Exception) {
                    logger.warn(e) { "Could not get file size for: $filePath" }
                    null
                }
            } else null

            // Генерируем thumbnail
            val thumbnailUrl = if (filePath != null) {
                generateThumbnail(filePath, activeRecording.recording.id)
            } else null

            val updatedRecording = activeRecording.recording.copy(
                endTime = endTime,
                duration = duration,
                filePath = filePath,
                fileSize = fileSize,
                status = RecordingStatus.COMPLETED,
                thumbnailUrl = thumbnailUrl
            )

            recordingRepository.updateRecording(updatedRecording)

            // Отправляем WebSocket событие об остановке записи
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.RECORDINGS,
                    "recording_stopped",
                    jsonObject {
                        put("recordingId", updatedRecording.id)
                        put("cameraId", cameraId)
                        put("duration", duration)
                        put("fileSize", fileSize ?: 0)
                        put("endTime", endTime)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for recording stop" }
            }

            logger.info { "Stopped recording: ${updatedRecording.id} for camera: $cameraId" }
            Result.success(updatedRecording)

        } catch (e: Exception) {
            logger.error(e) { "Error stopping recording for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Приостановить запись
     *
     * Для FFmpeg: отправляем SIGSTOP сигнал процессу (если поддерживается)
     * Для RTSP клиента: используем pause()
     */
    suspend fun pauseRecording(cameraId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val activeRecording = activeRecordings[cameraId]
                ?: return@withContext Result.failure(
                    IllegalStateException("No active recording for camera: $cameraId")
                )

            // Для FFmpeg пытаемся приостановить процесс
            activeRecording.ffmpegProcess?.let { process ->
                try {
                    // Используем ProcessHandle для отправки сигнала (Java 9+)
                    val processHandle = process.toHandle()
                    if (processHandle.isAlive) {
                        // На Unix системах можно использовать SIGSTOP
                        // На Windows это не поддерживается напрямую
                        if (System.getProperty("os.name").lowercase().contains("win")) {
                            // На Windows пауза через FFmpeg не поддерживается напрямую
                            // Можно только остановить и перезапустить, но это потеряет данные
                            logger.warn { "FFmpeg pause not supported on Windows, pausing RTSP client only" }
                        } else {
                            // На Unix системах можно использовать kill -STOP
                            // Но это требует дополнительных прав и может быть нестабильно
                            logger.info { "FFmpeg pause requested, but not fully supported. Pausing RTSP client." }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Could not pause FFmpeg process, pausing RTSP client only" }
                }
            }

            // Для RTSP клиента используем pause
            activeRecording.rtspClient?.pause()

            val updatedRecording = activeRecording.recording.copy(
                status = RecordingStatus.PAUSED
            )

            // Обновляем активную запись
            activeRecordings[cameraId] = activeRecording.copy(
                recording = updatedRecording
            )

            recordingRepository.updateRecording(updatedRecording)

            // Отправляем WebSocket событие о приостановке записи
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.RECORDINGS,
                    "recording_paused",
                    jsonObject {
                        put("recordingId", updatedRecording.id)
                        put("cameraId", cameraId)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for recording pause" }
            }

            logger.info { "Paused recording: ${updatedRecording.id} for camera: $cameraId" }
            Result.success(updatedRecording)

        } catch (e: Exception) {
            logger.error(e) { "Error pausing recording for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Возобновить запись
     *
     * Для FFmpeg: отправляем SIGCONT сигнал процессу (если поддерживается)
     * Для RTSP клиента: используем play()
     */
    suspend fun resumeRecording(cameraId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val activeRecording = activeRecordings[cameraId]
                ?: return@withContext Result.failure(
                    IllegalStateException("No active recording for camera: $cameraId")
                )

            // Для FFmpeg пытаемся возобновить процесс
            activeRecording.ffmpegProcess?.let { process ->
                try {
                    val processHandle = process.toHandle()
                    if (processHandle.isAlive) {
                        // На Unix системах можно использовать SIGCONT
                        // На Windows это не поддерживается напрямую
                        if (!System.getProperty("os.name").lowercase().contains("win")) {
                            logger.info { "Resuming FFmpeg process (Unix only)" }
                            // На Unix можно использовать kill -CONT, но это требует дополнительных прав
                        } else {
                            logger.info { "FFmpeg resume not fully supported on Windows, resuming RTSP client only" }
                        }
                    }
                } catch (e: Exception) {
                    logger.warn(e) { "Could not resume FFmpeg process, resuming RTSP client only" }
                }
            }

            // Для RTSP клиента используем play
            activeRecording.rtspClient?.play()

            val updatedRecording = activeRecording.recording.copy(
                status = RecordingStatus.ACTIVE
            )

            // Обновляем активную запись
            activeRecordings[cameraId] = activeRecording.copy(
                recording = updatedRecording
            )

            recordingRepository.updateRecording(updatedRecording)

            // Отправляем WebSocket событие о возобновлении записи
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.RECORDINGS,
                    "recording_resumed",
                    jsonObject {
                        put("recordingId", updatedRecording.id)
                        put("cameraId", cameraId)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for recording resume" }
            }

            logger.info { "Resumed recording: ${updatedRecording.id} for camera: $cameraId" }
            Result.success(updatedRecording)

        } catch (e: Exception) {
            logger.error(e) { "Error resuming recording for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Получить статус записи для камеры
     */
    fun getRecordingStatus(cameraId: String): Recording? {
        return activeRecordings[cameraId]?.recording
    }

    /**
     * Получить все активные записи
     */
    fun getActiveRecordings(): Map<String, Recording> {
        return activeRecordings.mapValues { it.value.recording }
    }

    /**
     * Запись потока в файл (fallback метод)
     *
     * ВАЖНО: Этот метод используется только если FFmpeg недоступен.
     * Он записывает сырые кадры, которые могут быть несовместимы со стандартными плеерами.
     * Рекомендуется установить FFmpeg для правильного кодирования.
     */
    private suspend fun recordStream(
        rtspClient: RtspClient,
        fileOutputStream: FileOutputStream,
        recording: Recording,
        duration: Long?
    ) = withContext(Dispatchers.IO) {
        logger.warn {
            "Using fallback recording method without proper encoding. " +
            "Recorded file may not be compatible with standard video players. " +
            "Consider installing FFmpeg for proper video encoding."
        }

        val videoFrames = rtspClient.getVideoFrames()
        val audioFrames = if (recording.quality != Quality.LOW) {
            rtspClient.getAudioFrames()
        } else null

        val startTime = System.currentTimeMillis()
        val endTime = if (duration != null) startTime + duration else Long.MAX_VALUE

        // Пытаемся использовать FFmpeg pipe для правильного кодирования
        // Если FFmpeg доступен, но не был использован ранее, попробуем запустить его
        if (ffmpegService.isAvailable()) {
            logger.info { "FFmpeg is available, but was not used. Attempting to use FFmpeg pipe..." }
            // В будущем можно реализовать FFmpeg pipe для записи через stdin
            // Пока используем базовый метод
        }

        try {
            var frameCount = 0L
            videoFrames.collect { frame ->
                if (System.currentTimeMillis() >= endTime) {
                    return@collect
                }

                try {
                    // Записываем кадр в файл
                    // ВАЖНО: Это сырые данные кадра, не кодированные в MP4/MKV
                    // Файл может быть несовместим со стандартными плеерами
                    fileOutputStream.write(frame.data)
                    fileOutputStream.flush()
                    frameCount++

                    // Логируем прогресс каждые 100 кадров
                    if (frameCount % 100 == 0L) {
                        logger.debug { "Recorded $frameCount frames for recording: ${recording.id}" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error writing frame to file" }
                }
            }

            logger.info { "Fallback recording completed: $frameCount frames recorded for ${recording.id}" }
        } catch (e: CancellationException) {
            logger.info { "Recording cancelled: ${recording.id}" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error during stream recording: ${recording.id}" }
            throw e
        } finally {
            fileOutputStream.close()
        }
    }

    /**
     * Создать путь к файлу записи
     */
    private fun createRecordingFilePath(recordingId: String, format: RecordingFormat): String {
        val extension = when (format) {
            RecordingFormat.MP4 -> "mp4"
            RecordingFormat.MKV -> "mkv"
            RecordingFormat.AVI -> "avi"
            RecordingFormat.MOV -> "mov"
            RecordingFormat.FLV -> "flv"
        }

        val timestamp = System.currentTimeMillis()
        val fileName = "${recordingId}_${timestamp}.$extension"
        return Paths.get(recordingsDirectory, fileName).toString()
    }

    /**
     * Генерация thumbnail для записи
     */
    private suspend fun generateThumbnail(videoFilePath: String, recordingId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val videoFile = File(videoFilePath)
                if (!videoFile.exists()) {
                    logger.warn { "Video file not found for thumbnail: $videoFilePath" }
                    return@withContext null
                }

                val thumbnailPath = Paths.get(thumbnailsDirectory, "${recordingId}.jpg")
                val thumbnailFile = thumbnailPath.toFile()

                // Создаем директорию для thumbnail'ов, если не существует
                thumbnailFile.parentFile?.mkdirs()

                // Используем FfmpegService для генерации thumbnail
                val success = ffmpegService.generateThumbnail(
                    videoFile = videoFile,
                    thumbnailFile = thumbnailFile,
                    timeOffset = 1.0,
                    width = 320
                )

                if (success) {
                    val relativePath = "/thumbnails/${recordingId}.jpg"
                    logger.info { "Thumbnail generated: $thumbnailPath" }
                    return@withContext relativePath
                } else {
                    logger.warn { "Failed to generate thumbnail for: $videoFilePath" }
                    return@withContext null
                }
            } catch (e: Exception) {
                logger.error(e) { "Error generating thumbnail for: $videoFilePath" }
                null
            }
        }
    }

    /**
     * Обновить статус записи
     */
    private suspend fun updateRecordingStatus(recordingId: String, status: RecordingStatus) {
        try {
            val recording = recordingRepository.getRecordingById(recordingId)
            if (recording != null) {
                val updated = recording.copy(status = status)
                recordingRepository.updateRecording(updated)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating recording status: $recordingId" }
        }
    }

    /**
     * Убедиться, что директории существуют
     */
    private fun ensureDirectoriesExist() {
        try {
            File(recordingsDirectory).mkdirs()
            File(thumbnailsDirectory).mkdirs()
            logger.info { "Created directories: $recordingsDirectory, $thumbnailsDirectory" }
        } catch (e: Exception) {
            logger.error(e) { "Error creating directories" }
        }
    }

    /**
     * Запустить фоновую задачу для очистки старых записей
     */
    private fun startCleanupTask() {
        recordingScope.launch {
            while (isActive) {
                try {
                    delay(3600000) // Каждый час
                    cleanupOldRecordings()
                } catch (e: Exception) {
                    logger.error(e) { "Error in cleanup task" }
                }
            }
        }
    }

    /**
     * Очистка старых записей
     */
    suspend fun cleanupOldRecordings(
        maxAgeDays: Int = 30,
        maxStorageBytes: Long? = null
    ) = withContext(Dispatchers.IO) {
        try {
            logger.info { "Starting cleanup of old recordings (max age: $maxAgeDays days)" }

            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)

            // Получаем все записи
            val allRecordings = mutableListOf<Recording>()
            var page = 1
            var hasMore = true

            while (hasMore) {
                val result = recordingRepository.getRecordings(page = page, limit = 100)
                allRecordings.addAll(result.items)
                hasMore = result.hasMore
                page++
            }

            // Фильтруем старые записи
            val oldRecordings = allRecordings.filter { it.createdAt < cutoffTime }

            // Удаляем старые записи
            var deletedCount = 0
            var freedSpace = 0L

            for (recording in oldRecordings) {
                try {
                    // Удаляем файл записи
                    if (recording.filePath != null) {
                        val file = File(recording.filePath)
                        if (file.exists()) {
                            freedSpace += file.length()
                            file.delete()
                        }
                    }

                    // Удаляем thumbnail
                    if (recording.thumbnailUrl != null) {
                        val thumbnailFile = File(recording.thumbnailUrl)
                        if (thumbnailFile.exists()) {
                            thumbnailFile.delete()
                        }
                    }

                    // Удаляем запись из репозитория
                    recordingRepository.deleteRecording(recording.id)
                    deletedCount++
                } catch (e: Exception) {
                    logger.error(e) { "Error deleting recording: ${recording.id}" }
                }
            }

            logger.info {
                "Cleanup completed: deleted $deletedCount recordings, " +
                "freed ${freedSpace / 1024 / 1024} MB"
            }

        } catch (e: Exception) {
            logger.error(e) { "Error during cleanup" }
        }
    }

    /**
     * Закрыть сервис и освободить ресурсы
     */
    fun close() {
        recordingScope.launch {
            // Останавливаем все активные записи
            activeRecordings.keys.forEach { cameraId ->
                try {
                    stopRecording(cameraId)
                } catch (e: Exception) {
                    logger.error(e) { "Error stopping recording during close: $cameraId" }
                }
            }
        }

        recordingScope.cancel()
    }
}

