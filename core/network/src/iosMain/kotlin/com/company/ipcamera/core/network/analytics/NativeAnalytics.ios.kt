package com.company.ipcamera.core.network.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS заглушка NativeAnalytics.
 *
 * Нативная библиотека аналитики (cinterop) для iOS не подключается в текущей конфигурации.
 * Все методы возвращают null/false; аналитика на iOS недоступна до сборки libanalytics для iOS
 * и добавления cinterop в core/network для iosX64/iosArm64.
 */
actual class NativeAnalytics {

    actual fun getVersion(): String = "ios-stub"

    actual fun createMotionDetector(
        width: Int,
        height: Int,
        threshold: Float,
        minArea: Int
    ): Long? = null

    actual suspend fun detectMotion(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): MotionDetectionResult? = withContext(Dispatchers.Default) { null }

    actual fun destroyMotionDetector(handle: Long) {}

    actual fun createObjectDetector(
        confidenceThreshold: Float,
        maxObjects: Int,
        useGPU: Boolean,
        inputSize: Int
    ): Long? = null

    actual suspend fun loadObjectDetectorModel(
        handle: Long,
        modelPath: String
    ): Boolean = withContext(Dispatchers.Default) { false }

    actual suspend fun detectObjects(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): ObjectDetectionResult? = withContext(Dispatchers.Default) { null }

    actual fun destroyObjectDetector(handle: Long) {}

    actual fun createFaceDetector(
        scaleFactor: Float,
        minNeighbors: Int,
        minSize: Int,
        maxSize: Int
    ): Long? = null

    actual suspend fun loadFaceDetectorCascade(
        handle: Long,
        cascadePath: String
    ): Boolean = withContext(Dispatchers.Default) { false }

    actual suspend fun detectFaces(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): FaceDetectionResult? = withContext(Dispatchers.Default) { null }

    actual fun destroyFaceDetector(handle: Long) {}

    actual fun createANPREngine(
        confidenceThreshold: Float,
        language: String
    ): Long? = null

    actual suspend fun recognizeLicensePlates(
        handle: Long,
        frameData: ByteArray,
        width: Int,
        height: Int
    ): ANPRResult? = withContext(Dispatchers.Default) { null }

    actual fun destroyANPREngine(handle: Long) {}

    actual fun createObjectTracker(
        iouThreshold: Float,
        maxAge: Int,
        minConfidence: Float
    ): Long? = null

    actual suspend fun updateTracking(
        handle: Long,
        detections: ObjectDetectionResult,
        timestampMs: Long
    ): TrackingResult? = withContext(Dispatchers.Default) { null }

    actual fun destroyObjectTracker(handle: Long) {}
}
