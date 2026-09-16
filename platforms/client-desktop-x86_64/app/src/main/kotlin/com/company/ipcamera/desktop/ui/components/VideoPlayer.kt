package com.company.ipcamera.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.SwingPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.video.VideoDecoder
import com.company.ipcamera.core.network.video.VideoCodec
import com.company.ipcamera.core.network.video.toBufferedImage
import com.company.ipcamera.desktop.media.toRgb24ByteArray
import com.company.ipcamera.desktop.stream.RtspStreamSession
import com.company.ipcamera.desktop.ui.viewmodel.StreamPriority
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.service.AnalyticsFrameProcessor
import com.company.ipcamera.shared.domain.service.AnalyticsFrameProcessorFactory
import mu.KotlinLogging
import org.koin.compose.koinInject
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JPanel
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.awt.Graphics
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

private fun envLong(name: String, default: Long): Long =
    System.getenv(name)?.toLongOrNull() ?: default

private val staggerNormalBaseMs =
    envLong("IPCSS_RTSP_STAGGER_NORMAL_BASE_MS", 120L).coerceIn(0L, 2_000L)
private val staggerNormalJitterMs =
    envLong("IPCSS_RTSP_STAGGER_NORMAL_JITTER_MS", 240L).coerceIn(0L, 3_000L)
private val staggerBackgroundBaseMs =
    envLong("IPCSS_RTSP_STAGGER_BACKGROUND_BASE_MS", 450L).coerceIn(0L, 5_000L)
private val staggerBackgroundJitterMs =
    envLong("IPCSS_RTSP_STAGGER_BACKGROUND_JITTER_MS", 500L).coerceIn(0L, 8_000L)

private fun calculateStaggeredConnectDelayMs(cameraId: String, priority: StreamPriority): Long {
    val positiveHash = cameraId.hashCode().toLong().let { if (it < 0) -it else it }
    val jitter = when (priority) {
        StreamPriority.HIGH -> 0L
        StreamPriority.NORMAL -> if (staggerNormalJitterMs == 0L) 0L else positiveHash % staggerNormalJitterMs
        StreamPriority.BACKGROUND ->
            if (staggerBackgroundJitterMs == 0L) 0L else positiveHash % staggerBackgroundJitterMs
    }
    val baseDelay = when (priority) {
        StreamPriority.HIGH -> 0L
        StreamPriority.NORMAL -> staggerNormalBaseMs
        StreamPriority.BACKGROUND -> staggerBackgroundBaseMs
    }
    return baseDelay + jitter
}

data class VideoPlaybackMetrics(
    val startupTimeMs: Long? = null,
    val reconnectAttempts: Int = 0,
    val streamErrorCount: Int = 0,
    val lastErrorAtMs: Long? = null,
    val renderFps: Int = 0,
    val droppedFrames: Int = 0
)

