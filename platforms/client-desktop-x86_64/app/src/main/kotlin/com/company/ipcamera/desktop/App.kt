package com.company.ipcamera.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.company.ipcamera.desktop.ui.components.AppSnackbarHost
import com.company.ipcamera.desktop.ui.components.rememberAppSnackbarHostState
import com.company.ipcamera.desktop.ui.navigation.Navigation
import com.company.ipcamera.desktop.ui.theme.AppTheme
import com.company.ipcamera.desktop.ui.theme.LocalThemeMode
import com.company.ipcamera.desktop.ui.theme.ThemeMode
import com.company.ipcamera.desktop.ui.theme.isDarkTheme
import com.company.ipcamera.desktop.ui.theme.rememberThemeManager
import com.company.ipcamera.desktop.performance.GlobalPerformanceMonitor
import com.company.ipcamera.shared.domain.service.CameraEventMonitoringService
import kotlinx.coroutines.delay
import org.koin.core.context.GlobalContext

@Composable
fun App() {
    val themeManager = rememberThemeManager()
    val snackbarHostState = rememberAppSnackbarHostState()

    val eventMonitoringService: CameraEventMonitoringService? = remember {
        runCatching { GlobalContext.get().get<CameraEventMonitoringService>() }.getOrNull()
    }

    // Инициализация мониторинга производительности
    val performanceMonitor = GlobalPerformanceMonitor.getInstance()

    LaunchedEffect(Unit) {
        // Запуск мониторинга ONVIF-событий при старте (через сервис, DI)
        eventMonitoringService?.initialize()

        // Запуск периодического мониторинга памяти
        while (true) {
            performanceMonitor.recordMemoryUsage()
            delay(5000) // Обновляем каждые 5 секунд
        }
    }

    CompositionLocalProvider(LocalThemeMode provides ThemeMode.SYSTEM) {
        AppTheme(darkTheme = themeManager.isDarkTheme()) {
            Scaffold(
                snackbarHost = {
                    AppSnackbarHost(snackbarHostState)
                },
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Navigation(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

