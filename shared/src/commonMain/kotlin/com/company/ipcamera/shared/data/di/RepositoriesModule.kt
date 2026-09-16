package com.company.ipcamera.shared.data.di

import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.shared.data.datasource.local.*
import com.company.ipcamera.shared.data.datasource.remote.*
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.repository.*
import com.company.ipcamera.shared.domain.repository.*
import org.koin.core.scope.Scope
import org.koin.dsl.module

private inline fun <reified T : Any> Scope.safeGetOptional(): T? = runCatching { getOrNull<T>() }.getOrNull()

/**
 * Koin модуль для репозиториев (с использованием Data Sources)
 *
 * Этот модуль должен быть добавлен в AppModule после dataSourcesModule.
 * Репозитории используют LocalDataSource и опционально RemoteDataSource.
 */
val repositoriesModule =
    module {
        // Camera Repository
        single<CameraRepository> {
            val localDataSource: CameraLocalDataSource = get<CameraLocalDataSource>()
            val remoteDataSource: CameraRemoteDataSource? = safeGetOptional<CameraRemoteDataSource>()
            val settingsRepository: SettingsRepository = get()
            CameraRepositoryImpl(localDataSource, remoteDataSource, settingsRepository)
        }

        // Recording Repository
        single<RecordingRepository> {
            val localDataSource: RecordingLocalDataSource = get<RecordingLocalDataSource>()
            val remoteDataSource: RecordingRemoteDataSource? = safeGetOptional<RecordingRemoteDataSource>()
            RecordingRepositoryImpl(localDataSource, remoteDataSource)
        }

        // Event Repository
        single<EventRepository> {
            val localDataSource: EventLocalDataSource = get<EventLocalDataSource>()
            val remoteDataSource: EventRemoteDataSource? = safeGetOptional<EventRemoteDataSource>()
            EventRepositoryImpl(localDataSource, remoteDataSource)
        }

        // User Repository
        single<UserRepository> {
            val localDataSource: UserLocalDataSource = get<UserLocalDataSource>()
            val remoteDataSource: UserRemoteDataSource? = safeGetOptional<UserRemoteDataSource>()
            val userApiService: UserApiService? =
                try {
                    get<UserApiService>()
                } catch (e: Exception) {
                    null
                }
            UserRepositoryImpl(localDataSource, remoteDataSource, userApiService)
        }

        // Settings Repository
        single<SettingsRepository> {
            val localDataSource: SettingsLocalDataSource = get<SettingsLocalDataSource>()
            val remoteDataSource: SettingsRemoteDataSource? = safeGetOptional<SettingsRemoteDataSource>()
            SettingsRepositoryImpl(localDataSource, remoteDataSource)
        }

        // Notification Repository
        single<NotificationRepository> {
            val localDataSource: NotificationLocalDataSource = get<NotificationLocalDataSource>()
            val remoteDataSource: NotificationRemoteDataSource? = safeGetOptional<NotificationRemoteDataSource>()
            NotificationRepositoryImpl(localDataSource, remoteDataSource)
        }

        // License Plate Repository (ANPR Phase 5) — SQLDelight, требует DatabaseFactory
        single<LicensePlateRepository> {
            LicensePlateRepositoryImplSqlDelight(get<DatabaseFactory>())
        }

        // Face Repository (Face Recognition, Phase 3) — SQLDelight, требует DatabaseFactory
        single<FaceRepository> {
            FaceRepositoryImplSqlDelight(get<DatabaseFactory>())
        }
    }
