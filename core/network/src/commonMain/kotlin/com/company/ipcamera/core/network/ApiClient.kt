package com.company.ipcamera.core.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

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
    val authToken: String? = null
) {
    companion object {
        fun default(baseUrl: String) = ApiClientConfig(baseUrl = baseUrl)
    }
}

/**
 * Типы ошибок API
 */
sealed class ApiError : Exception() {
    data class NetworkError(val cause: Throwable) : ApiError()
    data class HttpError(val statusCode: Int, val message: String, val body: String? = null) : ApiError()
    data class SerializationError(val cause: Throwable) : ApiError()
    data class TimeoutError(val message: String) : ApiError()
    data class UnknownError(val cause: Throwable) : ApiError()
    
    override val message: String
        get() = when (this) {
            is NetworkError -> "Network error: ${cause.message}"
            is HttpError -> "HTTP $statusCode: $message"
            is SerializationError -> "Serialization error: ${cause.message}"
            is TimeoutError -> "Timeout: $message"
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
    private val expirationTime: Duration = Duration.parse("5m")
) {
    private data class CacheEntry(
        val data: String,
        val timestamp: Long,
        val expirationTime: Duration
    )
    
    private val cache = mutableMapOf<String, CacheEntry>()
    
    fun get(key: String): String? {
        val entry = cache[key] ?: return null
        
        val now = System.currentTimeMillis()
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
            timestamp = System.currentTimeMillis(),
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
            val client = HttpClient(httpClientEngine ?: createDefaultEngine()) {
                // Базовая конфигурация
                install(HttpTimeout) {
                    connectTimeoutMillis = config.connectTimeout.inWholeMilliseconds.toInt()
                    socketTimeoutMillis = config.socketTimeout.inWholeMilliseconds.toInt()
                    requestTimeoutMillis = config.requestTimeout.inWholeMilliseconds.toInt()
                }
                
                // Сериализация
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = false
                    })
                }
                
