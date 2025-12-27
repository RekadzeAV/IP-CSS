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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.AddCameraUseCase
import com.company.ipcamera.shared.domain.usecase.GetCameraByIdUseCase
import com.company.ipcamera.shared.domain.usecase.UpdateCameraUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AddEditCameraScreen(
    cameraId: String? = null,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    addCameraUseCase: AddCameraUseCase = koinInject(),
    updateCameraUseCase: UpdateCameraUseCase = koinInject(),
    getCameraByIdUseCase: GetCameraByIdUseCase = koinInject()
) {
    val isEditMode = cameraId != null
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("1920") }
    var height by remember { mutableStateOf("1080") }
    var fps by remember { mutableStateOf("25") }
    var bitrate by remember { mutableStateOf("4096") }
    var codec by remember { mutableStateOf("H.264") }

    var isLoading by remember { mutableStateOf(isEditMode) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Загрузка данных камеры для редактирования
    LaunchedEffect(cameraId) {
        if (isEditMode && cameraId != null) {
            isLoading = true
            try {
                val camera = getCameraByIdUseCase.invoke(cameraId)
                camera?.let {
                    name = it.name
                    url = it.url
                    username = it.username ?: ""
                    password = it.password ?: ""
                    model = it.model ?: ""
                    width = it.resolution?.width?.toString() ?: "1920"
                    height = it.resolution?.height?.toString() ?: "1080"
                    fps = it.fps.toString()
                    bitrate = it.bitrate.toString()
                    codec = it.codec
                }
                isLoading = false
            } catch (e: Exception) {
                error = e.message ?: "Не удалось загрузить камеру"
                isLoading = false
            }
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
                text = if (isEditMode) "Редактировать камеру" else "Добавить камеру",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }

        Divider()

        // Контент
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackClick) {
                        Text("Назад")
                    }
                }
            }
            else -> {
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

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Название *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = url,
                                onValueChange = { url = it },
                                label = { Text("URL *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("rtsp://192.168.1.100:554/stream") }
                            )

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Имя пользователя") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Пароль") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )

                            OutlinedTextField(
                                value = model,
                                onValueChange = { model = it },
                                label = { Text("Модель") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Параметры видео
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Параметры видео",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Divider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = width,
                                    onValueChange = { width = it },
                                    label = { Text("Ширина") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    label = { Text("Высота") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = fps,
                                    onValueChange = { fps = it },
                                    label = { Text("FPS") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = bitrate,
                                    onValueChange = { bitrate = it },
                                    label = { Text("Битрейт (kbps)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = codec,
                                onValueChange = { codec = it },
                                label = { Text("Кодек") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Кнопки действий
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isSaving = true
                                    error = null
                                    try {
                                        val resolution = try {
                                            Resolution(
                                                width = width.toInt(),
                                                height = height.toInt()
                                            )
                                        } catch (e: Exception) {
                                            null
                                        }

                                        val camera = Camera(
                                            id = cameraId ?: "",
                                            name = name,
                                            url = url,
                                            username = username.ifBlank { null },
                                            password = password.ifBlank { null },
                                            model = model.ifBlank { null },
                                            status = CameraStatus.UNKNOWN,
                                            resolution = resolution,
                                            fps = fps.toIntOrNull() ?: 25,
                                            bitrate = bitrate.toIntOrNull() ?: 4096,
                                            codec = codec
                                        )

                                        if (isEditMode && cameraId != null) {
                                            updateCameraUseCase.invoke(camera)
                                        } else {
                                            addCameraUseCase.invoke(camera)
                                        }

                                        isSaving = false
                                        onSaveClick()
                                    } catch (e: Exception) {
                                        error = e.message ?: "Ошибка при сохранении"
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isSaving && name.isNotBlank() && url.isNotBlank()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isSaving) "Сохранение..." else "Сохранить")
                        }
                    }

                    // Отображение ошибки
                    error?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

