package com.company.ipcamera.core.network.interceptor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse

/**
 * Интерцептор для обработки запросов
 */
interface RequestInterceptor {
    /**
     * Вызывается перед отправкой запроса
     * Можно модифицировать запрос
     */
    suspend fun onRequest(request: HttpRequestBuilder)

    /**
     * Вызывается после получения ответа
     * Можно модифицировать ответ или выполнить дополнительные действия
     */
    suspend fun onResponse(request: HttpRequestBuilder, response: HttpResponse): HttpResponse {
        return response
    }

    /**
     * Вызывается при ошибке запроса
     */
    suspend fun onError(request: HttpRequestBuilder, error: Throwable) {}
}

/**
 * Цепочка интерцепторов
 */
class InterceptorChain(private val interceptors: List<RequestInterceptor>) : RequestInterceptor {
    override suspend fun onRequest(request: HttpRequestBuilder) {
        interceptors.forEach { it.onRequest(request) }
    }

    override suspend fun onResponse(request: HttpRequestBuilder, response: HttpResponse): HttpResponse {
        var result = response
        interceptors.forEach { result = it.onResponse(request, result) }
        return result
    }

    override suspend fun onError(request: HttpRequestBuilder, error: Throwable) {
        interceptors.forEach { it.onError(request, error) }
    }
}

