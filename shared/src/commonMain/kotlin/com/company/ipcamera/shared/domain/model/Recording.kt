package com.company.ipcamera.shared.domain.model

import com.company.ipcamera.shared.common.nowMillis
import kotlinx.serialization.Serializable

/**
 * Модель записи видео
 */
@Serializable
data class Recording(
    val id: String,
    val cameraId: String,
    val cameraName: String? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long,
    val filePath: String? = null,
    val fileSize: Long? = null,
    /** Фактический видеокодек итогового файла (например, H.264/H.265). */
    val codec: String? = null,
    val format: RecordingFormat = RecordingFormat.MP4,
    val quality: Quality = Quality.HIGH,
    val status: RecordingStatus = RecordingStatus.ACTIVE,
    val thumbnailUrl: String? = null,
    val createdAt: Long = nowMillis(),
) {
    /**
     * Проверка, завершена ли запись
     */
    fun isCompleted(): Boolean = status == RecordingStatus.COMPLETED

    /**
     * Проверка, активна ли запись
     */
    fun isActive(): Boolean = status == RecordingStatus.ACTIVE

    /**
     * Получение размера файла в человекочитаемом формате
     */
    fun getFormattedFileSize(): String {
        if (fileSize == null) return "Unknown"
        val kb = fileSize / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> "${((gb * 100).toInt() / 100.0)} GB"
            mb >= 1 -> "${((mb * 100).toInt() / 100.0)} MB"
            kb >= 1 -> "${((kb * 100).toInt() / 100.0)} KB"
            else -> "$fileSize bytes"
        }
    }
}

@Serializable
enum class RecordingFormat {
    MP4,
    AVI,
    MKV,
    MOV,
    FLV,
}

@Serializable
enum class RecordingStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED,
}
