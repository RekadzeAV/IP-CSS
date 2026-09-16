package com.company.ipcamera.core.network

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.network.rtsp.NativeRtspClient
import com.company.ipcamera.core.network.utils.FrameType
import com.company.ipcamera.core.network.utils.MediaFrame
import com.company.ipcamera.core.network.utils.createMediaFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val Dispatchers.IO get() = Dispatchers.Default

/**
 * Тип потока RTSP
 */
enum class RtspStreamType {
    VIDEO,
    AUDIO,
    METADATA
}

/**
 * RTSP кадр
 */
data class RtspFrame(
    val data: ByteArray,
    val timestamp: Long,
    val streamType: RtspStreamType,
    val width: Int = 0,
    val height: Int = 0
)

/**
 * Информация о потоке
 */
data class RtspStreamInfo(
    val index: Int,
    val type: RtspStreamType,
    val resolution: Resolution?,
    val fps: Int,
    val codec: String,
    val audioCodec: String? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null
)

/**
 * Callback для получения кадров
 */
typealias RtspFrameCallback = (RtspFrame) -> Unit

/**
 * Callback для изменения статуса
 */
typealias RtspStatusCallback = (RtspClientStatus, String?) -> Unit

data class RtspRuntimeDiagnostics(
    val connectAttempts: Long = 0,
    val connectSuccesses: Long = 0,
    val connectFailures: Long = 0,
    val reconnectAttempts: Long = 0,
    val reconnectSuccesses: Long = 0,
    val reconnectFailures: Long = 0,
    val consecutiveFailures: Long = 0,
    val lastError: String? = null,
    val lastErrorAt: Long? = null,
    val lastConnectedAt: Long? = null,
    val lastDisconnectedAt: Long? = null,
    val lastPlayingAt: Long? = null,
    val lastFrameAt: Long? = null
)

/**
 * RTSP клиент для работы с видеопотоками.
 *
 * Обертка над [NativeRtspClient]: при успешном `create()` + `connect()` — нативный путь и callbacks.
 * Синтетический fallback только при [RtspClientConfig.allowSimulatedFallback] = true.
 */
