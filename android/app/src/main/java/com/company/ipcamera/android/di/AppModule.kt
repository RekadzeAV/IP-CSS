package com.company.ipcamera.android.di

import android.content.Context
import com.company.ipcamera.core.license.LicenseManager
import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiClientConfig
import com.company.ipcamera.core.network.api.*
import com.company.ipcamera.core.network.api.StreamApiService
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.data.repository.*
import com.company.ipcamera.shared.domain.repository.*
import com.company.ipcamera.android.ui.viewmodel.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Context
    single<Context> { androidContext() }

    // Database Factory
    single<DatabaseFactory> { DatabaseFactory() }

    // Database
    single { createDatabase(get<DatabaseFactory>().createDriver()) }

    // API Client Configuration
    // TODO: Get base URL from BuildConfig or SharedPreferences
    single {
        ApiClientConfig.default(baseUrl = "http://localhost:8080")
    }

    // API Client
    single {
        ApiClient.create(get<ApiClientConfig>())
    }

    // API Services
    single<CameraApiService> { CameraApiService(get()) }
    single<EventApiService> { EventApiService(get()) }
    single<RecordingApiService> { RecordingApiService(get()) }
    single<SettingsApiService> { SettingsApiService(get()) }
    single<LicenseApiService> { LicenseApiService(get()) }
    single<UserApiService> { UserApiService(get()) }
    single<StreamApiService> { StreamApiService(get()) }

    // License Manager
    single<LicenseManager> { LicenseManager.getInstance() }

    // Repositories
    single<CameraRepository> { CameraRepositoryImpl(get()) }
    single<EventRepository> { EventRepositoryImpl(get()) }
    single<RecordingRepository> { RecordingRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<LicenseRepository> { LicenseRepositoryImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }

    // ViewModels
    viewModel { CameraListViewModel(get()) }
    viewModel { (cameraId: String) -> CameraDetailViewModel(get(), cameraId) }
    viewModel { CameraAddViewModel(get()) }
    viewModel { (cameraId: String) -> VideoViewViewModel(get(), get(), cameraId) }
    viewModel { RecordingsViewModel(get()) }
    viewModel { EventsViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { LicenseViewModel(get()) }
}

