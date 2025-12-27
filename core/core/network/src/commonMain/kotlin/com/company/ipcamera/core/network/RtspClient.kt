package com.company.ipcamera.core.network

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.network.rtsp.NativeRtspClient
import com.company.ipcamera.core.network.rtsp.NativeRtspClientHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Тип потока RTSP
 */
enum class RtspStreamType {
    VIDEO,
    AUDIO,
    METADATA
}

/**
 * Статус RTSP клиента
 */
enum class RtspClientStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PLAYING,
    ERROR
}

/**
 * RTSP кадр
 */
data class RtspFrame(
    val data: ByteArray,
    val timestamp: Long,
    val streamType: RtspStreamType,
    val width: Int = 0,
    val height: Int = 0
)

/**
 * Информация о потоке
 */
data class RtspStreamInfo(
    val index: Int,
    val type: RtspStreamType,
    val resolution: Resolution?,
    val fps: Int,
    val codec: String,
    val audioCodec: String? = null, // Кодек аудио (AAC, PCM, G.711 и т.д.)
    val sampleRate: Int? = null, // Частота дискретизации аудио (Hz)
    val channels: Int? = null // Количество аудио каналов
)

/**
 * Callback для получения кадров
 */
typealias RtspFrameCallback = (RtspFrame) -> Unit

/**
 * Callback для изменения статуса
 */
typealias RtspStatusCallback = (RtspClientStatus, String?) -> Unit

/**
 * Конфигурация RTSP клиента
 */
data class RtspClientConfig(
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val timeoutMillis: Long = 10000,
    val bufferSize: Int = 1024 * 1024, // 1MB
    val enableAudio: Boolean = true,
    val enableVideo: Boolean = true,
    val enableMetadata: Boolean = false
)

/**
 * RTSP клиент для работы с видеопотоками
 *
 * Этот класс является оберткой над нативной C++ библиотекой.
 * В текущей реализации используется упрощенная версия без нативных вызовов,
 * которая может быть интегрирована с реальной нативной библиотекой позже.
 */
