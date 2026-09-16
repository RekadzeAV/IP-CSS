package com.company.ipcamera.core.network.video

import com.company.ipcamera.core.network.RtspFrame

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
