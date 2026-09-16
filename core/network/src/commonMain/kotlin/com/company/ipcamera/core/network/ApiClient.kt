package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.interceptor.InterceptorChain
import com.company.ipcamera.core.network.interceptor.RequestInterceptor
import com.company.ipcamera.core.network.metrics.NetworkMetricsCollector
import com.company.ipcamera.core.network.metrics.RequestMetrics
import com.company.ipcamera.core.network.ratelimit.RateLimiter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val apiClientLogging = KotlinLogging.logger {}

/**
 * Максимальный размер файла для загрузки (100 MB)
 */
private const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100 MB

/**
 * Разрешенные MIME типы для загрузки файлов
 */
private val ALLOWED_MIME_TYPES = setOf(
    "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
    "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo",
    "application/pdf", "application/zip", "application/json",
    "text/plain", "text/csv"
)

/**
 * Конфигурация API клиента
 */
data class ApiClientConfig(
    val baseUrl: String,
    val connectTimeout: Duration = 30.seconds,
    val socketTimeout: Duration = 30.seconds,
    val requestTimeout: Duration = 30.seconds,
    val enableLogging: Boolean = true,
    val enableRetry: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val enableCache: Boolean = true,
    val cacheMaxSize: Int = 100,
    val cacheExpirationTime: Duration = Duration.parse("5m"),
    val headers: Map<String, String> = emptyMap(),
    val apiKey: String? = null,
    val authToken: String? = null,
    val certificatePinningConfig: com.company.ipcamera.core.network.security.CertificatePinningConfig? = null,
    val enableMetrics: Boolean = true,
    val rateLimiter: RateLimiter? = null,
    val interceptors: List<RequestInterceptor> = emptyList(),
    /**
     * При true клиент не подключается к http:// URL (настройка «только HTTPS»).
     * При передаче http:// baseUrl create() выбросит IllegalArgumentException.
     */
    val requireHttps: Boolean = false
) {
    companion object {
        fun default(baseUrl: String, requireHttps: Boolean = false) =
            ApiClientConfig(baseUrl = baseUrl, requireHttps = requireHttps)
    }
}

/**
 * Типы ошибок API
 */
sealed class ApiError : Exception() {
    data class NetworkError(override val cause: Throwable) : ApiError()
    data class HttpError(val statusCode: Int, val errorMessage: String, val body: String? = null) : ApiError()
    data class SerializationError(override val cause: Throwable) : ApiError()
    data class TimeoutError(val errorMessage: String) : ApiError()
    data class UnknownError(override val cause: Throwable) : ApiError()

    override val message: String
        get() = when (this) {
            is NetworkError -> "Network error: ${cause.message}"
            is HttpError -> "HTTP $statusCode: $errorMessage"
            is SerializationError -> "Serialization error: ${cause.message}"
            is TimeoutError -> "Timeout: $errorMessage"
            is UnknownError -> "Unknown error: ${cause.message}"
        }
}

/**
 * Результат API запроса
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val error: ApiError) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (ApiError) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(error)
    }
}

/**
 * Кэш для ответов API
 */
internal class ResponseCache(
    private val maxSize: Int = 100,
    private val expirationTime: Duration = Duration.parse("5m"),
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private data class CacheEntry(
        val data: String,
        val timestamp: Long,
        val expirationTime: Duration
    )

    private val cache = mutableMapOf<String, CacheEntry>()

    fun get(key: String): String? {
        val entry = cache[key] ?: return null

        val now = nowProvider()
        if (now - entry.timestamp > entry.expirationTime.inWholeMilliseconds) {
            cache.remove(key)
            return null
        }

        return entry.data
    }

    fun put(key: String, data: String) {
        if (cache.size >= maxSize) {
            // Удаляем самый старый элемент
            val oldestKey = cache.minByOrNull { it.value.timestamp }?.key
            oldestKey?.let { cache.remove(it) }
        }

        cache[key] = CacheEntry(
            data = data,
            timestamp = nowProvider(),
            expirationTime = expirationTime
        )
    }

    fun clear() {
        cache.clear()
    }

    fun remove(key: String) {
        cache.remove(key)
    }
}

/**
 * Базовый HTTP клиент для REST API
 *
 * Поддерживает:
 * - Конфигурацию (baseUrl, timeouts, interceptors)
 * - Сериализацию (Kotlinx Serialization)
 * - Обработку ошибок
 * - Retry логику
 * - Кэширование ответов
 */
