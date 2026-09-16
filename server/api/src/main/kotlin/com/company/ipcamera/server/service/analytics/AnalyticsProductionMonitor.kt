package com.company.ipcamera.server.service.analytics

import com.company.ipcamera.shared.domain.model.Camera
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Production Monitoring для Video Analytics
 *
 * Предоставляет метрики и мониторинг для AI-аналитики в production:
 * - Обработанные кадры
 * - Пропущенные кадры
 * - Ошибки пайплайна
 * - Задержки обработки
 * - Статус детекторов
 *
 * Интегрируется с /analytics/metrics endpoint
 */
class AnalyticsProductionMonitor {

    /**
     * Метрики по камерам
     */
    private val metricsByCamera = ConcurrentHashMap<String, CameraMetrics>()

    /**
     * Глобальные метрики
     */
    private val globalMetrics = GlobalMetrics()

    /**
     * История ошибок (последние 100 на камеру)
     */
    private val errorHistoryByCamera = ConcurrentHashMap<String, MutableList<AnalyticsError>>()

    /**
     * Статистика по детекторам
     */
    private val detectorStats = ConcurrentHashMap<String, DetectorStatistics>()

    /**
     * Запустить мониторинг для камеры
     */
    fun startMonitoring(cameraId: String, cameraName: String, frameSourceKind: String) {
        val metrics = CameraMetrics(
            cameraId = cameraId,
            cameraName = cameraName,
            frameSourceKind = frameSourceKind
        )
        metricsByCamera[cameraId] = metrics
        detectorStats[cameraId] = DetectorStatistics()
        
        logger.info { "Started analytics monitoring for camera: $cameraId ($cameraName)" }
    }

    /**
     * Остановить мониторинг для камеры
     */
    fun stopMonitoring(cameraId: String) {
        val metrics = metricsByCamera.remove(cameraId)
        errorHistoryByCamera.remove(cameraId)
        detectorStats.remove(cameraId)
        
        if (metrics != null) {
            logger.info { 
                "Stopped analytics monitoring for camera: $cameraId. " +
                "Total frames: ${metrics.totalFramesProcessed.get()}, " +
                "Errors: ${metrics.totalErrors.get()}"
            }
        }
    }

    /**
     * Зафиксировать обработанный кадр
     */
    fun recordFrameProcessed(cameraId: String, processingTimeMs: Long) {
        val metrics = metricsByCamera[cameraId] ?: return
        
        metrics.totalFramesProcessed.incrementAndGet()
        metrics.lastFrameProcessedAt = System.currentTimeMillis()
        
        // Скользящее среднее
        val recentTimes = metrics.recentProcessingTimes
        recentTimes.add(processingTimeMs)
        if (recentTimes.size > RECENT_WINDOW_SIZE) {
            recentTimes.removeFirst()
        }
        
        // Обновляем global metrics
        globalMetrics.totalFramesProcessed.incrementAndGet()
        globalMetrics.lastFrameProcessedAt = System.currentTimeMillis()
    }

    /**
     * Зафиксировать пропущенный кадр
     */
    fun recordFrameSkipped(cameraId: String, reason: String) {
        val metrics = metricsByCamera[cameraId] ?: return
        
        metrics.totalFramesSkipped.incrementAndGet()
        metrics.lastSkippedAt = System.currentTimeMillis()
        metrics.lastSkipReason = reason
        
        globalMetrics.totalFramesSkipped.incrementAndGet()
        
        logger.debug { "Frame skipped for camera $cameraId: $reason" }
    }

    /**
     * Зафиксировать ошибку
     */
    fun recordError(cameraId: String, error: Throwable, isErrorType: AnalyticsErrorType) {
        val metrics = metricsByCamera[cameraId] ?: return
        
        val errorRecord = AnalyticsError(
            cameraId = cameraId,
            timestamp = System.currentTimeMillis(),
            errorType = isErrorType,
            message = error.message ?: error::class.simpleName ?: "Unknown error",
            stackTrace = error.stackTrace.take(10).joinToString("\n")
        )
        
        metrics.totalErrors.incrementAndGet()
        metrics.lastErrorAt = System.currentTimeMillis()
        metrics.lastError = errorRecord
        
        // Добавляем в историю
        val history = errorHistoryByCamera.getOrPut(cameraId) { mutableListOf() }
        history.add(errorRecord)
        if (history.size > MAX_ERROR_HISTORY_SIZE) {
            history.removeAt(0)
        }
        
        // Обновляем global metrics
        globalMetrics.totalErrors.incrementAndGet()
        globalMetrics.lastErrorAt = System.currentTimeMillis()
        
        logger.error(error) { "Analytics error for camera $cameraId: ${errorRecord.message}" }
    }

