package com.company.ipcamera.server.di

import com.company.ipcamera.server.middleware.RateLimitMiddleware
import com.company.ipcamera.server.repository.ServerUserRepository
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.data.repository.CameraRepositoryImpl
import com.company.ipcamera.shared.domain.repository.CameraRepository
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
    
    // Rate Limiter
    single<RateLimitMiddleware> { RateLimitMiddleware() }
}

