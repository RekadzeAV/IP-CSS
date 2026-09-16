package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.analytics.MotionDetectionResult
import com.company.ipcamera.core.network.analytics.NativeAnalytics
import com.company.ipcamera.server.repository.MotionConfigRepository
import com.company.ipcamera.server.repository.MotionEventRepository
import com.company.ipcamera.server.service.analytics.MotionZoneFilter
import com.company.ipcamera.server.service.analytics.RtspFrameSource
import com.company.ipcamera.shared.domain.model.MotionConfig
import com.company.ipcamera.shared.domain.model.MotionEvent
import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Реальная серверная детекция движения (задача 1.1, часть «motion»).
 *
 * Конвейер: [RtspFrameSource] (ffmpeg → RGB24) → [NativeAnalytics.detectMotion]
 * (JNI → C++ OpenCV MOG2-детектор из native/analytics) → фильтрация по зонам
 * ([MotionZoneFilter]) → cooldown → сохранение [MotionEvent] → JPEG-снапшот
 * ([FrameCodec.encodeJpeg] + [MotionSnapshotService]) → уведомление
 * ([MotionNotificationService]).
 *
 * Graceful degradation: при недоступности ffmpeg или нативной библиотеки
 * [startDetection] возвращает [Result.failure] с понятным сообщением — сервер
 * продолжает работать, статус детектора не искажается.
 */