@Composable
fun VideoPlayer(
    camera: Camera,
    modifier: Modifier = Modifier,
    streamPriority: StreamPriority = StreamPriority.NORMAL,
    autoPlay: Boolean = true,
    showControls: Boolean = true,
    onStatusChange: (RtspClientStatus) -> Unit = {},
    onError: (String) -> Unit = {},
    onMetricsUpdate: (VideoPlaybackMetrics) -> Unit = {},
    /** Локальная кадровая аналитика (motion/object/face/ANPR по настройкам камеры). Включается, например, через `IPCSS_LOCAL_FRAME_ANALYTICS=true`. */
    localFrameAnalyticsEnabled: Boolean = false,
    /** Минимальный интервал между вызовами аналитики на декодированном кадре (мс). */
    localFrameAnalyticsIntervalMs: Long = 1000L
) {
    val coroutineScope = rememberCoroutineScope()
    val analyticsFactory = koinInject<AnalyticsFrameProcessorFactory>()
    val analyticsProcessorRef = remember { AtomicReference<AnalyticsFrameProcessor?>(null) }
    val lastLocalAnalyticsAtMs = remember { AtomicLong(0L) }

    LaunchedEffect(camera.id) {
        val proc = analyticsFactory.create(camera)
        analyticsProcessorRef.getAndSet(proc)?.dispose()
        try {
            awaitCancellation()
        } finally {
            analyticsProcessorRef.compareAndSet(proc, null)
            proc.dispose()
        }
    }

    val feedLocalAnalytics: (BufferedImage) -> Unit = remember(
        localFrameAnalyticsEnabled,
        localFrameAnalyticsIntervalMs,
        camera,
        coroutineScope
    ) {
        { image: BufferedImage ->
            tryFeedLocalFrameAnalytics(
                image = image,
                camera = camera,
                enabled = localFrameAnalyticsEnabled,
                intervalMs = localFrameAnalyticsIntervalMs,
                lastAtMs = lastLocalAnalyticsAtMs,
                processorRef = analyticsProcessorRef,
                scope = coroutineScope
            )
        }
    }
    var rtspSession by remember { mutableStateOf<RtspStreamSession?>(null) }
    var currentFrame by remember { mutableStateOf<BufferedImage?>(null) }
    var status by remember { mutableStateOf<RtspClientStatus>(RtspClientStatus.DISCONNECTED) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var userPaused by remember { mutableStateOf(false) }
    var pausedByPriority by remember { mutableStateOf(false) }
    var showControlsOverlay by remember { mutableStateOf(false) }
    var retryCount by remember { mutableStateOf(0) }
    val maxRetries = 3
    var startupStartedAtMs by remember { mutableStateOf<Long?>(null) }
    var startupTimeMs by remember { mutableStateOf<Long?>(null) }
    var reconnectAttempts by remember { mutableStateOf(0) }
    var streamErrorCount by remember { mutableStateOf(0) }
    var lastErrorAtMs by remember { mutableStateOf<Long?>(null) }
    var renderFps by remember { mutableStateOf(0) }
    var droppedFrames by remember { mutableStateOf(0) }
    var renderedInWindow by remember { mutableStateOf(0) }
    var renderWindowStartedAtMs by remember { mutableStateOf(System.currentTimeMillis()) }

    // Видео декодер
    var videoDecoder by remember { mutableStateOf<VideoDecoder?>(null) }
    var codec by remember { mutableStateOf<VideoCodec?>(null) }
    var frameWidth by remember { mutableStateOf(1920) }
    var frameHeight by remember { mutableStateOf(1080) }

    // Оптимизация рендеринга - frame skipping
    var lastFrameTime by remember { mutableStateOf(0L) }
    val targetFps = when (streamPriority) {
        StreamPriority.HIGH -> 30
        StreamPriority.NORMAL -> 20
        StreamPriority.BACKGROUND -> 8
    }
    val frameInterval = 1000L / targetFps // Интервал между кадрами в миллисекундах

    fun markRenderedFrame(nowMs: Long = System.currentTimeMillis()) {
        renderedInWindow += 1
        val elapsedMs = nowMs - renderWindowStartedAtMs
        if (elapsedMs >= 1000L) {
            renderFps = ((renderedInWindow * 1000L) / elapsedMs).toInt()
            renderedInWindow = 0
            renderWindowStartedAtMs = nowMs
        }
    }

    LaunchedEffect(startupTimeMs, reconnectAttempts, streamErrorCount, lastErrorAtMs, renderFps, droppedFrames) {
        onMetricsUpdate(
            VideoPlaybackMetrics(
                startupTimeMs = startupTimeMs,
                reconnectAttempts = reconnectAttempts,
                streamErrorCount = streamErrorCount,
                lastErrorAtMs = lastErrorAtMs,
                renderFps = renderFps,
                droppedFrames = droppedFrames
            )
        )
    }

    // Создание RTSP сессии
    LaunchedEffect(camera.id) {
        rtspSession?.close()
        val session = RtspStreamSession(camera)
        rtspSession = session

        // Подписка на ошибки потока (таймаут, обрыв, неверный кодек) для UI.
        coroutineScope.launch {
            session.streamError.collect { err ->
                errorMessage = err
                if (err != null) onError(err)
            }
        }

        // Подписка на статус
        coroutineScope.launch {
            session.status.collect { newStatus ->
                status = newStatus
                onStatusChange(newStatus)

                // Синхронизация состояния воспроизведения
                when (newStatus) {
                    RtspClientStatus.PLAYING -> {
                        if (startupTimeMs == null && startupStartedAtMs != null) {
                            startupTimeMs = System.currentTimeMillis() - startupStartedAtMs!!
                        }
                        isPlaying = true
                        isPaused = false
                        errorMessage = null
                    }
                    RtspClientStatus.CONNECTED -> {
                        if (!isPaused) {
                            isPlaying = false
                        }
                        errorMessage = null
                    }
                    RtspClientStatus.ERROR -> {
                        streamErrorCount++
                        lastErrorAtMs = System.currentTimeMillis()
                        isPlaying = false
                        isPaused = false
                        // Текст ошибки берётся из getStreamError() (таймаут, обрыв, кодек)
                        if (errorMessage == null) errorMessage = "Ошибка подключения к камере"
                        onError(errorMessage!!)
                    }
                    RtspClientStatus.DISCONNECTED -> {
                        isPlaying = false
                        isPaused = false
                    }
                    else -> {
                        errorMessage = null
                    }
                }
            }
        }

        // Подписка на видеокадры с оптимизацией (frame skipping для высокой частоты кадров)
        coroutineScope.launch {
            session.videoFrames.collect { frame ->
                try {
                    // Пропускаем кадры, если они приходят слишком часто
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastFrameTime < frameInterval) {
                        droppedFrames++
                        return@collect // Пропускаем этот кадр
                    }
                    lastFrameTime = currentTime
                    // Определение кодека при первом кадре
                    if (codec == null) {
                        codec = determineCodecFromFrame(frame)
                        frameWidth = frame.width
                        frameHeight = frame.height

                        // Создание декодера для H.264/H.265
                        codec?.let { detectedCodec ->
                            if (detectedCodec == VideoCodec.H264 || detectedCodec == VideoCodec.H265) {
                                try {
                                    videoDecoder = VideoDecoder(
                                        detectedCodec,
                                        frameWidth,
                                        frameHeight
                                    )

                                    // Установка callback для декодированных кадров
                                    videoDecoder?.setCallback { decodedFrame ->
                                        try {
                                            val image = decodedFrame.toBufferedImage()
                                            if (image != null) {
                                                currentFrame = image
                                                markRenderedFrame()
                                                feedLocalAnalytics(image)

                                                // Обновляем размеры если они изменились
                                                if (decodedFrame.width != frameWidth || decodedFrame.height != frameHeight) {
                                                    frameWidth = decodedFrame.width
                                                    frameHeight = decodedFrame.height
                                                    logger.info {
                                                        "Frame resolution updated to ${decodedFrame.width}x${decodedFrame.height}"
                                                    }
                                                }
                                            } else {
                                                logger.warn { "Failed to convert decoded frame to BufferedImage" }
                                            }
                                        } catch (e: Exception) {
                                            logger.error(e) { "Error in decoded frame callback" }
                                        }
                                    }

                                    logger.info { "VideoDecoder created for $detectedCodec, size: ${frameWidth}x${frameHeight}" }
                                } catch (e: Exception) {
                                    logger.error(e) { "Failed to create VideoDecoder: ${e.message}" }
                                    // Fallback на MJPEG
                                    logger.info { "Falling back to MJPEG decoding" }
                                    codec = VideoCodec.MJPEG
                                    videoDecoder?.release()
                                    videoDecoder = null
                                }
                            }
                        }
                    }

                    // Декодирование кадра
                    when (codec) {
                        VideoCodec.MJPEG -> {
                            // MJPEG через ImageIO
                            val image = try {
                                ImageIO.read(ByteArrayInputStream(frame.data))
                            } catch (e: Exception) {
                                logger.warn(e) { "Failed to decode MJPEG frame" }
                                null
                            }
                            image?.let {
                                currentFrame = it
                                markRenderedFrame()
                                feedLocalAnalytics(it)
                            }
                        }
                        VideoCodec.H264, VideoCodec.H265 -> {
                            // H.264/H.265 через декодер
                            try {
                                val decodeSuccess = videoDecoder?.decode(frame) ?: false
                                if (!decodeSuccess) {
                                    // Если декодирование не удалось, пробуем fallback на MJPEG
                                    logger.debug { "H.264/H.265 decoding failed, trying MJPEG fallback" }
                                    val image = try {
                                        ImageIO.read(ByteArrayInputStream(frame.data))
                                    } catch (e: Exception) {
                                        null
                                    }
                                    image?.let {
                                        currentFrame = it
                                        markRenderedFrame()
                                        feedLocalAnalytics(it)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error(e) { "Error during H.264/H.265 decoding, trying MJPEG fallback" }
                                // Fallback на MJPEG при ошибке
                                val image = try {
                                    ImageIO.read(ByteArrayInputStream(frame.data))
                                } catch (e2: Exception) {
                                    null
                                }
                                image?.let {
                                    currentFrame = it
                                    markRenderedFrame()
                                    feedLocalAnalytics(it)
                                }
                            }
                        }
                        else -> {
                            // Fallback на MJPEG
                            val image = try {
                                ImageIO.read(ByteArrayInputStream(frame.data))
                            } catch (e: Exception) {
                                logger.warn(e) { "Failed to decode frame as MJPEG" }
                                null
                            }
                            image?.let {
                                currentFrame = it
                                markRenderedFrame()
                                feedLocalAnalytics(it)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error processing frame" }
                }
            }
        }

        // Подключение и воспроизведение
        coroutineScope.launch {
            try {
                val startDelayMs = calculateStaggeredConnectDelayMs(camera.id, streamPriority)
                if (startDelayMs > 0L) delay(startDelayMs)
                if (startupStartedAtMs == null) startupStartedAtMs = System.currentTimeMillis()
                session.start(autoPlay = autoPlay && !isPaused)
                isPlaying = autoPlay && !isPaused
            } catch (e: Exception) {
                logger.error(e) { "Error connecting to RTSP stream" }
                errorMessage = e.message ?: "Ошибка подключения"
                streamErrorCount++
                lastErrorAtMs = System.currentTimeMillis()
                onError(errorMessage!!)

                // Автоматическое переподключение
                if (retryCount < maxRetries) {
                    retryCount++
                    reconnectAttempts++
                    delay(3000L * retryCount) // Экспоненциальная задержка
                    try {
                        session.reconnect(autoPlay = autoPlay)
                        isPlaying = autoPlay
                        retryCount = 0
                    } catch (retryError: Exception) {
                        logger.error(retryError) { "Retry connection failed" }
                    }
                }
            }
        }
    }

    // Очистка при размонтировании
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                videoDecoder?.release()
                rtspSession?.close()
            }
        }
    }

    // Background камеры автоматически ставим на паузу, чтобы снизить CPU/decoder нагрузку.
    LaunchedEffect(streamPriority, rtspSession, status, userPaused) {
        val session = rtspSession ?: return@LaunchedEffect
        when (streamPriority) {
            StreamPriority.BACKGROUND -> {
                if (status == RtspClientStatus.PLAYING) {
                    try {
                        session.pause()
                        pausedByPriority = true
                        isPlaying = false
                        isPaused = true
                    } catch (_: Exception) {
                        // noop: next status tick will reconcile UI state
                    }
                }
            }
            StreamPriority.HIGH, StreamPriority.NORMAL -> {
                if (pausedByPriority && !userPaused) {
                    try {
                        session.play()
                        pausedByPriority = false
                        isPlaying = true
                        isPaused = false
                    } catch (_: Exception) {
                        // noop: keep state and retry on next priority/status change
                    }
                }
            }
        }
    }

    // Обработчики управления
    val handlePlay: () -> Unit = {
        coroutineScope.launch {
            try {
                rtspSession?.play()
                if (startupStartedAtMs == null) startupStartedAtMs = System.currentTimeMillis()
                isPlaying = true
                isPaused = false
                userPaused = false
                pausedByPriority = false
            } catch (e: Exception) {
                logger.error(e) { "Error playing stream" }
                errorMessage = "Ошибка воспроизведения"
                streamErrorCount++
                lastErrorAtMs = System.currentTimeMillis()
            }
        }
    }

    val handlePause: () -> Unit = {
        coroutineScope.launch {
            try {
                rtspSession?.pause()
                isPlaying = false
                isPaused = true
                userPaused = true
            } catch (e: Exception) {
                logger.error(e) { "Error pausing stream" }
            }
        }
    }

    val handleStop: () -> Unit = {
        coroutineScope.launch {
            try {
                rtspSession?.stop()
                isPlaying = false
                isPaused = false
                userPaused = false
                pausedByPriority = false
                currentFrame = null
            } catch (e: Exception) {
                logger.error(e) { "Error stopping stream" }
            }
        }
    }

    val handleReconnect: () -> Unit = {
        coroutineScope.launch {
            retryCount = 0
            errorMessage = null
            reconnectAttempts++
            try {
                if (startupStartedAtMs == null) startupStartedAtMs = System.currentTimeMillis()
                rtspSession?.reconnect(autoPlay = autoPlay)
                isPlaying = autoPlay
                userPaused = false
                pausedByPriority = false
            } catch (e: Exception) {
                logger.error(e) { "Error reconnecting" }
                errorMessage = "Ошибка переподключения: ${e.message}"
                streamErrorCount++
                lastErrorAtMs = System.currentTimeMillis()
            }
        }
    }

    val handleScreenshot: () -> Unit = {
        val frame = currentFrame
        if (frame != null) {
            // JFileChooser должен выполняться в EDT (Event Dispatch Thread)
            javax.swing.SwingUtilities.invokeLater {
                try {
                    val fileChooser = JFileChooser()
                    fileChooser.fileFilter = FileNameExtensionFilter("PNG Images", "png")
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                    val fileName = "${camera.name}_${dateFormat.format(Date())}.png"
                    fileChooser.selectedFile = File(fileName)

                    val result = fileChooser.showSaveDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val file = fileChooser.selectedFile
                        ImageIO.write(frame, "png", file)
                        logger.info { "Screenshot saved to ${file.absolutePath}" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error saving screenshot" }
                    coroutineScope.launch {
                        errorMessage = "Ошибка сохранения снимка: ${e.message}"
                    }
                }
            }
        }
    }

    // UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable { showControlsOverlay = !showControlsOverlay },
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            RtspClientStatus.DISCONNECTED -> {
                VideoPlayerPlaceholder(
                    message = errorMessage ?: "Не подключено (офлайн)",
                    icon = Icons.Default.VideocamOff,
                    isError = errorMessage != null
                )
            }
            RtspClientStatus.CONNECTING -> {
                VideoPlayerPlaceholder(
                    message = if (retryCount > 0) "Переподключение... (попытка $retryCount/$maxRetries)" else "Подключение...",
                    icon = Icons.Default.Sync
                )
            }
            RtspClientStatus.CONNECTED -> {
                VideoPlayerPlaceholder(
                    message = "Подключено, ожидание потока...",
                    icon = Icons.Default.PlayArrow
                )
            }
            RtspClientStatus.PLAYING -> {
                if (currentFrame != null) {
                    // Отображение кадра через SwingPanel для BufferedImage
                    VideoPlayerFrame(
                        image = currentFrame!!,
                        cameraName = camera.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VideoPlayerPlaceholder(
                        message = "Ожидание кадров...",
                        icon = Icons.Default.VideoLibrary
                    )
                }
            }
            RtspClientStatus.ERROR -> {
                VideoPlayerPlaceholder(
                    message = errorMessage ?: "Ошибка",
                    icon = Icons.Default.Error,
                    isError = true,
                    onRetry = handleReconnect
                )
            }
            else -> {
                VideoPlayerPlaceholder(
                    message = "Неизвестный статус",
                    icon = Icons.Default.Help
                )
            }
        }

        // Индикатор статуса
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusIndicator(status = status)
                Text(
                    text = status.name,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Минимальная телеметрия стабильности воспроизведения (Phase 3.2.1)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            shape = MaterialTheme.shapes.small
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = "Startup: ${startupTimeMs?.let { "${it}ms" } ?: "n/a"}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Reconnects: $reconnectAttempts | Errors: $streamErrorCount",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Элементы управления (overlay)
        if (showControls && (showControlsOverlay || status == RtspClientStatus.PLAYING)) {
            VideoPlayerControls(
                isPlaying = isPlaying,
                isPaused = isPaused,
                canScreenshot = currentFrame != null,
                onPlay = handlePlay,
                onPause = handlePause,
                onStop = handleStop,
                onReconnect = handleReconnect,
                onScreenshot = handleScreenshot,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun VideoPlayerPlaceholder(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isError: Boolean = false,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = message,
            modifier = Modifier.size(64.dp),
            tint = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (isError && onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = "Переподключиться")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Переподключиться")
            }
        }
    }
}

@Composable
private fun VideoPlayerFrame(
    image: BufferedImage,
    cameraName: String,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_PARAMETER")
    val _cameraName = cameraName
    // Избегаем промежуточных BufferedImage, чтобы снизить churn памяти при 9/16 потоках.
    var currentImage by remember { mutableStateOf(image) }

    LaunchedEffect(image) {
        currentImage = image
    }

    SwingPanel(
        modifier = modifier,
        factory = {
            object : JPanel() {
                init {
                    background = java.awt.Color.BLACK
                    // Включаем двойную буферизацию для плавного рендеринга
                    isDoubleBuffered = true
                }

                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    val img = currentImage
                    if (width > 0 && height > 0) {
                        val scaleX = width.toDouble() / img.width
                        val scaleY = height.toDouble() / img.height
                        val scale = minOf(scaleX, scaleY)
                        val scaledWidth = (img.width * scale).toInt()
                        val scaledHeight = (img.height * scale).toInt()
                        val x = (width - scaledWidth) / 2
                        val y = (height - scaledHeight) / 2

                        val g2d = g as java.awt.Graphics2D
                        g2d.setRenderingHint(
                            java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
                        )
                        g2d.drawImage(img, x, y, scaledWidth, scaledHeight, null)
                    }
                }

                override fun getPreferredSize(): Dimension {
                    val img = currentImage
                    return Dimension(img.width, img.height)
                }
            }
        },
        update = { panel ->
            // Оптимизированное обновление - только при изменении размера
            panel.revalidate()
            panel.repaint()
        }
    )
}

@Composable
private fun StatusIndicator(status: RtspClientStatus) {
    val color = when (status) {
        RtspClientStatus.DISCONNECTED -> Color.Gray
        RtspClientStatus.CONNECTING -> Color.Yellow
        RtspClientStatus.CONNECTED -> Color.Blue
        RtspClientStatus.PLAYING -> Color.Green
        RtspClientStatus.ERROR -> Color.Red
        else -> Color.Gray
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, MaterialTheme.shapes.small)
    )
}

@Composable
private fun VideoPlayerControls(
    isPlaying: Boolean,
    isPaused: Boolean,
    canScreenshot: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onReconnect: () -> Unit,
    onScreenshot: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause
            IconButton(
                onClick = if (isPlaying) onPause else onPlay,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Воспроизведение",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Stop
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Остановить",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Screenshot
            IconButton(
                onClick = onScreenshot,
                enabled = canScreenshot,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Снимок экрана",
                    modifier = Modifier.size(32.dp),
                    tint = if (canScreenshot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Reconnect
            IconButton(
                onClick = onReconnect,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Переподключиться",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun tryFeedLocalFrameAnalytics(
    image: BufferedImage,
    camera: Camera,
    enabled: Boolean,
    intervalMs: Long,
    lastAtMs: AtomicLong,
    processorRef: AtomicReference<AnalyticsFrameProcessor?>,
    scope: CoroutineScope
) {
    if (!enabled) return
    val a = camera.settings.analytics
    if (!a.motionDetection && !a.objectDetection && !a.faceRecognition && !a.anprEnabled) return
    val proc = processorRef.get() ?: return
    val now = System.currentTimeMillis()
    while (true) {
        val prev = lastAtMs.get()
        if (now - prev < intervalMs) return
        if (lastAtMs.compareAndSet(prev, now)) break
    }
    val w = image.width
    val h = image.height
    val rgb = image.toRgb24ByteArray()
    scope.launch(Dispatchers.Default) {
        if (processorRef.get() === proc) {
            proc.processFrame(rgb, w, h)
        }
    }
}

/**
 * Определение кодека из данных кадра
 */
private fun determineCodecFromFrame(frame: RtspFrame): VideoCodec {
    if (frame.data.size < 4) return VideoCodec.UNKNOWN

    // Проверка на H.264 start code (0x00 0x00 0x00 0x01 или 0x00 0x00 0x01)
    if (frame.data[0] == 0x00.toByte() && frame.data[1] == 0x00.toByte()) {
        val startCodeLength = if (frame.data.size > 3 &&
            frame.data[2] == 0x00.toByte() && frame.data[3] == 0x01.toByte()) {
            4
        } else if (frame.data.size > 2 && frame.data[2] == 0x01.toByte()) {
            3
        } else {
            // Проверка на JPEG/MJPEG
            if (frame.data[0] == 0xFF.toByte() && frame.data[1] == 0xD8.toByte()) {
                return VideoCodec.MJPEG
            }
            return VideoCodec.UNKNOWN
        }

        // Извлечение NAL unit type
        if (frame.data.size > startCodeLength) {
            val nalHeader = frame.data[startCodeLength].toInt()
            val nalType = nalHeader and 0x1F

            // H.264 NAL types: 1-23
            if (nalType in 1..23) {
                return VideoCodec.H264
            }

            // H.265 NAL types: проверяем через старшие биты
            val nalTypeH265 = (nalHeader shr 1) and 0x3F
            if (nalTypeH265 in 0..47) {
                return VideoCodec.H265
            }
        }
    }

    // Проверка на JPEG/MJPEG (0xFF 0xD8)
    if (frame.data[0] == 0xFF.toByte() && frame.data[1] == 0xD8.toByte()) {
        return VideoCodec.MJPEG
    }
    return VideoCodec.UNKNOWN
}
