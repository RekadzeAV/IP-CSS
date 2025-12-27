package com.company.ipcamera.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Composable
fun VideoPlayer(
    camera: Camera,
    modifier: Modifier = Modifier,
    onStatusChange: (RtspClientStatus) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var rtspClient by remember { mutableStateOf<RtspClient?>(null) }
    var currentFrame by remember { mutableStateOf<BufferedImage?>(null) }
    var status by remember { mutableStateOf<RtspClientStatus>(RtspClientStatus.DISCONNECTED) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
                    // Преобразование ByteArray в BufferedImage
                    // Примечание: В реальной реализации нужно декодировать H.264/H.265
                    // Здесь используется упрощенная версия для MJPEG
                    if (frame.data.isNotEmpty()) {
                        val image = try {
                            ImageIO.read(ByteArrayInputStream(frame.data))
                        } catch (e: Exception) {
                            null
                        }
                        image?.let { currentFrame = it }
                    }
                } catch (e: Exception) {
                    // Ошибка обработки кадра
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
    // Примечание: В реальной реализации нужно использовать Canvas или Image
    // для отображения BufferedImage в Compose Desktop
    // Здесь используется упрощенная версия
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Реализовать отображение BufferedImage в Compose
        // Можно использовать androidx.compose.ui.awt.SwingPanel или
        // конвертировать BufferedImage в Compose ImageBitmap
        Text(
            text = "Видео: $cameraName\n${image.width}x${image.height}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

