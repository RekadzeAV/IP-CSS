package com.company.ipcamera.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.company.ipcamera.desktop.di.appModule
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    startKoin {
        modules(appModule)
    }

    val windowState = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center))
    var showWindow by mutableStateOf(true)

    // Системный трей (если поддерживается)
    val trayInstalled = SystemIntegration.installTray(
        onShow = { showWindow = true },
        onExit = {
            SystemIntegration.removeTray()
            exitApplication()
        }
    )

    if (showWindow) {
        Window(
            onCloseRequest = {
                if (trayInstalled) {
                    showWindow = false
                } else {
                    exitApplication()
                }
            },
            title = "IP Camera Surveillance System",
            state = windowState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when {
                            (event.key == Key.Escape) -> {
                                if (trayInstalled) {
                                    showWindow = false
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                }
            ) {
                App()
            }
        }
    }

    // Горячие клавиши описаны в UI (подсказки)
    // Ctrl+Q / Cmd+Q — выход через меню трея при сворачивании в трей
    // Escape — свернуть в трей (если трей установлен)
}

