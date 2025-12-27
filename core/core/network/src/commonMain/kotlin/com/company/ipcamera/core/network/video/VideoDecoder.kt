package com.company.ipcamera.core.network.video

import com.company.ipcamera.core.network.RtspFrame
import java.awt.image.BufferedImage

/**
 * Тип видеокодека
 */
enum class VideoCodec {
    H264,
    H265,
    MJPEG,
    UNKNOWN
}

/**
 * Декодированный видеокадр
 */
data class DecodedVideoFrame(
    val data: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val format: PixelFormat
) {
    enum class PixelFormat {
        YUV420,
        RGB24
    }
}

/**
 * Callback для получения декодированных кадров
 */
typealias DecodedFrameCallback = (DecodedVideoFrame) -> Unit

/**
 * Видео декодер для H.264/H.265/MJPEG
 */
expect class VideoDecoder {
    /**
     * Создать декодер для указанного кодека
     */
    constructor(codec: VideoCodec, width: Int, height: Int)

    /**
     * Декодировать кадр
     */
    fun decode(frame: RtspFrame): Boolean

    /**
     * Установить callback для декодированных кадров
     */
    fun setCallback(callback: DecodedFrameCallback?)

    /**
     * Получить информацию о декодере
     */
    fun getInfo(): DecoderInfo?

    /**
     * Освободить ресурсы
     */
    fun release()
}

/**
 * Информация о декодере
 */
data class DecoderInfo(
    val width: Int,
    val height: Int,
    val codec: VideoCodec
)

/**
 * Утилита для конвертации декодированного кадра в BufferedImage
 */
fun DecodedVideoFrame.toBufferedImage(): BufferedImage? {
    return when (format) {
        DecodedVideoFrame.PixelFormat.RGB24 -> {
            try {
                val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
                val raster = image.raster
                val dataBuffer = raster.dataBuffer as java.awt.image.DataBufferInt
                val pixels = dataBuffer.data

                // Конвертация RGB24 (ByteArray) в INT_RGB
                var pixelIndex = 0
                for (i in data.indices step 3) {
                    if (i + 2 < data.size) {
                        val r = data[i].toInt() and 0xFF
                        val g = data[i + 1].toInt() and 0xFF
                        val b = data[i + 2].toInt() and 0xFF
                        pixels[pixelIndex++] = (r shl 16) or (g shl 8) or b
                    }
                }
                image
            } catch (e: Exception) {
                null
            }
        }
        DecodedVideoFrame.PixelFormat.YUV420 -> {
            // TODO: Конвертация YUV420 в RGB
            null
        }
    }
}

/**
 * Определить кодек из строки кодека
 */
fun String.toVideoCodec(): VideoCodec {
    return when (this.uppercase()) {
        "H.264", "H264", "AVC" -> VideoCodec.H264
        "H.265", "H265", "HEVC" -> VideoCodec.H265
        "MJPEG", "JPEG" -> VideoCodec.MJPEG
        else -> VideoCodec.UNKNOWN
    }
}

