package com.company.ipcamera.desktop.stream

import com.company.ipcamera.core.network.RtspClient
import com.company.ipcamera.core.network.RtspClientConfig
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.desktop.reconnect.ReconnectController
import com.company.ipcamera.desktop.reconnect.ReconnectPolicy
import com.company.ipcamera.desktop.reconnect.shouldRetryError
import com.company.ipcamera.shared.domain.model.Camera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * RTSP session wrapper for desktop video player.
 * Encapsulates client lifecycle and exposes status/frame/error streams.
 * Supports automatic reconnect with configurable backoff policy.
 */
class RtspStreamSession(
    private val camera: Camera,
    private val reconnectPolicy: ReconnectPolicy = ReconnectPolicy.BALANCED,
    private val reconnectController: ReconnectController? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var client: RtspClient? = null
    private var streamCollectorsStarted = false
    private var isDisposed = false

    private val _status = MutableStateFlow(RtspClientStatus.DISCONNECTED)
    val status: StateFlow<RtspClientStatus> = _status.asStateFlow()

    private val _streamError = MutableStateFlow<String?>(null)
    val streamError: StateFlow<String?> = _streamError.asStateFlow()

    private val _videoFrames = MutableSharedFlow<RtspFrame>(extraBufferCapacity = 4)
    val videoFrames: SharedFlow<RtspFrame> = _videoFrames.asSharedFlow()

    private var reconnectJob: kotlinx.coroutines.Job? = null

    init {
        // Регистрируем камеру в контроллере reconnect если он предоставлен
        reconnectController?.let { controller ->
            controller.startReconnectMonitoring(camera.id, reconnectPolicy)
            
            // Подписываемся на события reconnect
            listenToReconnectEvents(camera.id, controller)
        }
    }

    /**
     * Подписаться на события reconnect и обновлять состояние.
     */
    private fun listenToReconnectEvents(cameraId: String, controller: ReconnectController) {
        reconnectJob = scope.launch {
            controller.globalEvents.collect { event ->
                if (event is com.company.ipcamera.desktop.reconnect.ReconnectEvent.ErrorOccurred && event.cameraId == cameraId) {
                    // Обработаем ошибку если она влияет на наш поток
                    val currentState = _status.value
                    if (currentState == RtspClientStatus.ERROR || currentState == RtspClientStatus.DISCONNECTED) {
                        _streamError.value = event.error
                    }
                }
            }
        }
    }

    suspend fun start(autoPlay: Boolean) {
        ensureClient()
        if (streamCollectorsStarted) {
            return
        }
        streamCollectorsStarted = true

        val activeClient = client ?: return
        activeClient.setStatusCallback { newStatus, message ->
            if (isDisposed) return@setStatusCallback

            _status.value = newStatus
            when (newStatus) {
                RtspClientStatus.ERROR -> {
                    val error = message ?: "RTSP stream error"
                    _streamError.value = error
                    
                    // Уведомляем контроллер reconnect об ошибке
                    reconnectController?.onStreamError(camera.id, error)
                }
                RtspClientStatus.CONNECTED, RtspClientStatus.PLAYING -> {
                    _streamError.value = null
                    // Сообщаем контроллеру об успешном подключении
                    reconnectController?.onConnected(camera.id)
                }
                else -> {}
            }
        }

        scope.launch {
            activeClient.getStatus().collect { status ->
                if (!isDisposed) {
                    _status.value = status
                }
            }
        }
        scope.launch {
            activeClient.getVideoFrames().collect { frame ->
                if (!isDisposed) {
                    _videoFrames.emit(frame)
                }
            }
        }

        val connected = activeClient.connect()
        if (connected && autoPlay) {
            activeClient.play()
        } else if (!connected) {
            // Первая попытка подключения не удалась - уведомляем контроллер
            val errorMsg = "Initial connection failed"
            _streamError.value = errorMsg
            reconnectController?.onStreamError(camera.id, errorMsg)
        }
    }

    suspend fun play() {
        ensureClient()
        client?.play()
    }

    suspend fun pause() {
        client?.pause()
    }

    suspend fun stop() {
        client?.disconnect()
    }

    suspend fun reconnect(autoPlay: Boolean) {
        close()
        start(autoPlay)
    }

    suspend fun close() {
        isDisposed = true
        
        // Отписываемся от контроллера reconnect
        reconnectController?.let { controller ->
            controller.stopReconnect(camera.id, "Session closed")
        }
        reconnectJob?.cancel()
        
        client?.disconnect()
        client?.close()
        client = null
        streamCollectorsStarted = false
        _status.value = RtspClientStatus.DISCONNECTED
        _streamError.value = null
    }
        
    /**
     * Запросить ручной reconnect.
     */
    suspend fun manualReconnect(): Boolean {
        if (isDisposed) return false
        
        // Сбрасываем состояние и пытаемся заново
        client?.disconnect()
        client = null
        streamCollectorsStarted = false
        
        return try {
            start(autoPlay = true)
            true
        } catch (e: Exception) {
            _streamError.value = "Manual reconnect failed: ${e.message}"
            false
        }
    }

    /**
     * Получить текущую политику reconnect.
     */
    fun getReconnectPolicy(): ReconnectPolicy = reconnectPolicy

    /**
     * Обновить политику reconnect на лету.
     */
    fun updateReconnectPolicy(newPolicy: ReconnectPolicy) {
        reconnectController?.startReconnectMonitoring(camera.id, newPolicy)
    }

    private fun ensureClient() {
        if (client != null) {
            return
        }
        val config = RtspClientConfig(
            url = camera.url,
            username = camera.username,
            password = camera.password,
            enableVideo = true,
            enableAudio = false,
            timeoutMillis = 10000,
            reconnectMaxRetries = if (reconnectPolicy.enabled) reconnectPolicy.maxAttempts else 0,
            reconnectInitialDelayMs = reconnectPolicy.initialDelayMs.toInt().coerceAtLeast(100),
            reconnectMaxDelayMs = reconnectPolicy.maxDelayMs.toInt(),
            reconnectBackoffMultiplier = reconnectPolicy.backoffMultiplier.toFloat()
        )
        client = RtspClient(config)
    }
}

