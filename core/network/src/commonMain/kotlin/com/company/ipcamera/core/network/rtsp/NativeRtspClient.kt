package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.RtspClientStatus
import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamInfo
import com.company.ipcamera.core.network.RtspStreamType

/**
 * Expect класс для нативного RTSP клиента
 * Реализации для разных платформ будут в actual классах
 */
expect class NativeRtspClient constructor() {
    /**
     * Создать нативный RTSP клиент
     */
    fun create(): Long

    /**
     * Подключиться к RTSP серверу
     */
    suspend fun connect(
        handle: Long,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean

    /**
     * Отключиться от сервера
     */
    suspend fun disconnect(handle: Long)

    /**
     * Получить статус подключения
     */
    fun getStatus(handle: Long): RtspClientStatus

    /**
     * Начать воспроизведение
     */
    suspend fun play(handle: Long): Boolean

    /**
     * Остановить воспроизведение
     */
    suspend fun stop(handle: Long): Boolean

    /**
     * Приостановить воспроизведение
     */
    suspend fun pause(handle: Long): Boolean

    /**
     * Получить количество потоков
     */
    fun getStreamCount(handle: Long): Int

    /**
     * Получить тип потока
     */
    fun getStreamType(handle: Long, streamIndex: Int): RtspStreamType?

    /**
     * Получить информацию о потоке
     */
    fun getStreamInfo(handle: Long, streamIndex: Int): RtspStreamInfo?

    /**
     * Установить callback для получения кадров
     */
    fun setFrameCallback(
        handle: Long,
        streamType: RtspStreamType,
        callback: (RtspFrame) -> Unit
    )

    /**
     * Установить callback для изменения статуса
     */
    fun setStatusCallback(
        handle: Long,
        callback: (RtspClientStatus, String?) -> Unit
    )

    /**
     * Установить параметры автоматического переподключения
     */
    fun setReconnectParams(
        handle: Long,
        enabled: Boolean,
        maxRetries: Int,
        initialDelayMs: Int,
        maxDelayMs: Int,
        backoffMultiplier: Float
    )

    /**
     * Освободить ресурсы
     */
    fun destroy(handle: Long)
}

/** Дескриптор нативного RTSP-клиента (на всех платформах — Long). */
typealias NativeRtspClientHandle = Long
