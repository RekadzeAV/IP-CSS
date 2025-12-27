package com.company.ipcamera.desktop.ui.screens.live

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
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.components.VideoPlayer
import com.company.ipcamera.desktop.ui.viewmodel.GridLayout
import com.company.ipcamera.desktop.ui.viewmodel.LiveViewViewModel
import org.koin.compose.koinInject

@Composable
fun LiveViewScreen(
    viewModel: LiveViewViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    var showCameraSelector by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Панель управления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Прямой эфир",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            
            // Выбор layout
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.SINGLE) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.SINGLE) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "1 камера")
                }
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.GRID_4) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.GRID_4) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.Grid4x4, contentDescription = "4 камеры")
                }
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.GRID_9) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.GRID_9) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = "9 камер")
                }
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.GRID_16) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.GRID_16) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.ViewModule, contentDescription = "16 камер")
                }
            }
            
            // Кнопка выбора камер
            Button(onClick = { showCameraSelector = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выбрать камеры")
            }
        }
        
        Divider()
        
        // Контент
        when {
            state.isLoading -> {
                LoadingIndicator(message = "Загрузка камер...")
            }
            state.error != null -> {
                ErrorView(
                    message = state.error!!,
                    onRetry = { viewModel.loadCameras() }
                )
            }
            else -> {
                val selectedCameras = viewModel.getSelectedCameras()
                
                if (selectedCameras.isEmpty()) {
                    EmptyLiveViewState(
                        onSelectCameras = { showCameraSelector = true }
                    )
                } else {
                    VideoGrid(
                        cameras = selectedCameras,
                        gridLayout = state.gridLayout,
                        onRemoveCamera = { cameraId ->
                            viewModel.removeCamera(cameraId)
                        }
                    )
                }
            }
        }
    }
    
    // Диалог выбора камер
    if (showCameraSelector) {
        CameraSelectorDialog(
            availableCameras = state.cameras,
            selectedCameras = state.selectedCameras,
            gridLayout = state.gridLayout,
            onDismiss = { showCameraSelector = false },
            onCameraSelected = { cameraId ->
                viewModel.selectCamera(cameraId)
            },
            onCameraDeselected = { cameraId ->
                viewModel.removeCamera(cameraId)
            }
        )
    }
}

@Composable
private fun VideoGrid(
    cameras: List<com.company.ipcamera.shared.domain.model.Camera>,
    gridLayout: GridLayout,
    onRemoveCamera: (String) -> Unit
) {
    val columns = when (gridLayout) {
        GridLayout.SINGLE -> 1
        GridLayout.GRID_4 -> 2
        GridLayout.GRID_9 -> 3
        GridLayout.GRID_16 -> 4
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(cameras) { camera ->
            Box(
                modifier = Modifier
                    .aspectRatio(16f / 9f)
                    .fillMaxWidth()
            ) {
                VideoPlayer(
                    camera = camera,
                    onError = { error ->
                        // Обработка ошибки
                    }
                )
                
                // Кнопка закрытия
                IconButton(
                    onClick = { onRemoveCamera(camera.id) },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLiveViewState(
    onSelectCameras: () -> Unit
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
            text = "Нет выбранных камер",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Выберите камеры для просмотра",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectCameras) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрать камеры")
        }
    }
}

@Composable
private fun CameraSelectorDialog(
    availableCameras: List<com.company.ipcamera.shared.domain.model.Camera>,
    selectedCameras: List<String>,
    gridLayout: GridLayout,
    onDismiss: () -> Unit,
    onCameraSelected: (String) -> Unit,
    onCameraDeselected: (String) -> Unit
) {
    val maxCameras = when (gridLayout) {
        GridLayout.SINGLE -> 1
        GridLayout.GRID_4 -> 4
        GridLayout.GRID_9 -> 9
        GridLayout.GRID_16 -> 16
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор камер (максимум $maxCameras)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                availableCameras.forEach { camera ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = camera.id in selectedCameras,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (selectedCameras.size < maxCameras) {
                                        onCameraSelected(camera.id)
                                    }
                                } else {
                                    onCameraDeselected(camera.id)
                                }
                            },
                            enabled = camera.id in selectedCameras || selectedCameras.size < maxCameras
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = camera.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}
