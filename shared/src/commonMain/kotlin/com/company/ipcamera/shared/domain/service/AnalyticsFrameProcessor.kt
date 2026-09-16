package com.company.ipcamera.shared.domain.service

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.common.nowMillis
import com.company.ipcamera.shared.domain.model.AnalyticsExecutionLocation
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.DetectFacesUseCase
import com.company.ipcamera.shared.domain.usecase.DetectMotionUseCase
import com.company.ipcamera.shared.domain.usecase.DetectObjectsUseCase
import com.company.ipcamera.shared.domain.usecase.RecognizeLicensePlateUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Обработчик кадров для аналитики: движение и опционально объекты, лица, ANPR.
 * Хранит предыдущий кадр для детекции движения, применяет троттлинг событий (дедупликация).
 */
class AnalyticsFrameProcessor(
    private val camera: Camera,
    private val detectMotionUseCase: DetectMotionUseCase,
    private val detectObjectsUseCase: DetectObjectsUseCase? = null,
    private val detectFacesUseCase: DetectFacesUseCase? = null,
    private val recognizeLicensePlateUseCase: RecognizeLicensePlateUseCase? = null,
    /** Минимальный интервал между событиями движения (мс). Рекомендуется брать из camera.settings.analytics.motionEventCooldownMs. */
    private val motionThrottleMs: Long = 5000L,
    private val objectThrottleMs: Long = 60_000L,
    /** Интервал между событиями детекции лиц (мс). */
    private val faceThrottleMs: Long = 3000L,
    /** Интервал между событиями ANPR (мс). */
    private val licensePlateThrottleMs: Long = 3000L,
    private val vsaasIngestClient: VsaasAnalyticsIngestClient? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) {
    private var previousFrame: ByteArray? = null
    private var lastMotionEventTime: Long = 0L
    private var lastObjectEventTime: Long = 0L
    private var lastFaceEventTime: Long = 0L
    private var lastLicensePlateEventTime: Long = 0L

    /**
     * Обработать один кадр: детекция движения и при включённых настройках камеры — объектов, лиц, номеров.
     * События создаются с троттлингом.
     */
    fun processFrame(
        frameData: ByteArray,
        width: Int,
        height: Int,
    ) {
        scope.launch {
            val cameraForFrame = camera.copy(resolution = Resolution(width, height))
            val now = nowMillis()
            val analytics = camera.settings.analytics

            // MVP-flow для VSaaS: кадр уходит в облачный ingest, локальная аналитика не запускается.
            if (analytics.executionLocation == AnalyticsExecutionLocation.VSAAS && vsaasIngestClient != null) {
                vsaasIngestClient.submitFrame(
                    cameraId = camera.id,
                    timestampMs = now,
                    width = width,
                    height = height,
                    frameData = frameData,
                )
                previousFrame = frameData.copyOf()
                return@launch
            }

            // Детекция движения (порог и зоны из настроек камеры, троттлинг — без дубликатов событий)
            val motionCooldownMs = camera.settings.analytics.motionEventCooldownMs
            val throttleMs = if (motionCooldownMs > 0L) motionCooldownMs else motionThrottleMs
            val createMotionEvent = (now - lastMotionEventTime) >= throttleMs
            detectMotionUseCase(
                camera = cameraForFrame,
                frameData = frameData,
                previousFrameData = previousFrame,
                zones = camera.settings.analytics.zones,
                threshold = camera.settings.analytics.motionThreshold,
                createEvent = createMotionEvent,
            ).onSuccess { result ->
                if (result.detected && createMotionEvent) lastMotionEventTime = now
            }

            previousFrame = frameData.copyOf()

            // Детекция объектов (если включена и use case передан)
            if (detectObjectsUseCase != null && camera.settings.analytics.objectDetection) {
                val createObjectEvent = (now - lastObjectEventTime) >= objectThrottleMs
                detectObjectsUseCase(
                    camera = cameraForFrame,
                    frameData = frameData,
                    objectTypes = camera.settings.analytics.objectTypes,
                    minConfidence = camera.settings.analytics.objectDetectionConfidenceThreshold,
                    createEvent = createObjectEvent,
                ).onSuccess { result ->
                    if (result.objects.isNotEmpty() && createObjectEvent) lastObjectEventTime = now
                }
            }

            if (detectFacesUseCase != null && analytics.faceRecognition) {
                val createFaceEvent = (now - lastFaceEventTime) >= faceThrottleMs
                detectFacesUseCase(
                    camera = cameraForFrame,
                    frameData = frameData,
                    minConfidence = analytics.faceRecognitionConfidenceThreshold,
                    includeLandmarks = false,
                    includeEmbeddings = false,
                    createEvent = createFaceEvent,
                ).onSuccess { result ->
                    if (result.faces.isNotEmpty() && createFaceEvent) lastFaceEventTime = now
                }
            }

            if (recognizeLicensePlateUseCase != null && analytics.anprEnabled) {
                val createPlateEvent = (now - lastLicensePlateEventTime) >= licensePlateThrottleMs
                recognizeLicensePlateUseCase(
                    camera = cameraForFrame,
                    frameData = frameData,
                    minConfidence = analytics.anprConfidenceThreshold,
                    country = analytics.anprLanguage,
                    createEvent = createPlateEvent,
                ).onSuccess { result ->
                    if (result.plates.isNotEmpty() && createPlateEvent) lastLicensePlateEventTime = now
                }
            }
        }
    }

    /** Отмена фоновых корутин обработки (при остановке камеры / выходе из экрана). */
    fun dispose() {
        scope.coroutineContext[Job]?.cancel()
    }
}
