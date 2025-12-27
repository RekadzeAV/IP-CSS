package com.company.ipcamera.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.company.ipcamera.desktop.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    // Инициализация Koin для Dependency Injection
    startKoin {
        modules(appModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "IP Camera Surveillance System"
    ) {
        App()
    }
}

