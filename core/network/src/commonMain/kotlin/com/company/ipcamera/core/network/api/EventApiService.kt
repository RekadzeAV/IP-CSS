package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.dto.*

/**
 * API сервис для работы с событиями
 */
class EventApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/events"
) {

    private suspend inline fun <T> ApiResult<T>.getOrThrow(): T = fold(
        onSuccess = { it },
        onError = { throw it }
    )

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
        cameraId?.let { queryParams["cameraId"] = it }
        severity?.let { queryParams["severity"] = it }
        acknowledged?.let { queryParams["acknowledged"] = it.toString() }
        startTime?.let { queryParams["startTime"] = it.toString() }
        endTime?.let { queryParams["endTime"] = it.toString() }

        return apiClient.get<PaginatedResponse<EventResponse>>(
            path = basePath,
            queryParameters = queryParams
        ).getOrThrow()
    }

    /**
     * Получить событие по ID
     *
     * @param id идентификатор события
     * @return информация о событии
     */
    suspend fun getEventById(id: String): EventResponse {
        return apiClient.get<EventResponse>(path = "$basePath/$id").getOrThrow()
    }

    /**
     * Подтвердить событие
     *
     * @param id идентификатор события
     * @return результат подтверждения
     */
    suspend fun acknowledgeEvent(id: String): AcknowledgeEventResponse {
        return apiClient.post<Unit, AcknowledgeEventResponse>(
            path = "$basePath/$id/acknowledge"
        ).getOrThrow()
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

        return apiClient.post<AcknowledgeRequest, ApiResponse<Map<String, Boolean>>>(
            path = "$basePath/acknowledge",
            body = AcknowledgeRequest(ids)
        ).getOrThrow()
    }

    /**
     * Удалить событие
     *
     * @param id идентификатор события
     */
    suspend fun deleteEvent(id: String): ApiResponse<Unit> {
        return apiClient.delete<ApiResponse<Unit>>(path = "$basePath/$id").getOrThrow()
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
        cameraId?.let { queryParams["cameraId"] = it }
        startTime?.let { queryParams["startTime"] = it.toString() }
        endTime?.let { queryParams["endTime"] = it.toString() }

        return apiClient.get<ApiResponse<Map<String, Any>>>(
            path = "$basePath/statistics",
            queryParameters = queryParams
        ).getOrThrow()
    }
}
