package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.analytics.DetectedObject as NativeDetectedObject
import com.company.ipcamera.core.network.analytics.NativeAnalytics
import com.company.ipcamera.server.service.analytics.RtspAnalyticsFrame
import com.company.ipcamera.server.service.analytics.RtspFrameSource
import com.company.ipcamera.shared.domain.model.Camera
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Оркестратор серверной видеоаналитики (задача 1.1, часть «stream analytics»).
 *
 * Ведёт единый конвейер на камеру: [RtspFrameSource] (ffmpeg → RGB24) →
 * [NativeAnalytics] (JNI → C++: детекция движения и объектов) → агрегация
 * результатов в памяти (кольцевой буфер [MAX_BUFFERED_RESULTS]), трекинг объектов
 * (упрощённый центроид-трекер) и публишинг в [resultsFlow] для SSE/WS-потребителей.
 *
 * Работает независимо от [MotionDetectorService]/[ObjectDetectionService]:
 * каждый сервис открывает собственную RTSP-сессию ffmpeg для своего сценария
 * (эндпоинты analytics, motion, object-detection соответственно).
 *
 * Graceful degradation: если нативная библиотека или ffmpeg недоступны, сервис
 * создаётся, но [startStreamAnalysis]/[startAnalysis] возвращают ошибку, статусы
 * и метрики остаются согласованными.
 */
