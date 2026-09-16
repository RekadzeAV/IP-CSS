package com.company.ipcamera.shared.domain.di

import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.LicensePlateRepository
import com.company.ipcamera.shared.domain.service.AnalyticsFrameProcessorFactory
import com.company.ipcamera.shared.domain.service.AnalyticsService
import com.company.ipcamera.shared.domain.usecase.AnalyzeVideoUseCase
import com.company.ipcamera.shared.domain.usecase.DetectFacesUseCase
import com.company.ipcamera.shared.domain.usecase.DetectMotionUseCase
import com.company.ipcamera.shared.domain.usecase.DetectObjectsUseCase
import com.company.ipcamera.shared.domain.usecase.RecognizeLicensePlateUseCase
import com.company.ipcamera.shared.domain.usecase.TrackObjectsUseCase
import org.koin.dsl.module

/**
 * Koin модуль для Use Cases аналитики (доменный слой).
 *
 * Регистрирует все Use Cases аналитики:
 * - [DetectMotionUseCase] — детекция движения по кадру (пороги, зоны, события)
 * - [DetectObjectsUseCase] — детекция объектов (person/vehicle/…) по кадру
 * - [TrackObjectsUseCase] — трекинг объектов между кадрами (IoU, события вход/выход)
 * - [RecognizeLicensePlateUseCase] — распознавание номеров (ANPR), сохранение в БД и события
 * - [DetectFacesUseCase] — детекция лиц на кадре
 * - [AnalyzeVideoUseCase] — комплексный анализ кадра (движение + объекты + лица + ANPR)
 * - [AnalyticsFrameProcessorFactory] — фабрика [AnalyticsFrameProcessor] по камере (single)
 *
 * Использование: `includes(analyticsUseCasesModule)`
 *
 * Зависимости:
 * - AnalyticsService (обязательно)
 * - EventRepository (опционально)
 * - LicensePlateRepository (опционально, для RecognizeLicensePlateUseCase)
 * - NotificationService (опционально, для AnalyzeVideoUseCase)
 */
val analyticsUseCasesModule =
    module {
        // DetectMotionUseCase
        factory<DetectMotionUseCase> {
            DetectMotionUseCase(
                analyticsService = get<AnalyticsService>(),
                eventRepository =
                    try {
                        get<EventRepository>()
                    } catch (e: Exception) {
                        null
                    },
            )
        }

        // DetectObjectsUseCase
        factory<DetectObjectsUseCase> {
            DetectObjectsUseCase(
                analyticsService = get<AnalyticsService>(),
                eventRepository =
                    try {
                        get<EventRepository>()
                    } catch (e: Exception) {
                        null
                    },
            )
        }

        // TrackObjectsUseCase
        factory<TrackObjectsUseCase> {
            TrackObjectsUseCase(
                eventRepository =
                    try {
                        get<EventRepository>()
                    } catch (e: Exception) {
                        null
                    },
            )
        }

        // RecognizeLicensePlateUseCase
        factory<RecognizeLicensePlateUseCase> {
            RecognizeLicensePlateUseCase(
                analyticsService = get<AnalyticsService>(),
                eventRepository =
                    try {
                        get<EventRepository>()
                    } catch (e: Exception) {
                        null
                    },
                licensePlateRepository =
                    try {
                        get<LicensePlateRepository>()
                    } catch (e: Exception) {
                        null
                    },
            )
        }

        // DetectFacesUseCase
        factory<DetectFacesUseCase> {
            DetectFacesUseCase(
                analyticsService = get<AnalyticsService>(),
                eventRepository =
                    try {
                        get<EventRepository>()
                    } catch (e: Exception) {
                        null
                    },
            )
        }

        // AnalyzeVideoUseCase (зависит от всех остальных Use Cases аналитики)
        factory<AnalyzeVideoUseCase> {
            AnalyzeVideoUseCase(
                detectMotionUseCase = get<DetectMotionUseCase>(),
                detectObjectsUseCase = get<DetectObjectsUseCase>(),
                detectFacesUseCase = get<DetectFacesUseCase>(),
                recognizeLicensePlateUseCase = get<RecognizeLicensePlateUseCase>(),
                eventRepository =
                    try {
                        get<EventRepository>()
                    } catch (e: Exception) {
                        null
                    },
                notificationService =
                    try {
                        get<com.company.ipcamera.shared.domain.service.NotificationService>()
                    } catch (
                        e: Exception,
                    ) {
                        null
                    },
            )
        }

        /** Единая фабрика кадровой аналитики для клиентов (Desktop/Android и т.д.). */
        single<AnalyticsFrameProcessorFactory> {
            AnalyticsFrameProcessorFactory(
                detectMotionUseCase = get(),
                detectObjectsUseCase = get(),
                detectFacesUseCase = get(),
                recognizeLicensePlateUseCase = get(),
            )
        }
    }
