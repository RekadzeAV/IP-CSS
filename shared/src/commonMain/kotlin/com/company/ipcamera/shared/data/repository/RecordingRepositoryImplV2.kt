package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.data.datasource.local.RecordingLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.RecordingRemoteDataSource
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация RecordingRepository с использованием Data Sources (новая архитектура)
 *
 * Использует стратегию local-first: сначала проверяет локальную БД,
 * затем синхронизирует с удаленным API при необходимости
 */
class RecordingRepositoryImplV2(
    private val localDataSource: RecordingLocalDataSource,
    private val remoteDataSource: RecordingRemoteDataSource? = null
) : RecordingRepository {

    override suspend fun getRecordings(
        cameraId: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int,
        limit: Int
    ): PaginatedResult<Recording> = withContext(Dispatchers.Default) {
        try {
            // Получаем записи локально с фильтрацией
            val allRecordings = when {
                cameraId != null && startTime != null && endTime != null -> {
                    localDataSource.getRecordingsByCameraId(cameraId)
                        .filter { it.startTime >= startTime && (it.endTime ?: Long.MAX_VALUE) <= endTime }
                }
                cameraId != null -> {
                    localDataSource.getRecordingsByCameraId(cameraId)
                }
                startTime != null && endTime != null -> {
                    localDataSource.getRecordingsByDateRange(startTime, endTime)
                }
                else -> {
                    localDataSource.getRecordings()
                }
            }

            // Применяем пагинацию
            val offset = (page - 1) * limit
            val paginatedItems = allRecordings.drop(offset).take(limit)
            val localResult = PaginatedResult(
                items = paginatedItems,
                total = allRecordings.size,
                page = page,
                limit = limit,
                hasMore = (offset + limit) < allRecordings.size
            )

            // Если локально пусто или есть удаленный источник, синхронизируем
            if ((localResult.items.isEmpty() || remoteDataSource != null) && remoteDataSource != null) {
                remoteDataSource.getRecordings(cameraId, startTime, endTime, page, limit).fold(
                    onSuccess = { remoteResult ->
                        // Сохраняем в локальную БД для кэширования
                        if (remoteResult.items.isNotEmpty()) {
                            localDataSource.saveRecordings(remoteResult.items).getOrNull()
                        }
                        remoteResult
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to get recordings from remote, using local" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recordings" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getRecordingById(id: String): Recording? = withContext(Dispatchers.Default) {
        try {
            // Сначала проверяем локально
            localDataSource.getRecordingById(id) ?: run {
                // Если не найдено локально, пытаемся получить с сервера
                remoteDataSource?.getRecordingById(id)?.fold(
                    onSuccess = { recording ->
                        // Сохраняем в локальную БД
                        localDataSource.saveRecording(recording).getOrNull()
                        recording
                    },
                    onError = {
                        logger.warn(it) { "Failed to get recording from remote: $id" }
                        null
                    }
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting recording by id: $id" }
            null
        }
    }

    override suspend fun addRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.Default) {
        try {
            // Записи обычно создаются на сервере, локально сохраняем только для кэширования
            localDataSource.saveRecording(recording)
        } catch (e: Exception) {
            logger.error(e) { "Error adding recording: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateRecording(recording: Recording): Result<Recording> = withContext(Dispatchers.Default) {
        try {
            // Обновляем локально (записи обычно обновляются на сервере автоматически)
            localDataSource.updateRecording(recording)
        } catch (e: Exception) {
            logger.error(e) { "Error updating recording: ${recording.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteRecording(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Удаляем локально
            val localResult = localDataSource.deleteRecording(id)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.deleteRecording(id).fold(
                    onSuccess = {
                        Result.success(Unit)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to delete recording from remote, but deleted locally: $id" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting recording: $id" }
            Result.failure(e)
        }
    }

    override suspend fun getDownloadUrl(id: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Для получения URL загрузки используем удаленный источник
            remoteDataSource?.getDownloadUrl(id)?.fold(
                onSuccess = { url ->
                    Result.success(url)
                },
                onError = { error ->
                    logger.error(error) { "Error getting download URL for recording: $id" }
                    Result.failure(Exception(error.message))
                }
            ) ?: Result.failure(Exception("Remote data source not available"))
        } catch (e: Exception) {
            logger.error(e) { "Error getting download URL: $id" }
            Result.failure(e)
        }
    }

    override suspend fun exportRecording(id: String, format: String, quality: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Для экспорта используем удаленный источник
            remoteDataSource?.exportRecording(id, format, quality)?.fold(
                onSuccess = { url ->
                    Result.success(url)
                },
                onError = { error ->
                    logger.error(error) { "Error exporting recording: $id" }
                    Result.failure(Exception(error.message))
                }
            ) ?: Result.failure(Exception("Remote data source not available"))
        } catch (e: Exception) {
            logger.error(e) { "Error exporting recording: $id" }
            Result.failure(e)
        }
    }
}

