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

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок с поиском и фильтрами
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "События",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )

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
                    modifier = Modifier.width(300.dp)
                )

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
                // Фильтр по типу
                var showTypeMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = viewModel.selectedType != null,
                        onClick = { showTypeMenu = true },
                        label = {
                            Text(viewModel.selectedType?.name ?: "Все типы")
                        }
                    )
                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все типы") },
                            onClick = {
                                viewModel.setSelectedType(null)
                                showTypeMenu = false
                            }
                        )
                        EventType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    viewModel.setSelectedType(type)
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Фильтр по важности
                var showSeverityMenu by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = viewModel.selectedSeverity != null,
                        onClick = { showSeverityMenu = true },
                        label = {
                            Text(viewModel.selectedSeverity?.name ?: "Все важности")
                        }
                    )
                    DropdownMenu(
                        expanded = showSeverityMenu,
                        onDismissRequest = { showSeverityMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Все важности") },
                            onClick = {
                                viewModel.setSelectedSeverity(null)
                                showSeverityMenu = false
                            }
                        )
                        EventSeverity.values().forEach { severity ->
                            DropdownMenuItem(
                                text = { Text(severity.name) },
                                onClick = {
                                    viewModel.setSelectedSeverity(severity)
                                    showSeverityMenu = false
                                }
                            )
                        }
                    }
                }

                // Фильтр неподтвержденных
                FilterChip(
                    selected = viewModel.showOnlyUnacknowledged,
                    onClick = { viewModel.setShowOnlyUnacknowledged(!viewModel.showOnlyUnacknowledged) },
                    label = {
                        Text("Только неподтвержденные")
                    }
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
                ErrorView(
                    message = uiState.message,
                    onRetry = { viewModel.loadEvents() }
                )
            }
            is EventsUiState.Success -> {
                if (uiState.events.isEmpty()) {
                    EmptyEventsView()
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Таблица событий
                        EventsTable(
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
                            page = uiState.page,
                            total = uiState.total,
                            hasMore = uiState.hasMore,
                            onPreviousPage = { viewModel.loadPreviousPage() },
                            onNextPage = { viewModel.loadNextPage() }
                        )
                    }
                }
            }
        }
    }

    // Диалог подтверждения события
    showAcknowledgeDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showAcknowledgeDialog = null },
            title = { Text("Подтвердить событие?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Событие: ${event.type.name}")
                    event.description?.let { Text(it) }
                }
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

    // Диалог подтверждения удаления
    showDeleteDialog?.let { event ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить событие?") },
            text = {
                Text("Вы уверены, что хотите удалить событие \"${event.type.name}\" от ${formatDate(event.timestamp)}? Это действие нельзя отменить.")
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
}

@Composable
private fun EventsTable(
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
            // Заголовок таблицы
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Время", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.5f))
                    Text("Тип", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.5f))
                    Text("Важность", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("Камера", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
                    Text("Описание", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(2f))
                    Text("Статус", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text("Действия", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1.5f))
                }
                HorizontalDivider()
            }

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDate(event.timestamp),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.5f)
        )

        Text(
            text = event.type.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1.5f)
        )

        SeverityChip(severity = event.severity, modifier = Modifier.weight(1f))

        Text(
            text = event.cameraName ?: event.cameraId,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = event.description ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        AcknowledgedChip(
            acknowledged = event.acknowledged,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.weight(1.5f),
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
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
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

@Composable
private fun SeverityChip(severity: EventSeverity, modifier: Modifier = Modifier) {
    val (color, text) = when (severity) {
        EventSeverity.INFO -> MaterialTheme.colorScheme.primary to "INFO"
        EventSeverity.WARNING -> MaterialTheme.colorScheme.secondary to "WARNING"
        EventSeverity.ERROR -> MaterialTheme.colorScheme.error to "ERROR"
        EventSeverity.CRITICAL -> MaterialTheme.colorScheme.error to "CRITICAL"
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
private fun AcknowledgedChip(acknowledged: Boolean, modifier: Modifier = Modifier) {
    val (color, text) = if (acknowledged) {
        MaterialTheme.colorScheme.tertiary to "Подтверждено"
    } else {
        MaterialTheme.colorScheme.secondary to "Не подтверждено"
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
            text = "События будут отображаться здесь",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
