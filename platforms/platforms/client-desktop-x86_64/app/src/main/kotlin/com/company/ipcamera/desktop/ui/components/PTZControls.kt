package com.company.ipcamera.desktop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.ControlPtzUseCase
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PTZControls(
    camera: Camera,
    modifier: Modifier = Modifier,
    controlPtzUseCase: ControlPtzUseCase = koinInject()
) {
    if (camera.ptz?.enabled != true) {
        return
    }

    val coroutineScope = rememberCoroutineScope()
    var isMoving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PTZ управление",
                style = MaterialTheme.typography.titleMedium
            )

            // Верхняя строка (вверх, зум)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isMoving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = controlPtzUseCase(camera, "up")
                            result.onFailure { error ->
                                errorMessage = error.message
                            }
                            isMoving = false
                        }
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх")
                }

                IconButton(
                    onClick = {
                        isMoving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = controlPtzUseCase(camera, "zoom_in")
                            result.onFailure { error ->
                                errorMessage = error.message
                            }
                            isMoving = false
                        }
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Приблизить")
                }
            }

            // Средняя строка (влево, центр, вправо)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isMoving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = controlPtzUseCase(camera, "left")
                            result.onFailure { error ->
                                errorMessage = error.message
                            }
                            isMoving = false
                        }
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Влево")
                }

                IconButton(
                    onClick = {
                        errorMessage = null
                        coroutineScope.launch {
                            val result = controlPtzUseCase(camera, "stop")
                            result.onFailure { error ->
                                errorMessage = error.message
                            }
                            isMoving = false
                        }
                    },
                    enabled = isMoving
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Стоп")
                }

                IconButton(
                    onClick = {
                        isMoving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = controlPtzUseCase(camera, "right")
                            result.onFailure { error ->
                                errorMessage = error.message
                            }
                            isMoving = false
                        }
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Вправо")
                }
            }

            // Нижняя строка (вниз, отдалить)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        isMoving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = controlPtzUseCase(camera, "down")
                            result.onFailure { error ->
                                errorMessage = error.message
                            }
                            isMoving = false
                        }
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз")
                }

                IconButton(
                    onClick = {
                        isMoving = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = controlPtzUseCase(camera, "zoom_out")
                            result.onFailure { error ->
                                errorMessage = error.message
                            }
                            isMoving = false
                        }
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Отдалить")
                }
            }

            if (isMoving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

