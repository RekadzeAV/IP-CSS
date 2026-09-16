package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.utils.MediaFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * JVM реализация RTSPSClient (заглушка)
 */
actual class RTSPSClient actual constructor(
    private val config: RTSPSClientConfig
) : RtspClientInterface {

    private val _status = MutableStateFlow(RtspClientStatus.DISCONNECTED)
    actual override fun getStatus(): StateFlow<RtspClientStatus> = _status

    actual override suspend fun connect(): Boolean {
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

    actual fun isCertificateValid(): Boolean = false
    actual fun getCertificateInfo(): CertificateInfo? = null
}

/**
 * TLS не поддерживается на JVM (нужна отдельная реализация)
 */
actual fun isTlsSupported(): Boolean = false

/**
 * Версия TLS библиотеки
 */
actual fun getTlsLibraryVersion(): String? = null
