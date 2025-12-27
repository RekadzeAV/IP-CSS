package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.dto.*
import kotlinx.serialization.builtins.serializer

/**
 * API сервис для работы с событиями
 */
class EventApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/events"
) {
    
    /**
     * Получить список событий
     * 
     * @param type фильтр по типу события (motion, object_detection, face_detection и т.д.)
     * @param cameraId фильтр по ID камеры
     * @param severity фильтр по важности (INFO, WARNING, ERROR, CRITICAL)
     * @param acknowledged фильтр по статусу подтверждения
     * @param startTime фильтр по начальному времени
     * @param endTime фильтр по конечному времени
     * @param page номер страницы
     * @param limit количество элементов на странице
     * @return список событий с пагинацией
     */
    suspend fun getEvents(
        type: String? = null,
        cameraId: String? = null,
        severity: String? = null,
        acknowledged: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        page: Int = 1,
        limit: Int = 20
    ): PaginatedResponse<EventResponse> {
        val queryParams = mutableMapOf<String, String>()
        queryParams["page"] = page.toString()
        queryParams["limit"] = limit.toString()
        type?.let { queryParams["type"] = it }
        cameraId?.let { queryParams["camera_id"] = it }
        severity?.let { queryParams["severity"] = it }
        acknowledged?.let { queryParams["acknowledged"] = it.toString() }
        startTime?.let { queryParams["start_time"] = it.toString() }
        endTime?.let { queryParams["end_time"] = it.toString() }
        
        return apiClient.get(
            path = basePath,
            queryParameters = queryParams,
            responseType = PaginatedResponse.serializer(EventResponse.serializer())
        )
    }
    
    /**
     * Получить событие по ID
     * 
     * @param id идентификатор события
     * @return информация о событии
     */
    suspend fun getEventById(id: String): EventResponse {
        return apiClient.get(
            path = "$basePath/$id",
            responseType = EventResponse.serializer()
        )
    }
    
    /**
     * Подтвердить событие
     * 
     * @param id идентификатор события
     * @return результат подтверждения
     */
    suspend fun acknowledgeEvent(id: String): AcknowledgeEventResponse {
        return apiClient.post(
            path = "$basePath/$id/acknowledge",
            responseType = AcknowledgeEventResponse.serializer()
        )
    }
    
    /**
     * Подтвердить несколько событий
     * 
     * @param ids список идентификаторов событий
     * @return результат подтверждения
     */
    suspend fun acknowledgeEvents(ids: List<String>): ApiResponse<Map<String, Boolean>> {
        @kotlinx.serialization.Serializable
        data class AcknowledgeRequest(val ids: List<String>)
        
        return apiClient.post(
            path = "$basePath/acknowledge",
            body = AcknowledgeRequest(ids),
            responseType = ApiResponse.serializer(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    Boolean.serializer()
                )
            )
        )
    }
    
    /**
     * Удалить событие
     * 
     * @param id идентификатор события
     */
    suspend fun deleteEvent(id: String): ApiResponse<Unit> {
        return apiClient.delete(
            path = "$basePath/$id",
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
    
    /**
     * Получить статистику событий
     * 
     * @param cameraId фильтр по ID камеры
     * @param startTime начальное время
     * @param endTime конечное время
     * @return статистика событий
     */
    suspend fun getEventStatistics(
        cameraId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): ApiResponse<Map<String, Any>> {
        val queryParams = mutableMapOf<String, String>()
        cameraId?.let { queryParams["camera_id"] = it }
        startTime?.let { queryParams["start_time"] = it.toString() }
        endTime?.let { queryParams["end_time"] = it.toString() }
        
        return apiClient.get(
            path = "$basePath/statistics",
            queryParameters = queryParams,
            responseType = ApiResponse.serializer(
                kotlinx.serialization.builtins.MapSerializer(
                    String.serializer(),
                    kotlinx.serialization.json.JsonElement.serializer()
                )
            )
        )
    }
}

