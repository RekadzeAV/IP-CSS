package com.company.ipcamera.android.ui.screens.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.company.ipcamera.android.ui.viewmodel.NotificationsViewModel
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    onNotificationClick: (Notification) -> Unit = {},
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Notifications")
                        if (uiState.unreadCount > 0) {
                            Badge {
                                Text(uiState.unreadCount.toString())
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterType == null,
                    onClick = { viewModel.setFilterType(null) },
                    label = { Text("All") }
                )
                NotificationType.values().forEach { type ->
                    FilterChip(
                        selected = uiState.filterType == type,
                        onClick = { viewModel.setFilterType(type) },
                        label = { Text(type.name) }
                    )
                }
            }

            // Priority filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterPriority == null,
                    onClick = { viewModel.setFilterPriority(null) },
                    label = { Text("All Priority") }
                )
                NotificationPriority.values().forEach { priority ->
                    FilterChip(
                        selected = uiState.filterPriority == priority,
                        onClick = { viewModel.setFilterPriority(priority) },
                        label = { Text(priority.name) }
                    )
                }
            }

            // Show read toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uiState.showRead,
                    onCheckedChange = { viewModel.setShowRead(it) }
                )
                Text(
                    text = "Show read notifications",
                    modifier = Modifier.clickable { viewModel.setShowRead(!uiState.showRead) }
                )
            }

            Divider()

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading && uiState.notifications.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Error: ${uiState.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.refresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                    uiState.notifications.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
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
                                text = "No notifications",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = uiState.notifications,
                                key = { it.id }
                            ) { notification ->
                                NotificationItem(
                                    notification = notification,
                                    dateFormat = dateFormat,
                                    onClick = { onNotificationClick(notification) },
                                    onMarkAsRead = { viewModel.markAsRead(notification.id) },
                                    onDelete = { viewModel.deleteNotification(notification.id) }
                                )
                            }
                            if (uiState.hasMore) {
                                item {
                                    Button(
                                        onClick = { viewModel.loadMore() },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uiState.isLoading
                                    ) {
                                        if (uiState.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text("Load More")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.read) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon based on type
            Icon(
                imageVector = when (notification.type) {
                    NotificationType.EVENT -> Icons.Default.Notifications
                    NotificationType.ALERT -> Icons.Default.Notifications
                    NotificationType.ERROR -> Icons.Default.Notifications
                    NotificationType.WARNING -> Icons.Default.Notifications
                    NotificationType.RECORDING -> Icons.Default.CheckCircle
                    else -> Icons.Default.Notifications
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (notification.priority) {
                    NotificationPriority.URGENT -> MaterialTheme.colorScheme.error
                    NotificationPriority.HIGH -> MaterialTheme.colorScheme.error
                    NotificationPriority.NORMAL -> MaterialTheme.colorScheme.primary
                    NotificationPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.read) {
                        Badge(
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(notification.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!notification.read) {
                            IconButton(
                                onClick = onMarkAsRead,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Mark as read",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
