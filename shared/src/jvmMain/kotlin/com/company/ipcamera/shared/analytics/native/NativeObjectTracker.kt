package com.company.ipcamera.shared.analytics.native

import com.company.ipcamera.shared.analytics.native.NativeObjectDetector.DetectionResult

/**
 * Нативная обертка для трекера объектов
 */
class NativeObjectTracker private constructor(private val handle: Long) {
    data class TrackingResult(
        var objectCount: Int = 0,
        var objects: Array<TrackedObject>? = null,
    )

    data class TrackedObject(
        var id: Int = 0,
        var type: Int = 0,
        var confidence: Float = 0.0f,
        var x: Int = 0,
        var y: Int = 0,
        var width: Int = 0,
        var height: Int = 0,
        var lastSeen: Long = 0,
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
            iouThreshold: Float,
            maxAge: Int,
            minConfidence: Float,
        ): Long

        @JvmStatic
        external fun nativeDestroy(handle: Long)

        @JvmStatic
        external fun nativeUpdate(
            handle: Long,
            detections: DetectionResult,
            result: TrackingResult,
        ): Boolean

        @JvmStatic
        fun create(
            iouThreshold: Float = 0.3f,
            maxAge: Int = 30,
            minConfidence: Float = 0.5f,
        ): NativeObjectTracker? {
            val handle = nativeCreate(iouThreshold, maxAge, minConfidence)
            return if (handle != 0L) NativeObjectTracker(handle) else null
        }
    }

    fun update(detections: DetectionResult): TrackingResult? {
        val result = TrackingResult()
        val success = nativeUpdate(handle, detections, result)
        return if (success) result else null
    }

    fun destroy() {
        nativeDestroy(handle)
    }
}
