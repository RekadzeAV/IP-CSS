package com.company.ipcamera.core.decoder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * VAAPI Video Decoder для Linux
 * 
 * Аппаратное декодирование видео с использованием Video Acceleration API
 */
actual class VideoDecoderVAAPI : VideoDecoder {
    
    private var isInitialized = false
    private var decoderRef: Long = 0L // VA Display/Context
    private val _status = MutableStateFlow<DecoderStatus>(DecoderStatus.UNINITIALIZED)
    private var stats = DecoderStats(
        framesDecoded = 0,
        framesDropped = 0,
        decodingTimeMs = 0,
        averageFrameTimeMs = 0.0,
        minFrameTimeMs = Long.MAX_VALUE,
        maxFrameTimeMs = 0,
        cpuUsagePercent = 0.0,
        memoryUsageBytes = 0
    )
    private var frameCount = 0L
    
    override suspend fun initialize(config: DecoderConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            _status.value = DecoderStatus.INITIALIZING
            
            // Проверка поддержки VAAPI
            if (!isHardwareDecoderSupported()) {
                logger.error { "VAAPI not supported on this system" }
                _status.value = DecoderStatus.ERROR
                return@withContext false
            }
            
            // Проверка формата
            if (!isFormatSupported(config.format, useHardware = true)) {
                logger.error { "Format ${config.format} not supported by VAAPI" }
                _status.value = DecoderStatus.ERROR
                return@withContext false
            }
            
            // Проверка разрешения
            val maxRes = getMaxSupportedResolution()
            if (config.width > maxRes.width || config.height > maxRes.height) {
                logger.error { "Resolution ${config.width}x${config.height} exceeds max ${maxRes.width}x${maxRes.height}" }
                _status.value = DecoderStatus.ERROR
                return@withContext false
            }
            
            // Инициализация VAAPI декодера
            decoderRef = initVAAPI(
                format = config.format.toNativeFormat(),
                width = config.width,
                height = config.height,
                outputFormat = config.outputFormat.toNativeFormat()
            )
            
            if (decoderRef == 0L) {
                logger.error { "Failed to initialize VAAPI decoder" }
                _status.value = DecoderStatus.ERROR
                return@withContext false
            }
            
            isInitialized = true
            _status.value = DecoderStatus.READY
            
            logger.info { "VAAPI decoder initialized: ${config.width}x${config.height} ${config.format}" }
            true
            
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize VAAPI decoder" }
            _status.value = DecoderStatus.ERROR
            false
        }
    }
    
    override suspend fun decode(frame: ByteArray, timestamp: Long): DecodedFrame? = 
        withContext(Dispatchers.IO) {
            if (!isInitialized) {
                logger.error { "Decoder not initialized" }
                return@withContext null
            }
            
            try {
                _status.value = DecoderStatus.DECODING
                
                val startTime = System.currentTimeMillis()
                
                // Декодирование через VAAPI
                val decodedData = decodeVAAPIFrame(decoderRef, frame, timestamp)
                
                if (decodedData == null) {
                    stats = stats.copy(framesDropped = stats.framesDropped + 1)
                    _status.value = DecoderStatus.READY
                    return@withContext null
                }
                
                val endTime = System.currentTimeMillis()
                val frameTime = endTime - startTime
                
                // Обновление статистики
                frameCount++
                stats = stats.copy(
                    framesDecoded = stats.framesDecoded + 1,
                    decodingTimeMs = stats.decodingTimeMs + frameTime,
                    averageFrameTimeMs = (stats.decodingTimeMs + frameTime).toDouble() / frameCount,
                    minFrameTimeMs = minOf(stats.minFrameTimeMs, frameTime),
                    maxFrameTimeMs = maxOf(stats.maxFrameTimeMs, frameTime)
                )
                
                _status.value = DecoderStatus.READY
                
                DecodedFrame(
                    data = decodedData,
                    width = stats.width,
                    height = stats.height,
                    timestamp = timestamp,
                    isKeyFrame = true,
                    format = PixelFormat.RGBA,
                    stride = stats.width * 4,
                    pts = timestamp,
                    dts = timestamp
                )
                
            } catch (e: Exception) {
                logger.error(e) { "Failed to decode frame" }
                _status.value = DecoderStatus.ERROR
                null
            }
        }
    
    override suspend fun decodeWithBuffer(buffer: VideoBuffer): DecodedFrame? {
        return decode(buffer.data, buffer.timestamp)
    }
    
    override fun getStatus(): StateFlow<DecoderStatus> = _status.asStateFlow()
    
    override fun getStats(): DecoderStats {
        val cpuUsage = getCpuUsage()
        val memoryUsage = getMemoryUsage()
        
        return stats.copy(
            cpuUsagePercent = cpuUsage,
            memoryUsageBytes = memoryUsage
        )
    }
    
    override fun close() {
        try {
            if (isInitialized && decoderRef != 0L) {
                releaseVAAPI(decoderRef)
                decoderRef = 0L
                isInitialized = false
            }
            _status.value = DecoderStatus.CLOSED
            logger.info { "VAAPI decoder closed" }
        } catch (e: Exception) {
            logger.error(e) { "Error closing VAAPI decoder" }
        }
    }
    
    companion object {
        private val stats = object {
            var width = 0
            var height = 0
        }
    }
}