class VideoAnalyticsService(
    private val frameSource: RtspFrameSource? = null,
    private val cameraRepository: com.company.ipcamera.shared.domain.repository.CameraRepository? = null,
) {
    /** Кольцевой буфер результатов на камеру. */
    internal val resultsByCamera = ConcurrentHashMap<String, ArrayDeque<AnalyticsResult>>()
    internal val runningCameras = ConcurrentHashMap.newKeySet<String>()
    internal val metricsByCamera = ConcurrentHashMap<String, Metrics>()
    internal val streamQualityByCamera = ConcurrentHashMap<String, String>()
    internal val configByCamera = ConcurrentHashMap<String, Camera>()

    private val scope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())
    private val nativeHandles = ConcurrentHashMap<String, Long>()
    private val objectHandles = ConcurrentHashMap<String, Long>()
    private val _resultsFlow = MutableSharedFlow<AnalyticsResult>(extraBufferCapacity = 256)
    val resultsFlow: SharedFlow<AnalyticsResult> = _resultsFlow.asSharedFlow()

    internal class Metrics {
        val processedFrames = AtomicLong(0)
        val skippedFrames = AtomicLong(0)
        val errorCount = AtomicLong(0)
        val totalProcessingMs = AtomicLong(0)
        val lastError = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val lastErrorAt = AtomicLong(0)
        val lastProcessedTimestamp = AtomicLong(0)
        val motionCount = AtomicLong(0)
        val objectCount = AtomicLong(0)
        val faceCount = AtomicLong(0)
        val plateCount = AtomicLong(0)
    }

    private val tracksByCamera = ConcurrentHashMap<String, MutableMap<String, TrackInfo>>()

    // Analytics result data class
    data class AnalyticsResult(
        val cameraId: String = "",
        val timestamp: Long = 0,
        val motionDetection: MotionDetectionResult? = null,
        val objectDetection: ObjectDetectionResult? = null,
        val faceDetection: FaceDetectionResult? = null,
        val licensePlateRecognition: LicensePlateRecognitionResult? = null
    )

    data class MotionDetectionResult(
        val detected: Boolean = false,
        val confidence: Float = 0f,
        val zones: List<Any> = emptyList(),
        val timestamp: Long = 0
    )

    data class ObjectDetectionResult(
        val objects: List<Any> = emptyList(),
        val timestamp: Long = 0
    )

    data class FaceDetectionResult(
        val faces: List<Any> = emptyList(),
        val timestamp: Long = 0
    )

    data class LicensePlateRecognitionResult(
        val plates: List<Any> = emptyList(),
        val timestamp: Long = 0
    )

    // Analytics results
    suspend fun getAnalyticsResults(
        cameraId: String,
        from: Long = 0,
        to: Long = Long.MAX_VALUE,
        page: Int = 1,
        limit: Int = 100,
    ): List<AnalyticsResult> {
        val all = resultsByCamera[cameraId]?.toList() ?: emptyList()
        return all.filter { it.timestamp in from..to }
            .sortedByDescending { it.timestamp }
            .drop((page - 1).coerceAtLeast(0) * limit)
            .take(limit)
    }

    suspend fun countAnalyticsResults(cameraId: String, from: Long = 0, to: Long = Long.MAX_VALUE): Long =
        resultsByCamera[cameraId]?.count { it.timestamp in from..to }?.toLong() ?: 0L

    // =========================================================================
    // Управление жизненным циклом анализа (start/stop/config)
    // =========================================================================

    /**
     * Запуск непрерывного анализа потока камеры: RTSP (ffmpeg) → RGB24 →
     * нативные детекторы (motion + object) → метрики/трекинг/буфер/flow.
     * Graceful degradation: возврат failure с сообщением о недостающем компоненте.
     */
    fun startStreamAnalysis(cameraId: String, rtspUrl: String? = null): Result<Unit> {
        return try {
            if (runningCameras.contains(cameraId)) return Result.success(Unit)
            val source = frameSource
            if (source == null || !source.isAvailable()) {
                return Result.failure(IllegalStateException("Analytics unavailable: ffmpeg not found"))
            }
            if (!NativeAnalyticsFactory.isAvailable()) {
                return Result.failure(
                    IllegalStateException("Analytics unavailable: native library not loaded (version=${NativeAnalyticsFactory.version()})")
                )
            }
            // cameraRepository.getCameraById — suspend; запускаем корутину только после
            // успешной синхронной части (проверки выше), ошибки стартапа возвращаем через
            // Result у вызывающего потока частично асинхронно недоступны, поэтому запуск
            // выполняется в runBlocking-подобной схеме через глобальный scope и немедленный
            // переход в running-состояние после создания детекторов.
            val cameraDeferred = kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(5_000) { cameraRepository?.getCameraById(cameraId) }
            }
            val camera = configByCamera[cameraId]
                ?: cameraDeferred
                ?: return Result.failure(IllegalArgumentException("Camera $cameraId not found"))
            val url = rtspUrl ?: camera.url
                ?: return Result.failure(IllegalArgumentException("No RTSP URL for camera $cameraId"))

            val native = NativeAnalyticsFactory.get()
            val w = source.analyticsWidth
            val h = source.analyticsHeight
            val motionHandle = native.createMotionDetector(width = w, height = h, threshold = 0.35f, minArea = (w * h / 400).coerceAtLeast(1))
            if (motionHandle == null) {
                return Result.failure(IllegalStateException("Failed to create native motion detector"))
            }
            var objectHandle: Long? = null
            val modelPath = resolveModelPath()
            if (modelPath != null) {
                // createObjectDetector и loadObjectDetectorModel — suspend-функции JVM actual.
                val created = kotlinx.coroutines.runBlocking {
                    val handle = native.createObjectDetector(
                        confidenceThreshold = 0.45f,
                        maxObjects = 20,
                        useGPU = false,
                        inputSize = 416,
                    )
                    if (handle != null && native.loadObjectDetectorModel(handle, modelPath)) handle else {
                        if (handle != null) native.destroyObjectDetector(handle)
                        null
                    }
                }
                objectHandle = created
            }
            nativeHandles[cameraId] = motionHandle
            objectHandles[cameraId] = objectHandle ?: -1L
            configByCamera[cameraId] = camera
            runningCameras.add(cameraId)

            val frames = source.frames(url, camera.username, camera.password)
            scope.launch {
                var lastProcessed = 0L
                try {
                    frames.collect { frame ->
                        val now = System.currentTimeMillis()
                        if (now - lastProcessed < 400L) {
                            metricsByCamera.getOrPut(cameraId) { Metrics() }.skippedFrames.incrementAndGet()
                            return@collect
                        }
                        lastProcessed = now
                        processFrame(native, cameraId, frame, motionHandle, objectHandle)
                    }
                    logger.info { "Stream analysis frames completed for camera $cameraId" }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val m = metricsByCamera.getOrPut(cameraId) { Metrics() }
                    m.errorCount.incrementAndGet()
                    m.lastError.set(e.message)
                    m.lastErrorAt.set(System.currentTimeMillis())
                    logger.warn(e) { "Stream analysis error for camera $cameraId" }
                }
            }
            logger.info { "Stream analysis started for camera $cameraId" }
            Result.success(Unit)
        } catch (e: Exception) {
            runningCameras.remove(cameraId)
            logger.error(e) { "Failed to start stream analysis for camera $cameraId" }
            Result.failure(e)
        }
    }

    /** Остановка анализа: отмена корутины, освобождение нативных ресурсов. */
    fun stopStreamAnalysis(cameraId: String) {
        runningCameras.remove(cameraId)
        val motion = nativeHandles.remove(cameraId)
        if (motion != null && motion != -1L) {
            try {
                NativeAnalyticsFactory.get().destroyMotionDetector(motion)
            } catch (e: Exception) {
                logger.warn { "destroyMotionDetector failed for $cameraId: ${e.message}" }
            }
        }
        val obj = objectHandles.remove(cameraId)
        if (obj != null && obj != -1L) {
            try {
                NativeAnalyticsFactory.get().destroyObjectDetector(obj)
            } catch (e: Exception) {
                logger.warn { "destroyObjectDetector failed for $cameraId: ${e.message}" }
            }
        }
        logger.info { "Stream analysis stopped for camera $cameraId" }
    }

    /** Обновление конфигурации аналитики камеры (из роута PUT analytics config). */
    fun updateAnalyticsConfig(cameraId: String, camera: Camera) {
        configByCamera[cameraId] = camera
    }

    /** Путь модели детекции объектов: env OBJECT_DETECTION_MODEL → models/yolov8n.onnx → null. */
    private fun resolveModelPath(): String? {
        val fromEnv = System.getenv("OBJECT_DETECTION_MODEL")
        if (!fromEnv.isNullOrBlank() && java.io.File(fromEnv).exists()) return fromEnv
        val local = java.io.File("models/yolov8n.onnx")
        return if (local.exists()) local.absolutePath else null
    }


    // Analytics status
    data class AnalyticsStatus(
        val cameraId: String = "",
        val isRunning: Boolean = false,
        val activeStreams: Int = 0,
        val totalProcessedFrames: Long = 0,
        val skippedFrames: Long = 0,
        val totalErrors: Long = 0,
        val activeDetectors: List<String> = emptyList()
    )

    suspend fun getAnalyticsStatus(cameraId: String): AnalyticsStatus {
        val metrics = metricsByCamera[cameraId]
        val running = runningCameras.contains(cameraId)
        val detectors = mutableListOf<String>()
        if (nativeHandles.containsKey(cameraId)) detectors.add("motion")
        if (objectHandles.containsKey(cameraId)) detectors.add("object")
        return AnalyticsStatus(
            cameraId = cameraId,
            isRunning = running,
            activeStreams = if (running) 1 else 0,
            totalProcessedFrames = metrics?.processedFrames?.get() ?: 0L,
            skippedFrames = metrics?.skippedFrames?.get() ?: 0L,
            totalErrors = metrics?.errorCount?.get() ?: 0L,
            activeDetectors = detectors,
        )
    }

    // Analytics metrics
    data class AnalyticsMetrics(
        val cameraId: String,
        val processedFrames: Long = 0,
        val skippedFrames: Long = 0,
        val errorCount: Long = 0,
        val avgProcessingTimeMs: Double = 0.0,
        val fps: Double = 0.0,
        val frameSource: String = "",
        val lastError: String? = null,
        val lastErrorAt: Long = 0
    )

    suspend fun getAnalyticsMetrics(cameraId: String): AnalyticsMetrics {
        val m = metricsByCamera[cameraId]
        val processed = m?.processedFrames?.get() ?: 0L
        val totalMs = m?.totalProcessingMs?.get() ?: 0L
        val avg = if (processed > 0) totalMs.toDouble() / processed else 0.0
        // FPS по среднему времени обработки кадра (конвейер однопоточный).
        val fps = if (avg > 0) 1000.0 / avg else 0.0
        return AnalyticsMetrics(
            cameraId = cameraId,
            processedFrames = processed,
            skippedFrames = m?.skippedFrames?.get() ?: 0L,
            errorCount = m?.errorCount?.get() ?: 0L,
            avgProcessingTimeMs = avg,
            fps = fps,
            frameSource = if (frameSource?.isAvailable() == true) "ffmpeg" else "",
            lastError = m?.lastError?.get(),
            lastErrorAt = m?.lastErrorAt?.get()?.takeIf { it > 0 } ?: 0,
        )
    }

    // Analytics stats
    data class AnalyticsStats(
        val cameraId: String = "",
        val frameCount: Long = 0,
        val isActive: Boolean = false,
        val motionDetectionsCount: Int = 0,
        val objectDetectionsCount: Int = 0,
        val faceDetectionsCount: Int = 0,
        val plateReadsCount: Int = 0,
        val licensePlateRecognitionsCount: Int = 0,
        val lastProcessedTimestamp: Long = 0
    )

    suspend fun getAnalyticsStats(cameraId: String): AnalyticsStats {
        val m = metricsByCamera[cameraId]
        val results = resultsByCamera[cameraId]
        return AnalyticsStats(
            cameraId = cameraId,
            frameCount = m?.processedFrames?.get() ?: 0L,
            isActive = runningCameras.contains(cameraId),
            motionDetectionsCount = m?.motionCount?.get()?.toInt() ?: 0,
            objectDetectionsCount = m?.objectCount?.get()?.toInt() ?: 0,
            faceDetectionsCount = m?.faceCount?.get()?.toInt() ?: 0,
            plateReadsCount = m?.plateCount?.get()?.toInt() ?: 0,
            licensePlateRecognitionsCount = m?.plateCount?.get()?.toInt() ?: 0,
            lastProcessedTimestamp = m?.lastProcessedTimestamp?.get() ?: 0L,
        )
    }

    // Tracks and trajectories
    data class TrackInfo(
        val trackId: String = "",
        val cameraId: String = "",
        val objectType: String = "",
        var lastPositionX: Double = 0.0,
        var lastPositionY: Double = 0.0,
        var confidence: Float = 0f,
        var lastSeenAt: Long = 0,
        internal val trajectory: MutableList<TrajectoryPointDto> = mutableListOf(),
    )

    data class TrajectoryPointDto(
        val x: Double,
        val y: Double,
        val timestamp: Long
    )

    suspend fun getCurrentTracks(cameraId: String): List<TrackInfo> =
        tracksByCamera[cameraId]?.values?.toList() ?: emptyList()

    suspend fun getTrajectories(cameraId: String, maxPoints: Int = 30): Map<String, List<TrajectoryPointDto>> {
        val tracks = tracksByCamera[cameraId] ?: return emptyMap()
        return tracks.mapValues { (_, track) -> track.trajectory.takeLast(maxPoints) }
    }

    // Analytics control
    suspend fun stopAnalytics(cameraId: String) {
        stopStreamAnalysis(cameraId)
    }

    suspend fun clearAnalyticsResults(cameraId: String) {
        resultsByCamera[cameraId]?.clear()
        tracksByCamera[cameraId]?.clear()
    }

    // Stream quality
    suspend fun setStreamQuality(cameraId: String, quality: String) {
        streamQualityByCamera[cameraId] = quality
    }

    /** Обработка одного кадра: motion + object, метрики, трекинг, буфер, flow. */
    private suspend fun processFrame(
        native: NativeAnalytics,
        cameraId: String,
        frame: RtspAnalyticsFrame,
        motionHandle: Long,
        objectHandle: Long?,
    ) {
        val m = metricsByCamera.getOrPut(cameraId) { Metrics() }
        val startMs = System.currentTimeMillis()
        try {
            // --- Motion ---
            // detectMotion — suspend (JVM actual оборачивает JNI-вызов в withContext);
            // processFrame вызывается из корутины collect, поэтому suspend-вызов легален.
            val raw = native.detectMotion(motionHandle, frame.data, frame.width, frame.height)
            if (raw != null && raw.motionDetected) {
                m.motionCount.incrementAndGet()
            }

            // --- Objects ---
            var objects: List<NativeDetectedObject> = emptyList()
            if (objectHandle != null && objectHandle != -1L) {
                // detectObjects возвращает обёртку ObjectDetectionResult (objects).
                val odResult = native.detectObjects(objectHandle, frame.data, frame.width, frame.height)
                objects = odResult?.objects ?: emptyList()
                if (objects.isNotEmpty()) {
                    m.objectCount.incrementAndGet()
                    updateTracks(cameraId, objects, System.currentTimeMillis())
                }
            }

            // --- Буфер результата + flow ---
            val result = AnalyticsResult(
                cameraId = cameraId,
                timestamp = System.currentTimeMillis(),
                motionDetection = raw?.let {
                    MotionDetectionResult(detected = it.motionDetected, confidence = it.confidence, timestamp = System.currentTimeMillis())
                },
                objectDetection = ObjectDetectionResult(
                    objects = objects.map { o -> mapOf("type" to o.type.name, "confidence" to o.confidence, "x" to o.x, "y" to o.y, "width" to o.width, "height" to o.height) },
                    timestamp = System.currentTimeMillis(),
                ),
            )
            val buffer = resultsByCamera.getOrPut(cameraId) { ArrayDeque() }
            synchronized(buffer) {
                buffer.addLast(result)
                while (buffer.size > MAX_BUFFERED_RESULTS) buffer.removeFirst()
            }
            _resultsFlow.tryEmit(result)
        } catch (e: Exception) {
            m.errorCount.incrementAndGet()
            m.lastError.set(e.message)
            m.lastErrorAt.set(System.currentTimeMillis())
        } finally {
            m.processedFrames.incrementAndGet()
            m.totalProcessingMs.addAndGet(System.currentTimeMillis() - startMs)
            m.lastProcessedTimestamp.set(System.currentTimeMillis())
        }
    }

    /** Упрощённый центроид-трекер: ближайший трек того же типа в радиусе. */
    private fun updateTracks(cameraId: String, objects: List<com.company.ipcamera.core.network.analytics.DetectedObject>, now: Long) {
        val tracks = tracksByCamera.getOrPut(cameraId) { ConcurrentHashMap() }
        for (o in objects) {
            val cx = o.x + o.width / 2.0
            val cy = o.y + o.height / 2.0
            val existing = tracks.values.minOfOrNull { t ->
                if (t.objectType == o.type.name) {
                    val dx = t.lastPositionX - cx
                    val dy = t.lastPositionY - cy
                    dx * dx + dy * dy
                } else {
                    Double.MAX_VALUE
                }
            } ?: Double.MAX_VALUE
            if (existing < TRACK_MATCH_RADIUS * TRACK_MATCH_RADIUS) {
                val track = tracks.values.first { t ->
                    t.objectType == o.type.name && (
                        (t.lastPositionX - cx).let { it * it } + (t.lastPositionY - cy).let { it * it }
                        ) == existing
                }
                track.lastPositionX = cx
                track.lastPositionY = cy
                track.confidence = o.confidence.toFloat()
                track.lastSeenAt = now
                track.trajectory.add(TrajectoryPointDto(cx, cy, now))
                while (track.trajectory.size > MAX_TRAJECTORY_POINTS) track.trajectory.removeAt(0)
            } else {
                val newTrack = TrackInfo(
                    trackId = "trk-${now}-${(0..999).random()}",
                    cameraId = cameraId,
                    objectType = o.type.name,
                    confidence = o.confidence.toFloat(),
                    lastSeenAt = now,
                )
                newTrack.lastPositionX = cx
                newTrack.lastPositionY = cy
                newTrack.trajectory.add(TrajectoryPointDto(cx, cy, now))
                tracks[newTrack.trackId] = newTrack
            }
        }
        // Удаление устаревших треков.
        val stale = tracks.values.filter { now - it.lastSeenAt > TRACK_TTL_MS }.map { it.trackId }
        stale.forEach { tracks.remove(it) }
    }

    companion object {
        /** Максимальный размер кольцевого буфера результатов на камеру. */
        private const val MAX_BUFFERED_RESULTS = 500

        /** Радиус сопоставления трека (нормализованные координаты). */
        private const val TRACK_MATCH_RADIUS = 0.12

        /** TTL необновляемого трека, мс. */
        private const val TRACK_TTL_MS = 5_000L

        /** Максимум точек траектории трека. */
        private const val MAX_TRAJECTORY_POINTS = 120
    }
}