class MotionDetectorService(
    private val motionConfigRepository: MotionConfigRepository,
    private val motionEventRepository: MotionEventRepository,
    private val notificationService: MotionNotificationService,
    private val snapshotService: MotionSnapshotService,
    private val frameSource: RtspFrameSource? = null,
    private val cameraRepository: CameraRepository? = null,
) {
    companion object {
        /** Минимальный интервал обработки кадров (троттлинг): ~2 fps анализа. */
        private const val MIN_FRAME_INTERVAL_MS = 500L
    }

    private val _detectors = MutableStateFlow<Map<String, DetectorStatus>>(emptyMap())
    val detectors: StateFlow<Map<String, DetectorStatus>> = _detectors.asStateFlow()

    private val runningDetectors = ConcurrentHashMap<String, Job>()
    private val nativeHandles = ConcurrentHashMap<String, Long>()
    private val framesProcessed = ConcurrentHashMap<String, AtomicLong>()
    private val lastMotionAt = ConcurrentHashMap<String, AtomicLong>()

    suspend fun startDetection(
        cameraId: String,
        config: MotionConfig,
        rtspUrl: String? = null,
    ): Result<Unit> {
        return try {
            if (runningDetectors.containsKey(cameraId)) {
                logger.warn { "Motion detection already running for camera $cameraId" }
                return Result.success(Unit)
            }
            val source = frameSource
            if (source == null || !source.isAvailable()) {
                return Result.failure(
                    IllegalStateException("Motion detection unavailable: ffmpeg not found (frame source disabled)")
                )
            }
            if (!NativeAnalyticsFactory.isAvailable()) {
                return Result.failure(
                    IllegalStateException(
                        "Motion detection unavailable: native analytics library not loaded (version=${NativeAnalyticsFactory.version()})"
                    )
                )
            }
            val camera = cameraRepository?.getCameraById(cameraId)
            val url = rtspUrl
                ?: camera?.url
                ?: return Result.failure(IllegalArgumentException("RTSP URL required: no url for camera $cameraId"))

            val frameW = source.analyticsWidth
            val frameH = source.analyticsHeight
            val native: NativeAnalytics = NativeAnalyticsFactory.get()
            // sensitivity (0..1) → threshold детектора: C++ трактует threshold как
            // порог confidence (0..1); высокая чувствительность = низкий порог.
            val threshold = config.sensitivity.toFloat().coerceIn(0.05f, 0.95f)
            // minArea — доля площади кадра (0..1) → площадь в пикселях RGB-кадра.
            val minAreaPx = (config.minArea * frameW * frameH).toInt().coerceAtLeast(1)
            val handle = native.createMotionDetector(
                width = frameW,
                height = frameH,
                threshold = threshold,
                minArea = minAreaPx,
            )
            if (handle == null) {
                return Result.failure(IllegalStateException("Failed to create native motion detector"))
            }
            nativeHandles[cameraId] = handle
            framesProcessed[cameraId] = AtomicLong(0)
            lastMotionAt[cameraId] = AtomicLong(0)

            val job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                runDetectionLoop(cameraId, config, url, camera?.username, camera?.password, handle)
            }
            runningDetectors[cameraId] = job
            _detectors.value = _detectors.value.plus(
                cameraId to DetectorStatus(cameraId = cameraId, isRunning = true)
            )
            logger.info {
                "Motion detection started for camera $cameraId (${frameW}x$frameH, threshold=$threshold, minAreaPx=$minAreaPx, zones=${config.zones.size})"
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to start motion detection for camera $cameraId" }
            cleanup(cameraId)
            Result.failure(e)
        }
    }

    suspend fun stopDetection(cameraId: String): Result<Unit> {
        return try {
            runningDetectors.remove(cameraId)?.cancel()
            cleanup(cameraId)
            _detectors.value = _detectors.value.minus(cameraId)
            logger.info { "Motion detection stopped for camera $cameraId" }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDetectorStatus(cameraId: String): DetectorStatus? {
        val running = runningDetectors.containsKey(cameraId)
        return DetectorStatus(
            cameraId = cameraId,
            isRunning = running,
            framesProcessed = framesProcessed[cameraId]?.get() ?: 0L,
            lastMotionAt = lastMotionAt[cameraId]?.get()?.takeIf { it > 0 },
            analyticsResolution = frameSource?.let { "${it.analyticsWidth}x${it.analyticsHeight}" },
            nativeVersion = NativeAnalyticsFactory.version(),
        )
    }

    fun getAllDetectorStatuses(): List<DetectorStatus> {
        return _detectors.value.keys.map { getDetectorStatus(it) ?: DetectorStatus(cameraId = it, isRunning = false) }
    }

    data class DetectorStatus(
        val cameraId: String,
        val isRunning: Boolean,
        val framesProcessed: Long = 0,
        val lastMotionAt: Long? = null,
        val analyticsResolution: String? = null,
        val nativeVersion: String? = null,
    )

    // =========================================================================
    // Внутренний цикл детекции
    // =========================================================================

    private suspend fun runDetectionLoop(
        cameraId: String,
        config: MotionConfig,
        rtspUrl: String,
        username: String?,
        password: String?,
        handle: Long,
    ) {
        val native = NativeAnalyticsFactory.get()
        val frames = frameSource?.frames(rtspUrl, username, password) ?: return
        var lastProcessed = 0L
        var lastEventAt = 0L
        try {
            frames.collect { frame ->
                val now = System.currentTimeMillis()
                if (now - lastProcessed < MIN_FRAME_INTERVAL_MS) return@collect
                lastProcessed = now
                framesProcessed[cameraId]?.incrementAndGet()

                val result: MotionDetectionResult? = native.detectMotion(handle, frame.data, frame.width, frame.height)
                val motion = result ?: return@collect
                if (!motion.motionDetected) return@collect

                // Фильтр зон: центр bbox должен попадать хотя бы в одну включённую зону.
                val matchedZones = MotionZoneFilter.zonesContaining(
                    motion.x, motion.y, motion.width, motion.height, frame.width, frame.height, config.zones
                )
                // Если зоны заданы — порог по чувствительности зоны.
                val zoneThreshold = matchedZones.minOfOrNull { it.sensitivity.toFloat() } ?: 0f
                if (motion.confidence < zoneThreshold) return@collect

                // Cooldown между событиями.
                if (now - lastEventAt < config.cooldownSeconds * 1000L) return@collect
                lastEventAt = now
                lastMotionAt[cameraId]?.set(now)

                val areaFraction = (motion.width.toDouble() * motion.height) / (frame.width * frame.height)
                var event = MotionEvent(
                    id = UUID.randomUUID().toString(),
                    cameraId = cameraId,
                    zoneId = matchedZones.firstOrNull()?.id,
                    timestamp = now,
                    confidence = motion.confidence.toDouble(),
                    area = areaFraction,
                    snapshotPath = null,
                    recordingId = null,
                    processed = false,
                    metadata = mapOf(
                        "source" to "native-cpp",
                        "bbox" to "${motion.x},${motion.y},${motion.width}x${motion.height}",
                        "resolution" to "${frame.width}x${frame.height}",
                    ),
                )
                motionEventRepository.save(event).getOrNull()?.let { event = it }

                // JPEG-снапшот из аналитического кадра.
                val jpeg = FrameCodec.encodeJpeg(frame.data, frame.width, frame.height)
                if (jpeg != null) {
                    snapshotService.saveSnapshot(event, jpeg).getOrNull()?.let { file ->
                        event = event.copy(snapshotPath = file.absolutePath)
                        motionEventRepository.save(event)
                    }
                }

                if (config.notifyOnMotion) {
                    notificationService.sendNotification(event)
                }
                logger.info {
                    "Motion event for camera $cameraId: confidence=${"%.2f".format(motion.confidence)}, area=${"%.3f".format(areaFraction)}, zones=${matchedZones.map { it.name }}"
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Motion detection loop error for camera $cameraId: ${e.message}" }
        } finally {
            logger.info { "Motion detection loop finished for camera $cameraId" }
        }
    }

    private fun cleanup(cameraId: String) {
        val handle = nativeHandles.remove(cameraId)
        if (handle != null) {
            try {
                NativeAnalyticsFactory.get().destroyMotionDetector(handle)
            } catch (e: Exception) {
                logger.warn { "Failed to destroy motion detector for camera $cameraId: ${e.message}" }
            }
        }
        framesProcessed.remove(cameraId)
        lastMotionAt.remove(cameraId)
    }
}
