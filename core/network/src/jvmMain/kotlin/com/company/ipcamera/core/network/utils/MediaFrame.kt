package com.company.ipcamera.core.network.utils

/**
 * JVM actual реализация MediaFrame
 */
actual class MediaFrame actual constructor(
    /**
     * Сырые данные фрейма
     */
    actual val data: ByteArray,

    /**
     * Тип фрейма
     */
    actual val type: FrameType,

    /**
     * Timestamp фрейма (в миллисекундах)
     */
    actual val timestamp: Long,

    /**
     * DTS (Decoding Time Stamp)
     */
    actual val dts: Long,

    /**
     * PTS (Presentation Time Stamp)
     */
    actual val pts: Long,

    /**
     * Key frame flag (для видео)
     */
    actual val isKeyFrame: Boolean,

    /**
     * Длительность фрейма в миллисекундах
     */
    actual val durationMs: Long
) {
    /**
     * Размер фрейма в байтах
     */
    actual fun size(): Int = data.size

    /**
     * Проверка на пустой фрейм
     */
    actual fun isEmpty(): Boolean = data.isEmpty()

    /**
     * Получить данные как hex строку (для отладки)
     */
    actual fun toHexString(limit: Int): String {
        return data.take(limit).joinToString(" ") { it.toUByte().toString(16).padStart(2, '0').uppercase() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MediaFrame
        return data.contentEquals(other.data) &&
            type == other.type &&
            timestamp == other.timestamp &&
            dts == other.dts &&
            pts == other.pts &&
            isKeyFrame == other.isKeyFrame &&
            durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + dts.hashCode()
        result = 31 * result + pts.hashCode()
        result = 31 * result + isKeyFrame.hashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }

    override fun toString(): String {
        return "MediaFrame(type=$type, size=${data.size}, timestamp=$timestamp, isKeyFrame=$isKeyFrame)"
    }
}

/**
 * JVM actual функция для создания MediaFrame
 */
actual fun createMediaFrame(
    data: ByteArray,
    type: FrameType,
    timestamp: Long,
    dts: Long,
    pts: Long,
    isKeyFrame: Boolean,
    durationMs: Long
): MediaFrame {
    return MediaFrame(
        data = data,
        type = type,
        timestamp = timestamp,
        dts = dts,
        pts = pts,
        isKeyFrame = isKeyFrame,
        durationMs = durationMs
    )
}

/**
 * JVM actual функция для получения текущего timestamp
 */
actual fun getCurrentTimestampMs(): Long = System.currentTimeMillis()

/**
 * Билдер для создания MediaFrame
 */
class MediaFrameBuilder {
    private var data: ByteArray = byteArrayOf()
    private var type: FrameType = FrameType.UNKNOWN
    private var timestamp: Long = System.currentTimeMillis()
    private var dts: Long = -1
    private var pts: Long = -1
    private var isKeyFrame: Boolean = false
    private var durationMs: Long = 0

    fun data(data: ByteArray) = apply { this.data = data }
    fun type(type: FrameType) = apply { this.type = type }
    fun timestamp(timestamp: Long) = apply { this.timestamp = timestamp }
    fun dts(dts: Long) = apply { this.dts = dts }
    fun pts(pts: Long) = apply { this.pts = pts }
    fun keyFrame(keyFrame: Boolean) = apply { this.isKeyFrame = keyFrame }
    fun durationMs(durationMs: Long) = apply { this.durationMs = durationMs }

    fun build(): MediaFrame {
        return MediaFrame(
            data = data,
            type = type,
            timestamp = timestamp,
            dts = dts,
            pts = pts,
            isKeyFrame = isKeyFrame,
            durationMs = durationMs
        )
    }
}

/**
 * Функция-хелпер для создания видео фреймов
 */
fun videoFrame(
    data: ByteArray,
    timestamp: Long = System.currentTimeMillis(),
    isKeyFrame: Boolean = false,
    durationMs: Long = 33
): MediaFrame {
    return MediaFrame(
        data = data,
        type = FrameType.VIDEO,
        timestamp = timestamp,
        isKeyFrame = isKeyFrame,
        durationMs = durationMs
    )
}

/**
 * Функция-хелпер для создания аудио фреймов
 */
fun audioFrame(
    data: ByteArray,
    timestamp: Long = System.currentTimeMillis(),
    durationMs: Long = 20
): MediaFrame {
    return MediaFrame(
        data = data,
        type = FrameType.AUDIO,
        timestamp = timestamp,
        durationMs = durationMs
    )
}
