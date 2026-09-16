package com.company.ipcamera.shared.domain.di

import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.core.network.onvif.OnvifEventService
import com.company.ipcamera.core.network.onvif.OnvifEventServiceImpl
import com.company.ipcamera.shared.common.createHttpClientEngine
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.service.OnvifEventIntegrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * Koin модуль для ONVIF сервисов
 *
 * Предоставляет:
 * - OnvifClient для работы с ONVIF камерами
 * - OnvifEventService для работы с событиями
 * - OnvifEventIntegrationService для автоматической интеграции событий
 */
val onvifServicesModule =
    module {
        // HTTP Client Engine для ONVIF
        single {
            createHttpClientEngine()
        }

        // OnvifClient
        single<OnvifClient> {
            val engine = get<io.ktor.client.engine.HttpClientEngine>()
            OnvifClient(engine)
        }

        // OnvifEventService
        single<OnvifEventService> {
            val engine = get<io.ktor.client.engine.HttpClientEngine>()
            OnvifEventServiceImpl(engine)
        }

        // OnvifEventIntegrationService
        single<OnvifEventIntegrationService> {
            val eventService = get<OnvifEventService>()
            val eventRepository = get<EventRepository>()
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            OnvifEventIntegrationService(
                eventService = eventService,
                eventRepository = eventRepository,
                scope = scope,
            )
        }

        // CameraEventMonitoringService
        single<com.company.ipcamera.shared.domain.service.CameraEventMonitoringService> {
            val cameraRepository = get<com.company.ipcamera.shared.domain.repository.CameraRepository>()
            val eventIntegrationService = get<OnvifEventIntegrationService>()
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            com.company.ipcamera.shared.domain.service.CameraEventMonitoringService(
                cameraRepository = cameraRepository,
                eventIntegrationService = eventIntegrationService,
                scope = scope,
            )
        }
    }
