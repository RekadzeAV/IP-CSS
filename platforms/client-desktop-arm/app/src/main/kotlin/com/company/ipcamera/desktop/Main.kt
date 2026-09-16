package com.company.ipcamera.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.WindowPosition
import com.company.ipcamera.desktop.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule)
    }

    val windowState = rememberWindowState(position = WindowPosition.Aligned(Alignment.Center))
    var showWindow by mutableStateOf(true)

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
            title = "IP Camera Surveillance System (ARM)",
            state = windowState
        ) {
            App()
        }
    }
}

