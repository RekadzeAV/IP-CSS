package com.company.ipcamera.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.video.VideoDecoder
import com.company.ipcamera.core.network.video.VideoCodec
import com.company.ipcamera.core.network.video.toVideoCodec
import com.company.ipcamera.core.network.video.toBufferedImage
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

@Composable
fun VideoPlayer(
    camera: Camera,
    modifier: Modifier = Modifier,
    onStatusChange: (RtspClientStatus) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var rtspClient by remember { mutableStateOf<RtspClient?>(null) }
    var videoDecoder by remember { mutableStateOf<VideoDecoder?>(null) }
    var currentFrame by remember { mutableStateOf<BufferedImage?>(null) }
    var status by remember { mutableStateOf<RtspClientStatus>(RtspClientStatus.DISCONNECTED) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detectedCodec by remember { mutableStateOf<VideoCodec?>(null) }

    // Создание RTSP клиента
    LaunchedEffect(camera.id) {
        val config = RtspClientConfig(
            url = camera.url,
            username = camera.username,
            password = camera.password,
            enableVideo = true,
            enableAudio = false,
            timeoutMillis = 10000
        )

        val client = RtspClient(config)
        rtspClient = client

        // Получение информации о потоках для определения кодека
        coroutineScope.launch {
            try {
                // Подключаемся и ждем получения информации о потоках
                client.connect()
                kotlinx.coroutines.delay(1000) // Даем время на подключение

                // Получение информации о видеопотоке
                val streams = client.getStreams()
                val videoStream = streams.firstOrNull { it.type == com.company.ipcamera.core.network.RtspStreamType.VIDEO }
                videoStream?.let { stream ->
                    val codecFromStream = stream.codec.toVideoCodec()
                    if (codecFromStream != VideoCodec.UNKNOWN) {
                        detectedCodec = codecFromStream
                        logger.info { "Detected codec from stream: $codecFromStream" }
                    }
                }
            } catch (e: Exception) {
                logger.debug(e) { "Could not get stream info, will detect from frames" }
            }
        }

        // Подписка на статус
        coroutineScope.launch {
            client.getStatus().collect { newStatus ->
                status = newStatus
                onStatusChange(newStatus)

                when (newStatus) {
                    RtspClientStatus.ERROR -> {
                        errorMessage = "Ошибка подключения к камере"
                        onError(errorMessage!!)
                    }
                    else -> errorMessage = null
                }
            }
        }

        // Подписка на видеокадры
        coroutineScope.launch {
            client.getVideoFrames().collect { frame ->
                try {
                    if (frame.data.isEmpty()) return@collect

                    // Определение кодека из информации о потоке или камере
                    val codec = if (detectedCodec != null) {
                        detectedCodec
                    } else {
                        // Попытка определить кодек из информации о камере
                        val cameraCodec = camera.codec.toVideoCodec()
                        if (cameraCodec != VideoCodec.UNKNOWN) {
                            detectedCodec = cameraCodec
                            cameraCodec
                        } else {
                            // Попытка определить по данным кадра (MJPEG обычно начинается с FF D8)
                            if (frame.data.size >= 2 && frame.data[0] == 0xFF.toByte() && frame.data[1] == 0xD8.toByte()) {
                                VideoCodec.MJPEG
                            } else {
                                // По умолчанию пробуем H.264
                                VideoCodec.H264
                            }
                        }
                    }

                    // Создание декодера при первом кадре или при смене кодека
                    val decoder = videoDecoder
                    if (decoder == null || detectedCodec != codec) {
                        videoDecoder?.release()
                        detectedCodec = codec

                        val width = frame.width.takeIf { it > 0 } ?: camera.resolution?.width ?: 1920
                        val height = frame.height.takeIf { it > 0 } ?: camera.resolution?.height ?: 1080

                        val newDecoder = try {
                            VideoDecoder(codec, width, height).apply {
                                setCallback { decodedFrame ->
                                    // Конвертация декодированного кадра в BufferedImage
                                    val image = decodedFrame.toBufferedImage()
                                    image?.let { currentFrame = it }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to create video decoder for codec: $codec" }
                            null
                        }
                        videoDecoder = newDecoder
                    }

                    // Декодирование кадра
                    val decoderToUse = videoDecoder
                    if (decoderToUse != null) {
                        val decoded = decoderToUse.decode(frame)
                        if (!decoded && codec == VideoCodec.MJPEG) {
                            // Fallback для MJPEG через ImageIO
                            try {
                                val image = ImageIO.read(ByteArrayInputStream(frame.data))
                                image?.let { currentFrame = it }
                            } catch (e: Exception) {
                                logger.debug(e) { "Failed to decode MJPEG via ImageIO" }
                            }
                        }
                    } else {
                        // Fallback: попытка декодировать как MJPEG
                        try {
                            val image = ImageIO.read(ByteArrayInputStream(frame.data))
                            image?.let { currentFrame = it }
                        } catch (e: Exception) {
                            logger.debug(e) { "Failed to decode frame" }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error processing video frame" }
                }
            }
        }

        // Подключение и воспроизведение
        coroutineScope.launch {
            try {
                client.connect()
                client.play()
            } catch (e: Exception) {
                errorMessage = e.message ?: "Ошибка подключения"
                onError(errorMessage!!)
            }
        }
    }

    // Очистка при размонтировании
    DisposableEffect(Unit) {
        onDispose {
            coroutineScope.launch {
                videoDecoder?.release()
                rtspClient?.disconnect()
                rtspClient?.close()
            }
        }
    }

    // UI
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            RtspClientStatus.DISCONNECTED -> {
                VideoPlayerPlaceholder(
                    message = "Не подключено",
                    icon = Icons.Default.VideocamOff
                )
            }
            RtspClientStatus.CONNECTING -> {
                VideoPlayerPlaceholder(
                    message = "Подключение...",
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
                    // Отображение кадра
                    // Примечание: В реальной реализации нужно использовать Canvas или Image
                    // для отображения BufferedImage в Compose
                    VideoPlayerFrame(
                        image = currentFrame!!,
                        cameraName = camera.name
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
                    isError = true
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
    }
}

@Composable
private fun VideoPlayerPlaceholder(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isError: Boolean = false,
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
    }
}

@Composable
private fun VideoPlayerFrame(
    image: BufferedImage,
    cameraName: String,
    modifier: Modifier = Modifier
) {
    // Конвертация BufferedImage в ImageBitmap для отображения в Compose
    val imageBitmap = remember(image) {
        image.toComposeImageBitmap()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Видео: $cameraName",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun StatusIndicator(status: RtspClientStatus) {
    val color = when (status) {
        RtspClientStatus.DISCONNECTED -> Color.Gray
        RtspClientStatus.CONNECTING -> Color.Yellow
        RtspClientStatus.CONNECTED -> Color.Blue
        RtspClientStatus.PLAYING -> Color.Green
        RtspClientStatus.ERROR -> Color.Red
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, MaterialTheme.shapes.small)
    )
}

