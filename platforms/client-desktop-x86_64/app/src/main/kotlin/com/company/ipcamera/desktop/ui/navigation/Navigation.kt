package com.company.ipcamera.desktop.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.screens.cameras.AddEditCameraScreen
import com.company.ipcamera.desktop.ui.screens.cameras.CamerasScreen
import com.company.ipcamera.desktop.ui.screens.cameras.CameraDetailScreen
import com.company.ipcamera.desktop.ui.screens.events.EventsScreen
import com.company.ipcamera.desktop.ui.screens.live.LiveViewScreen
import com.company.ipcamera.desktop.ui.screens.recordings.RecordingPlayerScreen
import com.company.ipcamera.desktop.ui.screens.recordings.RecordingsScreen
import com.company.ipcamera.desktop.ui.screens.settings.SettingsScreen

sealed class Screen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Cameras : Screen("Камеры", Icons.Default.Camera)
    data class CameraDetail(val cameraId: String) : Screen("Детали камеры", Icons.Default.Camera)
    data class AddEditCamera(val cameraId: String? = null) : Screen("Добавить/Редактировать камеру", Icons.Default.Add)
    object LiveView : Screen("Прямой эфир", Icons.Default.VideoLibrary)
    object Recordings : Screen("Записи", Icons.Default.PlayArrow)
    data class RecordingPlayer(val recordingId: String) : Screen("Воспроизведение записи", Icons.Default.PlayArrow)
    object Events : Screen("События", Icons.Default.Notifications)
    object Settings : Screen("Настройки", Icons.Default.Settings)
}

@Composable
fun Navigation(modifier: Modifier = Modifier) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Cameras) }

    Row(modifier = modifier) {
        // Боковая навигационная панель
        Surface(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavigationButton(
                    screen = Screen.Cameras,
                    selected = selectedScreen is Screen.Cameras,
                    onClick = { selectedScreen = Screen.Cameras }
                )
                NavigationButton(
                    screen = Screen.LiveView,
                    selected = selectedScreen == Screen.LiveView,
                    onClick = { selectedScreen = Screen.LiveView }
                )
                NavigationButton(
                    screen = Screen.Recordings,
                    selected = selectedScreen is Screen.Recordings,
                    onClick = { selectedScreen = Screen.Recordings }
                )
                NavigationButton(
                    screen = Screen.Events,
                    selected = selectedScreen == Screen.Events,
                    onClick = { selectedScreen = Screen.Events }
                )
                NavigationButton(
                    screen = Screen.Settings,
                    selected = selectedScreen == Screen.Settings,
                    onClick = { selectedScreen = Screen.Settings }
                )
            }
        }

        Divider(modifier = Modifier.width(1.dp).fillMaxHeight())

        // Основной контент
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (val screen = selectedScreen) {
                is Screen.Cameras -> CamerasScreen(
                    onCameraClick = { cameraId ->
                        selectedScreen = Screen.CameraDetail(cameraId)
                    },
                    onAddCameraClick = {
                        selectedScreen = Screen.AddEditCamera(null)
                    }
                )
                is Screen.CameraDetail -> CameraDetailScreen(
                    cameraId = screen.cameraId,
                    onBackClick = { selectedScreen = Screen.Cameras },
                    onEditClick = { cameraId ->
                        selectedScreen = Screen.AddEditCamera(cameraId)
                    },
                    onDeleteClick = { selectedScreen = Screen.Cameras }
                )
                is Screen.AddEditCamera -> AddEditCameraScreen(
                    cameraId = screen.cameraId,
                    onBackClick = {
                        if (screen.cameraId != null) {
                            selectedScreen = Screen.CameraDetail(screen.cameraId)
                        } else {
                            selectedScreen = Screen.Cameras
                        }
                    },
                    onSaveClick = {
                        if (screen.cameraId != null) {
                            selectedScreen = Screen.CameraDetail(screen.cameraId)
                        } else {
                            selectedScreen = Screen.Cameras
                        }
                    }
                )
                Screen.LiveView -> LiveViewScreen()
                Screen.Recordings -> RecordingsScreen(
                    onRecordingClick = { recordingId ->
                        selectedScreen = Screen.RecordingPlayer(recordingId)
                    }
                )
                is Screen.RecordingPlayer -> RecordingPlayerScreen(
                    recordingId = screen.recordingId,
                    onBackClick = { selectedScreen = Screen.Recordings }
                )
                Screen.Events -> EventsScreen()
                Screen.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun NavigationButton(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.title,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(screen.title)
        }
    }
}

