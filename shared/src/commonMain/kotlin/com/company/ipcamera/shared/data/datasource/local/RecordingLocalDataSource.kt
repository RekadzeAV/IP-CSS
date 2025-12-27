package com.company.ipcamera.shared.data.datasource.local

import com.company.ipcamera.shared.domain.model.Recording

/**
 * Локальный источник данных для записей.
 * Отвечает за работу с локальной базой данных SQLite.
 */
interface RecordingLocalDataSource {
    /**
     * Получить все записи из локальной БД
     */
    suspend fun getRecordings(): List<Recording>

    /**
     * Получить запись по ID из локальной БД
     */
    suspend fun getRecordingById(id: String): Recording?

    /**
     * Получить записи по ID камеры
     */
    suspend fun getRecordingsByCameraId(cameraId: String): List<Recording>

    /**
     * Получить записи в диапазоне дат
     */
    suspend fun getRecordingsByDateRange(startTime: Long, endTime: Long): List<Recording>

    /**
     * Получить записи по статусу
     */
    suspend fun getRecordingsByStatus(status: String): List<Recording>

    /**
     * Сохранить запись в локальную БД
     */
    suspend fun saveRecording(recording: Recording): Result<Recording>

    /**
     * Сохранить список записей в локальную БД (batch операция)
     */
    suspend fun saveRecordings(recordings: List<Recording>): Result<List<Recording>>

    /**
     * Обновить запись в локальной БД
     */
    suspend fun updateRecording(recording: Recording): Result<Recording>

    /**
     * Обновить статус записи
     */
    suspend fun updateRecordingStatus(id: String, status: String): Result<Unit>

    /**
     * Удалить запись из локальной БД
     */
    suspend fun deleteRecording(id: String): Result<Unit>

    /**
     * Удалить записи по ID камеры
     */
    suspend fun deleteRecordingsByCameraId(cameraId: String): Result<Unit>

    /**
     * Удалить все записи из локальной БД
     */
    suspend fun deleteAllRecordings(): Result<Unit>

    /**
     * Проверить существование записи
     */
    suspend fun recordingExists(id: String): Boolean
}

