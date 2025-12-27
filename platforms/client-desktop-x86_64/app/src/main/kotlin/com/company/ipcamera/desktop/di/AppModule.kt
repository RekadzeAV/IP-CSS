package com.company.ipcamera.desktop.di

import com.company.ipcamera.desktop.ui.viewmodel.CamerasViewModel
import com.company.ipcamera.desktop.ui.viewmodel.LiveViewViewModel
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.data.repository.*
import com.company.ipcamera.shared.domain.repository.*
import com.company.ipcamera.shared.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // ViewModels
    factory { CamerasViewModel(get(), get(), get(), CoroutineScope(Dispatchers.Main)) }
    factory { LiveViewViewModel(get(), CoroutineScope(Dispatchers.Main)) }
}

