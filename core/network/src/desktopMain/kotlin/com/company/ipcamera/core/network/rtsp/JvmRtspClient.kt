package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * JavaCV-based RTSP клиент для JVM/Desktop.
 *
 * Использует [FFmpegFrameGrabber] для подключения к RTSP потоку,
 * захвата видеокадров (H.264/H.265) и аудиокадров (AAC/G.711).
 *
 * Типобезопасная обёртка: не является expect/actual классом,
 * вызывается напрямую из Desktop-приложения или через [NativeRtspClient] bridge.
 */
class JvmRtspClient(
    private val config: JvmRtspConfig = JvmRtspConfig()
) {
    private var grabber: FFmpegFrameGrabber? = null
    private var frameGrabberJob: Job? = null
    private var connectionScope: CoroutineScope? = null

    private val _status = MutableStateFlow(RtspClientStatus.DISCONNECTED)
    val status: StateFlow<RtspClientStatus> = _status.asStateFlow()

    private var videoCallback: ((RtspFrame) -> Unit)? = null
    private var audioCallback: ((RtspFrame) -> Unit)? = null
    private var statusCallback: ((RtspClientStatus, String?) -> Unit)? = null

    private val streams = mutableListOf<RtspStreamInfo>()
    private val handlesCreated = AtomicLong(0)
    private val handlesDestroyed = AtomicLong(0)

    val activeHandleCount: Long get() = handlesCreated.get() - handlesDestroyed.get()

    // =========================================================================
    // Public API — mirrors NativeRtspClient interface
    // =========================================================================

    fun create(): Long {
        val handle = handlesCreated.incrementAndGet()
        logger.info { "JvmRtspClient handle created: $handle" }
        return handle
    }

    suspend fun connect(
        handle: Long,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = withContext(Dispatchers.IO) {
        if (handle <= 0L) return@withContext false

        try {
            _status.value = RtspClientStatus.CONNECTING
            statusCallback?.invoke(RtspClientStatus.CONNECTING, "Connecting to $url")

            val g = FFmpegFrameGrabber(url).apply {
                // FFmpeg options
                setOption("timeout", (timeoutMs * 2).toString())
                setOption("stimeout", timeoutMs.toString())
                setOption("rtsp_transport", config.rtspTransport)
                setOption("buffer_size", config.bufferSize.toString())
                setOption("max_delay", config.maxDelayMs.toString())
                setOption("reorder_queue_size", config.reorderQueueSize.toString())

                if (config.analyzeDurationMs > 0) {
                    setOption("analyzeduration", config.analyzeDurationMs.toString())
                    setOption("probesize", config.probeSize.toString())
                }

                if (!username.isNullOrBlank()) setOption("rtsp_user", username)
                if (!password.isNullOrBlank()) setOption("rtsp_pass", password)

                setOption("loglevel", config.ffmpegLogLevel)
                if (config.forceTcp) setOption("rtsp_flags", "prefer_tcp")

                // Video
                imageWidth = config.preferredWidth
                imageHeight = config.preferredHeight

                // Audio
                audioChannels = if (config.enableAudio) 2 else 0
                sampleRate = config.preferredSampleRate
            }

            logger.info { "Starting FFmpegFrameGrabber for URL: $url" }
            g.start()

            if (!g.hasVideo() && !g.hasAudio()) {
                logger.error { "No video or audio stream found in URL: $url" }
                g.stop(); g.release()
                _status.value = RtspClientStatus.ERROR
                statusCallback?.invoke(RtspClientStatus.ERROR, "No video or audio stream found")
                return@withContext false
            }

            grabber = g
            refreshStreams(g)

            _status.value = RtspClientStatus.CONNECTED
            statusCallback?.invoke(RtspClientStatus.CONNECTED, "Connected successfully")

            logger.info {
                "RTSP connected: url=$url " +
                    "video=${g.hasVideo()} audio=${g.hasAudio()} " +
                    "streams=${streams.size} codec=${g.videoCodecName} " +
                    "resolution=${g.imageWidth}x${g.imageHeight} fps=${g.frameRate}"
            }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to RTSP: $url" }
            cleanupGrabber()
            _status.value = RtspClientStatus.ERROR
            statusCallback?.invoke(RtspClientStatus.ERROR, e.message ?: "Connection failed")
            false
        }
    }

    suspend fun disconnect(handle: Long) = withContext(Dispatchers.IO) {
        stopFrameGrabber()
        cleanupGrabber()
        streams.clear()
        _status.value = RtspClientStatus.DISCONNECTED
        statusCallback?.invoke(RtspClientStatus.DISCONNECTED, "Disconnected")
        logger.info { "RTSP disconnected handle=$handle" }
    }

    fun getStatus(handle: Long): RtspClientStatus = _status.value

    suspend fun play(handle: Long): Boolean = withContext(Dispatchers.IO) {
        val g = grabber ?: return@withContext false
        if (_status.value != RtspClientStatus.CONNECTED) return@withContext false

        try {
            _status.value = RtspClientStatus.PLAYING
            statusCallback?.invoke(RtspClientStatus.PLAYING, "Playing")
            startFrameGrabber(g)
            logger.info { "RTSP play started handle=$handle" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to start playback" }
            _status.value = RtspClientStatus.ERROR
            statusCallback?.invoke(RtspClientStatus.ERROR, e.message)
            false
        }
    }

    suspend fun stop(handle: Long): Boolean = withContext(Dispatchers.IO) {
        stopFrameGrabber()
        if (_status.value == RtspClientStatus.PLAYING) {
            _status.value = RtspClientStatus.CONNECTED
            statusCallback?.invoke(RtspClientStatus.CONNECTED, "Stopped")
        }
        logger.info { "RTSP stop handle=$handle" }
        true
    }

    suspend fun pause(handle: Long): Boolean = withContext(Dispatchers.IO) {
        stopFrameGrabber()
        if (_status.value == RtspClientStatus.PLAYING) {
            _status.value = RtspClientStatus.CONNECTED
            statusCallback?.invoke(RtspClientStatus.CONNECTED, "Paused")
        }
        logger.info { "RTSP pause handle=$handle" }
        true
    }

    fun getStreamCount(handle: Long): Int = streams.size

    fun getStreamType(handle: Long, streamIndex: Int): RtspStreamType? =
        streams.getOrNull(streamIndex)?.type

    fun getStreamInfo(handle: Long, streamIndex: Int): RtspStreamInfo? =
        streams.getOrNull(streamIndex)

    fun setFrameCallback(
        handle: Long,
        streamType: RtspStreamType,
        callback: (RtspFrame) -> Unit
    ) {
        when (streamType) {
            RtspStreamType.VIDEO -> videoCallback = callback
            RtspStreamType.AUDIO -> audioCallback = callback
            RtspStreamType.METADATA -> { /* not supported */ }
        }
    }

    fun setStatusCallback(
        handle: Long,
        callback: (RtspClientStatus, String?) -> Unit
    ) {
        statusCallback = callback
    }

    fun setReconnectParams(
        handle: Long,
        enabled: Boolean,
        maxRetries: Int,
        initialDelayMs: Int,
        maxDelayMs: Int,
        backoffMultiplier: Float
    ) {
        config.reconnectEnabled = enabled
        config.reconnectMaxRetries = maxRetries
        config.reconnectInitialDelayMs = initialDelayMs
        config.reconnectMaxDelayMs = maxDelayMs
        config.reconnectBackoffMultiplier = backoffMultiplier
    }

    fun destroy(handle: Long) {
        runBlocking {
            if (grabber != null) disconnect(handle)
        }
        handlesDestroyed.incrementAndGet()
        logger.info { "JvmRtspClient handle destroyed: $handle" }
    }

    // =========================================================================
    // Private
    // =========================================================================

    private fun refreshStreams(g: FFmpegFrameGrabber) {
        streams.clear()
        var index = 0
        JvmRtspFrameConverter.getVideoStreamInfo(g)?.let {
            streams.add(it.copy(index = index++))
        }
        JvmRtspFrameConverter.getAudioStreamInfo(g)?.let {
            streams.add(it.copy(index = index++))
        }
    }

    private fun startFrameGrabber(g: FFmpegFrameGrabber) {
        stopFrameGrabber()
        connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        frameGrabberJob = connectionScope?.launch {
            var frameIndex = 0L
            val frameDelayMs = if (g.frameRate > 0) {
                (1000L / g.frameRate.toLong()).coerceAtLeast(16L)
            } else {
                40L
            }

            while (isActive && _status.value == RtspClientStatus.PLAYING) {
                try {
                    val frame = g.grab()
                    if (frame == null) {
                        if (!g.hasVideo() && !g.hasAudio()) break
                        delay(10)
                        continue
                    }

                    val now = System.currentTimeMillis()

                    when {
                        frame.image != null && frame.imageWidth > 0 ->
                            processVideoFrame(frame, now)
                        frame.samples != null ->
                            processAudioFrame(frame, now)
                    }

                    frameIndex++
                    if (frameDelayMs > 10) delay(frameDelayMs)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Frame grab error at frame $frameIndex" }
                    delay(100)
                }
            }
        }
    }

    private fun processVideoFrame(frame: Frame, timestamp: Long) {
        JvmRtspFrameConverter.convert(frame, timestamp, 0)?.let { rtspFrame ->
            videoCallback?.invoke(rtspFrame)
        }
    }

    private fun processAudioFrame(frame: Frame, timestamp: Long) {
        JvmRtspFrameConverter.convert(frame, timestamp, 1)?.let { rtspFrame ->
            audioCallback?.invoke(rtspFrame)
        }
    }

    private fun stopFrameGrabber() {
        frameGrabberJob?.cancel()
        frameGrabberJob = null
        connectionScope?.cancel()
        connectionScope = null
    }

    private fun cleanupGrabber() {
        grabber?.let { g ->
            try { g.stop(); g.release() } catch (_: Exception) { }
        }
        grabber = null
    }
}

/**
 * Конфигурация JVM RTSP клиента.
 */
data class JvmRtspConfig(
    val rtspTransport: String = "tcp",
    val bufferSize: Int = 2_097_152,
    val maxDelayMs: Int = 500_000,
    val reorderQueueSize: Int = 5,
    val analyzeDurationMs: Int = 3_000_000,
    val probeSize: Int = 5_242_880,
    val ffmpegLogLevel: String = "error",
    val forceTcp: Boolean = true,
    val preferredWidth: Int = 0,
    val preferredHeight: Int = 0,
    val enableAudio: Boolean = true,
    val preferredSampleRate: Int = 44100,
    var reconnectEnabled: Boolean = true,
    var reconnectMaxRetries: Int = 5,
    var reconnectInitialDelayMs: Int = 500,
    var reconnectMaxDelayMs: Int = 10_000,
    var reconnectBackoffMultiplier: Float = 2.0f
)
