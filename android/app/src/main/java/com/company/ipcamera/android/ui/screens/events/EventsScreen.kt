package com.company.ipcamera.android.ui.screens.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.android.ui.viewmodel.EventsViewModel
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onBackClick: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: EventsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterSeverity == null,
                    onClick = { viewModel.setFilterSeverity(null) },
                    label = { Text("All") }
                )
                EventSeverity.values().forEach { severity ->
                    FilterChip(
                        selected = uiState.filterSeverity == severity,
                        onClick = { viewModel.setFilterSeverity(severity) },
                        label = { Text(severity.name) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
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
                    uiState.events.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("No events found")
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.events) { event ->
                                EventItem(
                                    event = event,
                                    onClick = { onEventClick(event.id) },
                                    onAcknowledge = { viewModel.acknowledgeEvent(event.id, "current_user_id") }
                                )
                            }
                            if (uiState.hasMore) {
                                item {
                                    Button(
                                        onClick = { viewModel.loadMore() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
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

@Composable
fun EventItem(
    event: com.company.ipcamera.shared.domain.model.Event,
    onClick: () -> Unit,
    onAcknowledge: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = event.severity.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (event.severity) {
                        EventSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                        EventSeverity.ERROR -> MaterialTheme.colorScheme.error
                        EventSeverity.WARNING -> MaterialTheme.colorScheme.primary
                        EventSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            event.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.cameraName ?: "Unknown Camera",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!event.acknowledged) {
                    TextButton(onClick = onAcknowledge) {
                        Text("Acknowledge")
                    }
                }
            }
        }
    }
}

