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

@Composable
fun PTZControls(
    camera: Camera,
    modifier: Modifier = Modifier,
    onPTZCommand: (String) -> Unit = {}
) {
    if (camera.ptz?.enabled != true) {
        return
    }
    
    var isMoving by remember { mutableStateOf(false) }
    
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
                        onPTZCommand("up")
                        // TODO: Интеграция с ONVIF PTZ командами
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Вверх")
                }
                
                IconButton(
                    onClick = {
                        isMoving = true
                        onPTZCommand("zoom_in")
                        // TODO: Интеграция с ONVIF PTZ командами
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
                        onPTZCommand("left")
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Влево")
                }
                
                IconButton(
                    onClick = {
                        isMoving = false
                        onPTZCommand("stop")
                    },
                    enabled = isMoving
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Стоп")
                }
                
                IconButton(
                    onClick = {
                        isMoving = true
                        onPTZCommand("right")
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
                        onPTZCommand("down")
                    },
                    enabled = !isMoving
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Вниз")
                }
                
                IconButton(
                    onClick = {
                        isMoving = true
                        onPTZCommand("zoom_out")
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
        }
    }
}

