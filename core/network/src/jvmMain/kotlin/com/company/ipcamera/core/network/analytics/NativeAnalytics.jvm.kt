package com.company.ipcamera.core.network.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * JVM реализация NativeAnalytics через JNI
 * Использует существующий analytics_jni_native.cpp для вызова нативных функций
 */
actual class NativeAnalytics {
    private var nativeAvailable: Boolean = false

    init {
        // Загрузка нативной библиотеки при первом использовании
        try {
            System.loadLibrary("analytics")
            nativeAvailable = true
        } catch (e: UnsatisfiedLinkError) {
            try {
                val os = System.getProperty("os.name").lowercase()
                val arch = System.getProperty("os.arch").lowercase()
                val libName = when {
                    os.contains("win") -> "analytics.dll"
                    os.contains("mac") -> "libanalytics.dylib"
                    else -> "libanalytics.so"
                }
                val libPath = when {
                    os.contains("win") -> "native/analytics/lib/windows/x64/$libName"
                    os.contains("mac") -> {
                        val macArch = if (arch.contains("aarch64") || arch.contains("arm")) "arm64" else "x64"
                        "native/analytics/lib/macos/$macArch/$libName"
                    }
                    else -> "native/analytics/lib/linux/x64/$libName"
                }
                val libFile = File(libPath)
                if (libFile.exists()) {
                    System.load(libFile.absolutePath)
                    nativeAvailable = true
                } else {
                    println("Warning: analytics native library not found at: ${libFile.absolutePath}")
                }
            } catch (e2: Exception) {
                println("Warning: analytics native library not loaded: ${e2.message}")
            }
        }
    }

    private external fun nativeGetVersion(): String?
    actual fun getVersion(): String {
        if (!nativeAvailable) return "unavailable"
        return try {
            nativeGetVersion() ?: "unavailable"
        } catch (_: Throwable) {
            "unavailable"
        }
    }

    private external fun nativeCreateMotionDetector(width: Int, height: Int, threshold: Float, minArea: Int): Long
    private external fun nativeDestroyMotionDetector(handle: Long)
    private external fun nativeDetectMotion(handle: Long, frameData: ByteArray, width: Int, height: Int): MotionDetectionResult?

    private external fun nativeCreateObjectDetector(
        confidenceThreshold: Float,
        maxObjects: Int,
        useGPU: Boolean,
        inputSize: Int
    ): Long
    private external fun nativeDestroyObjectDetector(handle: Long)
    private external fun nativeLoadObjectDetectorModel(handle: Long, modelPath: String): Boolean
    private external fun nativeDetectObjects(handle: Long, frameData: ByteArray, width: Int, height: Int): ObjectDetectionResult?

    private external fun nativeCreateFaceDetector(scaleFactor: Float, minNeighbors: Int, minSize: Int, maxSize: Int): Long
    private external fun nativeDestroyFaceDetector(handle: Long)
    private external fun nativeLoadFaceDetectorCascade(handle: Long, cascadePath: String): Boolean
    private external fun nativeDetectFaces(handle: Long, frameData: ByteArray, width: Int, height: Int): FaceDetectionResult?

    private external fun nativeCreateANPREngine(confidenceThreshold: Float, language: String): Long
    private external fun nativeDestroyANPREngine(handle: Long)
    private external fun nativeRecognizeLicensePlates(handle: Long, frameData: ByteArray, width: Int, height: Int): ANPRResult?

    private external fun nativeCreateObjectTracker(iouThreshold: Float, maxAge: Int, minConfidence: Float): Long
    private external fun nativeDestroyObjectTracker(handle: Long)
    private external fun nativeUpdateTracking(handle: Long, detections: ObjectDetectionResult, timestampMs: Long): TrackingResult?

    actual fun createMotionDetector(width: Int, height: Int, threshold: Float, minArea: Int): Long? =
        if (!nativeAvailable) null else try { val h = nativeCreateMotionDetector(width, height, threshold, minArea); if (h != 0L) h else null } catch (
            _: Throwable
        ) { null }
    actual suspend fun detectMotion(handle: Long, frameData: ByteArray, width: Int, height: Int): MotionDetectionResult? =
        withContext(Dispatchers.Default) {
            if (!nativeAvailable) {
                null
            } else {
                try {
                    nativeDetectMotion(
                        handle,
                        frameData,
                        width,
                        height
                    )
                } catch (_: Throwable) { null }
            }
        }
    actual fun destroyMotionDetector(handle: Long) = if (nativeAvailable) try { nativeDestroyMotionDetector(handle) } catch (
        _: Throwable
    ) { } else Unit

    actual fun createObjectDetector(confidenceThreshold: Float, maxObjects: Int, useGPU: Boolean, inputSize: Int): Long? =
        if (!nativeAvailable) {
            null
        } else {
            try {
                val h = nativeCreateObjectDetector(
                    confidenceThreshold,
                    maxObjects,
                    useGPU,
                    inputSize
                ); if (h != 0L) h else null
            } catch (_: Throwable) { null }
        }
    actual suspend fun loadObjectDetectorModel(handle: Long, modelPath: String): Boolean =
        withContext(Dispatchers.Default) {
            if (!nativeAvailable) {
                false
            } else {
                try {
                    nativeLoadObjectDetectorModel(
                        handle,
                        modelPath
                    )
                } catch (_: Throwable) { false }
            }
        }
    actual suspend fun detectObjects(handle: Long, frameData: ByteArray, width: Int, height: Int): ObjectDetectionResult? =
        withContext(Dispatchers.Default) {
            if (!nativeAvailable) {
                null
            } else {
                try {
                    nativeDetectObjects(
                        handle,
                        frameData,
                        width,
                        height
                    )
                } catch (_: Throwable) { null }
            }
        }
    actual fun destroyObjectDetector(handle: Long) = if (nativeAvailable) try { nativeDestroyObjectDetector(handle) } catch (
        _: Throwable
    ) { } else Unit

    actual fun createFaceDetector(scaleFactor: Float, minNeighbors: Int, minSize: Int, maxSize: Int): Long? =
        if (!nativeAvailable) {
            null
        } else {
            try {
                val h = nativeCreateFaceDetector(
                    scaleFactor,
                    minNeighbors,
                    minSize,
                    maxSize
                ); if (h != 0L) h else null
            } catch (_: Throwable) { null }
        }
    actual suspend fun loadFaceDetectorCascade(handle: Long, cascadePath: String): Boolean =
        withContext(Dispatchers.Default) {
            if (!nativeAvailable) {
                false
            } else {
                try {
                    nativeLoadFaceDetectorCascade(
                        handle,
                        cascadePath
                    )
                } catch (_: Throwable) { false }
            }
        }
    actual suspend fun detectFaces(handle: Long, frameData: ByteArray, width: Int, height: Int): FaceDetectionResult? =
        withContext(Dispatchers.Default) {
            if (!nativeAvailable) {
                null
            } else {
                try {
                    nativeDetectFaces(
                        handle,
                        frameData,
                        width,
                        height
                    )
                } catch (_: Throwable) { null }
            }
        }
    actual fun destroyFaceDetector(handle: Long) = if (nativeAvailable) try { nativeDestroyFaceDetector(handle) } catch (
        _: Throwable
    ) { } else Unit

    actual fun createANPREngine(confidenceThreshold: Float, language: String): Long? =
        if (!nativeAvailable) null else try { val h = nativeCreateANPREngine(confidenceThreshold, language); if (h != 0L) h else null } catch (
            _: Throwable
        ) { null }
    actual suspend fun recognizeLicensePlates(handle: Long, frameData: ByteArray, width: Int, height: Int): ANPRResult? =
        withContext(Dispatchers.Default) {
            if (!nativeAvailable) {
                null
            } else {
                try {
                    nativeRecognizeLicensePlates(
                        handle,
                        frameData,
                        width,
                        height
                    )
                } catch (_: Throwable) { null }
            }
        }
    actual fun destroyANPREngine(handle: Long) = if (nativeAvailable) try { nativeDestroyANPREngine(handle) } catch (
        _: Throwable
    ) { } else Unit

    actual fun createObjectTracker(iouThreshold: Float, maxAge: Int, minConfidence: Float): Long? =
        if (!nativeAvailable) null else try { val h = nativeCreateObjectTracker(iouThreshold, maxAge, minConfidence); if (h != 0L) h else null } catch (
            _: Throwable
        ) { null }
    actual suspend fun updateTracking(handle: Long, detections: ObjectDetectionResult, timestampMs: Long): TrackingResult? =
        withContext(Dispatchers.Default) {
            if (!nativeAvailable) {
                null
            } else {
                try {
                    nativeUpdateTracking(
                        handle,
                        detections,
                        timestampMs
                    )
                } catch (_: Throwable) { null }
            }
        }
    actual fun destroyObjectTracker(handle: Long) = if (nativeAvailable) try { nativeDestroyObjectTracker(handle) } catch (
        _: Throwable
    ) { } else Unit
}
