package com.company.ipcamera.desktop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Временная шкала событий: события сгруппированы по дате и отображены в хронологическом порядке.
 */
@Composable
fun EventTimeline(
    events: List<Event>,
    onEventClick: (Event) -> Unit = {},
    onAcknowledgeClick: (Event) -> Unit = {},
    onDeleteClick: (Event) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет событий за выбранный период",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val groupedByDate = events.groupBy { event ->
        val cal = Calendar.getInstance()
        cal.timeInMillis = event.timestamp
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(cal.time)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        groupedByDate.forEach { (dateLabel, dayEvents) ->
            item(key = "header_$dateLabel") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }
            }

            items(
                items = dayEvents.sortedBy { it.timestamp },
                key = { it.id }
            ) { event ->
                EventTimelineItem(
                    event = event,
                    isFirstInGroup = event == dayEvents.minByOrNull { it.timestamp },
                    isLastInGroup = event == dayEvents.maxByOrNull { it.timestamp },
                    onEventClick = { onEventClick(event) },
                    onAcknowledgeClick = { onAcknowledgeClick(event) },
                    onDeleteClick = { onDeleteClick(event) }
                )
            }
        }
    }
}

@Composable
private fun EventTimelineItem(
    event: Event,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onEventClick: () -> Unit,
    onAcknowledgeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Вертикальная линия и точка
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            if (!isFirstInGroup) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (event.severity) {
                            EventSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                            EventSeverity.ERROR -> MaterialTheme.colorScheme.error
                            EventSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                            EventSeverity.INFO -> MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    )
            )
            if (!isLastInGroup) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp),
            onClick = onEventClick,
            colors = CardDefaults.cardColors(
                containerColor = when (event.severity) {
                    EventSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    EventSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    EventSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    EventSeverity.INFO -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatTime(event.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.cameraName ?: event.cameraId,
                        style = MaterialTheme.typography.titleSmall
                    )
                    event.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = event.type.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (event.acknowledged) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Подтверждено",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
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
                                Icons.Default.CheckCircle,
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
                            Icons.Default.Delete,
                            contentDescription = "Удалить",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
