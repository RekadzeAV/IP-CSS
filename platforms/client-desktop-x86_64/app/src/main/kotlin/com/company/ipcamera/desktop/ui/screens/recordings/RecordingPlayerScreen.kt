package com.company.ipcamera.desktop.ui.screens.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.components.VideoPlayer
import com.company.ipcamera.desktop.ui.viewmodel.StreamPriority
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingPlayerScreen(
    recordingId: String,
    onBackClick: () -> Unit = {},
    recordingRepository: RecordingRepository = koinInject()
) {
    var recording by remember { mutableStateOf<Recording?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var playbackUrl by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf<String?>(null) }
    var fullScreen by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(recordingId) {
        isLoading = true
        error = null
        try {
            recording = recordingRepository.getRecordingById(recordingId)
            recording?.let { rec ->
                recordingRepository.getDownloadUrl(recordingId).fold(
                    onSuccess = { url ->
                        playbackUrl = url
                    },
                    onFailure = { e ->
                        error = e.message ?: "Не удалось получить URL для воспроизведения"
                    }
                )
            }
            isLoading = false
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить запись"
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель управления
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recording?.cameraName ?: "Запись",
                            style = MaterialTheme.typography.titleMedium
                        )
                        recording?.let { rec ->
                            Text(
                                text = "${formatTimestamp(rec.startTime)} • ${formatDuration(rec.duration)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Скорость воспроизведения
                    SpeedSelector(
                        currentSpeed = playbackSpeed,
                        onSpeedChange = { playbackSpeed = it }
                    )

                    // Полноэкранный режим
                    IconButton(onClick = { fullScreen = !fullScreen }) {
                        Icon(
                            if (fullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (fullScreen) "Выйти из полноэкранного" else "Полноэкранный"
                        )
                    }

                    // Экспорт
                    IconButton(
                        onClick = { showExportDialog = true },
                        enabled = recording != null
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Экспорт")
                    }
                }
            }

            // Прогресс экспорта
            exportProgress?.let { progress ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = progress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Контент
            when {
                isLoading -> {
                    LoadingIndicator(message = "Загрузка записи...")
                }
                error != null -> {
                    ErrorView(
                        message = error!!,
                        onRetry = {
                            coroutineScope.launch {
                                isLoading = true
                                error = null
                                try {
                                    recording = recordingRepository.getRecordingById(recordingId)
                                    isLoading = false
                                } catch (e: Exception) {
                                    error = e.message ?: "Не удалось загрузить запись"
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
                recording != null && playbackUrl != null -> {
                    if (fullScreen) {
                        RecordingFullscreenPlayer(
                            recording = recording!!,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            onPlayPause = { isPlaying = !isPlaying },
                            onSeek = { position -> currentPosition = position },
                            onExitFullscreen = { fullScreen = false }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Видеоплеер
                            item {
                                RecordingVideoCard(
                                    recording = recording!!,
                                    playbackUrl = playbackUrl!!,
                                    isPlaying = isPlaying,
                                    currentPosition = currentPosition,
                                    onPlayPause = { isPlaying = !isPlaying },
                                    onSeek = { position -> currentPosition = position }
                                )
                            }

                            // Информация о записи
                            item {
                                RecordingInfoCard(recording = recording!!)
                            }

                            // Управление
                            item {
                                RecordingControlsCard(
                                    recording = recording!!,
                                    isPlaying = isPlaying,
                                    currentPosition = currentPosition,
                                    playbackSpeed = playbackSpeed,
                                    onPlayPause = { isPlaying = !isPlaying },
                                    onSeek = { position -> currentPosition = position },
                                    onSpeedChange = { playbackSpeed = it },
                                    onExport = { showExportDialog = true }
                                )
                            }
                        }
                    }
                }
                recording != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "URL для воспроизведения недоступен",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Возможно, запись ещё обрабатывается или была удалена",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Диалог экспорта
    if (showExportDialog) {
        ExportRecordingDialog(
            recording = recording!!,
            onDismiss = { showExportDialog = false },
            onExport = { format, quality, path ->
                showExportDialog = false
                exportProgress = "Экспорт в $format..."
                coroutineScope.launch {
                    recordingRepository.exportRecording(
                        recordingId,
                        format,
                        quality
                    ).fold(
                        onSuccess = {
                            exportProgress = null
                            snackbarHostState.showSnackbar("Экспорт завершён: $path")
                        },
                        onFailure = { e ->
                            exportProgress = null
                            snackbarHostState.showSnackbar("Ошибка экспорта: ${e.message}")
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun RecordingVideoCard(
    recording: Recording,
    playbackUrl: String,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Встраиваем видеоплеер
            val dummyCamera = Camera(
                id = recording.id,
                name = recording.cameraName ?: "Recording",
                url = playbackUrl,
                username = null,
                password = null
            )

            VideoPlayer(
                camera = dummyCamera,
                streamPriority = StreamPriority.HIGH,
                autoPlay = isPlaying,
                showControls = false,
                modifier = Modifier.fillMaxSize()
            )

            // Наложение управления внизу
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Кнопка play/pause
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Прогресс-бар
                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..recording.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.Gray
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = formatDuration(recording.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingInfoCard(recording: Recording) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Информация о записи",
                style = MaterialTheme.typography.titleLarge
            )
            HorizontalDivider()

            InfoRow("Камера", recording.cameraName ?: recording.cameraId)
            InfoRow("Время начала", formatTimestamp(recording.startTime))
            recording.endTime?.let {
                InfoRow("Время окончания", formatTimestamp(it))
            }
            InfoRow("Длительность", formatDuration(recording.duration))
            recording.fileSize?.let {
                InfoRow("Размер файла", recording.getFormattedFileSize())
            }
            InfoRow("Формат", recording.format.name)
            InfoRow("Качество", recording.quality.name)
        }
    }
}

@Composable
private fun RecordingControlsCard(
    recording: Recording,
    isPlaying: Boolean,
    currentPosition: Long,
    playbackSpeed: Float,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onExport: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Управление",
                style = MaterialTheme.typography.titleLarge
            )
            HorizontalDivider()

            // Кнопки управления
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayPause,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Воспроизведение"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "Пауза" else "Воспроизведение")
                }

                OutlinedButton(
                    onClick = { onSeek(0L) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "Сначала")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Сначала")
                }

                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Экспорт")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Экспорт")
                }
            }

            // Временная шкала
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = formatDuration(recording.duration),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Slider(
                    value = currentPosition.toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..recording.duration.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Скорость воспроизведения
            SpeedSelector(
                currentSpeed = playbackSpeed,
                onSpeedChange = onSpeedChange
            )
        }
    }
}

@Composable
private fun SpeedSelector(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Скорость:",
            style = MaterialTheme.typography.labelMedium
        )
        listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
            FilterChip(
                selected = currentSpeed == speed,
                onClick = { onSpeedChange(speed) },
                label = { Text("${speed}x") }
            )
        }
    }
}

@Composable
private fun RecordingFullscreenPlayer(
    recording: Recording,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onExitFullscreen: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Кнопка выхода из полноэкранного
        IconButton(
            onClick = onExitFullscreen,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.FullscreenExit,
                contentDescription = "Выйти из полноэкранного",
                tint = Color.White
            )
        }

        // Центральная кнопка play/pause
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Пауза" else "Воспроизвести",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(64.dp)
            )
        }

        // Прогресс-бар внизу
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..recording.duration.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.Gray
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = formatDuration(recording.duration),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ExportRecordingDialog(
    recording: Recording,
    onDismiss: () -> Unit,
    onExport: (format: String, quality: String, path: String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf("mp4") }
    var selectedQuality by remember { mutableStateOf("medium") }
    var customPath by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт записи") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Настройки экспорта для записи от ${formatTimestamp(recording.startTime)}",
                    style = MaterialTheme.typography.bodyMedium
                )

                // Формат
                Text("Формат:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("mp4", "mkv", "avi").forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.uppercase()) }
                        )
                    }
                }

                // Качество
                Text("Качество:", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "low" to "Низкое",
                        "medium" to "Среднее",
                        "high" to "Высокое"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = selectedQuality == value,
                            onClick = { selectedQuality = value },
                            label = { Text(label) }
                        )
                    }
                }

                // Путь сохранения
                OutlinedTextField(
                    value = customPath,
                    onValueChange = { customPath = it },
                    label = { Text("Путь сохранения (опционально)") },
                    placeholder = { Text("Оставьте пустым для сохранения в Downloads") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val path = customPath.ifBlank {
                        val home = System.getProperty("user.home")
                        "$home/Downloads/recording_${recording.id}.$selectedFormat"
                    }
                    onExport(selectedFormat, selectedQuality, path)
                }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Экспортировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "Неизвестно"
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format("%d:%02d", minutes, secs)
        else -> String.format("%d сек", secs)
    }
}