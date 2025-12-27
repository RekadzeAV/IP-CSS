package com.company.ipcamera.desktop.di

import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.data.repository.*
import com.company.ipcamera.shared.domain.repository.*
import com.company.ipcamera.shared.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    // Database
    single { DatabaseFactory(null) }
    single { createDatabase(get<DatabaseFactory>().createDriver()) }
    
    // Repositories (используем только локальные SQLDelight реализации для начала)
    single<CameraRepository> { CameraRepositoryImpl(get()) }
    single<EventRepository> { EventRepositoryImplSqlDelight(get()) }
    single<RecordingRepository> { RecordingRepositoryImplSqlDelight(get()) }
    single<SettingsRepository> { SettingsRepositoryImplSqlDelight(get()) }
    single<NotificationRepository> { NotificationRepositoryImplSqlDelight(get()) }
    
    // Use Cases
    factoryOf(::GetCamerasUseCase)
    factoryOf(::GetCameraByIdUseCase)
    factoryOf(::AddCameraUseCase)
    factoryOf(::UpdateCameraUseCase)
    factoryOf(::DeleteCameraUseCase)
    factoryOf(::DiscoverCamerasUseCase)
    factoryOf(::DiscoverAndAddCameraUseCase)
    factoryOf(::AddDiscoveredCameraUseCase)
    factoryOf(::TestDiscoveredCameraUseCase)
    factoryOf(::GetRecordingsUseCase)
    factoryOf(::DeleteRecordingUseCase)
}

