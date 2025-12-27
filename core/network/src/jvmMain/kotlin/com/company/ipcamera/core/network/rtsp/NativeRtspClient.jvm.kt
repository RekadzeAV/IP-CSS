package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JVM реализация RTSP клиента
 * Для JVM платформ (Desktop) можно использовать JNI или Java библиотеки
 */
actual class NativeRtspClient {
    actual fun create(): NativeRtspClientHandle {
        // TODO: JNI вызов или Java библиотека для RTSP
        return 0L as NativeRtspClientHandle
    }

    actual suspend fun connect(
        handle: NativeRtspClientHandle,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = withContext(Dispatchers.IO) {
        // TODO: JNI вызов
        false
    }

    actual suspend fun disconnect(handle: NativeRtspClientHandle) = withContext(Dispatchers.IO) {
        // TODO: JNI вызов
    }

    actual fun getStatus(handle: NativeRtspClientHandle): RtspClientStatus {
        return RtspClientStatus.DISCONNECTED
    }

    actual suspend fun play(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        false
    }

    actual suspend fun stop(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        false
    }

    actual suspend fun pause(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
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
        // TODO: JNI callback
    }

    actual fun setStatusCallback(
        handle: NativeRtspClientHandle,
        callback: (RtspClientStatus, String?) -> Unit
    ) {
        // TODO: JNI callback
    }

    actual fun destroy(handle: NativeRtspClientHandle) {
        // TODO: JNI вызов
    }
}

actual typealias NativeRtspClientHandle = Long

