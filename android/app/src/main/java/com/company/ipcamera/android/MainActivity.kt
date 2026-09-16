package com.company.ipcamera.android

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.company.ipcamera.android.di.appModule
import com.company.ipcamera.android.service.PushNotificationService
import com.company.ipcamera.android.service.SecurityEventUploader
import com.company.ipcamera.android.ui.navigation.AppNavigation
import com.company.ipcamera.android.ui.theme.IPCameraTheme
import com.company.ipcamera.shared.domain.service.CameraEventMonitoringService
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {

    private val eventMonitoringService: CameraEventMonitoringService by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Koin
        startKoin {
            androidContext(this@MainActivity)
            modules(appModule)
        }

        // Каналы уведомлений (используются FCM и локальными сервисами)
        PushNotificationService.createNotificationChannels(this@MainActivity)

        // Отправка критических security-событий на сервер (REMAINING_TASKS 2.6)
        SecurityEventUploader.install()

        // POST_NOTIFICATIONS runtime permission (Android 13+, задача 2.3)
        requestNotificationPermissionIfNeeded()

        // Инициализация мониторинга событий камер
        lifecycleScope.launch {
            try {
                eventMonitoringService.initialize()
            } catch (e: Exception) {
                // Игнорируем ошибки инициализации, если сервис недоступен
            }
        }

        setContent {
            IPCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)

        if (isInPictureInPictureMode) {
            // Скрываем системный UI в режиме PiP
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
            }
        } else {
            // Восстанавливаем системный UI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(true)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Обновляем параметры PiP при изменении конфигурации
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            updatePictureInPictureParams()
        }
    }

    override fun onResume() {
        super.onResume()
        // Переподписка на события камер при возврате в приложение (сеть/камера могли восстановиться)
        lifecycleScope.launch {
            try {
                eventMonitoringService.ensureMonitoring()
            } catch (e: Exception) {
                // Игнорируем ошибки переподписки
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Остановка мониторинга при завершении активности
        lifecycleScope.launch {
            try {
                eventMonitoringService.shutdown()
            } catch (e: Exception) {
                // Игнорируем ошибки при завершении
            }
        }
    }

    @android.annotation.SuppressLint("NewApi")
    private fun updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9) // Стандартное соотношение сторон
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            setPictureInPictureParams(params)
        }
    }
}



