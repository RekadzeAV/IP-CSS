package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.time.Duration.Companion.minutes

// Импортируем StreamQuality из HlsGeneratorService
import com.company.ipcamera.server.service.StreamQuality

private val logger = KotlinLogging.logger {}

/**
 * Сервис для трансляции видеопотоков с IP-камер
 *
 * Управляет трансляцией RTSP потоков в форматы, поддерживаемые веб-браузерами (HLS)
 * и мобильными приложениями (RTSP для ExoPlayer).
 *
 * Особенности:
 * - Оптимизированная буферизация с управлением памятью
 * - Автоматическая генерация HLS сегментов через FFmpeg
 * - Поддержка разных уровней качества
 * - Автоматическая очистка неактивных стримов
 */
class VideoStreamService(
    private val cameraRepository: CameraRepository,
    private val hlsGeneratorService: HlsGeneratorService? = null,
    private val streamsDirectory: String = "streams",
    private val maxBufferSize: Int = 50, // Максимальное количество кадров в буфере
    private val streamTimeoutMinutes: Int = 30 // Таймаут неактивного стрима
) {
    private val activeStreams = ConcurrentHashMap<String, ActiveStream>()
    private val streamScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        ensureDirectoriesExist()
        startCleanupTask()
    }

    /**
     * Активный стрим
     */
    private data class ActiveStream(
        val streamId: String,
        val cameraId: String,
        val rtspClient: RtspClient,
        val streamJob: Job,
        val videoFrames: SharedFlow<RtspFrame>,
        val startTime: Long,
        val lastActivityTime: Long = System.currentTimeMillis(),
        val hlsQuality: StreamQuality = StreamQuality.MEDIUM,
        val hlsPlaylistPath: String? = null
    )

    /**
     * Начать трансляцию для камеры
     */
    suspend fun startStream(cameraId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Проверяем, не идет ли уже трансляция
            if (activeStreams.containsKey(cameraId)) {
                val existingStream = activeStreams[cameraId]!!
                logger.info { "Stream already active for camera: $cameraId, streamId: ${existingStream.streamId}" }
                return@withContext Result.success(existingStream.streamId)
            }

            // Получаем информацию о камере
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            // Создаем ID для стрима
            val streamId = UUID.randomUUID().toString()
            val startTime = System.currentTimeMillis()

            // Создаем RTSP клиент
            val rtspConfig = RtspClientConfig(
                url = camera.url,
                username = camera.username,
                password = camera.password,
                timeoutMillis = 10000,
                enableVideo = true,
                enableAudio = camera.audio
            )

            val rtspClient = RtspClient(rtspConfig)

            // Подключаемся к RTSP потоку
            rtspClient.connect()

            // Ждем подключения (максимум 10 секунд)
            var connected = false
            val timeout = System.currentTimeMillis() + 10000

            while (System.currentTimeMillis() < timeout && !connected) {
                val status = rtspClient.getStatus().value
                if (status == RtspClientStatus.CONNECTED || status == RtspClientStatus.PLAYING) {
                    connected = true
                    break
                }
                delay(100)
            }

            if (!connected) {
                rtspClient.close()
                return@withContext Result.failure(
                    Exception("Failed to connect to RTSP stream within timeout")
                )
            }

            // Начинаем воспроизведение
            rtspClient.play()

            // Создаем SharedFlow для видеокадров с оптимизированной буферизацией
            // Используем ограниченный буфер для управления памятью
            val videoFrames = MutableSharedFlow<RtspFrame>(
                extraBufferCapacity = maxBufferSize,
                onBufferOverflow = BufferOverflow.DROP_OLDEST // Удаляем самые старые кадры при переполнении
            )

            // Подписываемся на видеокадры и перенаправляем их в SharedFlow
            // Обновляем время последней активности
            val streamJob = streamScope.launch {
                try {
                    rtspClient.getVideoFrames().collect { frame ->
                        try {
                            videoFrames.emit(frame)
                            // Обновляем время последней активности в активном стриме
                            activeStreams[cameraId]?.let { stream ->
                                activeStreams[cameraId] = stream.copy(
                                    lastActivityTime = System.currentTimeMillis()
                                )
                            }
                        } catch (e: Exception) {
                            // Игнорируем ошибки эмиссии (буфер переполнен)
                            logger.debug(e) { "Failed to emit frame to buffer (buffer full)" }
                        }
                    }
                } catch (e: CancellationException) {
                    logger.info { "Stream cancelled: $streamId" }
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Error in stream: $streamId" }
                }
            }

            // Получаем RTSP URL с credentials для HLS генерации
            val rtspUrlWithAuth = if (camera.username != null && camera.password != null) {
                val url = camera.url
                if (!url.contains("@")) {
                    val protocol = url.substringBefore("://")
                    val rest = url.substringAfter("://")
                    "$protocol://${camera.username}:${camera.password}@$rest"
                } else {
                    camera.url
                }
            } else {
                camera.url
            }

            // Запускаем генерацию HLS если доступен HlsGeneratorService
            val hlsPlaylistPath = hlsGeneratorService?.startHlsGeneration(
                streamId = streamId,
                rtspUrl = rtspUrlWithAuth,
                quality = StreamQuality.MEDIUM // Можно сделать конфигурируемым
            )

            // Сохраняем активный стрим
            val activeStream = ActiveStream(
                streamId = streamId,
                cameraId = cameraId,
                rtspClient = rtspClient,
                streamJob = streamJob,
                videoFrames = videoFrames.asSharedFlow(),
                startTime = startTime,
                lastActivityTime = System.currentTimeMillis(),
                hlsQuality = StreamQuality.MEDIUM,
                hlsPlaylistPath = hlsPlaylistPath
            )

            activeStreams[cameraId] = activeStream

            // Отправляем WebSocket событие о начале стрима
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.CAMERAS,
                    "stream_started",
                    jsonObject {
                        put("cameraId", cameraId)
                        put("streamId", streamId)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for stream start" }
            }

            logger.info { "Started stream: $streamId for camera: $cameraId" }
            Result.success(streamId)

        } catch (e: Exception) {
            logger.error(e) { "Error starting stream for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Остановить трансляцию для камеры
     */
    suspend fun stopStream(cameraId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val activeStream = activeStreams.remove(cameraId)
                ?: return@withContext Result.failure(
                    IllegalStateException("No active stream for camera: $cameraId")
                )

            // Останавливаем генерацию HLS
            hlsGeneratorService?.stopHlsGeneration(activeStream.streamId)

            // Отменяем задачу стрима
            activeStream.streamJob.cancel()

            // Останавливаем RTSP клиент
            activeStream.rtspClient.stop()
            activeStream.rtspClient.disconnect()
            activeStream.rtspClient.close()

            // Отправляем WebSocket событие об остановке стрима
            try {
                WebSocketManager.broadcastEvent(
                    WebSocketChannel.CAMERAS,
                    "stream_stopped",
                    jsonObject {
                        put("cameraId", cameraId)
                        put("streamId", activeStream.streamId)
                        put("timestamp", System.currentTimeMillis())
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Failed to send WebSocket event for stream stop" }
            }

            logger.info { "Stopped stream: ${activeStream.streamId} for camera: $cameraId" }
            Result.success(Unit)

        } catch (e: Exception) {
            logger.error(e) { "Error stopping stream for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Получить поток видеокадров для камеры
     */
    fun getVideoStream(cameraId: String): SharedFlow<RtspFrame>? {
        val activeStream = activeStreams[cameraId] ?: return null
        return activeStream.videoFrames
    }

    /**
     * Получить последний кадр из потока (для снимков)
     * Внимание: это может не работать оптимально, так как SharedFlow не хранит последнее значение
     * Для production рекомендуется использовать StateFlow или отдельный буфер последних кадров
     */
    fun getLatestFrame(cameraId: String): RtspFrame? {
        // TODO: Реализовать хранение последнего кадра в ActiveStream
        // Пока возвращаем null
        logger.warn { "getLatestFrame not yet fully implemented" }
        return null
    }

    /**
     * Получить RTSP URL для прямой трансляции (для ExoPlayer)
     * В Android ExoPlayer может напрямую работать с RTSP, поэтому возвращаем оригинальный URL
     */
    suspend fun getRtspUrl(cameraId: String): String? = withContext(Dispatchers.IO) {
        val camera = cameraRepository.getCameraById(cameraId) ?: return@withContext null

        // Если нужна авторизация, встраиваем credentials в URL
        if (camera.username != null && camera.password != null) {
            val url = camera.url
            if (!url.contains("@")) {
                // Формат: rtsp://username:password@host:port/path
                val protocol = url.substringBefore("://")
                val rest = url.substringAfter("://")
                return@withContext "$protocol://${camera.username}:${camera.password}@$rest"
            }
        }

        camera.url
    }

    /**
     * Получить URL для HLS потока (для веб-интерфейса)
     * Возвращает URL для доступа к HLS плейлисту
     */
    fun getHlsUrl(cameraId: String): String? {
        val streamId = activeStreams[cameraId]?.streamId ?: return null
        return hlsGeneratorService?.getPlaylistUrl(streamId)
            ?: "/api/v1/cameras/$cameraId/stream/hls/playlist.m3u8"
    }

    /**
     * Получить путь к HLS плейлисту на диске
     */
    fun getHlsPlaylistPath(cameraId: String): String? {
        return activeStreams[cameraId]?.hlsPlaylistPath
    }

    /**
     * Изменить качество HLS потока
     */
    suspend fun setStreamQuality(cameraId: String, quality: StreamQuality): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val activeStream = activeStreams[cameraId]
                ?: return@withContext Result.failure(
                    IllegalStateException("No active stream for camera: $cameraId")
                )

            // Останавливаем текущую генерацию HLS
            hlsGeneratorService?.stopHlsGeneration(activeStream.streamId)

            // Получаем RTSP URL для перезапуска с новым качеством
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val rtspUrlWithAuth = if (camera.username != null && camera.password != null) {
                val url = camera.url
                if (!url.contains("@")) {
                    val protocol = url.substringBefore("://")
                    val rest = url.substringAfter("://")
                    "$protocol://${camera.username}:${camera.password}@$rest"
                } else {
                    camera.url
                }
            } else {
                camera.url
            }

            // Запускаем генерацию HLS с новым качеством
            val hlsPlaylistPath = hlsGeneratorService?.startHlsGeneration(
                streamId = activeStream.streamId,
                rtspUrl = rtspUrlWithAuth,
                quality = quality
            )

            // Обновляем активный стрим
            activeStreams[cameraId] = activeStream.copy(
                hlsQuality = quality,
                hlsPlaylistPath = hlsPlaylistPath
            )

            logger.info { "Changed stream quality to $quality for camera: $cameraId" }
            Result.success(Unit)

        } catch (e: Exception) {
            logger.error(e) { "Error changing stream quality for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Проверить, активна ли трансляция для камеры
     */
    fun isStreamActive(cameraId: String): Boolean {
        return activeStreams.containsKey(cameraId)
    }

    /**
     * Получить ID активного стрима для камеры
     */
    fun getStreamId(cameraId: String): String? {
        return activeStreams[cameraId]?.streamId
    }

    /**
     * Убедиться, что директории существуют
     */
    private fun ensureDirectoriesExist() {
        try {
            File(streamsDirectory).mkdirs()
            logger.info { "Created streams directory: $streamsDirectory" }
        } catch (e: Exception) {
            logger.error(e) { "Error creating streams directory" }
        }
    }

    /**
     * Задача для очистки неактивных стримов
     */
    private fun startCleanupTask() {
        streamScope.launch {
            while (isActive) {
                try {
                    delay(5.minutes)
                    cleanupInactiveStreams()
                } catch (e: Exception) {
                    logger.error(e) { "Error in cleanup task" }
                }
            }
        }
    }

    /**
     * Очистить неактивные стримы
     */
    private suspend fun cleanupInactiveStreams() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val timeout = streamTimeoutMinutes * 60 * 1000L

        val inactiveStreams = activeStreams.values.filter { stream ->
            now - stream.lastActivityTime > timeout
        }

        inactiveStreams.forEach { stream ->
            try {
                logger.info { "Cleaning up inactive stream: ${stream.streamId} for camera: ${stream.cameraId}" }
                stopStream(stream.cameraId)
            } catch (e: Exception) {
                logger.error(e) { "Error cleaning up inactive stream: ${stream.streamId}" }
            }
        }
    }

    /**
     * Закрыть сервис и освободить ресурсы
     */
    fun close() {
        streamScope.launch {
            // Останавливаем все активные стримы
            activeStreams.keys.forEach { cameraId ->
                try {
                    stopStream(cameraId)
                } catch (e: Exception) {
                    logger.error(e) { "Error stopping stream during close: $cameraId" }
                }
            }
        }

        // Очищаем HLS генератор
        hlsGeneratorService?.cleanup()

        streamScope.cancel()
    }
}

