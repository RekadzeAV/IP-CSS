package com.company.ipcamera.core.network.test

import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.content.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*

/**
 * Фабрика для создания mock Ktor engine в тестах сетевого слоя.
 * Позволяет тестировать ApiClient, OnvifClient и другие компоненты
 * без реальных HTTP-вызовов.
 */
object MockEngineFactory {

    /**
     * Создать mock engine с предопределёнными ответами по пути запроса.
     *
     * @param responses Map путь → (статус код, тело ответа, content type)
     * @param delayMillis Искусственная задержка ответа (имитация сети)
     * @param onRequest Колбэк для проверки входящих запросов
     */
    fun create(
        responses: Map<String, MockResponse> = emptyMap(),
        delayMillis: Long = 0,
        onRequest: ((request: io.ktor.client.request.HttpRequestData) -> Unit)? = null
    ): HttpClientEngine {
        return MockEngine { request ->
            onRequest?.invoke(request)
            if (delayMillis > 0) delay(delayMillis)

            val path = request.url.encodedPath
            val response = responses[path] ?: responses[request.url.toString()]

            if (response != null) {
                respond(
                    content = response.body,
                    status = response.status,
                    headers = headersOf(
                        io.ktor.http.HttpHeaders.ContentType,
                        listOf(response.contentType)
                    )
                )
            } else {
                respondError(HttpStatusCode.NotFound)
            }
        }
    }

    /**
     * Создать mock engine для ONVIF SOAP-запросов.
     * Поддерживает проверку SOAP Action и возвращает соответствующий XML.
     */
    fun createOnvif(
        soapResponses: Map<String, String> = emptyMap(),
        delayMillis: Long = 0,
        onSoapRequest: ((soapAction: String, body: String) -> Unit)? = null
    ): HttpClientEngine {
        return MockEngine { request ->
            val body = (request.body as? TextContent)?.text ?: ""
            // Ktor отдаёт пустую строку, а не null, если заголовок отправлен как SOAPAction: ""
            val rawAction = request.headers["SOAPAction"] ?: ""
            val soapAction = if (rawAction.isBlank()) {
                extractSoapAction(body)
            } else {
                rawAction
            }

            onSoapRequest?.invoke(soapAction, body)
            if (delayMillis > 0) delay(delayMillis)

            val responseBody = soapResponses[soapAction] ?: soapResponses["default"]
                ?: return@MockEngine respondError(HttpStatusCode.NotFound)

            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(
                    io.ktor.http.HttpHeaders.ContentType,
                    listOf("application/soap+xml; charset=utf-8")
                )
            )
        }
    }

    /**
     * Создать mock engine с последовательностью ответов для одного пути
     * (полезно для тестирования retry, reconnect, backoff).
     */
    fun createSequential(
        vararg responses: Pair<String, MockResponse>
    ): HttpClientEngine {
        val iterator = responses.toList().iterator()
        return MockEngine { _ ->
            if (!iterator.hasNext()) {
                return@MockEngine respondError(HttpStatusCode.NotFound)
            }
            val (_, response) = iterator.next()
            respond(
                content = response.body,
                status = response.status,
                headers = headersOf(
                    io.ktor.http.HttpHeaders.ContentType,
                    listOf(response.contentType)
                )
            )
        }
    }

    private fun extractSoapAction(body: String): String {
        val regex = Regex("""Body>\s*<(?:\w+:)?(\w+)""")
        return regex.find(body)?.groupValues?.get(1) ?: ""
    }
}

/**
 * Описание mock-ответа HTTP.
 */
data class MockResponse(
    val body: String,
    val status: HttpStatusCode = HttpStatusCode.OK,
    val contentType: String = "application/json"
)
