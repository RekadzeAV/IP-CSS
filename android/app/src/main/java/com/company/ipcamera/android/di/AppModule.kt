package com.company.ipcamera.android.di

import android.content.Context
// import com.company.ipcamera.core.license.LicenseManager // Отложено: лицензирование вынесено за рамки проекта
import com.company.ipcamera.android.media.ApiClientCookieJar
import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiClientConfig
import com.company.ipcamera.core.network.security.CertificatePinningConfigLoader
import com.company.ipcamera.core.network.security.CertificatePinningManager
import com.company.ipcamera.core.network.api.*
import com.company.ipcamera.core.network.api.StreamApiService
import com.company.ipcamera.shared.data.di.dataSourcesModule
import com.company.ipcamera.shared.data.di.repositoriesModule
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabaseSync
import com.company.ipcamera.shared.domain.di.analyticsUseCasesModule
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.service.AnalyticsServiceImpl
import com.company.ipcamera.android.ui.viewmodel.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import org.koin.dsl.module

val appModule = module {
    // Context
    single<Context> { androidContext() }

    // Certificate pinning: инициализация загрузчика конфига (assets / filesDir)
    single(createdAtStart = true) {
        CertificatePinningManager.loadConfig()
        Unit
    }

    // Database Factory
    single<DatabaseFactory> { DatabaseFactory(get<Context>()) }

    // Database
    single { createDatabaseSync(get<DatabaseFactory>().createDriver()) }

    // API Client Configuration (pinning из config/certificate-pins.json при наличии)
    single {
        ApiClientConfig.default(baseUrl = "http://localhost:8080", requireHttps = false)
            .copy(certificatePinningConfig = CertificatePinningManager.loadConfig())
    }

    // API Client
    single {
        ApiClient.create(get<ApiClientConfig>())
    }

    // OkHttp с куками из Ktor — для ExoPlayer HLS (плейлист и .ts под jwt-auth)
    single<OkHttpClient> {
        OkHttpClient.Builder()
            .cookieJar(ApiClientCookieJar(get()))
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // API Services
    single<CameraApiService> { CameraApiService(get()) }
    single<EventApiService> { EventApiService(get()) }
    single<RecordingApiService> { RecordingApiService(get()) }
    single<SettingsApiService> { SettingsApiService(get()) }
    // single<LicenseApiService> { LicenseApiService(get()) } // Отложено: лицензирование вынесено за рамки проекта
    single<UserApiService> { UserApiService(get()) }
    single<StreamApiService> { StreamApiService(get()) }

    // License Manager - Отложено: лицензирование вынесено за рамки проекта
    // single<LicenseManager> { LicenseManager.getInstance() }

    // Data Sources Module (должен быть перед repositoriesModule)
    includes(dataSourcesModule)

    // Repositories Module (использует Data Sources)
    includes(repositoriesModule)

    // Analytics Service
    single<AnalyticsService> {
        AnalyticsServiceImpl()
    }

    // Analytics Use Cases Module
    includes(analyticsUseCasesModule)

    // ONVIF Services Module
    includes(com.company.ipcamera.shared.domain.di.onvifServicesModule)

    // ViewModels
    viewModel { CameraListViewModel(get()) }
    viewModel { (cameraId: String) -> CameraDetailViewModel(get(), cameraId) }
    viewModel { CameraAddViewModel(get()) }
    viewModel { (cameraId: String) -> VideoViewViewModel(get(), get(), get(), cameraId) }
    viewModel { RecordingsViewModel(get()) }
    viewModel { EventsViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { NotificationsViewModel(get()) }
    // viewModel { LicenseViewModel(get()) } // Отложено: лицензирование вынесено за рамки проекта

    // Services
    single { com.company.ipcamera.android.service.ServiceManager(get<Context>()) }
}



