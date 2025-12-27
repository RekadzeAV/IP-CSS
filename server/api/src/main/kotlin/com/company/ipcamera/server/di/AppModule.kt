package com.company.ipcamera.server.di

import com.company.ipcamera.server.config.RedisConfig
import com.company.ipcamera.server.middleware.RateLimitMiddleware
import com.company.ipcamera.server.repository.ServerUserRepository
import com.company.ipcamera.server.service.FfmpegService
import com.company.ipcamera.server.service.HlsGeneratorService
import com.company.ipcamera.server.service.ScreenshotService
import com.company.ipcamera.server.service.VideoRecordingService
import com.company.ipcamera.server.service.VideoStreamService
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.data.repository.CameraRepositoryImpl
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import org.koin.dsl.module

val appModule = module {
    // Database Factory
    single<DatabaseFactory> { DatabaseFactory() }

    // Database
    single { createDatabase(get<DatabaseFactory>().createDriver()) }

    // Repositories
    single<CameraRepository> { CameraRepositoryImpl(get()) }

    // Server User Repository (in-memory для MVP)
    single<ServerUserRepository> { ServerUserRepository() }

    // Server Recording Repository (SQLDelight для продакшена)
    single<com.company.ipcamera.server.repository.ServerRecordingRepositorySqlDelight> {
        com.company.ipcamera.server.repository.ServerRecordingRepositorySqlDelight(get())
    }
    single<com.company.ipcamera.shared.domain.repository.RecordingRepository> {
        get<com.company.ipcamera.server.repository.ServerRecordingRepositorySqlDelight>()
    }

    // Server Event Repository (in-memory для MVP)
    single<com.company.ipcamera.server.repository.ServerEventRepository> {
        com.company.ipcamera.server.repository.ServerEventRepository()
    }
    single<com.company.ipcamera.shared.domain.repository.EventRepository> {
        get<com.company.ipcamera.server.repository.ServerEventRepository>()
    }

    // Server Settings Repository (in-memory для MVP)
    single<com.company.ipcamera.server.repository.ServerSettingsRepository> {
        com.company.ipcamera.server.repository.ServerSettingsRepository()
    }
    single<com.company.ipcamera.shared.domain.repository.SettingsRepository> {
        get<com.company.ipcamera.server.repository.ServerSettingsRepository>()
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
}


