package com.company.ipcamera.shared.analytics.native

/**
 * Нативная обертка для детектора движения
 */
class NativeMotionDetector private constructor(
    private val handle: Long,
) {
    data class MotionResult(
        var motionDetected: Boolean = false,
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
            width: Int,
            height: Int,
            threshold: Float,
            minArea: Int,
        ): Long

        @JvmStatic
        external fun nativeDestroy(handle: Long)

        @JvmStatic
        external fun nativeDetect(
            handle: Long,
            frameData: ByteArray,
            width: Int,
            height: Int,
            result: MotionResult,
        ): Boolean

        @JvmStatic
        fun create(
            width: Int,
            height: Int,
            threshold: Float = 0.5f,
            minArea: Int = 500,
        ): NativeMotionDetector? {
            val handle = nativeCreate(width, height, threshold, minArea)
            return if (handle != 0L) NativeMotionDetector(handle) else null
        }
    }

    fun detect(
        frameData: ByteArray,
        width: Int,
        height: Int,
    ): MotionResult? {
        val result = MotionResult()
        val success = nativeDetect(handle, frameData, width, height, result)
        return if (success) result else null
    }

    fun destroy() {
        nativeDestroy(handle)
    }
}
