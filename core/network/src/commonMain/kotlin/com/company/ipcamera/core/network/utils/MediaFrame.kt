package com.company.ipcamera.core.network.utils

/**
 * Медиа фрейм (видео или аудио)
 * * Содержит сырые данные фрейма и метаданные
 */
expect class MediaFrame(
    /**
     * Сырые данные фрейма
     */
    data: ByteArray,

    /**
     * Тип фрейма
     */
    type: FrameType,

    /**
     * Timestamp фрейма (в миллисекундах)
     */
    timestamp: Long = getCurrentTimestampMs(),

    /**
     * DTS (Decoding Time Stamp)
     */
    dts: Long = -1,

    /**
     * PTS (Presentation Time Stamp)
     */
    pts: Long = -1,

    /**
     * Key frame flag (для видео)
     */
    isKeyFrame: Boolean = false,

    /**
     * Длительность фрейма в миллисекундах
     */
    durationMs: Long = 0
) {
    /**
     * Сырые данные фрейма
     */
    val data: ByteArray

    /**
     * Тип фрейма
     */
    val type: FrameType

    /**
     * Timestamp фрейма (в миллисекундах)
     */
    val timestamp: Long

    /**
     * DTS (Decoding Time Stamp)
     */
    val dts: Long

    /**
     * PTS (Presentation Time Stamp)
     */
    val pts: Long

    /**
     * Key frame flag (для видео)
     */
    val isKeyFrame: Boolean

    /**
     * Длительность фрейма в миллисекундах
     */
    val durationMs: Long

    /**
     * Размер фрейма в байтах
     */
    fun size(): Int

    /**
     * Проверка на пустой фрейм
     */
    fun isEmpty(): Boolean

    /**
     * Получить данные как hex строку (для отладки)
     */
    fun toHexString(limit: Int = 32): String
}

/**
 * Создать MediaFrame (платформенно-специфичная реализация)
 */
expect fun createMediaFrame(
    data: ByteArray,
    type: FrameType,
    timestamp: Long = getCurrentTimestampMs(),
    dts: Long = -1,
    pts: Long = -1,
    isKeyFrame: Boolean = false,
    durationMs: Long = 0
): MediaFrame

/**
 * Получить текущий timestamp в миллисекундах
 */
expect fun getCurrentTimestampMs(): Long

/**
 * Тип медиа фрейма
 */
enum class FrameType {
    /**
     * Видео фрейм (H.264/H.265)
     */
    VIDEO,

    /**
     * Аудио фрейм (AAC/G.711)
     */
    AUDIO,

    /**
     * Неизвестный тип
     */
    UNKNOWN
}
