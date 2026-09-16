package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.utils.MediaFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * JVM реализация Live555RTSPClient (заглушка - Live555 не доступен на JVM)
 */
actual class Live555RTSPClient actual constructor(
    private val config: RtspClientConfig
) : RtspClientInterface {

    private val _status = MutableStateFlow(RtspClientStatus.DISCONNECTED)
    actual override fun getStatus(): StateFlow<RtspClientStatus> = _status

    actual override suspend fun connect(): Boolean {
        // Live555 не доступен на JVM, используем RtspClient как fallback
        _status.value = RtspClientStatus.ERROR
        return false
    }

    actual override suspend fun disconnect() {
        _status.value = RtspClientStatus.DISCONNECTED
    }

    actual override suspend fun play(): Boolean {
        _status.value = RtspClientStatus.ERROR
        return false
    }

    actual override suspend fun pause(): Boolean {
        _status.value = RtspClientStatus.PAUSED
        return true
    }

    actual override suspend fun teardown() {
        disconnect()
    }

    actual override fun getVideoInfo(): VideoStreamInfo? = null
    actual override fun getAudioInfo(): AudioStreamInfo? = null
    actual override suspend fun getVideoFrame(timeoutMs: Long): MediaFrame? = null
    actual override suspend fun getAudioFrame(timeoutMs: Long): MediaFrame? = null
    actual override fun close() {}
}

/**
 * Live555 не поддерживается на JVM
 */
actual fun isLive555Supported(): Boolean = false

/**
 * Версия Live555 не доступна на JVM
 */
actual fun getLive555Version(): String? = null