class RtspClient(
    private val config: RtspClientConfig
) {
    private val nativeClient = NativeRtspClient()
    private var nativeHandle: NativeRtspClientHandle? = null

    private var status = MutableStateFlow<RtspClientStatus>(RtspClientStatus.DISCONNECTED)
    private val videoFrameFlow = MutableSharedFlow<RtspFrame>(extraBufferCapacity = 10)
    private val audioFrameFlow = MutableSharedFlow<RtspFrame>(extraBufferCapacity = 10)

    private var videoCallback: RtspFrameCallback? = null
    private var audioCallback: RtspFrameCallback? = null
    private var statusCallback: RtspStatusCallback? = null

    private var connectionJob: Job? = null
    private var receiveJob: Job? = null

    private val streams = mutableListOf<RtspStreamInfo>()

    /**
     * Получить статус подключения
     */
    fun getStatus(): StateFlow<RtspClientStatus> = status.asStateFlow()

    /**
     * Получить поток видеокадров
     */
    fun getVideoFrames(): SharedFlow<RtspFrame> = videoFrameFlow.asSharedFlow()

    /**
     * Получить поток аудиокадров
     */
    fun getAudioFrames(): SharedFlow<RtspFrame> = audioFrameFlow.asSharedFlow()

    /**
     * Подключиться к RTSP серверу
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        if (status.value != RtspClientStatus.DISCONNECTED) {
            logger.warn { "RTSP client already connected or connecting" }
            return@withContext
        }

        status.value = RtspClientStatus.CONNECTING
        statusCallback?.invoke(RtspClientStatus.CONNECTING, "Connecting to ${config.url}")

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Создание нативного клиента
                val handle = nativeClient.create()
                nativeHandle = handle

                // Установка callbacks
                nativeClient.setStatusCallback(handle) { newStatus, message ->
                    status.value = newStatus
                    statusCallback?.invoke(newStatus, message)
                }

                nativeClient.setFrameCallback(handle, RtspStreamType.VIDEO) { frame ->
                    try {
                        videoFrameFlow.tryEmit(frame)
                        videoCallback?.invoke(frame)
                    } catch (e: Exception) {
                        logger.error(e) { "Error emitting video frame" }
                    }
                }

                nativeClient.setFrameCallback(handle, RtspStreamType.AUDIO) { frame ->
                    try {
                        audioFrameFlow.tryEmit(frame)
                        audioCallback?.invoke(frame)
                    } catch (e: Exception) {
                        logger.error(e) { "Error emitting audio frame" }
                    }
                }

                // Подключение к серверу
                val success = nativeClient.connect(
                    handle = handle,
                    url = config.url,
                    username = config.username,
                    password = config.password,
                    timeoutMs = config.timeoutMillis.toInt()
                )

                if (success) {
                    // Получение информации о потоках
                    streams.clear()
                    val streamCount = nativeClient.getStreamCount(handle)

                    for (i in 0 until streamCount) {
                        val streamInfo = nativeClient.getStreamInfo(handle, i)
                        if (streamInfo != null) {
                            streams.add(streamInfo)
                        }
                    }

                    // Если потоки не получены, создаем базовые
                    if (streams.isEmpty()) {
                        if (config.enableVideo) {
                            streams.add(
                                RtspStreamInfo(
                                    index = 0,
                                    type = RtspStreamType.VIDEO,
                                    resolution = Resolution(1920, 1080),
                                    fps = 25,
                                    codec = "H.264"
                                )
                            )
                        }
                        if (config.enableAudio) {
                            streams.add(
                                RtspStreamInfo(
                                    index = streams.size,
                                    type = RtspStreamType.AUDIO,
                                    resolution = null,
                                    fps = 0,
                                    codec = "AAC", // По умолчанию AAC
                                    audioCodec = "AAC",
                                    sampleRate = 44100,
                                    channels = 2
                                )
                            )
                        }
                    }

                    status.value = RtspClientStatus.CONNECTED
                    statusCallback?.invoke(RtspClientStatus.CONNECTED, "Connected successfully")
                    logger.info { "RTSP client connected to ${config.url}" }
                } else {
                    throw Exception("Failed to connect to RTSP server")
                }

            } catch (e: Exception) {
                logger.error(e) { "Failed to connect to RTSP server" }
                status.value = RtspClientStatus.ERROR
                statusCallback?.invoke(RtspClientStatus.ERROR, e.message)
                nativeHandle?.let { nativeClient.destroy(it) }
                nativeHandle = null
            }
        }
    }

    /**
     * Начать воспроизведение
     */
    suspend fun play() = withContext(Dispatchers.IO) {
        val handle = nativeHandle ?: run {
            logger.warn { "Cannot play: not connected" }
            return@withContext
        }

        if (status.value != RtspClientStatus.CONNECTED) {
            logger.warn { "Cannot play: not connected" }
            return@withContext
        }

        val success = nativeClient.play(handle)
        if (success) {
            status.value = RtspClientStatus.PLAYING
            statusCallback?.invoke(RtspClientStatus.PLAYING, "Playing")
            logger.info { "RTSP client started playing" }
        } else {
            logger.error { "Failed to start playing" }
            status.value = RtspClientStatus.ERROR
            statusCallback?.invoke(RtspClientStatus.ERROR, "Failed to start playing")
        }
    }

    /**
     * Остановить воспроизведение
     */
    suspend fun stop() = withContext(Dispatchers.IO) {
        val handle = nativeHandle ?: return@withContext

        if (status.value != RtspClientStatus.PLAYING) {
            return@withContext
        }

        receiveJob?.cancel()
        receiveJob = null

        val success = nativeClient.stop(handle)
        if (success || status.value == RtspClientStatus.PLAYING) {
            status.value = RtspClientStatus.CONNECTED
            statusCallback?.invoke(RtspClientStatus.CONNECTED, "Stopped")
        }

        logger.info { "RTSP client stopped" }
    }

    /**
     * Приостановить воспроизведение
     */
    suspend fun pause() = withContext(Dispatchers.IO) {
        val handle = nativeHandle ?: return@withContext

        if (status.value != RtspClientStatus.PLAYING) {
            return@withContext
        }

        receiveJob?.cancel()
        receiveJob = null

        val success = nativeClient.pause(handle)
        if (success) {
            status.value = RtspClientStatus.CONNECTED
            statusCallback?.invoke(RtspClientStatus.CONNECTED, "Paused")
            logger.info { "RTSP client paused" }
        } else {
            logger.error { "Failed to pause RTSP client" }
            status.value = RtspClientStatus.ERROR
            statusCallback?.invoke(RtspClientStatus.ERROR, "Failed to pause")
        }
    }

    /**
     * Отключиться от сервера
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        stop()

        connectionJob?.cancel()
        connectionJob = null

        nativeHandle?.let { handle ->
            nativeClient.disconnect(handle)
            nativeClient.destroy(handle)
            nativeHandle = null
        }

        status.value = RtspClientStatus.DISCONNECTED
        statusCallback?.invoke(RtspClientStatus.DISCONNECTED, "Disconnected")

        streams.clear()
        logger.info { "RTSP client disconnected" }
    }

    /**
     * Получить список потоков
     */
    fun getStreams(): List<RtspStreamInfo> = streams.toList()

    /**
     * Получить информацию о потоке
     */
    fun getStreamInfo(index: Int): RtspStreamInfo? {
        return streams.getOrNull(index)
    }

    /**
     * Установить callback для видеокадров
     */
    fun setVideoFrameCallback(callback: RtspFrameCallback?) {
        videoCallback = callback
    }

    /**
     * Установить callback для аудиокадров
     */
    fun setAudioFrameCallback(callback: RtspFrameCallback?) {
        audioCallback = callback
    }

    /**
     * Установить callback для изменения статуса
     */
    fun setStatusCallback(callback: RtspStatusCallback?) {
        statusCallback = callback
    }

    /**
     * Начать прием кадров
     * Примечание: Callbacks уже установлены в connect(), поэтому
     * кадры будут приходить автоматически через нативные callbacks
     */
    private fun startReceiving() {
        // Callbacks установлены в connect(), поэтому дополнительная логика не требуется
        // Нативная библиотека будет вызывать callbacks автоматически
        logger.debug { "Frame receiving started via native callbacks" }
    }

    /**
     * Определить аудио кодек из данных кадра
     *
     * @param audioData Данные аудио кадра
     * @return Название кодека или null если не удалось определить
     */
    fun detectAudioCodec(audioData: ByteArray): String? {
        if (audioData.isEmpty()) return null

        // Проверяем AAC (ADTS заголовок: 0xFF 0xF1-0xF9)
        if (audioData.size >= 2 && audioData[0] == 0xFF.toByte() &&
            (audioData[1].toInt() and 0xF0) == 0xF0) {
            return "AAC"
        }

        // Проверяем G.711 PCMU (μ-law) - обычно payload type 0
        // G.711 имеет характерные паттерны в байтах
        if (audioData.size >= 2) {
            val firstBytes = audioData.sliceArray(0..minOf(1, audioData.size - 1))
            if (firstBytes.all { it.toInt() and 0x7F in 0..127 }) {
                // Может быть PCMU или PCMA
                return "PCMU" // По умолчанию PCMU
            }
        }

        // Проверяем G.711 PCMA (A-law) - обычно payload type 8
        // PCMA имеет схожие паттерны

        // PCM (raw) - сложно определить без метаданных
        // Возвращаем null для использования информации из потока

        return null
    }

    /**
     * Получить информацию об аудио кодеках из доступных потоков
     *
     * @return Список аудио кодеков или пустой список
     */
    fun getAudioCodecs(): List<String> {
        return streams
            .filter { it.type == RtspStreamType.AUDIO }
            .mapNotNull { it.audioCodec ?: it.codec }
            .distinct()
    }

    /**
     * Освободить ресурсы
     */
    fun close() {
        connectionJob?.cancel()
        receiveJob?.cancel()

        nativeHandle?.let { handle ->
            try {
                nativeClient.destroy(handle)
            } catch (e: Exception) {
                logger.error(e) { "Error destroying native client" }
            }
            nativeHandle = null
        }

        streams.clear()
        status.value = RtspClientStatus.DISCONNECTED
    }
}

