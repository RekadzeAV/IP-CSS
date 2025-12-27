package com.company.ipcamera.shared.data.di

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.api.*
import com.company.ipcamera.shared.data.datasource.local.*
import com.company.ipcamera.shared.data.datasource.local.impl.*
import com.company.ipcamera.shared.data.datasource.remote.*
import com.company.ipcamera.shared.data.datasource.remote.impl.*
import com.company.ipcamera.shared.data.local.DatabaseFactory
import org.koin.core.module.dsl.single
import org.koin.dsl.module

/**
 * Koin модуль для Data Sources
 *
 * Этот модуль должен быть добавлен в AppModule для использования Data Sources.
 * RemoteDataSource опциональны и создаются только если ApiClient доступен.
 */
val dataSourcesModule = module {
    // Local Data Sources
    single<CameraLocalDataSource> { CameraLocalDataSourceImpl(get<DatabaseFactory>()) }
    single<RecordingLocalDataSource> { RecordingLocalDataSourceImpl(get<DatabaseFactory>()) }
    single<EventLocalDataSource> { EventLocalDataSourceImpl(get<DatabaseFactory>()) }
    single<UserLocalDataSource> { UserLocalDataSourceImpl(get<DatabaseFactory>()) }
    single<SettingsLocalDataSource> { SettingsLocalDataSourceImpl(get<DatabaseFactory>()) }
    single<NotificationLocalDataSource> { NotificationLocalDataSourceImpl(get<DatabaseFactory>()) }

    // Remote Data Sources (опционально, только если ApiClient и API сервисы доступны)
    // Используем nullable типы, чтобы модуль работал и на сервере (где RemoteDataSource не нужны)

    // Camera Remote Data Source
    single<CameraRemoteDataSource?> {
        try {
            val cameraApiService: CameraApiService = get<CameraApiService>()
            CameraRemoteDataSourceImpl(cameraApiService)
        } catch (e: Exception) {
            null // ApiClient или CameraApiService недоступны
        }
    }

    // Recording Remote Data Source
    single<RecordingRemoteDataSource?> {
        try {
            val recordingApiService: RecordingApiService = get<RecordingApiService>()
            RecordingRemoteDataSourceImpl(recordingApiService)
        } catch (e: Exception) {
            null
        }
    }

    // Event Remote Data Source
    single<EventRemoteDataSource?> {
        try {
            val eventApiService: EventApiService = get<EventApiService>()
            EventRemoteDataSourceImpl(eventApiService)
        } catch (e: Exception) {
            null
        }
    }

    // User Remote Data Source
    single<UserRemoteDataSource?> {
        try {
            val userApiService: UserApiService = get<UserApiService>()
            UserRemoteDataSourceImpl(userApiService)
        } catch (e: Exception) {
            null
        }
    }

    // Settings Remote Data Source
    single<SettingsRemoteDataSource?> {
        try {
            val settingsApiService: SettingsApiService = get<SettingsApiService>()
            SettingsRemoteDataSourceImpl(settingsApiService)
        } catch (e: Exception) {
            null
        }
    }

    // Notification Remote Data Source
    single<NotificationRemoteDataSource?> {
        try {
            val apiClient: ApiClient = get<ApiClient>()
            NotificationRemoteDataSourceImpl(apiClient)
        } catch (e: Exception) {
            null
        }
    }
}
