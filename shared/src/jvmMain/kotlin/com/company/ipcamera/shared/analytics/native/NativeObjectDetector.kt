package com.company.ipcamera.shared.analytics.native

/**
 * Нативная обертка для детектора объектов
 */
class NativeObjectDetector private constructor(private val handle: Long) {
    data class DetectionResult(
        var objectCount: Int = 0,
        var objects: Array<DetectedObject>? = null,
    )

    data class DetectedObject(
        var type: Int = 0,
        var confidence: Float = 0.0f,
        var x: Int = 0,
        var y: Int = 0,
        var width: Int = 0,
        var height: Int = 0,
    )

    companion object {
        init {
            try {
                System.loadLibrary("analytics")
            } catch (e: UnsatisfiedLinkError) {
                System.err.println("Failed to load analytics library: ${e.message}")
            }
        }

        @JvmStatic
        external fun nativeCreate(
            confidenceThreshold: Float,
            maxObjects: Int,
            useGPU: Boolean,
        ): Long

        @JvmStatic
        external fun nativeDestroy(handle: Long)

        @JvmStatic
        external fun nativeLoadModel(
            handle: Long,
            modelPath: String,
        ): Boolean

        @JvmStatic
        external fun nativeIsModelLoaded(handle: Long): Boolean

        @JvmStatic
        external fun nativeDetect(
            handle: Long,
            frameData: ByteArray,
            width: Int,
            height: Int,
            result: DetectionResult,
        ): Boolean

        @JvmStatic
        fun create(
            confidenceThreshold: Float = 0.5f,
            maxObjects: Int = 10,
            useGPU: Boolean = false,
        ): NativeObjectDetector? {
            val handle = nativeCreate(confidenceThreshold, maxObjects, useGPU)
            return if (handle != 0L) NativeObjectDetector(handle) else null
        }
    }

    fun loadModel(modelPath: String): Boolean {
        return nativeLoadModel(handle, modelPath)
    }

    fun isModelLoaded(): Boolean {
        return nativeIsModelLoaded(handle)
    }

    fun detect(
        frameData: ByteArray,
        width: Int,
        height: Int,
    ): DetectionResult? {
        val result = DetectionResult()
        val success = nativeDetect(handle, frameData, width, height, result)
        return if (success) result else null
    }

    fun destroy() {
        nativeDestroy(handle)
    }
}
