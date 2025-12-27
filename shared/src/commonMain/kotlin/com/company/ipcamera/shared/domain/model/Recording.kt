package com.company.ipcamera.shared.domain.model

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
    val format: RecordingFormat = RecordingFormat.MP4,
    val quality: Quality = Quality.HIGH,
    val status: RecordingStatus = RecordingStatus.ACTIVE,
    val thumbnailUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
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
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
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
    FLV
}

@Serializable
enum class RecordingStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}


