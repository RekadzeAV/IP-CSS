package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.utils.MediaFrame
import kotlinx.coroutines.flow.StateFlow

/**
 * Интерфейс RTSP клиента (общий для всех реализаций)
 */
interface RtspClientInterface {

    /**
     * Подключиться к RTSP серверу
     */
    suspend fun connect(): Boolean

    /**
     * Отключиться от RTSP сервера
     */
    suspend fun disconnect()

    /**
     * Начать воспроизведение
     */
    suspend fun play(): Boolean

    /**
     * Приостановить воспроизведение
     */
    suspend fun pause(): Boolean

    /**
     * Завершить сессию
     */
    suspend fun teardown()

    /**
     * Получить текущий статус подключения
     */
    fun getStatus(): StateFlow<RtspClientStatus>

    /**
     * Получить информацию о видео потоке
     */
    fun getVideoInfo(): VideoStreamInfo?

    /**
     * Получить информацию об аудио потоке
     */
    fun getAudioInfo(): AudioStreamInfo?

    /**
     * Получить следующий видео фрейм
     */
    suspend fun getVideoFrame(timeoutMs: Long = 1000): MediaFrame?

    /**
     * Получить следующий аудио фрейм
     */
    suspend fun getAudioFrame(timeoutMs: Long = 1000): MediaFrame?

    /**
     * Освободить ресурсы
     */
    fun close()
}

/**
 * RTSP клиент на базе Live555 библиотеки
 * * Нативная реализация RTSP клиента с использованием Live555 для
 * обработки RTSP потоков (H.264/H.265 видео, AAC/G.711 аудио)
 * * @param config Конфигурация клиента
 */
expect class Live555RTSPClient(config: RtspClientConfig) : RtspClientInterface {
    override suspend fun connect(): Boolean
    override suspend fun disconnect()
    override suspend fun play(): Boolean
    override suspend fun pause(): Boolean
    override suspend fun teardown()
    override fun getStatus(): StateFlow<RtspClientStatus>
    override fun getVideoInfo(): VideoStreamInfo?
    override fun getAudioInfo(): AudioStreamInfo?
    override suspend fun getVideoFrame(timeoutMs: Long): MediaFrame?
    override suspend fun getAudioFrame(timeoutMs: Long): MediaFrame?
    override fun close()
}

/**
 * Проверка поддержки Live555 на текущей платформе
 */
expect fun isLive555Supported(): Boolean

/**
 * Получить версию Live555
 */
expect fun getLive555Version(): String?
