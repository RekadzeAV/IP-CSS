package com.company.ipcamera.desktop.di

import com.company.ipcamera.shared.data.di.dataSourcesModule
import com.company.ipcamera.shared.data.di.repositoriesModule
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.domain.di.analyticsUseCasesModule
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.AnalyticsServiceImpl
import com.company.ipcamera.shared.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val appModule = module {
    // Database
    single { DatabaseFactory(null) }
    single { createDatabaseSync(get<DatabaseFactory>().createDriver()) }

    // Data Sources Module (должен быть перед repositoriesModule)
    // На Desktop RemoteDataSource будут null, так как ApiClient не настроен
    includes(dataSourcesModule)

    // Repositories Module (использует Data Sources)
    // Использует local-first стратегию, работает только с локальной БД на Desktop
    includes(repositoriesModule)

    // Analytics Service
    single<AnalyticsService> {
        AnalyticsServiceImpl()
    }

    // Analytics Use Cases Module
    includes(analyticsUseCasesModule)

    // ONVIF Services Module
    includes(com.company.ipcamera.shared.domain.di.onvifServicesModule)

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