class RtspClient(
    private val config: RtspClientConfig
) : RtspClientInterface {
    private val nativeClient = NativeRtspClient()
    private var nativeHandle: Long = 0L
    private var usingNative: Boolean = false

    private var status = MutableStateFlow<RtspClientStatus>(RtspClientStatus.DISCONNECTED)
    private val runtimeDiagnostics = MutableStateFlow(RtspRuntimeDiagnostics())
    private val videoFrameFlow = MutableSharedFlow<RtspFrame>(extraBufferCapacity = 10)
    private val audioFrameFlow = MutableSharedFlow<RtspFrame>(extraBufferCapacity = 10)

    private var videoCallback: RtspFrameCallback? = null
    private var audioCallback: RtspFrameCallback? = null
    private var statusCallback: RtspStatusCallback? = null

    private var connectionJob: Job? = null
    private var receiveJob: Job? = null

    private val streams = mutableListOf<RtspStreamInfo>()

    /**
     * Получить статус подключения
     */
    override fun getStatus(): StateFlow<RtspClientStatus> = status.asStateFlow()

    /**
     * Получить текущие runtime diagnostics (синхронное значение).
     */
    fun getRuntimeDiagnosticsSnapshot(): RtspRuntimeDiagnostics = runtimeDiagnostics.value

    /**
     * Получить поток runtime diagnostics.
     */
    fun getRuntimeDiagnostics(): StateFlow<RtspRuntimeDiagnostics> = runtimeDiagnostics.asStateFlow()

    /**
     * Получить поток видеокадров
     */
    fun getVideoFrames(): SharedFlow<RtspFrame> = videoFrameFlow.asSharedFlow()

    /**
     * Получить поток аудиокадров
     */
    fun getAudioFrames(): SharedFlow<RtspFrame> = audioFrameFlow.asSharedFlow()

    /**
     * Подключиться к RTSP серверу
     */
    override suspend fun connect(): Boolean {
        if (status.value != RtspClientStatus.DISCONNECTED) {
            logger.warn { "RTSP client already connected or connecting" }
            return false
        }

        status.value = RtspClientStatus.CONNECTING
        statusCallback?.invoke(RtspClientStatus.CONNECTING, "Connecting to ${config.url}")
        updateDiagnostics { it.copy(connectAttempts = it.connectAttempts + 1) }

        try {
            nativeHandle = nativeClient.create()
            usingNative = nativeHandle != 0L

            val connected = if (usingNative) {
                nativeClient.setReconnectParams(
                    handle = nativeHandle,
                    enabled = config.reconnectEnabled,
                    maxRetries = config.reconnectMaxRetries,
                    initialDelayMs = config.reconnectInitialDelayMs,
                    maxDelayMs = config.reconnectMaxDelayMs,
                    backoffMultiplier = config.reconnectBackoffMultiplier
                )
                nativeClient.connect(
                    handle = nativeHandle,
                    url = config.url,
                    username = config.username,
                    password = config.password,
                    timeoutMs = config.timeoutMillis.toInt()
                )
            } else {
                false
            }

            if (connected) {
                configureNativeCallbacks()
                refreshStreamsFromNative()
                logger.info {
                    "RTSP connection successful: " +
                        "path=native url=${config.url} " +
                        "streams=${streams.size} " +
                        "diagnostics=${runtimeDiagnostics.value}"
                }
            } else {
                if (nativeHandle != 0L) {
                    try { nativeClient.disconnect(nativeHandle) } catch (t: Throwable) { }
                    try { nativeClient.destroy(nativeHandle) } catch (t: Throwable) { }
                    nativeHandle = 0L
                    usingNative = false
                }
                if (!config.allowSimulatedFallback) {
                    logger.warn {
                        "RTSP connection failed: " +
                            "path=error reason=native_failed url=${config.url} " +
                            "fallback=disabled " +
                            "diagnostics=${runtimeDiagnostics.value}"
                    }
                    status.value = RtspClientStatus.ERROR
                    statusCallback?.invoke(
                        RtspClientStatus.ERROR,
                        "Native RTSP unavailable or connection failed"
                    )
                    markFailure("Native RTSP unavailable or connection failed")
                    return false
                }
                delay(500)
                streams.clear()
                if (config.enableVideo) {
                    streams.add(
                        RtspStreamInfo(
                            index = 0,
                            type = RtspStreamType.VIDEO,
                            resolution = Resolution(1920, 1080),
                            fps = 25,
                            codec = "H.264"
                        )
                    )
                }

                if (config.enableAudio) {
                    streams.add(
                        RtspStreamInfo(
                            index = streams.size,
                            type = RtspStreamType.AUDIO,
                            resolution = null,
                            fps = 0,
                            codec = "AAC",
                            audioCodec = "AAC",
                            sampleRate = 48000,
                            channels = 2
                        )
                    )
                }
            }

            // Если во время fallback-подключения клиент был отключён, не перезаписываем статус.
            if (status.value == RtspClientStatus.DISCONNECTED) {
                logger.warn { "RTSP connect aborted: client was disconnected during fallback connect" }
                streams.clear()
                return false
            }
            status.value = RtspClientStatus.CONNECTED
            statusCallback?.invoke(RtspClientStatus.CONNECTED, "Connected successfully")
            updateDiagnostics {
                it.copy(
                    connectSuccesses = it.connectSuccesses + 1,
                    consecutiveFailures = 0,
                    lastConnectedAt = Clock.System.now().toEpochMilliseconds()
                )
            }

            val sessionPath = if (connected) "native" else "simulation"
            logger.info { "RTSP session path=$sessionPath url=${config.url}" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to RTSP server" }
            if (nativeHandle != 0L) {
                try { nativeClient.disconnect(nativeHandle) } catch (t: Throwable) { }
                try { nativeClient.destroy(nativeHandle) } catch (t: Throwable) { }
            }
            nativeHandle = 0L
            usingNative = false
            streams.clear()
            status.value = RtspClientStatus.ERROR
            statusCallback?.invoke(RtspClientStatus.ERROR, e.message)
            markFailure(e.message)
            return false
        }
    }

    /**
     * Попытаться переподключиться с экспоненциальным backoff на уровне Kotlin-обертки.
     * Используется как safety-net поверх нативного reconnect.
     */
    suspend fun reconnectWithBackoff(
        maxAttempts: Int = config.reconnectMaxRetries.coerceAtLeast(1),
        initialDelayMs: Long = config.reconnectInitialDelayMs.toLong().coerceAtLeast(1L),
        maxDelayMs: Long = config.reconnectMaxDelayMs.toLong().coerceAtLeast(initialDelayMs),
        backoffMultiplier: Double = config.reconnectBackoffMultiplier.toDouble().coerceAtLeast(1.0)
    ): Boolean = withContext(Dispatchers.IO) {
        var attempt = 0
        var delayMs = initialDelayMs
        val jitterRatio = config.reconnectJitterRatio.toDouble().coerceIn(0.0, 0.5)
        while (attempt < maxAttempts) {
            attempt++
            updateDiagnostics { it.copy(reconnectAttempts = it.reconnectAttempts + 1) }
            try {
                disconnect()
                connect()

                val deadline = Clock.System.now().toEpochMilliseconds() + config.timeoutMillis + 2_000L
                while (Clock.System.now().toEpochMilliseconds() < deadline) {
                    when (status.value) {
                        RtspClientStatus.CONNECTED, RtspClientStatus.PLAYING -> {
                            updateDiagnostics {
                                it.copy(
                                    reconnectSuccesses = it.reconnectSuccesses + 1,
                                    consecutiveFailures = 0
                                )
                            }
                            return@withContext true
                        }
                        RtspClientStatus.ERROR -> break
                        else -> delay(50)
                    }
                }
            } catch (e: Exception) {
                logger.warn(e) { "RTSP reconnect attempt $attempt failed" }
            }

            updateDiagnostics {
                it.copy(
                    reconnectFailures = it.reconnectFailures + 1,
                    consecutiveFailures = it.consecutiveFailures + 1
                )
            }
            if (attempt < maxAttempts) {
                val jitterDelta = (delayMs * jitterRatio).toLong()
                val jitteredDelay = if (jitterDelta > 0L) {
                    (delayMs - jitterDelta) + kotlin.random.Random.nextLong((jitterDelta * 2) + 1L)
                } else {
                    delayMs
                }
                delay(jitteredDelay.coerceAtLeast(1L))
                delayMs = (delayMs * backoffMultiplier).toLong().coerceAtMost(maxDelayMs)
            }
        }
        false
    }

    /**
     * Начать воспроизведение
     */
    override suspend fun play(): Boolean = withContext(Dispatchers.IO) {
        if (status.value != RtspClientStatus.CONNECTED) {
            logger.warn { "Cannot play: not connected" }
            return@withContext false
        }

        if (usingNative && nativeHandle != 0L) {
            val played = nativeClient.play(nativeHandle)
            if (!played) {
                status.value = RtspClientStatus.ERROR
                statusCallback?.invoke(RtspClientStatus.ERROR, "Native RTSP play failed")
                markFailure("Native RTSP play failed")
                return@withContext false
            }
        }
        status.value = RtspClientStatus.PLAYING
        statusCallback?.invoke(RtspClientStatus.PLAYING, "Playing")
        updateDiagnostics { it.copy(lastPlayingAt = Clock.System.now().toEpochMilliseconds()) }

        startReceiving()

        logger.info { "RTSP client started playing" }
        true
    }

    /**
     * Отключиться от сервера
     */
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        receiveJob?.cancel()
        receiveJob = null
        stop()

        connectionJob?.cancelAndJoin()
        connectionJob = null

        if (usingNative && nativeHandle != 0L) {
            nativeClient.disconnect(nativeHandle)
            nativeClient.destroy(nativeHandle)
            nativeHandle = 0L
            usingNative = false
        }
        status.value = RtspClientStatus.DISCONNECTED
        statusCallback?.invoke(RtspClientStatus.DISCONNECTED, "Disconnected")
        updateDiagnostics { it.copy(lastDisconnectedAt = Clock.System.now().toEpochMilliseconds()) }

        streams.clear()
        logger.info { "RTSP client disconnected" }
    }

    /**
     * Остановить воспроизведение
     */
    internal suspend fun stop() = withContext(Dispatchers.IO) {
        if (status.value != RtspClientStatus.PLAYING) {
            return@withContext
        }

        receiveJob?.cancel()
        receiveJob = null

        if (usingNative && nativeHandle != 0L) {
            nativeClient.stop(nativeHandle)
        }
        if (status.value == RtspClientStatus.PLAYING) {
            status.value = RtspClientStatus.CONNECTED
            statusCallback?.invoke(RtspClientStatus.CONNECTED, "Stopped")
        }

        logger.info { "RTSP client stopped" }
    }

    /**
     * Приостановить воспроизведение
     */
    override suspend fun pause(): Boolean = withContext(Dispatchers.IO) {
        if (status.value != RtspClientStatus.PLAYING) {
            return@withContext false
        }

        receiveJob?.cancel()
        receiveJob = null

        if (usingNative && nativeHandle != 0L) {
            nativeClient.pause(nativeHandle)
        }
        status.value = RtspClientStatus.CONNECTED
        statusCallback?.invoke(RtspClientStatus.CONNECTED, "Paused")

        logger.info { "RTSP client paused" }
        true
    }

    /**
     * Завершить сессию (alias для disconnect)
     */
    override suspend fun teardown() = disconnect()

    /**
     * Получить список потоков
     */
    fun getStreams(): List<RtspStreamInfo> = streams.toList()

    /**
     * Получить информацию о потоке
     */
    fun getStreamInfo(index: Int): RtspStreamInfo? {
        return streams.getOrNull(index)
    }

    /**
     * Получить информацию о видео потоке (для интерфейса RtspClientInterface)
     */
    override fun getVideoInfo(): VideoStreamInfo? {
        val videoStream = streams.firstOrNull { it.type == RtspStreamType.VIDEO } ?: return null
        return VideoStreamInfo(
            format = when (videoStream.codec.uppercase()) {
                "H264", "H.264" -> VideoFormat.H264
                "H265", "H.265" -> VideoFormat.H265
                "MPEG4" -> VideoFormat.MPEG4
                "MJPEG" -> VideoFormat.MJPEG
                else -> VideoFormat.UNKNOWN
            },
            width = videoStream.resolution?.width ?: 0,
            height = videoStream.resolution?.height ?: 0,
            fps = videoStream.fps,
            bitrate = 0
        )
    }

    /**
     * Получить информацию об аудио потоке (для интерфейса RtspClientInterface)
     */
    override fun getAudioInfo(): AudioStreamInfo? {
        val audioStream = streams.firstOrNull { it.type == RtspStreamType.AUDIO } ?: return null
        return AudioStreamInfo(
            format = when (audioStream.audioCodec?.uppercase() ?: audioStream.codec.uppercase()) {
                "AAC" -> AudioFormat.AAC
                "PCMU", "PCMA", "G711" -> AudioFormat.G711
                "G726" -> AudioFormat.G726
                "MP3" -> AudioFormat.MP3
                else -> AudioFormat.UNKNOWN
            },
            sampleRate = audioStream.sampleRate ?: 48000,
            channels = audioStream.channels ?: 2,
            bitrate = 0
        )
    }

    /**
     * Получить следующий видео фрейм
     */
    override suspend fun getVideoFrame(timeoutMs: Long): MediaFrame? {
        return try {
            withTimeout(timeoutMs) {
                val rtspFrame = videoFrameFlow.first()
                createMediaFrame(
                    data = rtspFrame.data,
                    type = FrameType.VIDEO,
                    timestamp = rtspFrame.timestamp
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Получить следующий аудио фрейм
     */
    override suspend fun getAudioFrame(timeoutMs: Long): MediaFrame? {
        return try {
            withTimeout(timeoutMs) {
                val rtspFrame = audioFrameFlow.first()
                createMediaFrame(
                    data = rtspFrame.data,
                    type = FrameType.AUDIO,
                    timestamp = rtspFrame.timestamp
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Установить callback для видеокадров
     */
    fun setVideoFrameCallback(callback: RtspFrameCallback?) {
        videoCallback = callback
    }

    /**
     * Установить callback для аудиокадров
     */
    fun setAudioFrameCallback(callback: RtspFrameCallback?) {
        audioCallback = callback
    }

    /**
     * Установить callback для изменения статуса
     */
    fun setStatusCallback(callback: RtspStatusCallback?) {
        statusCallback = callback
    }

    /**
     * Начать прием кадров
     */
    private fun startReceiving() {
        receiveJob?.cancel()

        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            if (usingNative && nativeHandle != 0L) {
                while (isActive && status.value == RtspClientStatus.PLAYING) {
                    delay(50)
                }
                return@launch
            }

            if (!config.allowSimulatedFallback) {
                return@launch
            }

            // Симуляция кадров (только при allowSimulatedFallback)
            if (config.enableVideo) {
                launch {
                    var frameNumber = 0
                    while (isActive && status.value == RtspClientStatus.PLAYING) {
                        val frame = RtspFrame(
                            data = ByteArray(100), // Заглушка
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            streamType = RtspStreamType.VIDEO,
                            width = 1920,
                            height = 1080
                        )

                        try {
                            videoFrameFlow.emit(frame)
                            videoCallback?.invoke(frame)
                            updateDiagnostics { it.copy(lastFrameAt = Clock.System.now().toEpochMilliseconds()) }
                        } catch (e: Exception) {
                            logger.error(e) { "Error emitting video frame" }
                        }

                        delay(40) // ~25 FPS
                        frameNumber++
                    }
                }
            }

            if (config.enableAudio) {
                launch {
                    while (isActive && status.value == RtspClientStatus.PLAYING) {
                        val frame = RtspFrame(
                            data = ByteArray(1024), // Заглушка
                            timestamp = Clock.System.now().toEpochMilliseconds(),
                            streamType = RtspStreamType.AUDIO
                        )

                        try {
                            audioFrameFlow.emit(frame)
                            audioCallback?.invoke(frame)
                            updateDiagnostics { it.copy(lastFrameAt = Clock.System.now().toEpochMilliseconds()) }
                        } catch (e: Exception) {
                            logger.error(e) { "Error emitting audio frame" }
                        }

                        delay(20) // Аудио частота зависит от формата
                    }
                }
            }
        }
    }

    /**
     * Освободить ресурсы
     */
    override fun close() {
        connectionJob?.cancel()
        receiveJob?.cancel()
        if (usingNative && nativeHandle != 0L) {
            runCatching { runBlocking { nativeClient.disconnect(nativeHandle) } }
            runCatching { nativeClient.destroy(nativeHandle) }
        }
        nativeHandle = 0L
        usingNative = false
        streams.clear()
        status.value = RtspClientStatus.DISCONNECTED
    }

    /**
     * Получить поддерживаемые аудио кодеки из текущих потоков.
     */
    fun getAudioCodecs(): Set<String> {
        return streams
            .filter { it.type == RtspStreamType.AUDIO }
            .mapNotNull { it.audioCodec ?: detectAudioCodecFromName(it.codec) ?: it.codec.takeIf { c -> c.isNotBlank() } }
            .toSet()
    }

    /**
     * Определить аудио кодек по начальным байтам кадра.
     */
    fun detectAudioCodec(data: ByteArray): String? {
        if (data.isEmpty()) return null
        if (data.size >= 2) {
            val b0 = data[0].toInt() and 0xFF
            val b1 = data[1].toInt() and 0xFF

            if (b0 == 0xFF && (b1 and 0xF0) == 0xF0) return "AAC" // ADTS
            if (b0 == 0xFF && (b1 and 0xE0) == 0xE0) return "MP3" // MPEG Audio
            if (b0 == 0xD5 && b1 == 0x00) return "PCMU"
            if (b0 == 0xD5 && b1 == 0x01) return "PCMA"
        }
        return null
    }

    private fun detectAudioCodecFromName(codecName: String?): String? {
        if (codecName.isNullOrBlank()) return null
        val normalized = codecName.uppercase()
        return when {
            "AAC" in normalized || "MPEG4-GENERIC" in normalized -> "AAC"
            "MP3" in normalized || "MPEG" in normalized -> "MP3"
            "PCMU" in normalized || "G711U" in normalized -> "PCMU"
            "PCMA" in normalized || "G711A" in normalized -> "PCMA"
            else -> null
        }
    }

    private fun configureNativeCallbacks() {
        if (!usingNative || nativeHandle == 0L) return

        nativeClient.setStatusCallback(nativeHandle) { nativeStatus, message ->
            status.value = nativeStatus
            statusCallback?.invoke(nativeStatus, message)
            when (nativeStatus) {
                RtspClientStatus.CONNECTED -> {
                    updateDiagnostics {
                        it.copy(
                            lastConnectedAt = Clock.System.now().toEpochMilliseconds(),
                            consecutiveFailures = 0
                        )
                    }
                }
                RtspClientStatus.PLAYING -> {
                    updateDiagnostics {
                        it.copy(
                            lastPlayingAt = Clock.System.now().toEpochMilliseconds(),
                            consecutiveFailures = 0
                        )
                    }
                }
                RtspClientStatus.PAUSED -> {}
                RtspClientStatus.DISCONNECTED -> {
                    updateDiagnostics { it.copy(lastDisconnectedAt = Clock.System.now().toEpochMilliseconds()) }
                }
                RtspClientStatus.ERROR -> {
                    markFailure(message)
                }
                RtspClientStatus.CONNECTING -> Unit
            }
        }

        if (config.enableVideo) {
            nativeClient.setFrameCallback(nativeHandle, RtspStreamType.VIDEO) { frame ->
                CoroutineScope(Dispatchers.IO).launch {
                    videoFrameFlow.emit(frame)
                    updateDiagnostics { it.copy(lastFrameAt = Clock.System.now().toEpochMilliseconds()) }
                }
                videoCallback?.invoke(frame)
            }
        }

        if (config.enableAudio) {
            nativeClient.setFrameCallback(nativeHandle, RtspStreamType.AUDIO) { frame ->
                CoroutineScope(Dispatchers.IO).launch {
                    audioFrameFlow.emit(frame)
                    updateDiagnostics { it.copy(lastFrameAt = Clock.System.now().toEpochMilliseconds()) }
                }
                audioCallback?.invoke(frame)
            }
        }
    }

    private fun updateDiagnostics(update: (RtspRuntimeDiagnostics) -> RtspRuntimeDiagnostics) {
        runtimeDiagnostics.value = update(runtimeDiagnostics.value)
    }

    private fun markFailure(message: String?) {
        updateDiagnostics {
            it.copy(
                connectFailures = it.connectFailures + 1,
                reconnectFailures = it.reconnectFailures + 1,
                consecutiveFailures = it.consecutiveFailures + 1,
                lastError = message ?: "RTSP error",
                lastErrorAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    private fun refreshStreamsFromNative() {
        if (!usingNative || nativeHandle == 0L) return
        streams.clear()
        val streamCount = nativeClient.getStreamCount(nativeHandle)
        repeat(streamCount) { index ->
            nativeClient.getStreamInfo(nativeHandle, index)?.let { info ->
                val normalized = if (info.type == RtspStreamType.AUDIO) {
                    info.copy(audioCodec = detectAudioCodecFromName(info.codec) ?: info.audioCodec ?: info.codec)
                } else {
                    info
                }
                streams.add(normalized)
            }
        }
    }
}
