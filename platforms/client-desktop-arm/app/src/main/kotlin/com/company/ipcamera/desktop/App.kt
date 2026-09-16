package com.company.ipcamera.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.company.ipcamera.desktop.ui.navigation.Navigation
import com.company.ipcamera.desktop.ui.theme.AppTheme
import com.company.ipcamera.shared.domain.service.CameraEventMonitoringService
import org.koin.core.context.GlobalContext

@Composable
fun App() {
    val eventMonitoringService: CameraEventMonitoringService? =
        runCatching { GlobalContext.get().get<CameraEventMonitoringService>() }.getOrNull()

    LaunchedEffect(Unit) {
        // Мониторинг ONVIF-событий через сервис (DI). Прежний путь через
        // CameraRepositoryWithEventMonitoring удалён из shared (см. b83b6140).
        eventMonitoringService?.initialize()
    }

    AppTheme {
        Navigation(
            modifier = Modifier.fillMaxSize()
        )
    }
}

