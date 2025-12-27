package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.cinterop.*
import com.company.ipcamera.core.network.rtsp.rtsp_client.*

/**
 * iOS реализация RTSP клиента через FFI
 * Использует cinterop для вызова нативных C++ функций
 */
actual class NativeRtspClient {

    // Thread-local хранилище для callbacks, чтобы избежать утечек памяти
    private val frameCallbacks = mutableMapOf<Long, StableRef<(RtspFrame) -> Unit>>()
    private val statusCallbacks = mutableMapOf<Long, StableRef<(RtspClientStatus, String?) -> Unit>>()

    actual fun create(): NativeRtspClientHandle {
        val client = rtsp_client_create()
        return if (client != null) {
            // Конвертируем CPointer в Long для хранения handle
            client.rawValue.toLong()
        } else {
            0L
        }
    }

    actual suspend fun connect(
        handle: NativeRtspClientHandle,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = withContext(Dispatchers.Default) {
        val client = handleToPointer(handle) ?: return@withContext false

        memScoped {
            val urlPtr = url.cstr.ptr
            val usernamePtr = username?.cstr?.ptr
            val passwordPtr = password?.cstr?.ptr

            val result = rtsp_client_connect(
                client,
                urlPtr,
                usernamePtr,
                passwordPtr,
                timeoutMs.convert()
            )

            return@withContext result.toBoolean()
        }
    }

    actual suspend fun disconnect(handle: NativeRtspClientHandle) = withContext(Dispatchers.Default) {
        val client = handleToPointer(handle) ?: return@withContext
        rtsp_client_disconnect(client)
    }

    actual fun getStatus(handle: NativeRtspClientHandle): RtspClientStatus {
        val client = handleToPointer(handle) ?: return RtspClientStatus.DISCONNECTED
        val nativeStatus = rtsp_client_get_status(client)
        return convertNativeStatus(nativeStatus)
    }

    actual suspend fun play(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.Default) {
        val client = handleToPointer(handle) ?: return@withContext false
        val result = rtsp_client_play(client)
        return@withContext result.toBoolean()
    }

    actual suspend fun stop(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.Default) {
        val client = handleToPointer(handle) ?: return@withContext false
        val result = rtsp_client_stop(client)
        return@withContext result.toBoolean()
    }

    actual suspend fun pause(handle: NativeRtspClientHandle): Boolean = withContext(Dispatchers.Default) {
        val client = handleToPointer(handle) ?: return@withContext false
        val result = rtsp_client_pause(client)
        return@withContext result.toBoolean()
    }

    actual fun getStreamCount(handle: NativeRtspClientHandle): Int {
        val client = handleToPointer(handle) ?: return 0
        return rtsp_client_get_stream_count(client).toInt()
    }

    actual fun getStreamType(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamType? {
        val client = handleToPointer(handle) ?: return null
        val nativeType = rtsp_client_get_stream_type(client, streamIndex.convert())
        return convertNativeStreamType(nativeType)
    }

    actual fun getStreamInfo(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamInfo? {
        val client = handleToPointer(handle) ?: return null

        memScoped {
            val width = alloc<IntVar>()
            val height = alloc<IntVar>()
            val fps = alloc<IntVar>()
            val codecBuffer = allocArray<ByteVar>(64)

            val success = rtsp_client_get_stream_info(
                client,
                streamIndex.convert(),
                width.ptr,
                height.ptr,
                fps.ptr,
                codecBuffer,
                64.convert()
            )

            if (!success.toBoolean()) return null

            val nativeType = rtsp_client_get_stream_type(client, streamIndex.convert())
            val streamType = convertNativeStreamType(nativeType) ?: return null
            val codec = codecBuffer.toKString()
            val resolution = if (width.value > 0 && height.value > 0) {
                Resolution(width.value, height.value)
            } else null

            return RtspStreamInfo(streamIndex, streamType, resolution, fps.value, codec)
        }
    }

    actual fun setFrameCallback(
        handle: NativeRtspClientHandle,
        streamType: RtspStreamType,
        callback: (RtspFrame) -> Unit
    ) {
        val client = handleToPointer(handle) ?: return

        // Освобождаем предыдущий callback, если есть
        frameCallbacks[handle]?.dispose()

        // Создаем StableRef для callback, чтобы он мог быть вызван из нативного кода
        val stableRef = StableRef.create(callback)
        frameCallbacks[handle] = stableRef

        // Конвертируем тип потока
        val nativeStreamType = convertStreamType(streamType)

        // Создаем C callback, который будет вызывать Kotlin callback
        // Тип callback: void (*RTSPFrameCallback)(RTSPFrame* frame, void* userData)
        val cCallback: CPointer<CFunction<(CPointer<RTSPFrameVar>?, COpaquePointer?) -> Unit>> =
            staticCFunction { framePtr: CPointer<RTSPFrameVar>?, userData: COpaquePointer? ->
                if (framePtr != null && userData != null) {
                    try {
                        val ref = userData.asStableRef<(RtspFrame) -> Unit>()
                        val frame = convertNativeFrame(framePtr)
                        ref.get().invoke(frame)
                        // Освобождаем кадр после использования
                        rtsp_frame_release(framePtr)
                    } catch (e: Throwable) {
                        // Игнорируем ошибки в callback, но пытаемся освободить кадр
                        try {
                            if (framePtr != null) {
                                rtsp_frame_release(framePtr)
                            }
                        } catch (e2: Throwable) {
                            // Игнорируем ошибки освобождения
                        }
                    }
                }
            }

        // Устанавливаем callback в нативную библиотеку
        rtsp_client_set_frame_callback(
            client,
            nativeStreamType,
            cCallback,
            stableRef.asCPointer()
        )
    }

    actual fun setStatusCallback(
        handle: NativeRtspClientHandle,
        callback: (RtspClientStatus, String?) -> Unit
    ) {
        val client = handleToPointer(handle) ?: return

        // Освобождаем предыдущий callback, если есть
        statusCallbacks[handle]?.dispose()

        // Создаем StableRef для callback
        val stableRef = StableRef.create(callback)
        statusCallbacks[handle] = stableRef

        // Создаем C callback
        // Тип callback: void (*RTSPStatusCallback)(RTSPStatus status, const char* message, void* userData)
        val cCallback: CPointer<CFunction<(RTSPStatus, CPointer<ByteVar>?, COpaquePointer?) -> Unit>> =
            staticCFunction { status: RTSPStatus, message: CPointer<ByteVar>?, userData: COpaquePointer? ->
                if (userData != null) {
                    try {
                        val ref = userData.asStableRef<(RtspClientStatus, String?) -> Unit>()
                        val kotlinStatus = convertNativeStatus(status)
                        val kotlinMessage = message?.toKString()
                        ref.get().invoke(kotlinStatus, kotlinMessage)
                    } catch (e: Throwable) {
                        // Игнорируем ошибки в callback
                    }
                }
            }

        // Устанавливаем callback в нативную библиотеку
        rtsp_client_set_status_callback(
            client,
            cCallback,
            stableRef.asCPointer()
        )
    }

    actual fun destroy(handle: NativeRtspClientHandle) {
        val client = handleToPointer(handle) ?: return

        // Освобождаем callbacks
        frameCallbacks[handle]?.dispose()
        frameCallbacks.remove(handle)
        statusCallbacks[handle]?.dispose()
        statusCallbacks.remove(handle)

        rtsp_client_destroy(client)
    }

    // Вспомогательные функции конвертации

    private fun handleToPointer(handle: Long): CPointer<RTSPClientVar>? {
        return if (handle == 0L) {
            null
        } else {
            // Конвертируем Long обратно в CPointer
            interpretCPointer<RTSPClientVar>(handle)
        }
    }

    private fun convertNativeStatus(status: RTSPStatus): RtspClientStatus {
        return when (status) {
            RTSP_STATUS_DISCONNECTED -> RtspClientStatus.DISCONNECTED
            RTSP_STATUS_CONNECTING -> RtspClientStatus.CONNECTING
            RTSP_STATUS_CONNECTED -> RtspClientStatus.CONNECTED
            RTSP_STATUS_PLAYING -> RtspClientStatus.PLAYING
            RTSP_STATUS_ERROR -> RtspClientStatus.ERROR
            else -> RtspClientStatus.DISCONNECTED
        }
    }

    private fun convertNativeStreamType(type: RTSPStreamType): RtspStreamType? {
        return when (type) {
            RTSP_STREAM_VIDEO -> RtspStreamType.VIDEO
            RTSP_STREAM_AUDIO -> RtspStreamType.AUDIO
            RTSP_STREAM_METADATA -> RtspStreamType.METADATA
            else -> null
        }
    }

    private fun convertStreamType(type: RtspStreamType): RTSPStreamType {
        return when (type) {
            RtspStreamType.VIDEO -> RTSP_STREAM_VIDEO
            RtspStreamType.AUDIO -> RTSP_STREAM_AUDIO
            RtspStreamType.METADATA -> RTSP_STREAM_METADATA
        }
    }

    private fun convertNativeFrame(framePtr: CPointer<RTSPFrameVar>?): RtspFrame {
        if (framePtr == null) {
            return RtspFrame(
                data = ByteArray(0),
                timestamp = 0L,
                streamType = RtspStreamType.VIDEO,
                width = 0,
                height = 0
            )
        }

        val frame = framePtr.pointed

        // Получаем размер кадра из структуры
        val size = frame.size.toInt()
        val data = ByteArray(size)

        // Получаем указатель на данные из структуры
        val dataPtr = frame.data
        if (dataPtr != null && size > 0) {
            // Копируем данные из нативного буфера
            for (i in 0 until size) {
                data[i] = dataPtr[i]
            }
        }

        // Получаем временную метку из структуры
        val timestamp = frame.timestamp.toLong()

        // Получаем тип потока
        val streamType = convertNativeStreamType(frame.type) ?: RtspStreamType.VIDEO

        return RtspFrame(
            data = data,
            timestamp = timestamp,
            streamType = streamType,
            width = frame.width.toInt(),
            height = frame.height.toInt()
        )
    }
}

actual typealias NativeRtspClientHandle = Long

