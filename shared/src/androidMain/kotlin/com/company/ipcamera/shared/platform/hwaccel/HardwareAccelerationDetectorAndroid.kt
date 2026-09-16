package com.company.ipcamera.shared.platform.hwaccel

import com.company.ipcamera.core.common.model.Resolution

/**
 * Android реализация детектора аппаратного ускорения
 */
actual object HardwareAccelerationDetector {
    actual suspend fun detectAvailableAcceleration(): HardwareAccelerationType {
        // Android: MediaCodec используется для HW ускорения
        return HardwareAccelerationType.None
    }

    actual suspend fun getOptimalEncoder(
        codec: VideoCodec,
        resolution: Resolution,
    ): EncoderConfig? {
        return null
    }

    actual suspend fun getOptimalDecoder(codec: VideoCodec): DecoderConfig? {
        return null
    }
}