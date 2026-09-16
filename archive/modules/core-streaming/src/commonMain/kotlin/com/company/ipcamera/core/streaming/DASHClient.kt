package com.company.ipcamera.core.streaming

import com.company.ipcamera.core.decoder.DecodedFrame
import com.company.ipcamera.core.decoder.VideoDecoder
import kotlinx.coroutines.flow.StateFlow

/**
 * DASH Client - Dynamic Adaptive Streaming over HTTP (MPEG-DASH)
 * 
 * Клиент для воспроизведения DASH потоков (.mpd manifests)
 */
expect class DASHClient(config: DASHClientConfig) {
    
    /**
     * Подключиться к DASH потоку
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
    fun getStatus(): StateFlow<DASHStatus>
    
    /**
     * Получить текущую информацию о потоке
     */
    fun getStreamInfo(): DASHStreamInfo?
    
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
 * Конфигурация DASH клиента
 */
data class DASHClientConfig(
    val manifestUrl: String,
    val timeoutMs: Long = 10000,
    val maxBufferSeconds: Int = 30,
    val minBufferSeconds: Int = 5,
    val enableAdaptiveBitrate: Boolean = true,
    val preferredVideoCodec: VideoCodec = VideoCodec.H264,
    val preferredAudioCodec: AudioCodec = AudioCodec.AAC,
    val decoder: VideoDecoder? = null,
    val userAgent: String = "IP-CSS DASH Client/1.0"
)

/**
 * Статус DASH клиента
 */
enum class DASHStatus {
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
 * Информация о DASH потоке
 */
data class DASHStreamInfo(
    val url: String,
    val duration: Double,
    val isLive: Boolean,
    val isEndlist: Boolean,
    val profiles: List<String>,
    val periods: List<DASHPeriod>,
    val adaptations: List<DASHAdaptationSet>,
    val currentBitrate: Long,
    availableBitrates: List<Long>
)

/**
 * DASH период
 */
data class DASHPeriod(
    val id: String?,
    val start: Double,
    val duration: Double?,
    val adaptationSets: List<DASHAdaptationSet>
)

/**
 * DASH адаптационный набор
 */
data class DASHAdaptationSet(
    val id: String?,
    val mimeType: String,
    val contentType: String,
    val codecs: String?,
    val width: Int?,
    val height: Int?,
    val frameRate: Double?,
    val audioSamplingRate: Int?,
    val representations: List<DASHRepresentation>
)

/**
 * DASH представление
 */
data class DASHRepresentation(
    val id: String,
    val bandwidth: Long,
    val mimeType: String?,
    val codecs: String?,
    val width: Int?,
    val height: Int?,
    val frameRate: Double?,
    val audioSamplingRate: Int?,
    val segments: List<DASHSegment>
)

/**
 * DASH сегмент
 */
data class DASHSegment(
    val url: String,
    val duration: Double,
    val sequence: Long,
    val byteRange: String?,
    val initialization: Boolean
)

/**
 * События DASH потока
 */
sealed class DASHEvent {
    data class ManifestLoaded(val manifest: DASHStreamInfo) : DASHEvent()
    data class SegmentDownloaded(val segment: DASHSegment, val duration: Long) : DASHEvent()
    data class SegmentFailed(val segment: DASHSegment, val error: String) : DASHEvent()
    data class BitrateChanged(val oldBitrate: Long, val newBitrate: Long) : DASHEvent()
    data class Buffering(val bufferSeconds: Double) : DASHEvent()
    data class Error(val message: String) : DASHEvent()
    data class EndOfStream : DASHEvent()
}

/**
 * DASH Stream Manager
 */
class DASHStreamManager {
    
    private val activeStreams = mutableMapOf<String, DASHClient>()
    
    /**
     * Создать новый DASH поток
     */
    fun createStream(config: DASHClientConfig): DASHClient {
        val client = DASHClient(config)
        activeStreams[config.manifestUrl] = client
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
    fun getStream(url: String): DASHClient? = activeStreams[url]
    
    /**
     * Получить количество активных потоков
     */
    fun getActiveStreamCount(): Int = activeStreams.size
}

/**
 * Parser для DASH манифестов
 */
class DASHManifestParser {
    
    /**
     * Парсинг MPD манифеста
     */
    fun parseManifest(content: String): DASHStreamInfo {
        // Упрощённый парсер для базового MPD
        val periods = mutableListOf<DASHPeriod>()
        val adaptations = mutableListOf<DASHAdaptationSet>()
        
        var duration = 0.0
        var isLive = false
        var isEndlist = false
        val profiles = mutableListOf<String>()
        
        // Простой XML парсинг (в реальности нужен полноценный XML parser)
        if (content.contains("type=\"live\"")) {
            isLive = true
        }
        
        if (content.contains("type=\"static\"") || content.contains('type="static"')) {
            isEndlist = true
            isLive = false
        }
        
        // Извлечение duration
        val durationMatch = """duration="([^"]+)"""".toRegex().find(content)
        if (durationMatch != null) {
            duration = parseDuration(durationMatch.groupValues[1])
        }
        
        // Извлечение profiles
        val profilesMatch = """profiles="([^"]+)"""".toRegex().find(content)
        if (profilesMatch != null) {
            profiles.addAll(profilesMatch.groupValues[1].split(","))
        }
        
        return DASHStreamInfo(
            url = "",
            duration = duration,
            isLive = isLive,
            isEndlist = isEndlist,
            profiles = profiles,
            periods = periods,
            adaptations = adaptations,
            currentBitrate = 0,
            availableBitrates = emptyList()
        )
    }
    
    /**
     * Парсинг ISO 8601 duration
     */
    private fun parseDuration(duration: String): Double {
        // Формат: PT1H30M45S
        val pattern = """PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d+)?)S)?""".toRegex()
        val match = pattern.find(duration) ?: return 0.0
        
        val hours = match.groupValues[1].toDoubleOrNull() ?: 0.0
        val minutes = match.groupValues[2].toDoubleOrNull() ?: 0.0
        val seconds = match.groupValues[3].toDoubleOrNull() ?: 0.0
        
        return hours * 3600 + minutes * 60 + seconds
    }
    
    /**
     * Извлечение URL инициализации
     */
    fun extractInitializationUrl(content: String): String? {
        val match = """InitializationURI="([^"]+)"""".toRegex().find(content)
        return match?.groupValues?.get(1)
    }
    
    /**
     * Извлечение segment template
     */
    fun extractSegmentTemplate(content: String): String? {
        val match = """media="([^"]+)"""".toRegex().find(content)
        return match?.groupValues?.get(1)
    }
}

/**
 * Adaptive Bitrate Manager
 */
class AdaptiveBitrateManager {
    
    private var currentBitrate: Long = 0
    private var availableBitrates: List<Long> = emptyList()
    private var bufferLevel: Double = 0.0
    
    /**
     * Обновить доступные битрейты
     */
    fun updateAvailableBitrates(bitrates: List<Long>) {
        availableBitrates = bitrates.sorted()
    }
    
    /**
     * Обновить уровень буфера
     */
    fun updateBufferLevel(seconds: Double) {
        bufferLevel = seconds
    }
    
    /**
     * Выбрать оптимальный битрейт
     */
    fun selectOptimalBitrate(): Long {
        if (availableBitrates.isEmpty()) return 0
        
        // Если буфер пустой, выбрать минимальный битрейт
        if (bufferLevel < 5.0) {
            return availableBitrates.minOrNull() ?: 0
        }
        
        // Если буфер заполнен, выбрать максимальный
        if (bufferLevel > 20.0) {
            return availableBitrates.maxOrNull() ?: 0
        }
        
        // Иначе выбрать оптимальный на основе буфера
        val targetBuffer = 10.0
        val bufferRatio = bufferLevel / targetBuffer
        val index = ((availableBitrates.size - 1) * bufferRatio).toInt()
        
        return availableBitrates.getOrNull(index.coerceIn(0, availableBitrates.size - 1)) 
            ?: availableBitrates.first()
    }
    
    /**
     * Получить текущий битрейт
     */
    fun getCurrentBitrate(): Long = currentBitrate
    
    /**
     * Установить текущий битрейт
     */
    fun setCurrentBitrate(bitrate: Long) {
        currentBitrate = bitrate
    }
}

/**
 * Extension для создания DASH клиента
 */
fun createDASHClient(url: String, decoder: VideoDecoder? = null): DASHClient {
    return DASHClient(
        DASHClientConfig(
            manifestUrl = url,
            decoder = decoder
        )
    )
}

/**
 * Extension для проверки DASH URL
 */
val String.isDashUrl: Boolean
    get() = endsWith(".mpd", ignoreCase = true)

/**
 * Unified streaming client factory
 */
class StreamingClientFactory {
    
    /**
     * Создать клиент для указанного URL
     */
    fun createClient(url: String, decoder: VideoDecoder? = null): Any? {
        return when {
            url.isHlsUrl -> createHLSClient(url, decoder)
            url.isDashUrl -> createDASHClient(url, decoder)
            url.isSecureRtspUrl -> createSecureRtspClient(
                RTSPSClientConfig(url = url)
            )
            url.startsWith("rtsp://", ignoreCase = true) -> 
                Live555RTSPClient(RtspClientConfig(url = url))
            else -> null
        }
    }
}
