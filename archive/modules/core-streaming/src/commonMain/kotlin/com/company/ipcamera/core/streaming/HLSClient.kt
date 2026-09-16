package com.company.ipcamera.core.streaming

import com.company.ipcamera.core.decoder.DecodedFrame
import com.company.ipcamera.core.decoder.VideoDecoder
import kotlinx.coroutines.flow.StateFlow

/**
 * HLS Client - HTTP Live Streaming
 * 
 * Клиент для воспроизведения HLS потоков (.m3u8 playlists)
 */
expect class HLSClient(config: HLSClientConfig) {
    
    /**
     * Подключиться к HLS потоку
     */
    suspend fun connect(): Boolean
    
    /**
     * Начать воспроизведение
     */
    suspend fun play()
    
    /**
     * Приостановить воспроизведение
     */
    suspend fun pause()
    
    /**
     * Остановить воспроизведение
     */
    suspend fun stop()
    
    /**
     * Получить текущий статус
     */
    fun getStatus(): StateFlow<HLSStatus>
    
    /**
     * Получить текущую информацию о потоке
     */
    fun getStreamInfo(): HLSStreamInfo?
    
    /**
     * Получить следующий кадр
     */
    suspend fun getNextFrame(): DecodedFrame?
    
    /**
     * Освободить ресурсы
     */
    fun close()
}

/**
 * Конфигурация HLS клиента
 */
data class HLSClientConfig(
    val playlistUrl: String,
    val timeoutMs: Long = 10000,
    val maxBufferSeconds: Int = 30,
    val minBufferSeconds: Int = 5,
    val enableAdaptiveBitrate: Boolean = true,
    val preferredVideoCodec: VideoCodec = VideoCodec.H264,
    val preferredAudioCodec: AudioCodec = AudioCodec.AAC,
    val decoder: VideoDecoder? = null,
    val userAgent: String = "IP-CSS HLS Client/1.0"
)

/**
 * Видео кодек
 */
enum class VideoCodec {
    H264,
    H265,
    VP8,
    VP9,
    MPEG4,
    UNKNOWN
}

/**
 * Аудио кодек
 */
enum class AudioCodec {
    AAC,
    MP3,
    AC3,
    OPUS,
    UNKNOWN
}

/**
 * Статус HLS клиента
 */
enum class HLSStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PLAYING,
    PAUSED,
    BUFFERING,
    ERROR,
    CLOSED
}

/**
 * Информация о HLS потоке
 */
data class HLSStreamInfo(
    val url: String,
    val duration: Double,
    val targetDuration: Double,
    val isLive: Boolean,
    val isEndlist: Boolean,
    val version: Int,
    val playlists: List<HLSPlaylist>,
    val segments: List<HLSSegment>,
    val currentBitrate: Long,
    val availableBitrates: List<Long>
)

/**
 * HLS плейлист
 */
data class HLSPlaylist(
    val url: String,
    val bitrate: Long,
    val resolution: String?,
    val codec: String?,
    val isDefault: Boolean
)

/**
 * HLS сегмент
 */
data class HLSSegment(
    val url: String,
    val duration: Double,
    val sequence: Long,
    val isKeyFrame: Boolean,
    val byteRange: String?
)

/**
 * Информация о вариативном плейлисте
 */
data class HLSVariantInfo(
    val bandwidth: Long,
    val averageBandwidth: Long?,
    val codecs: String?,
    val resolution: String?,
    val frameRate: Double?,
    val hdcpLevel: String?,
    val audio: String?,
    val video: String?,
    val subtitles: String?,
    val closedCaptions: String?
)

/**
 * События HLS потока
 */
sealed class HLSEvent {
    data class PlaylistLoaded(val playlist: HLSStreamInfo) : HLSEvent()
    data class SegmentDownloaded(val segment: HLSSegment, val duration: Long) : HLSEvent()
    data class SegmentFailed(val segment: HLSSegment, val error: String) : HLSEvent()
    data class BitrateChanged(val oldBitrate: Long, val newBitrate: Long) : HLSEvent()
    data class Buffering(val bufferSeconds: Double) : HLSEvent()
    data class Error(val message: String) : HLSEvent()
    data class EndOfStream : HLSEvent()
}

/**
 * HLS Stream Manager
 */
class HLSStreamManager {
    
    private val activeStreams = mutableMapOf<String, HLSClient>()
    
    /**
     * Создать новый HLS поток
     */
    fun createStream(config: HLSClientConfig): HLSClient {
        val client = HLSClient(config)
        activeStreams[config.playlistUrl] = client
        return client
    }
    
    /**
     * Остановить поток
     */
    fun stopStream(url: String) {
        activeStreams[url]?.close()
        activeStreams.remove(url)
    }
    
    /**
     * Остановить все потоки
     */
    fun stopAllStreams() {
        activeStreams.values.forEach { it.close() }
        activeStreams.clear()
    }
    
