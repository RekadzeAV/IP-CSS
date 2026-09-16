package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.DetectFacesUseCase
import com.company.ipcamera.shared.domain.usecase.DetectMotionUseCase
import com.company.ipcamera.shared.domain.usecase.DetectObjectsUseCase
import com.company.ipcamera.shared.domain.usecase.RecognizeLicensePlateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Создаёт [AnalyticsFrameProcessor] для выбранной камеры с общими зависимостями аналитики (KMP / Koin).
 * Флаги включения движения, объектов, лиц и ANPR читаются из [Camera.settings.analytics].
 */
class AnalyticsFrameProcessorFactory(
    private val detectMotionUseCase: DetectMotionUseCase,
    private val detectObjectsUseCase: DetectObjectsUseCase,
    private val detectFacesUseCase: DetectFacesUseCase,
    private val recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase,
    private val vsaasIngestClient: VsaasAnalyticsIngestClient = NoOpVsaasAnalyticsIngestClient(),
) {
    fun create(
        camera: Camera,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        motionThrottleMs: Long = 5000L,
        objectThrottleMs: Long = 60_000L,
        faceThrottleMs: Long = 3000L,
        licensePlateThrottleMs: Long = 3000L,
    ): AnalyticsFrameProcessor =
        AnalyticsFrameProcessor(
            camera = camera,
            detectMotionUseCase = detectMotionUseCase,
            detectObjectsUseCase = detectObjectsUseCase,
            detectFacesUseCase = detectFacesUseCase,
            recognizeLicensePlateUseCase = recognizeLicensePlateUseCase,
            motionThrottleMs = motionThrottleMs,
            objectThrottleMs = objectThrottleMs,
            faceThrottleMs = faceThrottleMs,
            licensePlateThrottleMs = licensePlateThrottleMs,
            vsaasIngestClient = vsaasIngestClient,
            scope = scope,
        )
}
