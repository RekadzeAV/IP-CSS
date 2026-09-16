package com.company.ipcamera.core.decoder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Утилита для определения поддержки аппаратного декодирования
 */
expect class HardwareDecoderSupport {
    
    /**
     * Проверить поддержку аппаратного декодирования
     */
    fun isHardwareDecoderSupported(): Boolean
    
    /**
     * Получить информацию о поддержке декодера
     */
    fun getDecoderInfo(): HardwareDecoderInfo
    
    /**
     * Получить список доступных декодеров
     */
    fun getAvailableDecoders(): List<DecoderType>
    
    /**
     * Получить статус декодера
     */
    fun getStatus(): StateFlow<DecoderSupportStatus>
}

/**
 * Статус поддержки декодера
 */
data class DecoderSupportStatus(
    val isSupported: Boolean,
    val decoderType: DecoderType?,
    val error: String? = null,
    val recommendedDecoder: DecoderType
)

/**
 * Получить предпочтительный декодер для платформы
 */
expect fun getPreferredDecoder(): DecoderType

/**
 * Создать декодер для указанной конфигурации
 */
expect fun createDecoder(config: DecoderConfig): VideoDecoder?

/**
 * Создать программный декодер (fallback)
 */
expect fun createSoftwareDecoder(config: DecoderConfig): VideoDecoder

/**
 * Проверить поддержку формата видео
 */
expect fun isFormatSupported(format: VideoFormat, useHardware: Boolean): Boolean

/**
 * Получить список поддерживаемых разрешений
 */
expect fun getSupportedResolutions(): List<IntWidthHeight>

/**
 * Получить максимальное поддерживаемое разрешение
 */
expect fun getMaxSupportedResolution(): IntWidthHeight

/**
 * Создать интеллектуальный декодер (автоматический выбор)
 */
fun createSmartDecoder(config: DecoderConfig): VideoDecoder {
    return try {
        // Пытаемся создать аппаратный декодер
        if (config.useHardwareAcceleration) {
            createDecoder(config)?.let { return it }
        }
    } catch (e: Exception) {
        // Игнорируем ошибку, перейдём к программному
    }
    
    // Fallback на программный декодер
    return createSoftwareDecoder(config.copy(useHardwareAcceleration = false))
}

/**
 * Фабрика декодеров
 */
class DecoderFactory {
    
    private val cache = mutableMapOf<DecoderType, VideoDecoder>()
    
    /**
     * Получить или создать декодер
     */
    fun getDecoder(type: DecoderType, config: DecoderConfig): VideoDecoder {
        return cache.getOrPut(type) {
            createDecoder(config) ?: createSoftwareDecoder(config)
        }
    }
    
    /**
     * Освободить все декодеры
     */
    fun closeAll() {
        cache.values.forEach { it.close() }
        cache.clear()
    }
    
    /**
     * Освободить конкретный декодер
     */
    fun closeDecoder(type: DecoderType) {
        cache.remove(type)?.close()
    }
}

/**
 * Расширение для получения строкового представления декодера
 */
val DecoderType.displayName: String
    get() = when (this) {
        DecoderType.SOFTWARE -> "Software"
        DecoderType.DXVA2 -> "DXVA2 (DirectX)"
        DecoderType.VAAPI -> "VAAPI (Linux)"
        DecoderType.VIDEOTOOLBOX -> "VideoToolbox (Apple)"
        DecoderType.QSV -> "Quick Sync (Intel)"
        DecoderType.NVDEC -> "NVIDIA Decoder"
        DecoderType.UNKNOWN -> "Unknown"
    }

/**
 * Расширение для получения строкового представления формата
 */
val VideoFormat.displayName: String
    get() = when (this) {
        VideoFormat.H264 -> "H.264 / AVC"
        VideoFormat.H265 -> "H.265 / HEVC"
        VideoFormat.MPEG4 -> "MPEG-4"
        VideoFormat.VP8 -> "VP8"
        VideoFormat.VP9 -> "VP9"
        VideoFormat.UNKNOWN -> "Unknown"
    }

/**
 * Расширение для получения строкового представления формата пикселей
 */
val PixelFormat.displayName: String
    get() = when (this) {
        PixelFormat.RGBA -> "RGBA (32-bit)"
        PixelFormat.RGB24 -> "RGB24 (24-bit)"
        PixelFormat.NV12 -> "NV12 (YUV)"
        PixelFormat.I420 -> "I420 (YUV)"
        PixelFormat.YUYV -> "YUYV (YUV)"
        PixelFormat.UNKNOWN -> "Unknown"
    }

/**
 * Проверка поддержки H.264
 */
fun isH264Supported(useHardware: Boolean = true): Boolean = 
    isFormatSupported(VideoFormat.H264, useHardware)

/**
 * Проверка поддержки H.265
 */
fun isH265Supported(useHardware: Boolean = true): Boolean = 
    isFormatSupported(VideoFormat.H265, useHardware)

/**
 * Проверка поддержки 4K
 */
fun is4KSupported(useHardware: Boolean = true): Boolean {
    val maxRes = getMaxSupportedResolution()
    return maxRes.width >= 3840 && maxRes.height >= 2160
}

/**
 * Проверка поддержки 1080p
 */
fun is1080pSupported(useHardware: Boolean = true): Boolean {
    val maxRes = getMaxSupportedResolution()
    return maxRes.width >= 1920 && maxRes.height >= 1080
}
