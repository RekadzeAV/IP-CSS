package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.dto.*

/**
 * API сервис для работы с записями
 */
class RecordingApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/recordings"
) {

    private suspend inline fun <T> ApiResult<T>.getOrThrow(): T = fold(
        onSuccess = { it },
        onError = { throw it }
    )

    /**
     * Получить список записей
     *
     * @param cameraId фильтр по ID камеры
     * @param startTime фильтр по начальному времени (timestamp в миллисекундах)
     * @param endTime фильтр по конечному времени (timestamp в миллисекундах)
     * @param page номер страницы
     * @param limit количество элементов на странице
     * @return список записей с пагинацией
     */
    suspend fun getRecordings(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResponse<RecordingResponse> {
        val queryParams = mutableMapOf<String, String>()
        queryParams["page"] = page.toString()
        queryParams["limit"] = limit.toString()
        cameraId?.let { queryParams["cameraId"] = it }
        startTime?.let { queryParams["startTime"] = it.toString() }
        endTime?.let { queryParams["endTime"] = it.toString() }

        return apiClient.get<PaginatedResponse<RecordingResponse>>(
            path = basePath,
            queryParameters = queryParams
        ).getOrThrow()
    }

    /**
     * Получить запись по ID
     *
     * @param id идентификатор записи
     * @return информация о записи
     */
    suspend fun getRecordingById(id: String): RecordingResponse {
        return apiClient.get<RecordingResponse>(path = "$basePath/$id").getOrThrow()
    }

    /**
     * Начать запись
     *
     * @param request параметры записи
     * @return информация о начатой записи
     */
    suspend fun startRecording(request: StartRecordingRequest): StartRecordingResponse {
        return apiClient.post<StartRecordingRequest, StartRecordingResponse>(
            path = "$basePath/start",
            body = request
        ).getOrThrow()
    }

    /**
     * Остановить запись
     *
     * @param cameraId идентификатор камеры
     * @return результат остановки
     */
    suspend fun stopRecording(cameraId: String): ApiResponse<Unit> {
        return apiClient.post<Unit, ApiResponse<Unit>>(path = "$basePath/stop/$cameraId").getOrThrow()
    }

    /**
     * Удалить запись
     *
     * @param id идентификатор записи
     */
    suspend fun deleteRecording(id: String): ApiResponse<Unit> {
        return apiClient.delete<ApiResponse<Unit>>(path = "$basePath/$id").getOrThrow()
    }

    /**
     * Экспортировать запись
     *
     * @param id идентификатор записи
     * @param request параметры экспорта
     * @return информация об экспорте
     */
    suspend fun exportRecording(
        id: String,
        request: ExportRecordingRequest
    ): ExportRecordingResponse {
        return apiClient.post<ExportRecordingRequest, ExportRecordingResponse>(
            path = "$basePath/$id/export",
            body = request
        ).getOrThrow()
    }

    /**
     * Получить URL для скачивания записи
     *
     * @param id идентификатор записи
     * @return URL для скачивания
     */
    suspend fun getDownloadUrl(id: String): ApiResponse<Map<String, String>> {
        return apiClient.get<ApiResponse<Map<String, String>>>(path = "$basePath/$id/download").getOrThrow()
    }
}
