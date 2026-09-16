package com.company.ipcamera.core.decoder

import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс видео декодера
 * 
 * Абстракция для аппаратного и программной декодирования видео
 */
interface VideoDecoder {
    
    /**
     * Инициализировать декодер с указанными параметрами
     */
    suspend fun initialize(config: DecoderConfig): Boolean
    
    /**
     * Декодировать видеопоток
     */
    suspend fun decode(frame: ByteArray, timestamp: Long): DecodedFrame?
    
    /**
     * Декодировать видеопоток с буфером
     */
    suspend fun decodeWithBuffer(buffer: VideoBuffer): DecodedFrame?
    
    /**
     * Получить текущий статус декодера
     */
    fun getStatus(): StateFlow<DecoderStatus>
    
    /**
     * Получить статистику декодирования
     */
    fun getStats(): DecoderStats
    
    /**
     * Освободить ресурсы
     */
    fun close()
}

/**
 * Конфигурация декодера
 */
data class DecoderConfig(
    val format: VideoFormat,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val useHardwareAcceleration: Boolean = true,
    val maxBufferFrames: Int = 10,
    val outputFormat: PixelFormat = PixelFormat.RGBA
)

/**
 * Формат видео
 */
enum class VideoFormat {
    H264,   ///< H.264 / AVC
    H265,   ///< H.265 / HEVC
    MPEG4,  ///< MPEG-4
    VP8,    ///< VP8
    VP9,    ///< VP9
    UNKNOWN
}

/**
 * Формат пикселей
 */
enum class PixelFormat {
    RGBA,   ///< RGBA (32-bit)
    RGB24,  ///< RGB24 (24-bit)
    NV12,   ///< NV12 (YUV)
    I420,   ///< I420 (YUV)
    YUYV,   ///< YUYV (YUV)
    UNKNOWN
}

/**
 * Состояние декодера
 */
enum class DecoderStatus {
    UNINITIALIZED,  ///< Не инициализирован
    INITIALIZING,   ///< Инициализация
    READY,          ///< Готов к работе
    DECODING,       ///< Декодирование
    PAUSED,         ///< Приостановлен
    ERROR,          ///< Ошибка
    CLOSED          ///< Закрыт
}

/**
 * Декодированный кадр
 */
data class DecodedFrame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val isKeyFrame: Boolean,
    val format: PixelFormat,
    val stride: Int,
    val pts: Long,    ///< Presentation timestamp
    val dts: Long     ///< Decode timestamp
)

/**
 * Видеовой буфер
 */
data class VideoBuffer(
    val data: ByteArray,
    val size: Int,
    val timestamp: Long,
    val isKeyFrame: Boolean
)

/**
 * Статистика декодирования
 */
data class DecoderStats(
    val framesDecoded: Long,
    val framesDropped: Long,
    val decodingTimeMs: Long,
    val averageFrameTimeMs: Double,
    val minFrameTimeMs: Long,
    val maxFrameTimeMs: Long,
    val cpuUsagePercent: Double,
    val memoryUsageBytes: Long
)

/**
 * Информация о поддержке аппаратного декодирования
 */
data class HardwareDecoderInfo(
    val supported: Boolean,
    val decoderType: DecoderType,
    val supportedFormats: List<VideoFormat>,
    val maxResolution: IntWidthHeight,
    val capabilities: DecoderCapabilities
)

/**
 * Тип декодера
 */
enum class DecoderType {
    SOFTWARE,     ///< Программный декодер (x264, x265)
    DXVA2,        ///< DirectX Video Acceleration 2 (Windows)
    VAAPI,        ///< Video Acceleration API (Linux)
    VIDEOTOOLBOX, ///< Video Toolbox (macOS/iOS)
    QSV,          ///< Quick Sync Video (Intel)
    NVDEC,        ///< NVIDIA Decoder
    UNKNOWN
}

/**
 * Максимальное разрешение
 */
data class IntWidthHeight(
    val width: Int,
    val height: Int
)

/**
 * Возможности декодера
 */
data class DecoderCapabilities(
    val maxConcurrentStreams: Int,
    val supportedResolutions: List<IntWidthHeight>,
    val supportsBFrames: Boolean,
    val supportsRefFrames: Boolean,
    val maxReferenceFrames: Int
)
