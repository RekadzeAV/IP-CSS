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
    controlPtzUseCase: ControlPtzUseCase = koinInject(),
    onError: (String) -> Unit = {}
) {
    if (camera.ptz?.enabled != true) {
        return
    }

    val coroutineScope = rememberCoroutineScope()
    var isMoving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun executePTZCommand(command: String) {
        coroutineScope.launch {
            isMoving = true
            errorMessage = null
            try {
                controlPtzUseCase.invoke(camera, command).fold(
                    onSuccess = {
                        if (command == "stop") {
                            isMoving = false
                        }
                    },
                    onFailure = { e ->
                        isMoving = false
                        errorMessage = e.message ?: "Ошибка выполнения PTZ команды"
                        onError(errorMessage!!)
                    }
                )
            } catch (e: Exception) {
                isMoving = false
                errorMessage = e.message ?: "Ошибка выполнения PTZ команды"
                onError(errorMessage!!)
            }
        }
    }

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
                    onClick = { executePTZCommand("up") },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх")
                }

                IconButton(
                    onClick = { executePTZCommand("zoom_in") },
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
                    onClick = { executePTZCommand("left") },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Влево")
                }

                IconButton(
                    onClick = { executePTZCommand("stop") },
                    enabled = isMoving
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Стоп")
                }

                IconButton(
                    onClick = { executePTZCommand("right") },
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
                    onClick = { executePTZCommand("down") },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз")
                }

                IconButton(
                    onClick = { executePTZCommand("zoom_out") },
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

            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

