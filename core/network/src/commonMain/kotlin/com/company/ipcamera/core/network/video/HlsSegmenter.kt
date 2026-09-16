package com.company.ipcamera.core.network.video

/**
 * Информация о HLS сегменте
 */
data class HlsSegment(
    val index: Int,
    val filename: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val isInitSegment: Boolean = false
)

/**
 * Состояние HLS плейлиста
 */
data class HlsPlaylist(
    val version: Int = 3,
    val targetDuration: Int,
    val sequenceNumber: Int,
    val segments: List<HlsSegment>,
    val isLive: Boolean = true,
    val path: String
)

/**
 * Конфигурация HLS сегментера
 */
data class HlsSegmenterConfig(
    /** Длительность сегмента в секундах */
    val segmentDurationSec: Int = 1,
    /** Максимальное количество сегментов в плейлисте (live) */
    val playlistSize: Int = 3,
    /** Директория для вывода HLS файлов */
    val outputDir: String = "streams/hls",
    /** Использовать fMP4 вместо TS */
    val useFmp4: Boolean = true,
    /** FPS видео */
    val fps: Int = 25,
    /** Битрейт видео */
    val videoBitrate: Int = 1500_000, // 1.5 Mbps
    /** Битрейт аудио */
    val audioBitrate: Int = 128_000 // 128 kbps
) {
    /** Расширение сегмента */
    val segmentExtension: String get() = if (useFmp4) ".mp4" else ".ts"

    /** Формат для FFmpeg */
    val hlsFormat: String get() = if (useFmp4) "hls" else "hls"

    /** HLS flags */
    val hlsFlags: String
        get() = buildString {
            append("delete_segments+append_list")
            if (useFmp4) append("+fmp4")
        }
}

/**
 * Callback для уведомления о новых сегментах
 */
typealias HlsSegmentCallback = (HlsSegment) -> Unit

/**
 * Callback для уведомления об обновлении плейлиста
 */
typealias HlsPlaylistCallback = (HlsPlaylist) -> Unit

/**
 * Интерфейс HLS сегментера.
 *
 * Преобразует видеопоток в HLS сегменты и управляет плейлистом.
 * Реализации могут использовать FFmpeg CLI или JavaCV FFmpeg API.
 */
interface HlsSegmenter {
    /**
     * Запустить сегментацию.
     * Сегментер начинает принимать кадры и генерировать сегменты.
     */
    suspend fun start(streamId: String): Boolean

    /**
     * Остановить сегментацию и финализировать плейлист.
     */
    suspend fun stop()

    /**
     * Передать кадр для сегментации.
     * @param frame данные кадра (RGB24 или JPEG)
     * @param width ширина
     * @param height высота
     * @param timestamp временная метка в мс
     */
    suspend fun feedFrame(data: ByteArray, width: Int, height: Int, timestamp: Long)

    /**
     * Получить путь к текущему плейлисту.
     */
    fun getPlaylistPath(): String?

    /**
     * Получить путь к директории с сегментами.
     */
    fun getOutputDir(): String?

    /**
     * Установить callback для новых сегментов.
     */
    fun setSegmentCallback(callback: HlsSegmentCallback?)

    /**
     * Установить callback для обновления плейлиста.
     */
    fun setPlaylistCallback(callback: HlsPlaylistCallback?)

    /**
     * Активна ли сегментация.
     */
    fun isActive(): Boolean
}
