package com.company.ipcamera.core.network

/**
 * Статус RTSP клиента
 */
enum class RtspClientStatus {
    DISCONNECTED, // /< Не подключён
    CONNECTING, // /< Подключение
    CONNECTED, // /< Подключён (но не воспроизводит)
    PLAYING, // /< Воспроизведение
    PAUSED, // /< Приостановлено
    ERROR // /< Ошибка
}

/**
 * Конфигурация RTSP клиента
 */
data class RtspClientConfig(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val timeoutMs: Long = 5000,
    val timeoutMillis: Long = 10000,
    val bufferSize: Int = 1024 * 1024,
    val enableAudio: Boolean = true,
    val enableVideo: Boolean = true,
    val enableMetadata: Boolean = false,
    val allowSimulatedFallback: Boolean = false,
    val reconnectEnabled: Boolean = true,
    val reconnectMaxRetries: Int = 5,
    val reconnectInitialDelayMs: Int = 500,
    val reconnectMaxDelayMs: Int = 10_000,
    val reconnectBackoffMultiplier: Float = 2.0f,
    val reconnectJitterRatio: Float = 0.15f,
    val rtpTransport: RtpTransport = RtpTransport.UDP
)

/**
 * Тип транспорта RTP
 */
enum class RtpTransport {
    UDP, // /< RTP over UDP (default)
    TCP, // /< RTP over TCP (interleaved)
    MULTICAST // /< RTP over Multicast
}

/**
 * Информация о видео потоке
 */
data class VideoStreamInfo(
    val format: VideoFormat,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int
)

/**
 * Формат видео
 */
enum class VideoFormat {
    UNKNOWN,
    H264, // /< H.264 / AVC
    H265, // /< H.265 / HEVC
    MPEG4, // /< MPEG-4
    MJPEG // /< Motion JPEG
}

/**
 * Информация об аудио потоке
 */
data class AudioStreamInfo(
    val format: AudioFormat,
    val sampleRate: Int,
    val channels: Int,
    val bitrate: Int
)

/**
 * Формат аудио
 */
enum class AudioFormat {
    UNKNOWN,
    AAC, // /< AAC
    G711, // /< G.711 (PCMU/PCMA)
    G726, // /< G.726
    MP3 // /< MP3
}
