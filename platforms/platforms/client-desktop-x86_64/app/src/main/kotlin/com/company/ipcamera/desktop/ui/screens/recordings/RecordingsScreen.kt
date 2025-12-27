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
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.components.RecordingCard
import com.company.ipcamera.desktop.ui.viewmodel.RecordingsUiState
import com.company.ipcamera.desktop.ui.viewmodel.RecordingsViewModel
import com.company.ipcamera.shared.domain.model.Recording
import org.koin.compose.koinInject

@Composable
fun RecordingsScreen(
    onRecordingClick: (String) -> Unit = {},
    viewModel: RecordingsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredRecordings = remember(viewModel.searchQuery, uiState) {
        viewModel.getFilteredRecordings()
    }

    var showFilters by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Recording?>(null) }
    var showExportDialog by remember { mutableStateOf<Recording?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок с поиском и действиями
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

            // Кнопка фильтров
            IconButton(onClick = { showFilters = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
            }

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
                ErrorView(
                    message = uiState.message,
                    onRetry = { viewModel.loadRecordings() }
                )
            }
            is RecordingsUiState.Success -> {
                if (filteredRecordings.isEmpty()) {
                    EmptyRecordingsView(
                        hasSearchQuery = viewModel.searchQuery.isNotEmpty(),
                        onClearSearch = { viewModel.updateSearchQuery("") }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredRecordings) { recording ->
                            RecordingCard(
                                recording = recording,
                                onClick = { onRecordingClick(recording.id) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Действия для записи
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { showExportDialog = recording }
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Экспорт")
                                }
                                IconButton(
                                    onClick = { showDeleteDialog = recording }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                                }
                            }
                        }

                        // Кнопка загрузки следующей страницы
                        if (uiState.recordings.hasMore) {
                            item {
                                Button(
                                    onClick = { viewModel.loadNextPage() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Загрузить еще")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог фильтров
    if (showFilters) {
        FiltersDialog(
            selectedCameraId = viewModel.selectedCameraId,
            onDismiss = { showFilters = false },
            onApplyFilters = { cameraId, startDate, endDate ->
                viewModel.setCameraFilter(cameraId)
                viewModel.setDateRange(startDate, endDate)
                showFilters = false
            },
            onClearFilters = {
                viewModel.clearFilters()
                showFilters = false
            }
        )
    }

    // Диалог удаления
    showDeleteDialog?.let { recording ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить запись?") },
            text = {
                Text("Вы уверены, что хотите удалить запись от ${recording.cameraName}? Это действие нельзя отменить.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecording(recording.id) {
                            showDeleteDialog = null
                        }
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

    // Диалог экспорта
    showExportDialog?.let { recording ->
        ExportDialog(
            recording = recording,
            onDismiss = { showExportDialog = null },
            onExport = { format, quality ->
                viewModel.exportRecording(recording.id, format, quality) {
                    showExportDialog = null
                }
            }
        )
    }
}

@Composable
private fun EmptyRecordingsView(
    hasSearchQuery: Boolean,
    onClearSearch: () -> Unit
) {
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
            text = if (hasSearchQuery) {
                "Записи не найдены"
            } else {
                "Нет записей"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasSearchQuery) {
                "Попробуйте изменить поисковый запрос или фильтры"
            } else {
                "Записи появятся здесь после начала записи с камер"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasSearchQuery) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onClearSearch) {
                Text("Очистить поиск")
            }
        }
    }
}

@Composable
private fun FiltersDialog(
    selectedCameraId: String?,
    onDismiss: () -> Unit,
    onApplyFilters: (String?, Long?, Long?) -> Unit,
    onClearFilters: () -> Unit
) {
    var cameraId by remember { mutableStateOf(selectedCameraId) }
    // TODO: Добавить выбор даты через DatePicker

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фильтры") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TODO: Добавить выбор камеры и даты
                Text("Фильтры будут реализованы в следующей версии")
            }
        },
        confirmButton = {
            Button(onClick = { onApplyFilters(cameraId, null, null) }) {
                Text("Применить")
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
private fun ExportDialog(
    recording: Recording,
    onDismiss: () -> Unit,
    onExport: (String, String) -> Unit
) {
    var format by remember { mutableStateOf(recording.format.name) }
    var quality by remember { mutableStateOf(recording.quality.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт записи") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Выбор формата
                Text("Формат:")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("MP4", "AVI", "MKV").forEach { fmt ->
                        FilterChip(
                            selected = format == fmt,
                            onClick = { format = fmt },
                            label = { Text(fmt) }
                        )
                    }
                }

                // Выбор качества
                Text("Качество:")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LOW", "MEDIUM", "HIGH", "ULTRA").forEach { q ->
                        FilterChip(
                            selected = quality == q,
                            onClick = { quality = q },
                            label = { Text(q) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(format, quality) }) {
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
