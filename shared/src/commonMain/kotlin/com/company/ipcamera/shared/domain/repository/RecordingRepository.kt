package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.Recording

/**
 * Репозиторий для работы с записями видео
 */
interface RecordingRepository {
    /**
     * Получить все записи
     */
    suspend fun getRecordings(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResult<Recording>
    
    /**
     * Получить запись по ID
     */
    suspend fun getRecordingById(id: String): Recording?
    
    /**
     * Добавить новую запись
     */
    suspend fun addRecording(recording: Recording): Result<Recording>
    
    /**
     * Обновить запись
     */
    suspend fun updateRecording(recording: Recording): Result<Recording>
    
    /**
     * Удалить запись
     */
    suspend fun deleteRecording(id: String): Result<Unit>
    
    /**
     * Получить URL для скачивания записи
     */
    suspend fun getDownloadUrl(id: String): Result<String>
    
    /**
     * Экспортировать запись
     */
    suspend fun exportRecording(id: String, format: String, quality: String): Result<String>
}

/**
 * Результат пагинации
 */
data class PaginatedResult<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)


