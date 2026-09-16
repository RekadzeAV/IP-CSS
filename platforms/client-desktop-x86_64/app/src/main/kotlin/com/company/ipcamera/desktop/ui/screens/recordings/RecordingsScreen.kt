package com.company.ipcamera.desktop.ui.screens.recordings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.viewmodel.RecordingsUiState
import com.company.ipcamera.desktop.ui.viewmodel.RecordingsViewModel
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingStatus
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordingsScreen(
    viewModel: RecordingsViewModel = koinInject(),
    onRecordingClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Recording?>(null) }
    var exportMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // message, isSuccess

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок с поиском и фильтрами
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Записи",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            // Поиск
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Поиск записей...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                },
                trailingIcon = {
                    if (viewModel.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.width(300.dp)
            )

            // Кнопка обновления
            IconButton(
                onClick = { viewModel.loadRecordings() },
                enabled = uiState !is RecordingsUiState.Loading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }

        Divider()

        // Контент
        when (uiState) {
            is RecordingsUiState.Loading -> {
                LoadingIndicator(message = "Загрузка записей...")
            }
            is RecordingsUiState.Error -> {
                val errorState = uiState as RecordingsUiState.Error
                ErrorView(
                    message = errorState.message,
                    onRetry = { viewModel.loadRecordings() }
                )
            }
            is RecordingsUiState.Success -> {
                val successState = uiState as RecordingsUiState.Success
                if (successState.recordings.isEmpty()) {
                    EmptyRecordingsView()
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Таблица записей
                        RecordingsTable(
                            recordings = viewModel.getFilteredRecordings(),
                            onRecordingClick = { recording ->
                                onRecordingClick(recording.id)
                            },
                            onDeleteClick = { recording ->
                                showDeleteDialog = recording
                            },
                            onExportClick = { recording ->
                                viewModel.exportRecording(
                                    recordingId = recording.id,
                                    format = "mp4",
                                    quality = "medium",
                                    onSuccess = { url -> exportMessage = "Экспорт выполнен: $url" to true },
                                    onError = { msg -> exportMessage = msg to false }
                                )
                            }
                        )

                        // Пагинация
                        PaginationControls(
                            page = successState.page,
                            total = successState.total,
                            hasMore = successState.hasMore,
                            onPreviousPage = { viewModel.loadPreviousPage() },
                            onNextPage = { viewModel.loadNextPage() }
                        )
                    }
                }
            }
        }
    }

    // Результат экспорта (оверлей внизу экрана)
    exportMessage?.let { (message, success) ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (success) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { exportMessage = null }) { Text("OK") }
                }
            }
        }
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(5000)
            exportMessage = null
        }
    }
    } // Box

    // Диалог подтверждения удаления
    showDeleteDialog?.let { recording ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить запись?") },
            text = {
                Text("Вы уверены, что хотите удалить запись от ${formatDate(recording.startTime)}? Это действие нельзя отменить.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(
                            recordingId = recording.id,
                            onSuccess = { showDeleteDialog = null },
                            onError = { showDeleteDialog = null }
                        )
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun RecordingsTable(
    recordings: List<Recording>,
    onRecordingClick: (Recording) -> Unit = {},
    onDeleteClick: (Recording) -> Unit,
    onExportClick: (Recording) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Заголовок таблицы
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Камера", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
                    Text("Начало", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
                    Text("Длительность", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.5f))
                    Text("Размер", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("Статус", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("Формат", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("Действия", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()
            }

            items(recordings) { recording ->
                RecordingRow(
                    recording = recording,
                    onRecordingClick = { onRecordingClick(recording) },
                    onDeleteClick = { onDeleteClick(recording) },
                    onExportClick = { onExportClick(recording) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecordingRow(
    recording: Recording,
    onRecordingClick: () -> Unit = {},
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = recording.cameraName ?: recording.cameraId,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = formatDate(recording.startTime),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f)
        )

        Text(
            text = formatDuration(recording.duration),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.5f)
        )

        Text(
            text = recording.getFormattedFileSize(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        StatusChip(status = recording.status, modifier = Modifier.weight(1f))

        Text(
            text = recording.format.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onRecordingClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Воспроизвести",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onExportClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Экспорт",
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: RecordingStatus, modifier: Modifier = Modifier) {
    val (color, text) = when (status) {
        RecordingStatus.ACTIVE -> MaterialTheme.colorScheme.primary to "Активна"
        RecordingStatus.COMPLETED -> MaterialTheme.colorScheme.tertiary to "Завершена"
        RecordingStatus.PAUSED -> MaterialTheme.colorScheme.secondary to "Приостановлена"
        RecordingStatus.FAILED -> MaterialTheme.colorScheme.error to "Ошибка"
        RecordingStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer to "Отменена"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PaginationControls(
    page: Int,
    total: Int,
    hasMore: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Страница $page из ${(total / 20) + 1} (всего: $total)",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onPreviousPage,
                enabled = page > 1
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Предыдущая")
            }

            Button(
                onClick = onNextPage,
                enabled = hasMore
            ) {
                Text("Следующая")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun EmptyRecordingsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Нет записей",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Записи будут отображаться здесь после начала записи",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}