                // Логирование
                if (config.enableLogging) {
                    install(Logging) {
                        level = LogLevel.INFO
                        logger = object : Logger {
                            override fun log(message: String) {
                                logger.info { message }
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
            } else null
            
            return ApiClient(client, config, responseCache)
        }
        
        /**
         * Создает движок HTTP клиента по умолчанию
         * Должен быть переопределен в платформо-специфичных реализациях
         */
        expect fun createDefaultEngine(): HttpClientEngine
    }
    
    /**
     * Выполняет GET запрос
     */
    suspend inline fun <reified T> get(
        path: String,
        queryParameters: Map<String, String> = emptyMap(),
        useCache: Boolean = true
    ): ApiResult<T> {
        return executeRequest(
            method = HttpMethod.Get,
            path = path,
            queryParameters = queryParameters,
            useCache = useCache
        )
    }
    
    /**
     * Выполняет POST запрос
     */
    suspend inline fun <reified T> post(
        path: String,
        body: Any? = null
    ): ApiResult<T> {
        return executeRequest(
            method = HttpMethod.Post,
            path = path,
            body = body
        )
    }
    
    /**
     * Выполняет PUT запрос
     */
    suspend inline fun <reified T> put(
        path: String,
        body: Any? = null
    ): ApiResult<T> {
        return executeRequest(
            method = HttpMethod.Put,
            path = path,
            body = body
        )
    }
    
    /**
     * Выполняет PATCH запрос
     */
    suspend inline fun <reified T> patch(
        path: String,
        body: Any? = null
    ): ApiResult<T> {
        return executeRequest(
            method = HttpMethod.Patch,
            path = path,
            body = body
        )
    }
    
    /**
     * Выполняет DELETE запрос
     */
    suspend inline fun <reified T> delete(
        path: String
    ): ApiResult<T> {
        return executeRequest(
            method = HttpMethod.Delete,
            path = path
        )
    }
    
    /**
     * Выполняет запрос с загрузкой файла
     */
    suspend inline fun <reified T> upload(
        path: String,
        fileData: ByteArray,
        fileName: String,
        contentType: ContentType = ContentType.Application.OctetStream
    ): ApiResult<T> {
        return try {
            val response = httpClient.post(path) {
                setBody(fileData)
                header(HttpHeaders.ContentType, contentType)
                header("Content-Disposition", "attachment; filename=\"$fileName\"")
            }
            
            handleResponse<T>(response)
        } catch (e: Exception) {
            ApiResult.Error(handleException(e))
        }
    }
    
    /**
     * Выполняет запрос с загрузкой файла (multipart)
     */
    suspend inline fun <reified T> uploadMultipart(
        path: String,
        fileData: ByteArray,
        fileName: String,
        fieldName: String = "file",
        additionalFields: Map<String, String> = emptyMap()
    ): ApiResult<T> {
        return try {
            val response = httpClient.post(path) {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                fieldName,
                                fileData,
                                Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                                    append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                }
                            )
                            additionalFields.forEach { (key, value) ->
                                append(key, value)
                            }
                        }
                    )
                )
            }
            
            handleResponse<T>(response)
        } catch (e: Exception) {
            ApiResult.Error(handleException(e))
        }
    }
    
    /**
     * Выполняет базовый HTTP запрос с поддержкой retry
     */
    private suspend inline fun <reified T> executeRequest(
        method: HttpMethod,
        path: String,
        queryParameters: Map<String, String> = emptyMap(),
        body: Any? = null,
        useCache: Boolean = true
    ): ApiResult<T> {
        // Проверяем кэш для GET запросов
        if (method == HttpMethod.Get && useCache && cache != null) {
            val cacheKey = buildCacheKey(path, queryParameters)
            val cachedData = cache.get(cacheKey)
            if (cachedData != null) {
                try {
                    val deserialized = json.decodeFromString<T>(cachedData)
                    logger.debug { "Cache hit for $path" }
                    return ApiResult.Success(deserialized)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to deserialize cached data" }
                    cache.remove(cacheKey)
                }
            }
        }
        
        // Retry логика
        var lastError: ApiError? = null
        var delay = config.retryDelay
        
        repeat(if (config.enableRetry) config.maxRetries + 1 else 1) { attempt ->
            if (attempt > 0) {
                logger.debug { "Retry attempt $attempt for $path" }
                kotlinx.coroutines.delay(delay)
                // Экспоненциальная задержка (максимум 10 секунд)
                val newDelayMs = (delay.inWholeMilliseconds * 2).coerceAtMost(10000)
                delay = newDelayMs.toLong().milliseconds
            }
            
            val result = try {
                val response = httpClient.request(path) {
                    this.method = method
                    
                    // Query параметры
                    queryParameters.forEach { (key, value) ->
                        parameter(key, value)
                    }
                    
                    // Тело запроса
                    if (body != null) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
                
                handleResponse<T>(response)
            } catch (e: Exception) {
                val error = handleException(e)
                lastError = error
                
                // Проверяем, нужно ли повторять попытку
                val shouldRetry = config.enableRetry && 
                    attempt < config.maxRetries && 
                    (error is ApiError.NetworkError || 
                     error is ApiError.TimeoutError ||
                     (error is ApiError.HttpError && error.statusCode in 500..599))
                
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
                        val serialized = json.encodeToString(it.data)
                        val cacheKey = buildCacheKey(path, queryParameters)
                        cache.put(cacheKey, serialized)
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to cache response" }
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
    private suspend inline fun <reified T> handleResponse(
        response: io.ktor.client.statement.HttpResponse
    ): ApiResult<T> {
        return try {
            when {
                response.status.isSuccess() -> {
                    val body = response.body<String>()
                    try {
                        val deserialized = json.decodeFromString<T>(body)
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
                            message = response.status.description,
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
     * Создает ключ для кэша
     */
    private fun buildCacheKey(path: String, queryParameters: Map<String, String>): String {
        val queryString = queryParameters.entries
            .sortedBy { it.key }
            .joinToString("&") { "${it.key}=${it.value}" }
        return if (queryString.isNotEmpty()) {
            "$path?$queryString"
        } else {
            path
        }
    }
    
    /**
     * Обновляет токен авторизации
     * Примечание: для обновления токена нужно пересоздать клиент с новой конфигурацией
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
}

