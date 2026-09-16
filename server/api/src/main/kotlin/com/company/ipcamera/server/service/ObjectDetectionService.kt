package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.analytics.DetectedObject as NativeDetectedObject
import com.company.ipcamera.core.network.analytics.NativeAnalytics
import com.company.ipcamera.core.network.analytics.ObjectDetectionResult
import com.company.ipcamera.server.repository.ObjectDetectionConfigRepository
import com.company.ipcamera.server.repository.ObjectDetectionEventRepository
import com.company.ipcamera.server.service.analytics.RtspFrameSource
import com.company.ipcamera.shared.domain.model.BoundingBox
import com.company.ipcamera.shared.domain.model.DetectedObject
import com.company.ipcamera.shared.domain.model.ObjectDetectionConfig
import com.company.ipcamera.shared.domain.model.ObjectDetectionEvent
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
 * Реальная серверная детекция объектов (задача 1.1, часть «object detection»).
 *
 * Конвейер: [RtspFrameSource] (ffmpeg → RGB24) → [NativeAnalytics.detectObjects]
 * (JNI → C++ OpenCV DNN + YOLO ONNX из native/analytics) → фильтр классов
 * (detectClasses из конфига) → cooldown → сохранение [ObjectDetectionEvent] →
 * JPEG-снапшот ([FrameCodec.encodeJpeg] + [MotionSnapshotService]) → уведомление
 * ([MotionNotificationService]).
 *
 * Модель: путь из env `OBJECT_DETECTION_MODEL`, иначе `models/<file>.onnx`.
 * Если модель/нативная библиотека недоступны — [startDetection] возвращает
 * [Result.failure] с понятным сообщением.
 *
 * `processFrame`/`analyzeFrame` (on-demand API роутов) теперь работают по-настоящему:
 * JPEG → [FrameCodec.decodeToRgb] → нативная детекция → доменные модели.
 */
