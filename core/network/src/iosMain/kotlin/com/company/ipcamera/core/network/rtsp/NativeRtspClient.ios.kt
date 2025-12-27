package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS реализация RTSP клиента
 * Использует cinterop для вызова нативных C++ функций
 */
actual class NativeRtspClient {
    actual fun create(): NativeRtspClientHandle {
        // TODO: cinterop вызов после настройки
        return 0L as NativeRtspClientHandle
    }

    actual suspend fun connect(
        handle: NativeRtspClientHandle,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = withContext(Dispatchers.Default) {
        // TODO: cinterop вызов
        false
    }

    actual suspend fun disconnect(handle: NativeRtspClientHandle) = withContext(Dispatchers.Default) {
        // TODO: cinterop вызов
    }

    actual fun getStatus(handle: NativeRtspClientHandle): RtspClientStatus {
        return RtspClientStatus.DISCONNECTED
    }

    actual suspend fun play(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.Default) {
        false
    }

    actual suspend fun stop(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.Default) {
        false
    }

    actual suspend fun pause(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.Default) {
        false
    }

    actual fun getStreamCount(handle: NativeRtspClientHandle): Int {
        return 0
    }

    actual fun getStreamType(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamType? {
        return null
    }

    actual fun getStreamInfo(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamInfo? {
        return null
    }

    actual fun setFrameCallback(
        handle: NativeRtspClientHandle,
        streamType: RtspStreamType,
        callback: (RtspFrame) -> Unit
    ) {
        // TODO: cinterop callback
    }

    actual fun setStatusCallback(
        handle: NativeRtspClientHandle,
        callback: (RtspClientStatus, String?) -> Unit
    ) {
        // TODO: cinterop callback
    }

    actual fun destroy(handle: NativeRtspClientHandle) {
        // TODO: cinterop вызов
    }
}

actual typealias NativeRtspClientHandle = Long

