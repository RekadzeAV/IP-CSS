package com.company.ipcamera.shared.platform.hwaccel

/**
 * Интерфейс для видеокодирования с поддержкой аппаратного ускорения
 */
interface VideoEncoder {
    /**
     * Начать кодирование видео
     *
     * @param inputSource источник видео (RTSP URL, файл, pipe)
     * @param outputPath путь для сохранения результата
     * @param codec конфигурация кодек
     * @param encoder конфигурация энкодера (HW или SW)
     */
    suspend fun startEncoding(
        inputSource: String,
        outputPath: String,
        codec: VideoCodec,
        encoder: EncoderConfig?,
    ): Result<Unit>

    /**
     * Остановить кодирование
     */
    suspend fun stopEncoding(): Result<Unit>

    /**
     * Приостановить кодирование
     */
    suspend fun pauseEncoding(): Result<Unit>

    /**
     * Возобновить кодирование
     */
    suspend fun resumeEncoding(): Result<Unit>

    /**
     * Получить статус кодирования
     */
    suspend fun getEncodingStatus(): EncodingStatus?

    /**
     * Проверить, активно ли кодирование
     */
    fun isEncoding(): Boolean
}

/**
 * Статус кодирования видео
 */
data class EncodingStatus(
    val isActive: Boolean,
    val isPaused: Boolean,
    val startTime: Long?,
    val bytesEncoded: Long,
    val duration: Long,
)