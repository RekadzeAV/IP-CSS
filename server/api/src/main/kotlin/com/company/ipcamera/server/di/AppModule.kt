package com.company.ipcamera.server.di

import com.company.ipcamera.server.config.RedisConfig
import com.company.ipcamera.server.middleware.RateLimitMiddleware
import com.company.ipcamera.server.repository.ServerUserRepository
import com.company.ipcamera.server.service.CameraService
import com.company.ipcamera.server.service.EventService
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.HlsGeneratorService
import com.company.ipcamera.server.service.NotificationService
import com.company.ipcamera.server.service.ScreenshotService
import com.company.ipcamera.server.service.VideoRecordingService
import com.company.ipcamera.server.service.VideoStreamService
import com.company.ipcamera.shared.data.di.dataSourcesModule
import com.company.ipcamera.shared.data.di.repositoriesV2Module
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import com.company.ipcamera.shared.domain.usecase.GetNotificationsUseCase
import com.company.ipcamera.shared.domain.usecase.MarkNotificationAsReadUseCase
import com.company.ipcamera.shared.domain.usecase.SendNotificationUseCase
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.koin.dsl.module

val appModule = module {
    // Database Factory (использует PostgreSQL, если установлена переменная окружения DATABASE_URL)
    single<DatabaseFactory> { DatabaseFactory(null) }

    // Database
    single { createDatabase(get<DatabaseFactory>().createDriver()) }

    // Data Sources Module (должен быть перед repositoriesV2Module)
    includes(dataSourcesModule)

    // Repositories V2 Module (использует Data Sources)
    includes(repositoriesV2Module)

    // Server User Repository (in-memory для MVP)
    single<ServerUserRepository> { ServerUserRepository() }

    // Server Recording Repository (SQLDelight для продакшена) - используется RecordingRepository из repositoriesV2Module
    single<com.company.ipcamera.server.repository.ServerRecordingRepositorySqlDelight> {
        com.company.ipcamera.server.repository.ServerRecordingRepositorySqlDelight(get())
    }

    // Notification Use Cases
    single<SendNotificationUseCase> {
        SendNotificationUseCase(get())
    }
    single<GetNotificationsUseCase> {
        GetNotificationsUseCase(get())
    }
    single<MarkNotificationAsReadUseCase> {
        MarkNotificationAsReadUseCase(get())
    }

    // Redis Commands (инициализируется при старте приложения)
    single<RedisCoroutinesCommands<String, String>> {
        RedisConfig.initialize()
    }

    // Rate Limiter (использует Redis для распределенного rate limiting)
    single<RateLimitMiddleware> {
        RateLimitMiddleware(get<RedisCoroutinesCommands<String, String>>())
    }

    // FFmpeg Service
    single<FfmpegService> { FfmpegService() }

    // HLS Generator Service
    single<HlsGeneratorService> {
        HlsGeneratorService(
            ffmpegService = get(),
            hlsOutputDirectory = "streams/hls"
        )
    }

    // Video Recording Service
    single<VideoRecordingService> {
        VideoRecordingService(
            recordingRepository = get(),
            recordingsDirectory = "recordings",
            thumbnailsDirectory = "thumbnails",
            ffmpegService = get()
        )
    }

    // Screenshot Service
    single<ScreenshotService> {
        ScreenshotService(
            screenshotsDirectory = "screenshots"
        )
    }

    // Video Stream Service
    single<VideoStreamService> {
        VideoStreamService(
            cameraRepository = get(),
            hlsGeneratorService = get(),
            streamsDirectory = "streams",
            maxBufferSize = 50, // Максимальное количество кадров в буфере
            streamTimeoutMinutes = 30 // Таймаут неактивного стрима в минутах
        )
    }

    // Notification Service
    single<NotificationService> {
        NotificationService(
            notificationRepository = get(),
            sendNotificationUseCase = get()
        )
    }

    // Event Service (создается до CameraService, так как CameraService зависит от него)
    single<EventService> {
        EventService(
            eventRepository = get(),
            notificationService = get()
        )
    }

    // Camera Service
    single<CameraService> {
        CameraService(
            cameraRepository = get(),
            notificationService = get(),
            eventService = get(),
            monitoringIntervalMinutes = 5, // Проверка каждые 5 минут
            offlineThresholdMinutes = 10 // Считается offline после 10 минут недоступности
        )
    }
}