class ObjectDetectionService(
    private val configRepository: ObjectDetectionConfigRepository,
    private val eventRepository: ObjectDetectionEventRepository,
    private val frameSource: RtspFrameSource? = null,
    private val snapshotService: MotionSnapshotService? = null,
    private val notificationService: MotionNotificationService? = null,
    private val cameraRepository: CameraRepository? = null,
) {
    companion object {
        private const val MIN_FRAME_INTERVAL_MS = 500L
        private const val ENV_MODEL_PATH = "OBJECT_DETECTION_MODEL"

        private fun modelFileName(type: ObjectDetectionConfig.ModelType): String = when (type) {
            ObjectDetectionConfig.ModelType.YOLOV8N -> "yolov8n.onnx"
            ObjectDetectionConfig.ModelType.YOLOV8S -> "yolov8s.onnx"
            ObjectDetectionConfig.ModelType.YOLOV8M -> "yolov8m.onnx"
            ObjectDetectionConfig.ModelType.YOLOV8L -> "yolov8l.onnx"
            ObjectDetectionConfig.ModelType.YOLOV8X -> "yolov8x.onnx"
        }
    }

    private val _detectors = MutableStateFlow<Map<String, DetectorStatus>>(emptyMap())
    val detectors: StateFlow<Map<String, DetectorStatus>> = _detectors.asStateFlow()

    private val runningDetectors = ConcurrentHashMap<String, Job>()
    private val nativeHandles = ConcurrentHashMap<String, Long>()
    private val framesProcessed = ConcurrentHashMap<String, AtomicLong>()

    suspend fun startDetection(cameraId: String, rtspUrl: String): Result<Unit> {
        return try {
            if (runningDetectors.containsKey(cameraId)) {
                logger.warn { "Object detection already running for camera $cameraId" }
                return Result.success(Unit)
            }
            val source = frameSource
            if (source == null || !source.isAvailable()) {
                return Result.failure(
                    IllegalStateException("Object detection unavailable: ffmpeg not found (frame source disabled)")
                )
            }
            if (!NativeAnalyticsFactory.isAvailable()) {
                return Result.failure(
                    IllegalStateException(
                        "Object detection unavailable: native analytics library not loaded (version=${NativeAnalyticsFactory.version()})"
                    )
                )
            }
            val config = configRepository.findByCameraId(cameraId)
                ?: ObjectDetectionConfig(
                    id = "default-$cameraId",
                    cameraId = cameraId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            if (!config.enabled) {
                return Result.failure(IllegalStateException("Object detection disabled for camera $cameraId"))
            }

            val native = NativeAnalyticsFactory.get()
            val modelPath = resolveModelPath(config.modelType)
            val handle = native.createObjectDetector(
                confidenceThreshold = config.confidence.toFloat().coerceIn(0.05f, 0.95f),
                maxObjects = 20,
                useGPU = false,
                inputSize = 640,
            ) ?: return Result.failure(IllegalStateException("Failed to create native object detector"))
            if (!native.loadObjectDetectorModel(handle, modelPath)) {
                native.destroyObjectDetector(handle)
                return Result.failure(
                    IllegalStateException("Failed to load YOLO model from '$modelPath' (set $ENV_MODEL_PATH or place file under models/)")
                )
            }
            nativeHandles[cameraId] = handle
            framesProcessed[cameraId] = AtomicLong(0)

            val job = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                runDetectionLoop(cameraId, config, rtspUrl, handle)
            }
            runningDetectors[cameraId] = job
            _detectors.value = _detectors.value.plus(
                cameraId to DetectorStatus(cameraId = cameraId, isRunning = true)
            )
            logger.info { "Object detection started for camera $cameraId (model=$modelPath, classes=${config.detectClasses})" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to start object detection for camera $cameraId" }
            cleanup(cameraId)
            Result.failure(e)
        }
    }

    suspend fun stopDetection(cameraId: String): Result<Unit> {
        return try {
            runningDetectors.remove(cameraId)?.cancel()
            cleanup(cameraId)
            _detectors.value = _detectors.value.minus(cameraId)
            logger.info { "Object detection stopped for camera $cameraId" }
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
            nativeVersion = NativeAnalyticsFactory.version(),
        )
    }

    data class DetectorStatus(
        val cameraId: String,
        val isRunning: Boolean,
        val framesProcessed: Long = 0,
        val nativeVersion: String? = null,
    )

    // =========================================================================
    // Внутренний цикл детекции
    // =========================================================================

    private suspend fun runDetectionLoop(
        cameraId: String,
        config: ObjectDetectionConfig,
        rtspUrl: String,
        handle: Long,
    ) {
        val native = NativeAnalyticsFactory.get()
        val camera = cameraRepository?.getCameraById(cameraId)
        val frames = frameSource?.frames(rtspUrl, camera?.username, camera?.password) ?: return
        var lastProcessed = 0L
        var lastEventAt = 0L
        try {
            frames.collect { frame ->
                val now = System.currentTimeMillis()
                if (now - lastProcessed < MIN_FRAME_INTERVAL_MS) return@collect
                lastProcessed = now
                framesProcessed[cameraId]?.incrementAndGet()

                val nativeResult = native.detectObjects(handle, frame.data, frame.width, frame.height)
                val objects = nativeResult?.objects.orEmpty()
                if (objects.isEmpty()) return@collect

                // Фильтр по классам из конфига (detectClasses; тип нативного объекта → нижний регистр).
                val filtered = objects.filter { obj ->
                    val cls = obj.type.name.lowercase()
                    config.detectClasses.isEmpty() || cls in config.detectClasses
                }
                if (filtered.isEmpty()) return@collect

                // Cooldown между событиями.
                if (now - lastEventAt < config.cooldownSeconds * 1000L) return@collect
                lastEventAt = now

                val domainObjects = filtered.map { obj ->
                    DetectedObject(
                        type = obj.type.name,
                        confidence = obj.confidence.toFloat(),
                        boundingBox = BoundingBox(x = obj.x, y = obj.y, width = obj.width, height = obj.height),
                    )
                }
                var event = ObjectDetectionEvent(
                    id = UUID.randomUUID().toString(),
                    cameraId = cameraId,
                    timestamp = now,
                    detectedObjects = domainObjects,
                    zoneId = null,
                    snapshotPath = null,
                    recordingId = null,
                    processed = false,
                    metadata = mapOf("source" to "native-cpp", "resolution" to "${frame.width}x${frame.height}"),
                )
                eventRepository.save(event).getOrNull()?.let { event = it }

                val jpeg = FrameCodec.encodeJpeg(frame.data, frame.width, frame.height)
                if (jpeg != null) {
                    val snapshot = snapshotService?.saveObjectSnapshot(event.id, jpeg)
                    snapshot?.getOrNull()?.let { file ->
                        event = event.copy(snapshotPath = file.absolutePath)
                        eventRepository.save(event)
                    }
                }
                if (config.notifyOnDetection) {
                    notificationService?.sendNotification(event)
                }
                logger.info { "Object detection event for camera $cameraId: ${filtered.map { it.type.name }} at ${frame.width}x${frame.height}" }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(e) { "Object detection loop error for camera $cameraId: ${e.message}" }
        } finally {
            logger.info { "Object detection loop finished for camera $cameraId" }
        }
    }

    private fun cleanup(cameraId: String) {
        val handle = nativeHandles.remove(cameraId)
        if (handle != null) {
            try {
                NativeAnalyticsFactory.get().destroyObjectDetector(handle)
            } catch (e: Exception) {
                logger.warn { "Failed to destroy object detector for camera $cameraId: ${e.message}" }
            }
        }
        framesProcessed.remove(cameraId)
    }

    /**
     * Обработка одиночного JPEG-кадра: реальная нативная детекция объектов.
     * Создаёт временный детектор на вызов (on-demand), т.к. модель загружается один раз.
     */
    suspend fun processFrame(cameraId: String, frame: ByteArray): List<Map<String, Any>> {
        val rgb = FrameCodec.decodeToRgb(frame) ?: run {
            logger.warn { "processFrame: cannot decode JPEG frame for camera $cameraId" }
            return emptyList()
        }
        return runNativeDetection(cameraId, rgb.first, rgb.second, rgb.third)
    }

    suspend fun analyzeFrame(cameraId: String, frame: ByteArray): Map<String, Any> {
        val started = System.currentTimeMillis()
        val objects = processFrame(cameraId, frame)
        return mapOf(
            "cameraId" to cameraId,
            "objects" to objects,
            "objectCount" to objects.size,
            "processingTimeMs" to (System.currentTimeMillis() - started),
            "timestamp" to System.currentTimeMillis(),
        )
    }

    /**
     * Нативная детекция по RGB-буферу: одноразовый детектор + YOLO-модель.
     */
    private suspend fun runNativeDetection(
        cameraId: String,
        rgb: ByteArray,
        width: Int,
        height: Int,
    ): List<Map<String, Any>> {
        if (!NativeAnalyticsFactory.isAvailable()) {
            logger.warn { "processFrame: native analytics unavailable (version=${NativeAnalyticsFactory.version()})" }
            return emptyList()
        }
        val native = NativeAnalyticsFactory.get()
        val config = configRepository.findByCameraId(cameraId)
        val modelType = config?.modelType ?: ObjectDetectionConfig.ModelType.YOLOV8N
        val modelPath = resolveModelPath(modelType)
        val handle = native.createObjectDetector(
            confidenceThreshold = (config?.confidence ?: 0.5).toFloat().coerceIn(0.05f, 0.95f),
            maxObjects = 20,
            useGPU = false,
            inputSize = 640,
        ) ?: return emptyList()
        try {
            if (!native.loadObjectDetectorModel(handle, modelPath)) {
                logger.warn { "processFrame: model load failed for camera $cameraId (path=$modelPath)" }
                return emptyList()
            }
            val nativeResult: ObjectDetectionResult? = native.detectObjects(handle, rgb, width, height)
            val detected = nativeResult?.objects.orEmpty()
            return detected.map { obj ->
                mapOf(
                    "class" to obj.type.name,
                    "confidence" to obj.confidence,
                    "bbox" to mapOf(
                        "x" to obj.x,
                        "y" to obj.y,
                        "width" to obj.width,
                        "height" to obj.height,
                    ),
                )
            }
        } finally {
            try {
                native.destroyObjectDetector(handle)
            } catch (e: Exception) {
                logger.warn { "Failed to destroy temp detector: ${e.message}" }
            }
        }
    }

    // =========================================================================
    // Вспомогательные методы
    // =========================================================================

    /**
     * Путь к модели: env `OBJECT_DETECTION_MODEL` (приоритет), иначе `models/<файл>`.
     */
    private fun resolveModelPath(type: ObjectDetectionConfig.ModelType): String {
        System.getenv(ENV_MODEL_PATH)?.let { if (it.isNotBlank()) return it }
        return "models/${modelFileName(type)}"
    }

    private fun modelFileName(type: ObjectDetectionConfig.ModelType): String = when (type) {
        ObjectDetectionConfig.ModelType.YOLOV8N -> "yolov8n.onnx"
        ObjectDetectionConfig.ModelType.YOLOV8S -> "yolov8s.onnx"
        ObjectDetectionConfig.ModelType.YOLOV8M -> "yolov8m.onnx"
        ObjectDetectionConfig.ModelType.YOLOV8L -> "yolov8l.onnx"
        ObjectDetectionConfig.ModelType.YOLOV8X -> "yolov8x.onnx"
    }
}
