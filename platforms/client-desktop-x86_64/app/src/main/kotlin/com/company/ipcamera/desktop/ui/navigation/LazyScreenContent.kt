package com.company.ipcamera.desktop.ui.navigation

import androidx.compose.runtime.*
import com.company.ipcamera.desktop.ui.screens.cameras.AddEditCameraScreen
import com.company.ipcamera.desktop.ui.screens.cameras.CamerasScreen
import com.company.ipcamera.desktop.ui.screens.cameras.CameraDetailScreen
import com.company.ipcamera.desktop.ui.screens.events.EventsScreen
import com.company.ipcamera.desktop.ui.screens.live.LiveViewScreen
import com.company.ipcamera.desktop.ui.screens.recordings.RecordingPlayerScreen
import com.company.ipcamera.desktop.ui.screens.recordings.RecordingsScreen
import com.company.ipcamera.desktop.ui.screens.settings.SettingsScreen

/**
 * Ленивая загрузка экранов для оптимизации производительности
 * Компоненты загружаются только когда экран становится активным
 * Использует ключи для правильной перекомпозиции
 */
@Composable
fun LazyScreenContent(
    screen: Screen,
    onCameraClick: (String) -> Unit,
    onAddCameraClick: () -> Unit,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRecordingClick: (String) -> Unit,
    onRecordingBackClick: () -> Unit
) {
    // Используем ключи для правильной перекомпозиции и ленивой загрузки
    when (screen) {
        is Screen.Cameras -> {
            key("cameras") {
                CamerasScreen(
                    onCameraClick = onCameraClick,
                    onAddCameraClick = onAddCameraClick
                )
            }
        }
        is Screen.CameraDetail -> {
            key("camera_detail_${screen.cameraId}") {
                CameraDetailScreen(
                    cameraId = screen.cameraId,
                    onBackClick = onBackClick,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
        is Screen.AddEditCamera -> {
            key("add_edit_camera_${screen.cameraId ?: "new"}") {
                AddEditCameraScreen(
                    cameraId = screen.cameraId,
                    onBackClick = {
                        if (screen.cameraId != null) {
                            onBackClick()
                        } else {
                            onBackClick()
                        }
                    },
                    onSaveClick = onSaveClick
                )
            }
        }
        Screen.LiveView -> {
            key("live_view") {
                LiveViewScreen()
            }
        }
        Screen.Recordings -> {
            key("recordings") {
                RecordingsScreen(
                    onRecordingClick = onRecordingClick
                )
            }
        }
        is Screen.RecordingPlayer -> {
            key("recording_player_${screen.recordingId}") {
                RecordingPlayerScreen(
                    recordingId = screen.recordingId,
                    onBackClick = onRecordingBackClick
                )
            }
        }
        Screen.Events -> {
            key("events") {
                EventsScreen()
            }
        }
        Screen.Settings -> {
            key("settings") {
                SettingsScreen()
            }
        }
    }
}
