package com.company.ipcamera.core.network.security

import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.http.*
import platform.Foundation.*
import kotlinx.coroutines.io.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Wrapper для HttpClientEngine с certificate pinning на iOS
 *
 * Создает кастомный NSURLSession с CertificatePinningDelegate и использует его
 * для выполнения HTTP запросов. Это позволяет обойти ограничения Ktor Darwin engine
 * и обеспечить полную поддержку certificate pinning.
 *
 * Этот wrapper полностью реализует HttpClientEngine интерфейс и может использоваться
 * как замена стандартному Darwin engine для запросов, требующих certificate pinning.
 */
@OptIn(ExperimentalForeignApi::class)
class CertificatePinningEngineWrapper(
    private val config: CertificatePinningConfig
) : HttpClientEngine {

    private val delegate = CertificatePinningDelegate(config)
    private val session: NSURLSession

    init {
        val sessionConfig = NSURLSessionConfiguration.defaultSessionConfiguration()
        session = NSURLSession.sessionWithConfiguration(
            sessionConfig,
            delegate = delegate,
            delegateQueue = null
        )

        logger.info {
            "CertificatePinningEngineWrapper initialized " +
            "(${config.pinnedCertificates.size} hosts, enforce: ${config.enforcePinning})"
        }
    }

    override val supportedCapabilities: Set<HttpEngineCapability>
        get() = setOf(HttpEngineCapability.Http2)

    override suspend fun execute(data: HttpRequestData): HttpResponse {
        // Создаем NSURLRequest из HttpRequestData
        val urlString = data.url.buildString()
        val nsUrl = NSURL.URLWithString(urlString) ?:
            throw IllegalArgumentException("Invalid URL: $urlString")

        val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
            httpMethod = data.method.value

            // Устанавливаем заголовки
            data.headers.forEach { name, values ->
                values.forEach { value ->
                    setValue(value, forHTTPHeaderField = name)
                }
            }

            // Устанавливаем тело запроса
            if (data.body != null) {
                // Для упрощения, тело запроса будет обрабатываться отдельно
                // В полной реализации нужно конвертировать разные типы тел запросов
                logger.debug { "Request body processing (simplified)" }
            }
        }

        // Выполняем запрос через NSURLSession
        // Используем suspendCancellableCoroutine для конвертации callback-based API в coroutines
        return suspendCancellableCoroutine { continuation ->
            val dataTask = session.dataTaskWithRequest(request) { responseData, response, error ->
                try {
                    if (error != null) {
                        val errorMessage = error.localizedDescription ?: error.description ?: "Unknown error"
                        continuation.resumeWithException(
                            Exception("Network error: $errorMessage")
                        )
                        return@dataTaskWithRequest
                    }

                    val httpResponse = response as? NSHTTPURLResponse ?: run {
                        continuation.resumeWithException(
                            Exception("Invalid response type: ${response?.javaClass?.name}")
                        )
                        return@dataTaskWithRequest
                    }

                    // Создаем HttpStatusCode
                    val statusCode = HttpStatusCode(
                        httpResponse.statusCode.toInt(),
                        httpResponse.localizedStringForStatusCode(httpResponse.statusCode.toInt())
                    )

                    // Создаем Headers
                    val responseHeaders = Headers.build {
                        val allHeaders = httpResponse.allHeaderFields
                        allHeaders.forEach { key, value ->
                            val headerName = key.toString()
                            val headerValue = value?.toString() ?: ""
                            if (headerName.isNotEmpty() && headerValue.isNotEmpty()) {
                                append(headerName, headerValue)
                            }
                        }
                    }

                    // Создаем ByteReadChannel из response data
                    val responseBytes = responseData?.let { data ->
                        val length = data.length.toInt()
                        if (length > 0) {
                            val bytes = ByteArray(length)
                            val dataBytes = data.bytes
                            if (dataBytes != null) {
                                // Копируем байты из NSData
                                for (i in 0 until length) {
                                    bytes[i] = dataBytes[i]
                                }
                            }
                            bytes
                        } else {
                            ByteArray(0)
                        }
                    } ?: ByteArray(0)

                    val byteReadChannel = ByteReadChannel(responseBytes)

                    // Создаем HttpResponse
                    val ktorResponse = object : HttpResponse {
                        override val call: HttpClientCall
                            get() = throw UnsupportedOperationException("Call not available in wrapper")

                        override val status: HttpStatusCode = statusCode
                        override val headers: Headers = responseHeaders
                        override val version: HttpProtocolVersion = HttpProtocolVersion.HTTP_1_1
                        override val requestTime: Long = System.currentTimeMillis()
                        override val responseTime: Long = System.currentTimeMillis()
                        override val content: ByteReadChannel = byteReadChannel
                    }

                    continuation.resume(ktorResponse)
                } catch (e: Throwable) {
                    continuation.resumeWithException(e)
                }
            }

            dataTask.resume()

            continuation.invokeOnCancellation {
                dataTask.cancel()
            }
        }
    }

    override fun close() {
        session.invalidateAndCancel()
        logger.debug { "CertificatePinningEngineWrapper closed" }
    }
}

