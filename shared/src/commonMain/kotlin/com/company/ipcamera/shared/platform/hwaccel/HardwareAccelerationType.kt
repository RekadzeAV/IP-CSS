package com.company.ipcamera.shared.platform.hwaccel

/**
 * Тип аппаратного ускорения видео
 */
sealed class HardwareAccelerationType {
    /** Нет аппаратного ускорения */
    data object None : HardwareAccelerationType()

    /** Intel Quick Sync Video */
    data class IntelQSV(val generation: String) : HardwareAccelerationType()

    /** NVIDIA NVENC/NVDEC */
    data class NVIDIA(val encoder: String, val decoder: String) : HardwareAccelerationType()

    /** AMD VAAPI */
    data class AMDVAAPI(val driver: String) : HardwareAccelerationType()

    /** ARM Mali (V4L2) */
    data class ARMMali(val version: String) : HardwareAccelerationType()

    /** Apple VideoToolbox */
    data object AppleVideoToolbox : HardwareAccelerationType()

    /** Android MediaCodec */
    data object AndroidMediaCodec : HardwareAccelerationType()
}