    /**
     * Зафиксировать детекцию (motion, object, face, ANPR)
     */
    fun recordDetection(
        cameraId: String,
        detectionType: DetectionType,
        confidence: Float
    ) {
        val stats = detectorStats[cameraId] ?: return
        
        when (detectionType) {
            DetectionType.MOTION -> {
                stats.motionDetections.incrementAndGet()
                stats.lastMotionDetectionAt = System.currentTimeMillis()
            }
            DetectionType.OBJECT -> {
                stats.objectDetections.incrementAndGet()
                stats.lastObjectDetectionAt = System.currentTimeMillis()
            }
            DetectionType.FACE -> {
                stats.faceDetections.incrementAndGet()
                stats.lastFaceDetectionAt = System.currentTimeMillis()
            }
            DetectionType.ANPR -> {
                stats.anprDetections.incrementAndGet()
                stats.lastAnprDetectionAt = System.currentTimeMillis()
            }
        }
        
        // Запись в global
        when (detectionType) {
            DetectionType.MOTION -> globalMetrics.totalMotionDetections.incrementAndGet()
            DetectionType.OBJECT -> globalMetrics.totalObjectDetections.incrementAndGet()
            DetectionType.FACE -> globalMetrics.totalFaceDetections.incrementAndGet()
            DetectionType.ANPR -> globalMetrics.totalAnprDetections.incrementAndGet()
        }
    }

    /**
     * Получить метрики для камеры
     */
    fun getCameraMetrics(cameraId: String): CameraMetricsReport? {
        val metrics = metricsByCamera[cameraId] ?: return null
        val stats = detectorStats[cameraId]
        val errors = errorHistoryByCamera[cameraId]?.takeLast(10) ?: emptyList()
        
        val avgProcessingTime = if (metrics.recentProcessingTimes.isNotEmpty()) {
            metrics.recentProcessingTimes.average().toFloat()
        } else {
            0f
        }
        
        val fps = calculateFPS(metrics)
        
        return CameraMetricsReport(
            cameraId = metrics.cameraId,
            cameraName = metrics.cameraName,
            frameSourceKind = metrics.frameSourceKind,
            isRunning = System.currentTimeMillis() - metrics.lastFrameProcessedAt < 60000,
            totalFramesProcessed = metrics.totalFramesProcessed.get(),
            totalFramesSkipped = metrics.totalFramesSkipped.get(),
            totalErrors = metrics.totalErrors.get(),
            averageProcessingTimeMs = avgProcessingTime,
            currentFps = fps,
            lastFrameProcessedAt = metrics.lastFrameProcessedAt,
            lastError = errors.lastOrNull()?.let { e ->
                ErrorReport(e.timestamp, e.errorType.name, e.message)
            },
            errorHistory = errors.map { e ->
                ErrorReport(e.timestamp, e.errorType.name, e.message)
            },
            detectorStats = stats?.let { s ->
                DetectorStatsReport(
                    motionDetections = s.motionDetections.get(),
                    objectDetections = s.objectDetections.get(),
                    faceDetections = s.faceDetections.get(),
                    anprDetections = s.anprDetections.get(),
                    lastMotionDetectionAt = s.lastMotionDetectionAt,
                    lastObjectDetectionAt = s.lastObjectDetectionAt,
                    lastFaceDetectionAt = s.lastFaceDetectionAt,
                    lastAnprDetectionAt = s.lastAnprDetectionAt
                )
            }
        )
    }

    /**
     * Получить глобальные метрики
     */
    fun getGlobalMetrics(): GlobalMetricsReport {
        val activeCameras = metricsByCamera.count { (_, m) ->
            System.currentTimeMillis() - m.lastFrameProcessedAt < 60000
        }
        
        return GlobalMetricsReport(
            totalCameras = metricsByCamera.size,
            activeCameras = activeCameras,
            totalFramesProcessed = globalMetrics.totalFramesProcessed.get(),
            totalFramesSkipped = globalMetrics.totalFramesSkipped.get(),
            totalErrors = globalMetrics.totalErrors.get(),
            totalMotionDetections = globalMetrics.totalMotionDetections.get(),
            totalObjectDetections = globalMetrics.totalObjectDetections.get(),
            totalFaceDetections = globalMetrics.totalFaceDetections.get(),
            totalAnprDetections = globalMetrics.totalAnprDetections.get(),
            lastFrameProcessedAt = globalMetrics.lastFrameProcessedAt,
            lastErrorAt = globalMetrics.lastErrorAt
        )
    }