    /**
     * Получить активный поток
     */
    fun getStream(url: String): HLSClient? = activeStreams[url]
    
    /**
     * Получить количество активных потоков
     */
    fun getActiveStreamCount(): Int = activeStreams.size
}

/**
 * Parser для HLS плейлистов
 */
class HLSPlaylistParser {
    
    /**
     * Парсинг м3у8 плейлиста
     */
    fun parsePlaylist(content: String): HLSStreamInfo {
        val lines = content.split("\n")
        val segments = mutableListOf<HLSSegment>()
        val playlists = mutableListOf<HLSPlaylist>()
        
        var duration = 0.0
        var targetDuration = 0.0
        var isLive = true
        var isEndlist = false
        var version = 1
        
        var currentSeq = 0L
        
        for (line in lines) {
            val trimmed = line.trim()
            
            when {
                trimmed.startsWith("#EXTM3U") -> continue
                trimmed.startsWith("#EXT-X-VERSION") -> {
                    version = trimmed.split(":").getOrNull(1)?.toIntOrNull() ?: 1
                }
                trimmed.startsWith("#EXT-X-TARGETDURATION") -> {
                    targetDuration = trimmed.split(":").getOrNull(1)?.toDoubleOrNull() ?: 0.0
                }
                trimmed.startsWith("#EXT-X-MEDIA-SEQUENCE") -> {
                    currentSeq = trimmed.split(":").getOrNull(1)?.toLongOrNull() ?: 0L
                }
                trimmed.startsWith("#EXT-X-PLAYLIST-TYPE") -> {
                    isLive = !trimmed.contains("VOD", ignoreCase = true)
                }
                trimmed.startsWith("#EXT-X-ENDLIST") -> {
                    isEndlist = true
                    isLive = false
                }
                trimmed.startsWith("#EXTINF") -> {
                    val infDuration = trimmed.split(":").getOrNull(1)?.toDoubleOrNull() ?: 0.0
                    duration += infDuration
                }
                trimmed.startsWith("#") -> continue
                trimmed.isNotEmpty() && !trimmed.startsWith("#") -> {
                    // Это URL сегмента
                    segments.add(
                        HLSSegment(
                            url = trimmed,
                            duration = duration,
                            sequence = currentSeq++,
                            isKeyFrame = true,
                            byteRange = null
                        )
                    )
                    duration = 0.0
                }
            }
        }
        
        return HLSStreamInfo(
            url = "",
            duration = duration,
            targetDuration = targetDuration,
            isLive = isLive,
            isEndlist = isEndlist,
            version = version,
            playlists = playlists,
            segments = segments,
            currentBitrate = 0,
            availableBitrates = emptyList()
        )
    }
    
    /**
     * Парсинг вариативного плейлиста
     */
    fun parseVariantPlaylist(content: String): List<HLSVariantInfo> {
        val variants = mutableListOf<HLSVariantInfo>()
        
        var currentVariant: MutableMap<String, String> = mutableMapOf()
        
        for (line in content.split("\n")) {
            val trimmed = line.trim()
            
            if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
                val attrs = trimmed.removePrefix("#EXT-X-STREAM-INF:")
                currentVariant = parseAttributes(attrs)
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                currentVariant["URI"] = trimmed
                variants.add(mapToVariantInfo(currentVariant))
                currentVariant = mutableMapOf()
            }
        }
        
        return variants
    }
    
    private fun parseAttributes(attrs: String): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        
        val pattern = "([A-Z]+)=([^,]+)".toRegex()
        pattern.findAll(attrs).forEach { match ->
            if (match.groupValues.size >= 3) {
                map[match.groupValues[1]] = match.groupValues[2]
            }
        }
        
        return map
    }
    
    private fun mapToVariantInfo(map: Map<String, String>): HLSVariantInfo {
        return HLSVariantInfo(
            bandwidth = map["BANDWIDTH"]?.toLongOrNull() ?: 0,
            averageBandwidth = map["AVERAGE-BANDWIDTH"]?.toLongOrNull(),
            codecs = map["CODECS"],
            resolution = map["RESOLUTION"],
            frameRate = map["FRAME-RATE"]?.toDoubleOrNull(),
            hdcpLevel = map["HDCP-LEVEL"],
            audio = map["AUDIO"],
            video = map["VIDEO"],
            subtitles = map["SUBTITLES"],
            closedCaptions = map["CLOSED-CAPTIONS"]
        )
    }
}

/**
 * Extension для создания HLS клиента
 */
fun createHLSClient(url: String, decoder: VideoDecoder? = null): HLSClient {
    return HLSClient(
        HLSClientConfig(
            playlistUrl = url,
            decoder = decoder
        )
    )
}

/**
 * Extension для проверки HLS URL
 */
val String.isHlsUrl: Boolean
    get() = endsWith(".m3u8", ignoreCase = true) || endsWith(".m3u", ignoreCase = true)