class ApiClient private constructor(
    private val httpClient: HttpClient,
    private val config: ApiClientConfig,
    private val cache: ResponseCache? = null
) {
    /** Базовый URL API (без завершающего `/`), тот же, что в [ApiClientConfig.baseUrl]. */
    val baseUrl: String get() = config.baseUrl

    private val metricsCollector = if (config.enableMetrics) {
        NetworkMetricsCollector()
    } else {
        null
    }

    private val interceptorChain = if (config.interceptors.isNotEmpty()) {
        InterceptorChain(config.interceptors)
    } else {
        null
    }
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
        prettyPrint = false
    }

    companion object {
        /**
         * Создает новый экземпляр ApiClient
         */
        fun create(
            config: ApiClientConfig,
            httpClientEngine: HttpClientEngine? = null
        ): ApiClient {
            // При включённой настройке «только HTTPS» блокируем http:// URL
            if (config.requireHttps) {
                val base = config.baseUrl.trim().lowercase()
                if (base.startsWith("http://")) {
                    throw IllegalArgumentException(
                        "При включённой настройке «только HTTPS» подключение к API по адресу http:// запрещено. " +
                            "Укажите https:// URL или отключите настройку «только HTTPS»."
                    )
                }
            }
            // Создаем engine с certificate pinning, если настроено
            val engine = httpClientEngine ?: createEngineWithCertificatePinning(config)

            val client = HttpClient(engine) {
                // Базовая конфигурация
                install(HttpTimeout) {
                    connectTimeoutMillis = config.connectTimeout.inWholeMilliseconds
                    socketTimeoutMillis = config.socketTimeout.inWholeMilliseconds
                    requestTimeoutMillis = config.requestTimeout.inWholeMilliseconds
                }

                // Поддержка cookies (httpOnly cookies для JWT)
                install(HttpCookies) {
                    // Cookies будут автоматически сохраняться и отправляться с запросами
                    // Это позволяет работать с httpOnly cookies, установленными сервером
                }

                // Сериализация
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            encodeDefaults = false
                        }
                    )
                }

                // Логирование
                if (config.enableLogging) {
                    install(Logging) {
                        level = LogLevel.INFO
                        logger = object : Logger {
                            override fun log(message: String) {
                                apiClientLogging.info { message }
                            }
                        }
                    }
                }

                // Retry логика реализована вручную в executeRequest

                // Базовые заголовки
                defaultRequest {
                    url(config.baseUrl)
                    config.headers.forEach { (key, value) ->
                        header(key, value)
                    }
                    config.apiKey?.let {
                        header("X-API-Key", it)
                    }
                    // authToken теперь не используется, так как токены в httpOnly cookies
                    // Оставляем для обратной совместимости, но приоритет у cookies
                    config.authToken?.let {
                        header(HttpHeaders.Authorization, "Bearer $it")
                    }
                }
            }

            val responseCache = if (config.enableCache) {
                ResponseCache(
                    maxSize = config.cacheMaxSize,
                    expirationTime = config.cacheExpirationTime
                )
            } else {
                null
            }

            return ApiClient(client, config, responseCache)
        }

        /**
         * Создает HTTP engine с certificate pinning, если настроено.
         * Всегда применяет enforcePinning = true (отклонение соединения при несовпадении сертификата).
         */
        fun createEngineWithCertificatePinning(config: ApiClientConfig): HttpClientEngine {
            val pinningConfig = config.certificatePinningConfig
            return if (pinningConfig != null && pinningConfig.enablePinning) {
                createEngineWithPinning(pinningConfig.copy(enforcePinning = true))
            } else {
                createDefaultEngine()
            }
        }
    }

    /**
     * Выполняет GET запрос (с явным сериализатором; для reified используйте перегрузку с reified T).
     */
    suspend fun <T> get(
        path: String,
        queryParameters: Map<String, String> = emptyMap(),
        useCache: Boolean = true,
        serializer: KSerializer<T>
    ): ApiResult<T> = executeRequest(HttpMethod.Get, path, queryParameters, null, useCache, null, serializer)

    /**
     * Выполняет GET запрос (reified T).
     */
    suspend inline fun <reified T> get(
        path: String,
        queryParameters: Map<String, String> = emptyMap(),
        useCache: Boolean = true
    ): ApiResult<T> = get(path, queryParameters, useCache, serializer())

    /**
     * Выполняет POST запрос (с явным сериализатором тела и ответа).
     */
    suspend fun <B, T> post(
        path: String,
        body: B? = null,
        bodySerializer: KSerializer<B>? = null,
        serializer: KSerializer<T>
    ): ApiResult<T> = executeRequest(
        HttpMethod.Post,
        path,
        emptyMap(),
        body,
        true,
        bodySerializer as KSerializer<Any>?,
        serializer
    )

    /**
     * Выполняет POST запрос (reified B = тип тела, T = тип ответа).
     */
    suspend inline fun <reified B, reified T> post(path: String, body: B? = null): ApiResult<T> =
        post(path, body, if (body != null) serializer() else null, serializer())

    /**
     * Выполняет PUT запрос (с явным сериализатором тела и ответа).
     */
    suspend fun <B, T> put(
        path: String,
        body: B? = null,
        bodySerializer: KSerializer<B>? = null,
        serializer: KSerializer<T>
    ): ApiResult<T> = executeRequest(
        HttpMethod.Put,
        path,
        emptyMap(),
        body,
        true,
        bodySerializer as KSerializer<Any>?,
        serializer
    )

    /**
     * Выполняет PUT запрос (reified B = тип тела, T = тип ответа).
     */
    suspend inline fun <reified B, reified T> put(path: String, body: B? = null): ApiResult<T> =
        put(path, body, if (body != null) serializer() else null, serializer())

    /**
     * Выполняет PATCH запрос (с явным сериализатором тела и ответа).
     */
    suspend fun <B, T> patch(
        path: String,
        body: B? = null,
        bodySerializer: KSerializer<B>? = null,
        serializer: KSerializer<T>
    ): ApiResult<T> = executeRequest(
        HttpMethod.Patch,
        path,
        emptyMap(),
        body,
        true,
        bodySerializer as KSerializer<Any>?,
        serializer
    )

    /**
     * Выполняет PATCH запрос (reified B = тип тела, T = тип ответа).
     */
    suspend inline fun <reified B, reified T> patch(path: String, body: B? = null): ApiResult<T> =
        patch(path, body, if (body != null) serializer() else null, serializer())

    /**
     * Выполняет DELETE запрос (с явным сериализатором).
     */
    suspend fun <T> delete(path: String, serializer: KSerializer<T>): ApiResult<T> =
        executeRequest(HttpMethod.Delete, path, emptyMap(), null, true, null, serializer)

    /**
     * Выполняет DELETE запрос (reified T).
     */
    suspend inline fun <reified T> delete(path: String): ApiResult<T> = delete(path, serializer())

    /**
     * Выполняет запрос с загрузкой файла
     *
     * @param path Путь к endpoint для загрузки
     * @param fileData Данные файла
     * @param fileName Имя файла
     * @param contentType MIME тип файла
     * @return Результат загрузки
     */
    suspend fun <T> upload(
        path: String,
        fileData: ByteArray,
        fileName: String,
        contentType: ContentType = ContentType.Application.OctetStream,
        serializer: KSerializer<T>
    ): ApiResult<T> = uploadImpl(path, fileData, fileName, contentType, null, emptyMap(), serializer)

    suspend inline fun <reified T> upload(
        path: String,
        fileData: ByteArray,
        fileName: String,
        contentType: ContentType = ContentType.Application.OctetStream
    ): ApiResult<T> = upload(path, fileData, fileName, contentType, serializer())

    private suspend fun <T> uploadImpl(
        path: String,
        fileData: ByteArray,
        fileName: String,
        contentType: ContentType,
        multipartFieldName: String?,
        additionalFields: Map<String, String>,
        serializer: KSerializer<T>
    ): ApiResult<T> {
        return try {
            if (fileData.size > MAX_FILE_SIZE) {
                apiClientLogging.warn { "File size exceeds maximum allowed size: ${fileData.size} bytes > $MAX_FILE_SIZE bytes" }
                return ApiResult.Error(
                    ApiError.HttpError(
                        statusCode = 413,
                        errorMessage = "File size exceeds maximum allowed size of ${MAX_FILE_SIZE / (1024 * 1024)}MB"
                    )
                )
            }

            // Валидация MIME типа
            val mimeType = contentType.toString()
            if (!ALLOWED_MIME_TYPES.contains(mimeType) && mimeType != ContentType.Application.OctetStream.toString()) {
                apiClientLogging.warn { "File MIME type not allowed: $mimeType" }
                return ApiResult.Error(
                    ApiError.HttpError(
                        statusCode = 415,
                        errorMessage = "File type not allowed: $mimeType"
                    )
                )
            }

            // Валидация расширения файла
            val fileExtension = fileName.substringAfterLast('.', "").lowercase()
            val allowedExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "mp4", "mpeg", "mov", "avi", "pdf", "zip", "json", "txt", "csv")
            if (fileExtension.isNotEmpty() && !allowedExtensions.contains(fileExtension)) {
                apiClientLogging.warn { "File extension not allowed: $fileExtension" }
                return ApiResult.Error(
                    ApiError.HttpError(
                        statusCode = 415,
                        errorMessage = "File extension not allowed: .$fileExtension"
                    )
                )
            }

            val response = if (multipartFieldName != null) {
                httpClient.post(path) {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    multipartFieldName,
                                    fileData,
                                    Headers.build {
                                        append(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                    }
                                )
                                additionalFields.forEach { (key, value) -> append(key, value) }
                            }
                        )
                    )
                }
            } else {
                httpClient.post(path) {
                    setBody(fileData)
                    header(HttpHeaders.ContentType, contentType)
                    header("Content-Disposition", "attachment; filename=\"$fileName\"")
                    header("Content-Length", fileData.size.toString())
                }
            }
            handleResponse(response, serializer)
        } catch (e: Exception) {
            ApiResult.Error(handleException(e))
        }
    }

    /**
     * Выполняет запрос с загрузкой файла (multipart)
     *
     * @param path Путь к endpoint для загрузки
     * @param fileData Данные файла
     * @param fileName Имя файла
     * @param fieldName Имя поля формы
     * @param additionalFields Дополнительные поля формы
     * @return Результат загрузки
     */
    suspend fun <T> uploadMultipart(
        path: String,
        fileData: ByteArray,
        fileName: String,
        fieldName: String = "file",
        additionalFields: Map<String, String> = emptyMap(),
        serializer: KSerializer<T>
    ): ApiResult<T> = uploadImpl(
        path,
        fileData,
        fileName,
        ContentType.Application.OctetStream,
        fieldName,
        additionalFields,
        serializer
    )

    suspend inline fun <reified T> uploadMultipart(
        path: String,
        fileData: ByteArray,
        fileName: String,
        fieldName: String = "file",
        additionalFields: Map<String, String> = emptyMap()
    ): ApiResult<T> = uploadMultipart(path, fileData, fileName, fieldName, additionalFields, serializer())

    /**
     * Получить сборщик метрик
     */
    fun getMetricsCollector(): NetworkMetricsCollector? = metricsCollector

    /**
     * Выполняет базовый HTTP запрос с поддержкой retry
     */
    private suspend fun <T> executeRequest(
        method: HttpMethod,
        path: String,
        queryParameters: Map<String, String> = emptyMap(),
        body: Any? = null,
        useCache: Boolean = true,
        bodySerializer: KSerializer<Any>? = null,
        serializer: KSerializer<T>
    ): ApiResult<T> {
        // Rate limiting
        config.rateLimiter?.let { limiter ->
            if (!limiter.acquire()) {
                val waitTime = limiter.getWaitTime()
                if (waitTime > Duration.ZERO) {
                    delay(waitTime)
                    // Повторная попытка
                    if (!limiter.acquire()) {
                        return ApiResult.Error(
                            ApiError.TimeoutError("Rate limit exceeded. Please wait before retrying.")
                        )
                    }
                } else {
                    return ApiResult.Error(
                        ApiError.TimeoutError("Rate limit exceeded. Please wait before retrying.")
                    )
                }
            }
        }
        // Проверяем кэш для GET запросов
        if (method == HttpMethod.Get && useCache && cache != null) {
            val cacheKey = buildCacheKey(path, queryParameters)
            val cachedData = cache.get(cacheKey)
            if (cachedData != null) {
                try {
                    val deserialized = json.decodeFromString(serializer, cachedData)
                    apiClientLogging.debug { "Cache hit for $path" }
                    return ApiResult.Success(deserialized)
                } catch (e: Exception) {
                    apiClientLogging.warn(e) { "Failed to deserialize cached data" }
                    cache.remove(cacheKey)
                }
            }
        }

        // Retry логика
        var lastError: ApiError? = null
        var delay = config.retryDelay

        repeat(if (config.enableRetry) config.maxRetries + 1 else 1) { attempt ->
            if (attempt > 0) {
                apiClientLogging.debug { "Retry attempt $attempt for $path" }
                kotlinx.coroutines.delay(delay)
                // Экспоненциальная задержка (максимум 10 секунд)
                val newDelayMs = (delay.inWholeMilliseconds * 2).coerceAtMost(10000)
                delay = newDelayMs.toLong().milliseconds
            }

            val result = try {
                val startTime = TimeSource.Monotonic.markNow()
                var requestSize = 0L
                var responseSize = 0L

                // Создаем HttpRequestBuilder для интерцепторов
                // Сохраняем builder для использования в интерцепторах после ответа
                var savedRequestBuilder: HttpRequestBuilder? = null

                val response = httpClient.request(path) {
                    savedRequestBuilder = this
                    this.method = method

                    // Query параметры
                    queryParameters.forEach { (key, value) ->
                        parameter(key, value)
                    }

                    // Тело запроса
                    if (body != null && bodySerializer != null) {
                        contentType(ContentType.Application.Json)
                        val bodyBytes = json.encodeToString(bodySerializer, body).encodeToByteArray()
                        requestSize = bodyBytes.size.toLong()
                        setBody(bodyBytes)
                    }

                    // Вызов интерцепторов перед запросом
                    interceptorChain?.onRequest(this)
                }

                responseSize = response.headers["Content-Length"]?.toLongOrNull() ?: 0L
                val duration = startTime.elapsedNow()

                // Вызов интерцепторов после ответа
                // Используем сохраненный builder или создаем новый
                val requestBuilderForInterceptor = savedRequestBuilder ?: run {
                    HttpRequestBuilder().apply {
                        this.method = method
                        url.takeFrom(config.baseUrl + path)
                        queryParameters.forEach { (key, value) ->
                            parameter(key, value)
                        }
                    }
                }
                val finalResponse = interceptorChain?.onResponse(
                    requestBuilderForInterceptor,
                    response
                ) ?: response

                // Запись метрик
                metricsCollector?.recordMetric(
                    RequestMetrics(
                        url = path,
                        method = method.value,
                        statusCode = finalResponse.status.value,
                        duration = duration,
                        requestSize = requestSize,
                        responseSize = responseSize,
                        success = finalResponse.status.isSuccess()
                    )
                )

                handleResponse(finalResponse, serializer)
            } catch (e: Exception) {
                val error = handleException(e)
                lastError = error

                // Вызов интерцепторов при ошибке
                try {
                    // Создаем HttpRequestBuilder для интерцептора ошибки
                    val requestBuilder = HttpRequestBuilder().apply {
                        this.method = method
                        url.takeFrom(config.baseUrl + path)
                        queryParameters.forEach { (key, value) ->
                            parameter(key, value)
                        }
                    }
                    interceptorChain?.onError(
                        requestBuilder,
                        e
                    )
                } catch (interceptorError: Exception) {
                    apiClientLogging.warn(interceptorError) { "Error in interceptor onError" }
                }

                // Запись метрик для ошибки
                metricsCollector?.recordMetric(
                    RequestMetrics(
                        url = path,
                        method = method.value,
                        statusCode = (error as? ApiError.HttpError)?.statusCode,
                        duration = Duration.ZERO, // Не измеряем время при ошибке
                        requestSize = 0,
                        responseSize = 0,
                        success = false
                    )
                )

                // Проверяем, нужно ли повторять попытку
                val shouldRetry = config.enableRetry &&
                    attempt < config.maxRetries &&
                    (
                        error is ApiError.NetworkError ||
                            error is ApiError.TimeoutError ||
                            (error is ApiError.HttpError && error.statusCode in 500..599)
                        )

                if (!shouldRetry) {
                    return ApiResult.Error(error)
                }

                null // Продолжаем retry
            }

            // Если получили результат, возвращаем его
            result?.let {
                // Сохраняем в кэш для успешных GET запросов
                if (it is ApiResult.Success && method == HttpMethod.Get && useCache && cache != null) {
                    try {
                        val serialized = json.encodeToString(serializer, it.data)
                        val cacheKey = buildCacheKey(path, queryParameters)
                        cache.put(cacheKey, serialized)
                    } catch (e: Exception) {
                        apiClientLogging.warn(e) { "Failed to cache response" }
                    }
                }
                return it
            }
        }

        // Если все попытки исчерпаны, возвращаем последнюю ошибку
        return ApiResult.Error(lastError ?: ApiError.UnknownError(Exception("Unknown error")))
    }

    /**
     * Обрабатывает HTTP ответ
     */
    private suspend fun <T> handleResponse(
        response: io.ktor.client.statement.HttpResponse,
        serializer: KSerializer<T>
    ): ApiResult<T> {
        return try {
            when {
                response.status.isSuccess() -> {
                    val rawBody = try {
                        response.body<String>()
                    } catch (e: Exception) {
                        null
                    }
                    // 204 No Content / пустое тело: для String отдаём пустую строку как успех.
                    if (rawBody.isNullOrEmpty() && serializer == serializer<String>()) {
                        @Suppress("UNCHECKED_CAST")
                        return ApiResult.Success("" as T)
                    }
                    val body = rawBody ?: ""
                    try {
                        val deserialized = json.decodeFromString(serializer, body)
                        ApiResult.Success(deserialized)
                    } catch (e: SerializationException) {
                        ApiResult.Error(ApiError.SerializationError(e))
                    } catch (e: Exception) {
                        ApiResult.Error(ApiError.SerializationError(e))
                    }
                }
                else -> {
                    val errorBody = try {
                        response.body<String>()
                    } catch (e: Exception) {
                        null
                    }
                    ApiResult.Error(
                        ApiError.HttpError(
                            statusCode = response.status.value,
                            errorMessage = response.status.description,
                            body = errorBody
                        )
                    )
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(handleException(e))
        }
    }

    /**
     * Обрабатывает исключения
     */
    private fun handleException(e: Throwable): ApiError {
        return when {
            e is CancellationException -> {
                throw e // Пробрасываем отмену корутин
            }
            e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("Timeout", ignoreCase = true) == true -> {
                ApiError.TimeoutError(e.message ?: "Request timeout")
            }
            e.message?.contains("UnknownHost", ignoreCase = true) == true ||
                e.message?.contains("Connect", ignoreCase = true) == true ||
                e.message?.contains("Network", ignoreCase = true) == true -> {
                ApiError.NetworkError(e)
            }
            e is SerializationException -> {
                ApiError.SerializationError(e)
            }
            else -> {
                ApiError.UnknownError(e)
            }
        }
    }

    /**
     * Создает ключ для кэша.
     * Включает контекст авторизации (authToken/apiKey), чтобы ответы разных пользователей не смешивались.
     */
    private fun buildCacheKey(path: String, queryParameters: Map<String, String>): String {
        val authPrefix = when {
            config.authToken != null -> "auth:${config.authToken.hashCode()}:"
            config.apiKey != null -> "key:${config.apiKey.hashCode()}:"
            else -> ""
        }
        val queryString = queryParameters.entries
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        val pathAndQuery = if (queryString.isNotEmpty()) {
            "$path?$queryString"
        } else {
            path
        }
        return authPrefix + pathAndQuery
    }

    /**
     * Обновляет токен авторизации
     * Примечание: для обновления токена нужно пересоздать клиент с новой конфигурацией
     *
     * ВАЖНО: При использовании httpOnly cookies токены управляются сервером автоматически.
     * Этот метод оставлен для обратной совместимости, но токены теперь хранятся в cookies.
     */
    fun updateAuthToken(token: String): ApiClient {
        val newConfig = config.copy(authToken = token)
        return create(newConfig, httpClient.engine)
    }

    /**
     * Очищает кэш
     */
    fun clearCache() {
        cache?.clear()
    }

    /**
     * Закрывает клиент и освобождает ресурсы
     */
    fun close() {
        httpClient.close()
        cache?.clear()
    }

    /**
     * Куки Ktor для полного URL (включая httpOnly), для синхронизации с OkHttp (ExoPlayer HLS).
     */
    suspend fun cookiesForHttpUrl(urlString: String): List<Cookie> =
        httpClient.cookies(Url(urlString))
}

/**
 * Создает движок HTTP клиента по умолчанию
 * Должен быть переопределен в платформо-специфичных реализациях
 */
expect fun ApiClient.Companion.createDefaultEngine(): HttpClientEngine

/**
 * Создает HTTP engine с certificate pinning (платформо-специфичная реализация)
 */
expect fun ApiClient.Companion.createEngineWithPinning(
    config: com.company.ipcamera.core.network.security.CertificatePinningConfig
): HttpClientEngine
