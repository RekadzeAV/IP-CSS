package com.company.ipcamera.shared.data.datasource.remote

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult

/**
 * Удаленный источник данных для записей.
 * Отвечает за работу с REST API.
 */
interface RecordingRemoteDataSource {
    /**
     * Получить список записей с сервера
     */
    suspend fun getRecordings(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<PaginatedResult<Recording>>

    /**
     * Получить запись по ID с сервера
     */
    suspend fun getRecordingById(id: String): ApiResult<Recording>

    /**
     * Удалить запись с сервера
     */
    suspend fun deleteRecording(id: String): ApiResult<Unit>

    /**
     * Получить URL для скачивания записи
     */
    suspend fun getDownloadUrl(id: String): ApiResult<String>

    /**
     * Экспортировать запись
     */
    suspend fun exportRecording(id: String, format: String, quality: String): ApiResult<String>
}

