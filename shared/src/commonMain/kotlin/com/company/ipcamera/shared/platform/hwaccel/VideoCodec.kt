package com.company.ipcamera.shared.platform.hwaccel

/**
 * Поддерживаемые видеокодеки
 */
enum class VideoCodec {
    H264,
    H265,
    MJPEG;

    companion object {
        fun fromString(codec: String): VideoCodec {
            return when (codec.uppercase()) {
                "H.264", "H264", "AVC" -> H264
                "H.265", "H265", "HEVC" -> H265
                "MJPEG", "JPEG" -> MJPEG
                else -> H264 // fallback
            }
        }
    }
}