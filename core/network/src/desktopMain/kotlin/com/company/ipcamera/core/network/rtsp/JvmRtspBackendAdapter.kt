package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType

internal class JvmRtspBackendAdapter(
    private val client: JvmRtspClient
) : JvmRtspBackend {
    override fun create(): Long = client.create()

    override suspend fun connect(
        handle: Long,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = client.connect(handle, url, username, password, timeoutMs)

    override suspend fun disconnect(handle: Long) = client.disconnect(handle)

    override fun getStatus(handle: Long): RtspClientStatus = client.getStatus(handle)

    override suspend fun play(handle: Long): Boolean = client.play(handle)

    override suspend fun stop(handle: Long): Boolean = client.stop(handle)

    override suspend fun pause(handle: Long): Boolean = client.pause(handle)

    override fun getStreamCount(handle: Long): Int = client.getStreamCount(handle)

    override fun getStreamType(handle: Long, streamIndex: Int): RtspStreamType? =
        client.getStreamType(handle, streamIndex)

    override fun getStreamInfo(handle: Long, streamIndex: Int): RtspStreamInfo? =
        client.getStreamInfo(handle, streamIndex)

    override fun setFrameCallback(handle: Long, streamType: RtspStreamType, callback: (RtspFrame) -> Unit) {
        client.setFrameCallback(handle, streamType, callback)
    }

    override fun setStatusCallback(handle: Long, callback: (RtspClientStatus, String?) -> Unit) {
        client.setStatusCallback(handle, callback)
    }

    override fun setReconnectParams(
        handle: Long,
        enabled: Boolean,
        maxRetries: Int,
        initialDelayMs: Int,
        maxDelayMs: Int,
        backoffMultiplier: Float
    ) {
        client.setReconnectParams(
            handle,
            enabled,
            maxRetries,
            initialDelayMs,
            maxDelayMs,
            backoffMultiplier
        )
    }

    override fun destroy(handle: Long) = client.destroy(handle)
}

internal object JvmRtspBackendInstall {
    init {
        if (isJvmRtspBackendEnabled()) {
            jvmRtspBackendFactory = { JvmRtspBackendAdapter(JvmRtspClient()) }
        }
    }
}

/** Включение JavaCV RTSP: property `ipcss.jvm.rtsp.backend=true` или env `IPCSS_JVM_RTSP_BACKEND=true`. */
internal fun isJvmRtspBackendEnabled(): Boolean {
    System.getProperty("ipcss.jvm.rtsp.backend")?.let { prop ->
        return prop.equals("true", ignoreCase = true)
    }
    return System.getenv("IPCSS_JVM_RTSP_BACKEND")?.equals("true", ignoreCase = true) == true
}