    /**
     * Получить статус всех камер
     */
    fun getAllCamerasStatus(): List<CameraMetricsReport> {
        return metricsByCamera.keys.mapNotNull { getCameraMetrics(it) }
    }

    /**
     * Проверка здоровья пайплайна (для health check)
     */
    fun isPipelineHealthy(cameraId: String): Boolean {
        val metrics = metricsByCamera[cameraId] ?: return false
        
        // Проверка: нет ошибок последние 5 минут
        val recentErrors = errorHistoryByCamera[cameraId]
            ?.filter { it.timestamp > System.currentTimeMillis() - 5 * 60 * 1000 }
            ?.size ?: 0
        
        if (recentErrors > 10) {
            logger.warn { "Camera $cameraId has too many recent errors: $recentErrors" }
            return false
        }
        
        // Проверка: обрабатываем кадры
        val timeSinceLastFrame = System.currentTimeMillis() - metrics.lastFrameProcessedAt
        if (timeSinceLastFrame > 60000) {
            logger.warn { "Camera $cameraId hasn't processed frames for ${timeSinceLastFrame}ms" }
            return false
        }
        
        return true
    }

    /**
     * Получить рекомендации по оптимизации
     */
    fun getOptimizationRecommendations(cameraId: String): List<String> {
        val recommendations = mutableListOf<String>()
        val metrics = metricsByCamera[cameraId] ?: return recommendations
        
        // Высокая задержка обработки
        val avgProcessingTime = metrics.recentProcessingTimes.average()
        if (avgProcessingTime > FRAME_PROCESSING_INTERVAL_MS) {
            recommendations.add(
                "Высокая задержка обработки: ${avgProcessingTime.toInt()}мс (целевая: ${FRAME_PROCESSING_INTERVAL_MS}мс). " +
                "Рекомендуется: уменьшить разрешение или отключить некоторые детекторы"
            )
        }
        
        // Много пропущенных кадров
        val skipRatio = if (metrics.totalFramesProcessed.get() > 0) {
            metrics.totalFramesSkipped.get().toDouble() / metrics.totalFramesProcessed.get()
        } else {
            0.0
        }
        
        if (skipRatio > 0.1) {
            recommendations.add(
                "Высокий процент пропущенных кадров: ${String.format("%.1f", skipRatio * 100)}%. " +
                "Рекомендуется: увеличить frameProcessingInterval или оптимизировать детекторы"
            )
        }
        
        // Частые ошибки
        val recentErrorRate = errorHistoryByCamera[cameraId]
            ?.filter { it.timestamp > System.currentTimeMillis() - 10 * 60 * 1000 }
            ?.size ?: 0
        
        if (recentErrorRate > 5) {
            recommendations.add(
                "Частые ошибки: $recentErrorRate за последние 10 минут. " +
                "Рекомендуется: проверить логи и конфигурацию детекторов"
            )
        }
        
        return recommendations
    }

    private fun calculateFPS(metrics: CameraMetrics): Float {
        val now = System.currentTimeMillis()
        
        // Считаем кадры за последнюю минуту
        val framesLastMinute = metrics.totalFramesProcessed.get()
        val timeSinceStart = now - metrics.startedAt
        
        return if (timeSinceStart > 0) {
            (framesLastMinute.toDouble() / timeSinceStart * 1000).toFloat()
        } else {
            0f
        }
    }

    companion object {
        private const val RECENT_WINDOW_SIZE = 60 // Последние 60 измерений
        private const val MAX_ERROR_HISTORY_SIZE = 100
        private const val FRAME_PROCESSING_INTERVAL_MS = 1000L // 1 кадр в секунду
    }
}

// ============================================================================
// Data classes
// ============================================================================

