package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.cinterop.*
import platform.posix.*
import live555.*

/**
 * Live555 RTSP Client для Linux
 * 
 * Нативная реализация с использованием Live555 библиотеки
 */
actual class Live555RTSPClient actual constructor(
    actual config: RtspClientConfig
) : RtspClient {
    
    private var clientRef: RtspClientRef? = null
    private val _status = MutableStateFlow<RtspClientStatus>(RtspClientStatus.DISCONNECTED)
    private val config = config
    
    init {
        // Проверка поддержки
        if (!isLive555Supported()) {
            throw IllegalStateException("Live555 is not supported on this platform")
        }
    }
    
    actual override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nativeConfig = createNativeConfig()
            clientRef = rtsp_client_create(nativeConfig.ptr)
            
            if (clientRef == null) {
                _status.value = RtspClientStatus.ERROR
                return@withContext false
            }
            
            _status.value = RtspClientStatus.CONNECTING
            
            val result = rtsp_client_connect(clientRef)
            
            _status.value = if (result) {
                RtspClientStatus.CONNECTED
            } else {
                RtspClientStatus.ERROR
            }
            
            result
        } catch (e: Exception) {
            _status.value = RtspClientStatus.ERROR
            false
        }
    }
    
    actual override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            clientRef?.let {
                rtsp_client_disconnect(it)
                _status.value = RtspClientStatus.DISCONNECTED
            }
        } catch (e: Exception) {
            _status.value = RtspClientStatus.ERROR
        }
    }
    
    actual override suspend fun play(): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = clientRef
            if (client == null) {
                return@withContext false
            }
            
            val result = rtsp_client_play(client)
            
            _status.value = if (result) {
                RtspClientStatus.PLAYING
            } else {
                RtspClientStatus.ERROR
            }
            
            result
        } catch (e: Exception) {
            _status.value = RtspClientStatus.ERROR
            false
        }
    }
    
    actual override suspend fun pause(): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = clientRef
            if (client == null) {
                return@withContext false
            }
            
            val result = rtsp_client_pause(client)
            
            _status.value = if (result) {
                RtspClientStatus.PAUSED
            } else {
                RtspClientStatus.ERROR
            }
            
            result
        } catch (e: Exception) {
            _status.value = RtspClientStatus.ERROR
            false
        }
    }
    
    actual override suspend fun teardown() = withContext(Dispatchers.IO) {
        try {
            clientRef?.let {
                rtsp_client_disconnect(it)
                _status.value = RtspClientStatus.DISCONNECTED
            }
        } catch (e: Exception) {
            _status.value = RtspClientStatus.ERROR
        }
    }
    
    actual override fun getStatus(): StateFlow<RtspClientStatus> = _status.asStateFlow()
    
    actual override fun getVideoInfo(): VideoStreamInfo? {
        return try {
            val info = alloc<VideoStreamInfo>()
            val result = clientRef?.let { rtsp_client_get_video_info(it, info.ptr) }
            
            if (result == true) {
                VideoStreamInfo(
                    format = info.format.toVideoFormat(),
                    width = info.width.toInt(),
                    height = info.height.toInt(),
                    fps = info.fps.toInt(),
                    bitrate = info.bitrate.toInt()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual override fun getAudioInfo(): AudioStreamInfo? {
        return try {
            val info = alloc<AudioStreamInfo>()
            val result = clientRef?.let { rtsp_client_get_audio_info(it, info.ptr) }
            
            if (result == true) {
                AudioStreamInfo(
                    format = info.format.toAudioFormat(),
                    sampleRate = info.sampleRate.toInt(),
                    channels = info.channels.toInt(),
                    bitrate = info.bitrate.toInt()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    actual override suspend fun getVideoFrame(timeoutMs: Long): MediaFrame? =
        withContext(Dispatchers.IO) {
            try {
                val client = clientRef ?: return@withContext null
                
                val frame = alloc<MediaFrame>()
                val result = rtsp_client_get_video_frame(client, frame.ptr, timeoutMs.toInt())
                
                if (result == true && frame.data != null) {
                    MediaFrame(
                        data = frame.data!!.readBytes(frame.size.toInt()),
                        size = frame.size.toInt(),
                        timestamp = frame.timestamp,
                        isKeyFrame = frame.isKeyFrame == 1,
                        type = MediaType.MEDIA_TYPE_VIDEO
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    
    actual override suspend fun getAudioFrame(timeoutMs: Long): MediaFrame? =
        withContext(Dispatchers.IO) {
            try {
                val client = clientRef ?: return@withContext null
                
                val frame = alloc<MediaFrame>()
                val result = rtsp_client_get_audio_frame(client, frame.ptr, timeoutMs.toInt())
                
                if (result == true && frame.data != null) {
                    MediaFrame(
                        data = frame.data!!.readBytes(frame.size.toInt()),
                        size = frame.size.toInt(),
                        timestamp = frame.timestamp,
                        isKeyFrame = frame.isKeyFrame == 1,
                        type = MediaType.MEDIA_TYPE_AUDIO
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    
    actual override fun close() {
        try {
            teardown()
            clientRef?.let {
                rtsp_client_destroy(it)
                clientRef = null
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    /**
     * Создание нативной конфигурации
     */
    private fun createNativeConfig(): CValuesRef<RtspClientConfigVar> {
        return memScoped {
            alloc<RtspClientConfigVar> {
                url = config.url.cstr.ptr
                username = config.username?.cstr?.ptr ?: null
                password = config.password?.cstr?.ptr ?: null
                timeoutMs = config.timeoutMs.toInt()
                enableVideo = if (config.enableVideo) 1 else 0
                enableAudio = if (config.enableAudio) 1 else 0
                rtpTransport = config.rtpTransport.toNativeTransport()
            }
        }
    }
}

/**
 * Проверка поддержки Live555 на Linux
 */
actual fun isLive555Supported(): Boolean = runCatching {
    rtsp_is_supported()
}.getOrDefault(false)

/**
 * Получение версии Live555
 */
actual fun getLive555Version(): String? = runCatching {
    rtsp_get_version()?.toKString()
}.getOrNull()

// ============================================================================
// Extension функции для конвертации типов
// ============================================================================

private fun RtpTransport.toNativeTransport(): Int = when (this) {
    RtpTransport.UDP -> 0
    RtpTransport.TCP -> 1
    RtpTransport.MULTICAST -> 2
}

private fun RtspClientStatus.toKotlinStatus(): RtspClientStatus = when (this) {
    RTSP_STATUS_DISCONNECTED -> RtspClientStatus.DISCONNECTED
    RTSP_STATUS_CONNECTING -> RtspClientStatus.CONNECTING
    RTSP_STATUS_CONNECTED -> RtspClientStatus.CONNECTED
    RTSP_STATUS_PLAYING -> RtspClientStatus.PLAYING
    RTSP_STATUS_PAUSED -> RtspClientStatus.PAUSED
    RTSP_STATUS_ERROR -> RtspClientStatus.ERROR
    else -> RtspClientStatus.ERROR
}

private fun VideoFormat.toVideoFormat(): VideoFormat = when (this) {
    VIDEO_FORMAT_H264 -> VideoFormat.H264
    VIDEO_FORMAT_H265 -> VideoFormat.H265
    VIDEO_FORMAT_MPEG4 -> VideoFormat.MPEG4
    VIDEO_FORMAT_MJPEG -> VideoFormat.MJPEG
    else -> VideoFormat.UNKNOWN
}

private fun AudioFormat.toAudioFormat(): AudioFormat = when (this) {
    AUDIO_FORMAT_AAC -> AudioFormat.AAC
    AUDIO_FORMAT_G711 -> AudioFormat.G711
    AUDIO_FORMAT_G726 -> AudioFormat.G726
    AUDIO_FORMAT_MP3 -> AudioFormat.MP3
    else -> AudioFormat.UNKNOWN
}

private fun MediaType.toKotlinType(): MediaType = when (this) {
    MEDIA_TYPE_VIDEO -> MediaType.MEDIA_TYPE_VIDEO
    MEDIA_TYPE_AUDIO -> MediaType.MEDIA_TYPE_AUDIO
    else -> MediaType.MEDIA_TYPE_UNKNOWN
}