/**
 * Linux-специфичная реализация проверки поддержки
 */
actual fun isHardwareDecoderSupported(): Boolean = runCatching {
    checkVAAPISupport()
}.getOrDefault(false)

/**
 * Проверка поддержки VAAPI
 */
private external fun checkVAAPISupport(): Boolean

/**
 * Инициализация VAAPI декодера
 */
private external fun initVAAPI(
    format: Int,
    width: Int,
    height: Int,
    outputFormat: Int
): Long

/**
 * Декодирование кадра через VAAPI
 */
private external fun decodeVAAPIFrame(
    decoderRef: Long,
    inputFrame: ByteArray,
    timestamp: Long
): ByteArray?

/**
 * Освобождение ресурсов VAAPI
 */
private external fun releaseVAAPI(decoderRef: Long)

/**
 * Получение использования CPU
 */
private external fun getCpuUsage(): Double

/**
 * Получение использования памяти
 */
private external fun getMemoryUsage(): Long

/**
 * Конвертация формата видео в нативный
 */
private fun VideoFormat.toNativeFormat(): Int = when (this) {
    VideoFormat.H264 -> 1
    VideoFormat.H265 -> 2
    VideoFormat.MPEG4 -> 3
    else -> 0
}

/**
 * Конвертация формата пикселей в нативный
 */
private fun PixelFormat.toNativeFormat(): Int = when (this) {
    PixelFormat.RGBA -> 1
    PixelFormat.NV12 -> 2
    PixelFormat.I420 -> 3
    else -> 0
}

/**
 * Linux-специфичная реализация предпочтительного декодера
 */
actual fun getPreferredDecoder(): DecoderType = DecoderType.VAAPI

/**
 * Linux-специфичная реализация создания декодера
 */
actual fun createDecoder(config: DecoderConfig): VideoDecoder? {
    return if (config.useHardwareAcceleration && isHardwareDecoderSupported()) {
        VideoDecoderVAAPI().also {
            it.initialize(config)
        }
    } else {
        null
    }
}

/**
 * Linux-специфичная реализация программного декодера
 */
actual fun createSoftwareDecoder(config: DecoderConfig): VideoDecoder {
    return VideoDecoderSoftware()
}

/**
 * Linux-специфичная реализация проверки формата
 */
actual fun isFormatSupported(format: VideoFormat, useHardware: Boolean): Boolean {
    return when (format) {
        VideoFormat.H264, VideoFormat.H265 -> useHardware && isHardwareDecoderSupported()
        VideoFormat.MPEG4 -> true
        else -> false
    }
}

/**
 * Linux-специфичная реализация получения поддерживаемых разрешений
 */
actual fun getSupportedResolutions(): List<IntWidthHeight> = listOf(
    IntWidthHeight(640, 480),
    IntWidthHeight(1280, 720),
    IntWidthHeight(1920, 1080),
    IntWidthHeight(2560, 1440),
    IntWidthHeight(3840, 2160)
)

/**
 * Linux-специфичная реализация получения максимального разрешения
 */
actual fun getMaxSupportedResolution(): IntWidthHeight = IntWidthHeight(3840, 2160)

/**
 * Программный декодер (fallback)
 */
class VideoDecoderSoftware : VideoDecoder {
    private val _status = MutableStateFlow(DecoderStatus.READY)
    
    override suspend fun initialize(config: DecoderConfig): Boolean {
        _status.value = DecoderStatus.READY
        return true
    }
    
    override suspend fun decode(frame: ByteArray, timestamp: Long): DecodedFrame? {
        return DecodedFrame(
            data = frame,
            width = 1920,
            height = 1080,
            timestamp = timestamp,
            isKeyFrame = true,
            format = PixelFormat.RGBA,
            stride = 1920 * 4,
            pts = timestamp,
            dts = timestamp
        )
    }
    
    override suspend fun decodeWithBuffer(buffer: VideoBuffer): DecodedFrame? = 
        decode(buffer.data, buffer.timestamp)
    
    override fun getStatus(): StateFlow<DecoderStatus> = _status
    override fun getStats(): DecoderStats = DecoderStats(0, 0, 0, 0.0, 0, 0, 0.0, 0)
    override fun close() {}
}
