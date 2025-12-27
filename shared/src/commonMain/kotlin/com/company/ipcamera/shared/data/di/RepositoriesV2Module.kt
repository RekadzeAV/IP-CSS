package com.company.ipcamera.shared.data.di

import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.shared.data.datasource.local.*
import com.company.ipcamera.shared.data.datasource.remote.*
import com.company.ipcamera.shared.data.repository.*
import com.company.ipcamera.shared.domain.repository.*
import org.koin.core.module.dsl.single
import org.koin.dsl.module

/**
 * Koin модуль для репозиториев V2 (с использованием Data Sources)
 *
 * Этот модуль должен быть добавлен в AppModule после dataSourcesModule.
 * Репозитории V2 используют LocalDataSource и опционально RemoteDataSource.
 */
val repositoriesV2Module = module {
    // Camera Repository V2
    single<CameraRepository> {
        val localDataSource: CameraLocalDataSource = get<CameraLocalDataSource>()
        val remoteDataSource: CameraRemoteDataSource? = try { get<CameraRemoteDataSource?>() } catch (e: Exception) { null }
        CameraRepositoryImplV2(localDataSource, remoteDataSource)
    }

    // Recording Repository V2
    single<RecordingRepository> {
        val localDataSource: RecordingLocalDataSource = get<RecordingLocalDataSource>()
        val remoteDataSource: RecordingRemoteDataSource? = try { get<RecordingRemoteDataSource?>() } catch (e: Exception) { null }
        RecordingRepositoryImplV2(localDataSource, remoteDataSource)
    }

    // Event Repository V2
    single<EventRepository> {
        val localDataSource: EventLocalDataSource = get<EventLocalDataSource>()
        val remoteDataSource: EventRemoteDataSource? = try { get<EventRemoteDataSource?>() } catch (e: Exception) { null }
        EventRepositoryImplV2(localDataSource, remoteDataSource)
    }

    // User Repository V2
    single<UserRepository> {
        val localDataSource: UserLocalDataSource = get<UserLocalDataSource>()
        val remoteDataSource: UserRemoteDataSource? = try { get<UserRemoteDataSource?>() } catch (e: Exception) { null }
        val userApiService: UserApiService? = try { get<UserApiService>() } catch (e: Exception) { null }
        UserRepositoryImplV2(localDataSource, remoteDataSource, userApiService)
    }

    // Settings Repository V2
    single<SettingsRepository> {
        val localDataSource: SettingsLocalDataSource = get<SettingsLocalDataSource>()
        val remoteDataSource: SettingsRemoteDataSource? = try { get<SettingsRemoteDataSource?>() } catch (e: Exception) { null }
        SettingsRepositoryImplV2(localDataSource, remoteDataSource)
    }

    // Notification Repository V2
    single<NotificationRepository> {
        val localDataSource: NotificationLocalDataSource = get<NotificationLocalDataSource>()
        val remoteDataSource: NotificationRemoteDataSource? = try { get<NotificationRemoteDataSource?>() } catch (e: Exception) { null }
        NotificationRepositoryImplV2(localDataSource, remoteDataSource)
    }
}

