package com.company.ipcamera.desktop.ui.screens.cameras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.DeleteCameraUseCase
import com.company.ipcamera.shared.domain.usecase.GetCameraByIdUseCase
import com.company.ipcamera.shared.domain.usecase.TestDiscoveredCameraUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun CameraDetailScreen(
    cameraId: String,
    onBackClick: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {},
    getCameraByIdUseCase: GetCameraByIdUseCase = koinInject(),
    deleteCameraUseCase: DeleteCameraUseCase = koinInject(),
    testCameraUseCase: TestDiscoveredCameraUseCase = koinInject()
) {
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(cameraId) {
        isLoading = true
        error = null
        try {
            camera = getCameraByIdUseCase.invoke(cameraId)
            isLoading = false
        } catch (e: Exception) {
            error = e.message ?: "Не удалось загрузить камеру"
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Заголовок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
            Text(
                text = "Детали камеры",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
            if (camera != null) {
                IconButton(onClick = { onEditClick(cameraId) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать")
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleting
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }

        Divider()

        // Контент
        when {
            isLoading -> {
                LoadingIndicator(message = "Загрузка камеры...")
            }
            error != null -> {
                ErrorView(
                    message = error!!,
                    onRetry = {
                        coroutineScope.launch {
                            isLoading = true
                            error = null
                            try {
                                camera = getCameraByIdUseCase.invoke(cameraId)
                                isLoading = false
                            } catch (e: Exception) {
                                error = e.message ?: "Не удалось загрузить камеру"
                                isLoading = false
                            }
                        }
                    }
                )
            }
            camera != null -> {
                CameraDetailContent(
                    camera = camera!!,
                    isTestingConnection = isTestingConnection,
                    testResult = testResult,
                    onTestConnection = {
                        coroutineScope.launch {
                            isTestingConnection = true
                            testResult = null
                            try {
                                val result = testCameraUseCase.invoke(camera!!)
                                testResult = if (result) {
                                    "Подключение успешно"
                                } else {
                                    "Не удалось подключиться"
                                }
                            } catch (e: Exception) {
                                testResult = "Ошибка: ${e.message}"
                            } finally {
                                isTestingConnection = false
                            }
                        }
                    }
                )
            }
        }

        // Диалог подтверждения удаления
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить камеру?") },
                text = { Text("Вы уверены, что хотите удалить камеру \"${camera?.name}\"? Это действие нельзя отменить.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isDeleting = true
                                try {
                                    deleteCameraUseCase.invoke(cameraId)
                                    isDeleting = false
                                    showDeleteDialog = false
                                    onDeleteClick()
                                } catch (e: Exception) {
                                    error = e.message ?: "Не удалось удалить камеру"
                                    isDeleting = false
                                    showDeleteDialog = false
                                }
                            }
                        },
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
private fun CameraDetailContent(
    camera: Camera,
    isTestingConnection: Boolean,
    testResult: String?,
    onTestConnection: () -> Unit
) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
        // Основная информация
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Основная информация",
                    style = MaterialTheme.typography.titleLarge
                )
                Divider()

                InfoRow("Название", camera.name)
                InfoRow("URL", camera.url)
                camera.model?.let { InfoRow("Модель", it) }
                camera.resolution?.let {
                    InfoRow("Разрешение", "${it.width}x${it.height}")
                }
                InfoRow("FPS", camera.fps.toString())
                InfoRow("Битрейт", "${camera.bitrate} kbps")
                InfoRow("Кодек", camera.codec)
            }
        }

        // Действия
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Действия",
                    style = MaterialTheme.typography.titleLarge
                )
                Divider()

                Button(
                    onClick = onTestConnection,
                    enabled = !isTestingConnection,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTestingConnection) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Тестировать подключение")
                }

                testResult?.let { result ->
                    Text(
                        text = result,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (result.contains("успешно")) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

