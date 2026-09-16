package com.company.ipcamera.shared.platform.hwaccel

/**
 * Детектор доступного аппаратного ускорения видео
 */
expect object HardwareAccelerationDetector {
    /**
     * Определяет доступный тип аппаратного ускорения
     */
    suspend fun detectAvailableAcceleration(): HardwareAccelerationType

    /**
     * Возвращает оптимальный энкодер для заданного кодека и разрешения
     */
    suspend fun getOptimalEncoder(
        codec: VideoCodec,
        resolution: com.company.ipcamera.core.common.model.Resolution,
    ): EncoderConfig?

    /**
     * Возвращает оптимальный декодер для заданного кодека
     */
    suspend fun getOptimalDecoder(codec: VideoCodec): DecoderConfig?
}
