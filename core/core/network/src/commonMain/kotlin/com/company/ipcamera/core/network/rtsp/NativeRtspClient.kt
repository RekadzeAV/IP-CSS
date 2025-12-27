package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType

/**
 * Expect класс для нативного RTSP клиента
 * Реализации для разных платформ будут в actual классах
 */
expect class NativeRtspClient {
    /**
     * Создать нативный RTSP клиент
     */
    fun create(): NativeRtspClientHandle

    /**
     * Подключиться к RTSP серверу
     */
    suspend fun connect(
        handle: NativeRtspClientHandle,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean

    /**
     * Отключиться от сервера
     */
    suspend fun disconnect(handle: NativeRtspClientHandle)

    /**
     * Получить статус подключения
     */
    fun getStatus(handle: NativeRtspClientHandle): RtspClientStatus

    /**
     * Начать воспроизведение
     */
    suspend fun play(handle: NativeRtspClientHandle): Boolean

    /**
     * Остановить воспроизведение
     */
    suspend fun stop(handle: NativeRtspClientHandle): Boolean

    /**
     * Приостановить воспроизведение
     */
    suspend fun pause(handle: NativeRtspClientHandle): Boolean

    /**
     * Получить количество потоков
     */
    fun getStreamCount(handle: NativeRtspClientHandle): Int

    /**
     * Получить тип потока
     */
    fun getStreamType(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamType?

    /**
     * Получить информацию о потоке
     */
    fun getStreamInfo(handle: NativeRtspClientHandle, streamIndex: Int): RtspStreamInfo?

    /**
     * Установить callback для получения кадров
     */
    fun setFrameCallback(
        handle: NativeRtspClientHandle,
        streamType: RtspStreamType,
        callback: (RtspFrame) -> Unit
    )

    /**
     * Установить callback для изменения статуса
     */
    fun setStatusCallback(
        handle: NativeRtspClientHandle,
        callback: (RtspClientStatus, String?) -> Unit
    )

    /**
     * Освободить ресурсы
     */
    fun destroy(handle: NativeRtspClientHandle)
}

/**
 * Handle для нативного RTSP клиента
 * Тип зависит от платформы (может быть указатель на C структуру)
 */
expect typealias NativeRtspClientHandle

