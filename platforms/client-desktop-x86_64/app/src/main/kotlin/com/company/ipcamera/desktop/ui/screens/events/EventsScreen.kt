package com.company.ipcamera.desktop.ui.screens.events

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
import com.company.ipcamera.desktop.ui.components.EventTimeline
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.viewmodel.EventsUiState
import com.company.ipcamera.desktop.ui.viewmodel.EventsViewModel
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventsScreen(
    viewModel: EventsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Event?>(null) }
    var showAcknowledgeDialog by remember { mutableStateOf<Event?>(null) }
    var showTimelineView by remember { mutableStateOf(false) } // false = список, true = временная шкала

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок с фильтрами
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "События",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )

                // Переключатель: список / временная шкала
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showTimelineView,
                        onClick = { showTimelineView = false },
                        label = { Text("Список") }
                    )
                    FilterChip(
                        selected = showTimelineView,
                        onClick = { showTimelineView = true },
                        label = { Text("Временная шкала") }
                    )
                }

                // Кнопка обновления
                IconButton(
                    onClick = { viewModel.loadEvents() },
                    enabled = uiState !is EventsUiState.Loading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                }
            }

            // Фильтры
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Поиск
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Поиск событий...") },
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
                    modifier = Modifier.weight(1f)
                )

                // Фильтр по типу
                var expandedType by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = viewModel.selectedEventType != null,
                        onClick = { expandedType = true },
                        label = {
                            Text(
                                viewModel.selectedEventType?.name ?: "Тип события"
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все типы") },
                            onClick = {
                                viewModel.setEventType(null)
                                expandedType = false
                            }
                        )
                        EventType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    viewModel.setEventType(type)
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // Фильтр по серьезности
                var expandedSeverity by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = viewModel.selectedSeverity != null,
                        onClick = { expandedSeverity = true },
                        label = {
                            Text(
                                viewModel.selectedSeverity?.name ?: "Серьезность"
                            )
                        }
                    )
                    DropdownMenu(
                        expanded = expandedSeverity,
                        onDismissRequest = { expandedSeverity = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все") },
                            onClick = {
                                viewModel.setSeverity(null)
                                expandedSeverity = false
                            }
                        )
                        EventSeverity.values().forEach { severity ->
                            DropdownMenuItem(
                                text = { Text(severity.name) },
                                onClick = {
                                    viewModel.setSeverity(severity)
                                    expandedSeverity = false
                                }
                            )
                        }
                    }
                }

                // Фильтр подтвержденных
                FilterChip(
                    selected = viewModel.showAcknowledgedOnly,
                    onClick = { viewModel.applyShowAcknowledgedOnly(!viewModel.showAcknowledgedOnly) },
                    label = { Text("Только подтвержденные") }
                )
            }
        }

        Divider()

        // Контент
        when (uiState) {
            is EventsUiState.Loading -> {
                LoadingIndicator(message = "Загрузка событий...")
            }
            is EventsUiState.Error -> {
                val errorState = uiState as EventsUiState.Error
                ErrorView(
                    message = errorState.message,
                    onRetry = { viewModel.loadEvents() }
                )
            }
            is EventsUiState.Success -> {
                val successState = uiState as EventsUiState.Success
                if (viewModel.getFilteredEvents().isEmpty()) {
                    EmptyEventsView()
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (showTimelineView) {
                            EventTimeline(
                                events = viewModel.getFilteredEvents(),
                                onAcknowledgeClick = { showAcknowledgeDialog = it },
                                onDeleteClick = { showDeleteDialog = it },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            EventsList(
                                events = viewModel.getFilteredEvents(),
                                onAcknowledgeClick = { event ->
                                    showAcknowledgeDialog = event
                                },
                                onDeleteClick = { event ->
                                    showDeleteDialog = event
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
    }

    // Диалог подтверждения удаления
    showDeleteDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить событие?") },
            text = {
                Text("Вы уверены, что хотите удалить событие от ${formatDate(event.timestamp)}? Это действие нельзя отменить.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(
                            eventId = event.id,
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

    // Диалог подтверждения события
    showAcknowledgeDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showAcknowledgeDialog = null },
            title = { Text("Подтвердить событие?") },
            text = {
                Text("Вы хотите подтвердить событие от ${formatDate(event.timestamp)}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acknowledgeEvent(
                            eventId = event.id,
                            onSuccess = { showAcknowledgeDialog = null },
                            onError = { showAcknowledgeDialog = null }
                        )
                    }
                ) {
                    Text("Подтвердить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcknowledgeDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun EventsList(
    events: List<Event>,
    onAcknowledgeClick: (Event) -> Unit,
    onDeleteClick: (Event) -> Unit
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
            items(events) { event ->
                EventRow(
                    event = event,
                    onAcknowledgeClick = { onAcknowledgeClick(event) },
                    onDeleteClick = { onDeleteClick(event) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun EventRow(
    event: Event,
    onAcknowledgeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (event.severity) {
                EventSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                EventSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                EventSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                EventSeverity.INFO -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SeverityChip(severity = event.severity)
                    TypeChip(type = event.type)
                    if (event.acknowledged) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "Подтверждено",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = event.cameraName ?: event.cameraId,
                    style = MaterialTheme.typography.titleMedium
                )

                event.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = formatDate(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!event.acknowledged) {
                    IconButton(
                        onClick = onAcknowledgeClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Подтвердить",
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
}

@Composable
private fun SeverityChip(severity: EventSeverity, modifier: Modifier = Modifier) {
    val (color, text) = when (severity) {
        EventSeverity.CRITICAL -> MaterialTheme.colorScheme.error to "КРИТИЧНО"
        EventSeverity.ERROR -> MaterialTheme.colorScheme.error to "ОШИБКА"
        EventSeverity.WARNING -> MaterialTheme.colorScheme.tertiary to "ПРЕДУПРЕЖДЕНИЕ"
        EventSeverity.INFO -> MaterialTheme.colorScheme.primary to "ИНФО"
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
private fun TypeChip(type: EventType, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
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
private fun EmptyEventsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Нет событий",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "События будут отображаться здесь при их возникновении",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}