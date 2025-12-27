package com.company.ipcamera.desktop.ui.screens.recordings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(recordingId) {
        isLoading = true
        error = null
        try {
            recording = recordingRepository.getRecordingById(recordingId)
            recording?.let { rec ->
                // Получаем URL для воспроизведения (HLS)
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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text(
                text = recording?.cameraName ?: "Воспроизведение записи",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Divider()

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
                RecordingPlayerContent(
                    recording = recording!!,
                    playbackUrl = playbackUrl!!,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    onPlayPause = { isPlaying = !isPlaying },
                    onSeek = { position -> currentPosition = position }
                )
            }
            recording != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "URL для воспроизведения недоступен",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingPlayerContent(
    recording: Recording,
    playbackUrl: String,
    isPlaying: Boolean,
    currentPosition: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Видео плеер
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // TODO: Интеграция с HLS плеером
                // Можно использовать VLC или другой видеоплеер
                Text(
                    text = "Видео плеер\n$playbackUrl",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Информация о записи
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Информация о записи",
                    style = MaterialTheme.typography.titleLarge
                )
                Divider()

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

        // Управление воспроизведением
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Управление",
                    style = MaterialTheme.typography.titleLarge
                )
                Divider()

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
                }

                // Временная шкала
                Column {
                    Text(
                        text = "Позиция: ${formatDuration(currentPosition)} / ${formatDuration(recording.duration)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..recording.duration.toFloat(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
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
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        "${localDateTime.date} ${localDateTime.time}"
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

