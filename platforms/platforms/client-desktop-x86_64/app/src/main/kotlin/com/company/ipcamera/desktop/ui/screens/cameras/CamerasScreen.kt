package com.company.ipcamera.desktop.ui.screens.cameras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.CameraCard
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.viewmodel.CamerasUiState
import com.company.ipcamera.desktop.ui.viewmodel.CamerasViewModel
import org.koin.compose.koinInject

@Composable
fun CamerasScreen(
    onCameraClick: (String) -> Unit = {},
    onAddCameraClick: () -> Unit = {},
    viewModel: CamerasViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredCameras = remember(viewModel.searchQuery, uiState) {
        viewModel.getFilteredCameras()
    }

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
                text = "Камеры",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            // Поиск
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                placeholder = { Text("Поиск камер...") },
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

            // Кнопка добавления камеры
            Button(
                onClick = onAddCameraClick,
                enabled = uiState !is CamerasUiState.Loading
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить")
            }

            // Кнопка обнаружения камер
            Button(
                onClick = { viewModel.discoverCameras() },
                enabled = uiState !is CamerasUiState.Loading
            ) {
                Icon(Icons.Default.Radar, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Обнаружить")
            }

            // Кнопка обновления
            IconButton(
                onClick = { viewModel.loadCameras() },
                enabled = uiState !is CamerasUiState.Loading
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Обновить")
            }
        }

        Divider()

        // Контент
        when (uiState) {
            is CamerasUiState.Loading -> {
                LoadingIndicator(message = "Загрузка камер...")
            }
            is CamerasUiState.Error -> {
                ErrorView(
                    message = uiState.message,
                    onRetry = { viewModel.loadCameras() }
                )
            }
            is CamerasUiState.Success -> {
                if (filteredCameras.isEmpty()) {
                    EmptyCamerasView(
                        hasSearchQuery = viewModel.searchQuery.isNotEmpty(),
                        onClearSearch = { viewModel.updateSearchQuery("") }
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredCameras) { camera ->
                            CameraCard(
                                camera = camera,
                                onClick = { onCameraClick(camera.id) },
                                modifier = Modifier.height(200.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCamerasView(
    hasSearchQuery: Boolean,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSearchQuery) {
                "Камеры не найдены"
            } else {
                "Нет камер"
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasSearchQuery) {
                "Попробуйте изменить поисковый запрос"
            } else {
                "Добавьте камеру или обнаружьте существующие"
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
