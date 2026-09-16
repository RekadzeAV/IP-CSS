package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType

/**
 * JVM/Desktop fallback RTSP backend (JavaCV), когда JNI `video_processing` недоступен.
 * Регистрируется из `desktopMain` через [jvmRtspBackendFactory].
 */
internal interface JvmRtspBackend {
    fun create(): Long
    suspend fun connect(
        handle: Long,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean

    suspend fun disconnect(handle: Long)
    fun getStatus(handle: Long): RtspClientStatus
    suspend fun play(handle: Long): Boolean
    suspend fun stop(handle: Long): Boolean
    suspend fun pause(handle: Long): Boolean
    fun getStreamCount(handle: Long): Int
    fun getStreamType(handle: Long, streamIndex: Int): RtspStreamType?
    fun getStreamInfo(handle: Long, streamIndex: Int): RtspStreamInfo?
    fun setFrameCallback(handle: Long, streamType: RtspStreamType, callback: (RtspFrame) -> Unit)
    fun setStatusCallback(handle: Long, callback: (RtspClientStatus, String?) -> Unit)
    fun setReconnectParams(
        handle: Long,
        enabled: Boolean,
        maxRetries: Int,
        initialDelayMs: Int,
        maxDelayMs: Int,
        backoffMultiplier: Float
    )

    fun destroy(handle: Long)
}

/** Устанавливается из `desktopMain` при загрузке модуля на Desktop JVM. */
internal var jvmRtspBackendFactory: (() -> JvmRtspBackend)? = null

internal fun ensureJvmRtspBackendRegistered() {
    if (jvmRtspBackendFactory != null) return
    if (!isJvmRtspBackendEnabledByPropertyOrEnv()) return
    runCatching {
        Class.forName("com.company.ipcamera.core.network.rtsp.JvmRtspBackendInstall")
            .kotlin.objectInstance
    }
}

private fun isJvmRtspBackendEnabledByPropertyOrEnv(): Boolean {
    System.getProperty("ipcss.jvm.rtsp.backend")?.let { prop ->
        return prop.equals("true", ignoreCase = true)
    }
    return System.getenv("IPCSS_JVM_RTSP_BACKEND")?.equals("true", ignoreCase = true) == true
}
