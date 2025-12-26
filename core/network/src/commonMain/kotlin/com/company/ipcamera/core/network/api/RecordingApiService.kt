package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.dto.*
import kotlinx.serialization.builtins.serializer

/**
 * API сервис для работы с записями
 */
class RecordingApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/recordings"
) {
    
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
        cameraId?.let { queryParams["camera_id"] = it }
        startTime?.let { queryParams["start_time"] = it.toString() }
        endTime?.let { queryParams["end_time"] = it.toString() }
        
        return apiClient.get(
            path = basePath,
            queryParameters = queryParams,
            responseType = PaginatedResponse.serializer(RecordingResponse.serializer())
        )
    }
    
    /**
     * Получить запись по ID
     * 
     * @param id идентификатор записи
     * @return информация о записи
     */
    suspend fun getRecordingById(id: String): RecordingResponse {
        return apiClient.get(
            path = "$basePath/$id",
            responseType = RecordingResponse.serializer()
        )
    }
    
    /**
     * Начать запись
     * 
     * @param request параметры записи
     * @return информация о начатой записи
     */
    suspend fun startRecording(request: StartRecordingRequest): StartRecordingResponse {
        return apiClient.post(
            path = "$basePath/start",
            body = request,
            responseType = StartRecordingResponse.serializer()
        )
    }
    
    /**
     * Остановить запись
     * 
     * @param id идентификатор записи
     * @return результат остановки
     */
    suspend fun stopRecording(id: String): ApiResponse<Unit> {
        return apiClient.post(
            path = "$basePath/$id/stop",
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
    
    /**
     * Удалить запись
     * 
     * @param id идентификатор записи
     */
    suspend fun deleteRecording(id: String): ApiResponse<Unit> {
        return apiClient.delete(
            path = "$basePath/$id",
            responseType = ApiResponse.serializer(Unit.serializer())
        )
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
        return apiClient.post(
            path = "$basePath/$id/export",
            body = request,
            responseType = ExportRecordingResponse.serializer()
        )
    }
    
    /**
     * Получить URL для скачивания записи
     * 
     * @param id идентификатор записи
     * @return URL для скачивания
     */
    suspend fun getDownloadUrl(id: String): ApiResponse<Map<String, String>> {
        return apiClient.get(
            path = "$basePath/$id/download",
            responseType = ApiResponse.serializer(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    String.serializer()
                )
            )
        )
    }
}


