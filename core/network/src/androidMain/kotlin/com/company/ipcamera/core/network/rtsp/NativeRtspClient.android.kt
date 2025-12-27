package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android реализация RTSP клиента
 * Использует JNI для вызова нативных C++ функций
 *
 * Для полной интеграции требуется:
 * 1. Создать JNI обертку в native/video-processing/src/jni/rtsp_client_jni.cpp
 * 2. Загрузить библиотеку через System.loadLibrary("video_processing")
 * 3. Реализовать JNI методы для всех функций RTSP клиента
 * 4. Настроить callbacks через JNIEnv для передачи кадров и статусов
 *
 * Структура JNI методов должна соответствовать:
 * - Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeCreate
 * - Java_com_company_ipcamera_core_network_rtsp_NativeRtspClient_nativeConnect
 * и т.д.
 */
actual class NativeRtspClient {

    init {
        // Загрузка нативной библиотеки при первом использовании
        try {
            System.loadLibrary("video_processing")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.e("NativeRtspClient", "Failed to load native library", e)
        }
    }

    actual fun create(): NativeRtspClientHandle {
        return nativeCreate()
    }

    actual suspend fun connect(
        handle: NativeRtspClientHandle,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = withContext(Dispatchers.IO) {
        return nativeConnect(handle, url, username, password, timeoutMs)
    }

    actual suspend fun disconnect(handle: NativeRtspClientHandle) = withContext(Dispatchers.IO) {
        nativeDisconnect(handle)
    }

    actual fun getStatus(handle: NativeRtspClientHandle): RtspClientStatus {
        return convertNativeStatus(nativeGetStatus(handle))
    }

    actual suspend fun play(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        return nativePlay(handle)
    }

    actual suspend fun stop(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        return nativeStop(handle)
    }

    actual suspend fun pause(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.IO) {
        return nativePause(handle)
    }

    actual fun getStreamCount(handle: NativeRtspClientHandle): Int {
        return nativeGetStreamCount(handle)
    }

    actual fun getStreamType(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamType? {
        return convertNativeStreamType(nativeGetStreamType(handle, streamIndex))
    }

    actual fun getStreamInfo(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamInfo? {
        val widthArray = IntArray(1)
        val heightArray = IntArray(1)
        val fpsArray = IntArray(1)
        val codecArray = ByteArray(64)

        val success = nativeGetStreamInfo(handle, streamIndex, widthArray, heightArray, fpsArray, codecArray)

        if (!success) {
            return null
        }

        val streamType = getStreamType(handle, streamIndex) ?: return null
        val codec = codecArray.decodeToString().trim('\u0000')
        val resolution = if (widthArray[0] > 0 && heightArray[0] > 0) {
            com.company.ipcamera.core.common.model.Resolution(widthArray[0], heightArray[0])
        } else null

        return RtspStreamInfo(streamIndex, streamType, resolution, fpsArray[0], codec)
    }

    actual fun setFrameCallback(
        handle: NativeRtspClientHandle,
        streamType: RtspStreamType,
        callback: (RtspFrame) -> Unit
    ) {
        // Создаем функциональный интерфейс для callback'а
        val callbackInterface = object : java.util.function.Consumer<RtspFrame> {
            override fun accept(frame: RtspFrame) {
                callback(frame)
            }
        }
        nativeSetFrameCallback(handle, convertStreamTypeToInt(streamType), callbackInterface)
    }

    actual fun setStatusCallback(
        handle: NativeRtspClientHandle,
        callback: (RtspClientStatus, String?) -> Unit
    ) {
        // Создаем функциональный интерфейс для callback'а
        val callbackInterface = object : java.util.function.BiConsumer<RtspClientStatus, String?> {
            override fun accept(status: RtspClientStatus, message: String?) {
                callback(status, message)
            }
        }
        nativeSetStatusCallback(handle, callbackInterface)
    }

    actual fun destroy(handle: NativeRtspClientHandle) {
        nativeDestroy(handle)
    }

    private fun convertStreamTypeToInt(streamType: RtspStreamType): Int {
        return when (streamType) {
            RtspStreamType.VIDEO -> 0
            RtspStreamType.AUDIO -> 1
            RtspStreamType.METADATA -> 2
        }
    }

    // Вспомогательные функции для конвертации (будут использоваться после реализации JNI)
    private fun convertNativeStatus(status: Int): RtspClientStatus {
        return when (status) {
            0 -> RtspClientStatus.DISCONNECTED
            1 -> RtspClientStatus.CONNECTING
            2 -> RtspClientStatus.CONNECTED
            3 -> RtspClientStatus.PLAYING
            4 -> RtspClientStatus.ERROR
            else -> RtspClientStatus.DISCONNECTED
        }
    }

    private fun convertNativeStreamType(type: Int): RtspStreamType? {
        return when (type) {
            0 -> RtspStreamType.VIDEO
            1 -> RtspStreamType.AUDIO
            2 -> RtspStreamType.METADATA
            else -> null
        }
    }

    // JNI функции
    private external fun nativeCreate(): Long
    private external fun nativeDestroy(handle: Long)
    private external fun nativeConnect(handle: Long, url: String, username: String?, password: String?, timeoutMs: Int): Boolean
    private external fun nativeDisconnect(handle: Long)
    private external fun nativeGetStatus(handle: Long): Int
    private external fun nativePlay(handle: Long): Boolean
    private external fun nativeStop(handle: Long): Boolean
    private external fun nativePause(handle: Long): Boolean
    private external fun nativeGetStreamCount(handle: Long): Int
    private external fun nativeGetStreamType(handle: Long, streamIndex: Int): Int
    private external fun nativeGetStreamInfo(handle: Long, streamIndex: Int, width: IntArray, height: IntArray, fps: IntArray, codec: ByteArray): Boolean
    private external fun nativeSetFrameCallback(handle: Long, streamType: Int, callback: java.util.function.Consumer<RtspFrame>)
    private external fun nativeSetStatusCallback(handle: Long, callback: java.util.function.BiConsumer<RtspClientStatus, String?>)
}

actual typealias NativeRtspClientHandle = Long

