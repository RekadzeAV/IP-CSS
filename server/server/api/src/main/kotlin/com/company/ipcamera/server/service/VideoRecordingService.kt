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
     */
    suspend fun pauseRecording(cameraId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val activeRecording = activeRecordings[cameraId]
                ?: return@withContext Result.failure(
                    IllegalStateException("No active recording for camera: $cameraId")
                )

            // Для FFmpeg пауза не поддерживается напрямую, нужно остановить и перезапустить
            // Для RTSP клиента используем pause
            activeRecording.rtspClient?.pause()

            val updatedRecording = activeRecording.recording.copy(
                status = RecordingStatus.PAUSED
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
     */
    suspend fun resumeRecording(cameraId: String): Result<Recording> = withContext(Dispatchers.IO) {
        try {
            val activeRecording = activeRecordings[cameraId]
                ?: return@withContext Result.failure(
                    IllegalStateException("No active recording for camera: $cameraId")
                )

            // Для FFmpeg возобновление не поддерживается напрямую
            // Для RTSP клиента используем play
            activeRecording.rtspClient?.play()

            val updatedRecording = activeRecording.recording.copy(
                status = RecordingStatus.ACTIVE
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
     * Запись потока в файл
     *
     * Использует FFmpeg pipe для правильного мультиплексирования видео и аудио.
     * Если FFmpeg недоступен, записывает только видео (fallback режим).
     */
    private suspend fun recordStream(
        rtspClient: RtspClient,
        fileOutputStream: FileOutputStream,
        recording: Recording,
        duration: Long?
    ) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val endTime = if (duration != null) startTime + duration else Long.MAX_VALUE

        // Пытаемся использовать FFmpeg pipe для записи с аудио
        if (ffmpegService.isAvailable()) {
            try {
                recordStreamWithFfmpegPipe(
                    rtspClient = rtspClient,
                    outputFile = File(recording.filePath ?: return@withContext),
                    recording = recording,
                    duration = duration
                )
                return@withContext
            } catch (e: Exception) {
                logger.warn(e) { "FFmpeg pipe recording failed, falling back to basic recording" }
            }
        }

        // Fallback: записываем только видео (старый метод)
        // ВАЖНО: Этот метод не поддерживает аудио из-за сложности мультиплексирования
        // Рекомендуется использовать FFmpeg для полной поддержки аудио
        logger.warn {
            "Using fallback recording method without audio support. " +
            "Audio frames will be discarded. Consider installing FFmpeg for full audio support."
        }

        val videoFrames = rtspClient.getVideoFrames()
        // В fallback режиме аудио не записывается из-за сложности мультиплексирования
        // Получаем аудиокадры только для предотвращения блокировки потока
        val audioFrames = if (recording.quality != Quality.LOW) {
            rtspClient.getAudioFrames()
        } else null

        try {
            // Запускаем корутину для сбора аудио (но не записываем)
            val audioJob = audioFrames?.let { frames ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        frames.collect {
                            // Аудиокадры получаются, но не записываются в fallback режиме
                            // Это ограничение текущей реализации
                        }
                    } catch (e: CancellationException) {
                        // Игнорируем отмену
                    } catch (e: Exception) {
                        logger.debug(e) { "Audio frame collection error (ignored in fallback mode)" }
                    }
                }
            }

            // Записываем только видеокадры
            videoFrames.collect { frame ->
                if (System.currentTimeMillis() >= endTime) {
                    audioJob?.cancel()
                    return@collect
                }

                try {
                    // Записываем кадр в файл
                    // ВАЖНО: Это сырые кадры без правильного контейнера
                    // Файл может быть некорректным для воспроизведения
                    fileOutputStream.write(frame.data)
                    fileOutputStream.flush()
                } catch (e: Exception) {
                    logger.error(e) { "Error writing frame to file" }
                }
            }

            audioJob?.cancel()
        } catch (e: CancellationException) {
            logger.info { "Recording cancelled: ${recording.id}" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error during stream recording: ${recording.id}" }
            throw e
        }
    }

    /**
     * Запись потока через FFmpeg pipe с поддержкой аудио
     *
     * Использует FFmpeg для правильного мультиплексирования видео и аудио потоков
     * в выбранный формат контейнера (MP4, MKV и т.д.)
     */
    private suspend fun recordStreamWithFfmpegPipe(
        rtspClient: RtspClient,
        outputFile: File,
        recording: Recording,
        duration: Long?
    ) = withContext(Dispatchers.IO) {
        val videoFrames = rtspClient.getVideoFrames()
        val audioFrames = rtspClient.getAudioFrames()

        // Создаем временные файлы для видео и аудио потоков
        val tempVideoFile = File(outputFile.parent, "${outputFile.nameWithoutExtension}.video.tmp")
        val tempAudioFile = File(outputFile.parent, "${outputFile.nameWithoutExtension}.audio.tmp")

        try {
            // Записываем видео и аудио в отдельные временные файлы
            val videoJob = CoroutineScope(Dispatchers.IO).launch {
                FileOutputStream(tempVideoFile).use { videoOut ->
                    videoFrames.collect { frame ->
                        try {
                            videoOut.write(frame.data)
                            videoOut.flush()
                        } catch (e: Exception) {
                            logger.error(e) { "Error writing video frame" }
                        }
                    }
                }
            }

            val audioJob = CoroutineScope(Dispatchers.IO).launch {
                FileOutputStream(tempAudioFile).use { audioOut ->
                    audioFrames.collect { frame ->
                        try {
                            audioOut.write(frame.data)
                            audioOut.flush()
                        } catch (e: Exception) {
                            logger.debug(e) { "Error writing audio frame" }
                        }
                    }
                }
            }

            // Ждем завершения записи или таймаут
            val startTime = System.currentTimeMillis()
            val endTime = if (duration != null) startTime + duration else Long.MAX_VALUE

            while (System.currentTimeMillis() < endTime && (videoJob.isActive || audioJob.isActive)) {
                delay(1000)
            }

            // Останавливаем запись
            videoJob.cancel()
            audioJob.cancel()
            videoJob.join()
            audioJob.join()

            // Используем FFmpeg для мультиплексирования видео и аудио
            val success = ffmpegService.muxVideoAndAudio(
                videoFile = tempVideoFile,
                audioFile = tempAudioFile,
                outputFile = outputFile,
                format = recording.format,
                quality = recording.quality
            )

            if (!success) {
                throw Exception("Failed to mux video and audio streams")
            }

            logger.info { "Successfully recorded stream with audio: ${outputFile.absolutePath}" }

        } finally {
            // Удаляем временные файлы
            tempVideoFile.delete()
            tempAudioFile.delete()
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