data class CameraMetrics(
    val cameraId: String,
    val cameraName: String,
    val frameSourceKind: String,
    val totalFramesProcessed: AtomicLong,
    val totalFramesSkipped: AtomicLong,
    val totalErrors: AtomicLong,
    val startedAt: Long,
    var lastFrameProcessedAt: Long,
    var lastSkippedAt: Long,
    var lastSkipReason: String?,
    var lastErrorAt: Long,
    var lastError: AnalyticsError?,
    val recentProcessingTimes: java.util.LinkedList<Long>
) {
    constructor(cameraId: String, cameraName: String, frameSourceKind: String) : this(
        cameraId = cameraId,
        cameraName = cameraName,
        frameSourceKind = frameSourceKind,
        totalFramesProcessed = AtomicLong(0),
        totalFramesSkipped = AtomicLong(0),
        totalErrors = AtomicLong(0),
        startedAt = System.currentTimeMillis(),
        lastFrameProcessedAt = 0,
        lastSkippedAt = 0,
        lastSkipReason = null,
        lastErrorAt = 0,
        lastError = null,
        recentProcessingTimes = java.util.LinkedList()
    )
}

data class GlobalMetrics(
    val totalFramesProcessed: AtomicLong,
    val totalFramesSkipped: AtomicLong,
    val totalErrors: AtomicLong,
    val totalMotionDetections: AtomicLong,
    val totalObjectDetections: AtomicLong,
    val totalFaceDetections: AtomicLong,
    val totalAnprDetections: AtomicLong,
    var lastFrameProcessedAt: Long,
    var lastErrorAt: Long
) {
    constructor() : this(
        totalFramesProcessed = AtomicLong(0),
        totalFramesSkipped = AtomicLong(0),
        totalErrors = AtomicLong(0),
        totalMotionDetections = AtomicLong(0),
        totalObjectDetections = AtomicLong(0),
        totalFaceDetections = AtomicLong(0),
        totalAnprDetections = AtomicLong(0),
        lastFrameProcessedAt = 0,
        lastErrorAt = 0
    )
}

data class DetectorStatistics(
    val motionDetections: AtomicLong,
    val objectDetections: AtomicLong,
    val faceDetections: AtomicLong,
    val anprDetections: AtomicLong,
    var lastMotionDetectionAt: Long,
    var lastObjectDetectionAt: Long,
    var lastFaceDetectionAt: Long,
    var lastAnprDetectionAt: Long
) {
    constructor() : this(
        motionDetections = AtomicLong(0),
        objectDetections = AtomicLong(0),
        faceDetections = AtomicLong(0),
        anprDetections = AtomicLong(0),
        lastMotionDetectionAt = 0,
        lastObjectDetectionAt = 0,
        lastFaceDetectionAt = 0,
        lastAnprDetectionAt = 0
    )
}

data class AnalyticsError(
    val cameraId: String,
    val timestamp: Long,
    val errorType: AnalyticsErrorType,
    val message: String,
    val stackTrace: String
)

enum class AnalyticsErrorType {
    FRAME_DECODE_ERROR,
    DETECTION_ERROR,
    MODEL_LOAD_ERROR,
    TIMEOUT_ERROR,
    RESOURCE_EXHAUSTED,
    UNKNOWN
}

enum class DetectionType {
    MOTION,
    OBJECT,
    FACE,
    ANPR
}

// Data classes для отчётов

data class CameraMetricsReport(
    val cameraId: String,
    val cameraName: String,
    val frameSourceKind: String,
    val isRunning: Boolean,
    val totalFramesProcessed: Long,
    val totalFramesSkipped: Long,
    val totalErrors: Long,
    val averageProcessingTimeMs: Float,
    val currentFps: Float,
    val lastFrameProcessedAt: Long,
    val lastError: ErrorReport?,
    val errorHistory: List<ErrorReport>,
    val detectorStats: DetectorStatsReport?
)

data class GlobalMetricsReport(
    val totalCameras: Int,
    val activeCameras: Int,
    val totalFramesProcessed: Long,
    val totalFramesSkipped: Long,
    val totalErrors: Long,
    val totalMotionDetections: Long,
    val totalObjectDetections: Long,
    val totalFaceDetections: Long,
    val totalAnprDetections: Long,
    val lastFrameProcessedAt: Long,
    val lastErrorAt: Long
)

data class DetectorStatsReport(
    val motionDetections: Long,
    val objectDetections: Long,
    val faceDetections: Long,
    val anprDetections: Long,
    val lastMotionDetectionAt: Long,
    val lastObjectDetectionAt: Long,
    val lastFaceDetectionAt: Long,
    val lastAnprDetectionAt: Long
)

data class ErrorReport(
    val timestamp: Long,
    val errorType: String,
    val message: String
)
