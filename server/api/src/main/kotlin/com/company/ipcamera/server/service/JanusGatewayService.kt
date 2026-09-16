package com.company.ipcamera.server.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Сервис для интеграции с Janus Gateway Media Server
 *
 * Janus Gateway - легковесный WebRTC медиа-сервер с поддержкой RTSP-to-WebRTC
 *
 * Документация: https://janus.conf.meetecho.com/docs/
 */
class JanusGatewayService(
    private val janusUrl: String = System.getenv("JANUS_URL") ?: "http://localhost:8088/janus",
    private val enabled: Boolean = System.getenv("JANUS_ENABLED")?.toBoolean() ?: false
) : WebRtcMediaGateway {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
    }

    private val activeSessions = ConcurrentHashMap<String, JanusSession>()
    private val activeHandles = ConcurrentHashMap<String, JanusHandle>()
    /** Кэш streamId по RTSP URL — чтобы не создавать дубликаты потоков в Janus. */
    private val streamIdsByUrl = ConcurrentHashMap<String, Long>()

    /**
     * Janus сессия
     */
    data class JanusSession(
        val sessionId: Long,
        val createdAt: Long = System.currentTimeMillis()
    )

    /**
     * Janus handle (присоединение к плагину)
     */
    data class JanusHandle(
        val handleId: Long,
        val sessionId: Long,
        val cameraId: String,
        val createdAt: Long = System.currentTimeMillis()
    )

    /**
     * Проверить, доступен ли Janus Gateway
     */
    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (!enabled) {
            logger.debug { "Janus Gateway is disabled" }
            return@withContext false
        }

        return@withContext try {
            val response = httpClient.get("$janusUrl/info")
            response.status.isSuccess()
        } catch (e: Exception) {
            logger.warn(e) { "Janus Gateway is not available at $janusUrl" }
            false
        }
    }

    /**
     * Создать сессию в Janus Gateway
     */
    suspend fun createSession(): Result<Long> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "create",
                transaction = generateTransactionId()
            )

            val response = httpClient.post("$janusUrl") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val result = response.body<JsonObject>()
            val sessionId = result["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

            if (sessionId != null) {
                activeSessions[sessionId.toString()] = JanusSession(sessionId)
                logger.info { "Created Janus session: $sessionId" }
                Result.success(sessionId)
            } else {
                Result.failure(Exception("Failed to create Janus session: ${result["error"]}"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating Janus session" }
            Result.failure(e)
        }
    }

    /**
     * Присоединиться к плагину RTSP-to-WebRTC
     */
    suspend fun attachToRtspPlugin(sessionId: Long, cameraId: String): Result<Long> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "attach",
                plugin = "janus.plugin.streaming",
                transaction = generateTransactionId()
            )

            val response = httpClient.post("$janusUrl/$sessionId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val result = response.body<JsonObject>()
            val handleId = result["data"]?.jsonObject?.get("id")?.jsonPrimitive?.long

            if (handleId != null) {
                activeHandles[handleId.toString()] = JanusHandle(handleId, sessionId, cameraId)
                logger.info { "Attached to RTSP plugin: handle=$handleId, session=$sessionId" }
                Result.success(handleId)
            } else {
                Result.failure(Exception("Failed to attach to RTSP plugin: ${result["error"]}"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error attaching to RTSP plugin" }
            Result.failure(e)
        }
    }

    /**
     * Создать RTSP поток в Janus
     */
    suspend fun createRtspStream(
        sessionId: Long,
        handleId: Long,
        rtspUrl: String,
        video: Boolean = true,
        audio: Boolean = true
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val streamConfig = buildJsonObject {
                put("type", "rtsp")
                put("url", rtspUrl)
                put("video", video)
                put("audio", audio)
            }

            val request = JanusRequest(
                janus = "message",
                transaction = generateTransactionId(),
                body = buildJsonObject {
                    put("request", "create")
                    put("type", "rtsp")
                    put("url", rtspUrl)
                    put("video", video)
                    put("audio", audio)
                }
            )

            val response = httpClient.post("$janusUrl/$sessionId/$handleId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val result = response.body<JsonObject>()
            val streamId = result["plugindata"]?.jsonObject
                ?.get("data")?.jsonObject
                ?.get("streaming_id")?.jsonPrimitive?.long

            if (streamId != null) {
                logger.info { "Created RTSP stream in Janus: streamId=$streamId, rtspUrl=$rtspUrl" }
                Result.success(streamId)
            } else {
                Result.failure(Exception("Failed to create RTSP stream: ${result["error"]}"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating RTSP stream in Janus" }
            Result.failure(e)
        }
    }

    /**
     * Начать воспроизведение RTSP потока
     */
    override suspend fun startStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "message",
                transaction = generateTransactionId(),
                body = buildJsonObject {
                    put("request", "watch")
                    put("id", streamId)
                }
            )

            val response = httpClient.post("$janusUrl/$sessionId/$handleId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val result = response.body<JsonObject>()
            val status = result["plugindata"]?.jsonObject
                ?.get("data")?.jsonObject
                ?.get("status")?.jsonPrimitive?.content

            if (status == "starting" || status == "started") {
                logger.info { "Started RTSP stream in Janus: streamId=$streamId" }
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to start stream: $status"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error starting RTSP stream in Janus" }
            Result.failure(e)
        }
    }

    /**
     * Обработать WebRTC offer и создать answer через Janus
     */
    override suspend fun handleOffer(
        sessionId: Long,
        handleId: Long,
        offer: String
    ): Result<JanusWebRtcAnswer> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "message",
                transaction = generateTransactionId(),
                jsep = buildJsonObject {
                    put("type", "offer")
                    put("sdp", offer)
                },
                body = buildJsonObject {
                    put("request", "start")
                }
            )

            val response = httpClient.post("$janusUrl/$sessionId/$handleId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val result = response.body<JsonObject>()
            val jsep = result["jsep"]?.jsonObject

            if (jsep != null) {
                val answerType = jsep["type"]?.jsonPrimitive?.content
                val answerSdp = jsep["sdp"]?.jsonPrimitive?.content

                if (answerType == "answer" && answerSdp != null) {
                    logger.info { "Created WebRTC answer via Janus for session=$sessionId, handle=$handleId" }
                    Result.success(
                        JanusWebRtcAnswer(
                            answer = answerSdp,
                            iceCandidates = emptyList() // Janus возвращает ICE candidates отдельно
                        )
                    )
                } else {
                    Result.failure(Exception("Invalid answer from Janus: type=$answerType"))
                }
            } else {
                Result.failure(Exception("No answer from Janus: ${result["error"]}"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error handling WebRTC offer via Janus" }
            Result.failure(e)
        }
    }

    /**
     * Остановить RTSP поток
     */
    override suspend fun stopStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "message",
                transaction = generateTransactionId(),
                body = buildJsonObject {
                    put("request", "stop")
                }
            )

            httpClient.post("$janusUrl/$sessionId/$handleId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            logger.info { "Stopped RTSP stream in Janus: streamId=$streamId" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error stopping RTSP stream in Janus" }
            Result.failure(e)
        }
    }

    /**
     * Удалить RTSP поток
     */
    override suspend fun destroyStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "message",
                transaction = generateTransactionId(),
                body = buildJsonObject {
                    put("request", "destroy")
                    put("id", streamId)
                }
            )

            httpClient.post("$janusUrl/$sessionId/$handleId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            logger.info { "Destroyed RTSP stream in Janus: streamId=$streamId" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error destroying RTSP stream in Janus" }
            Result.failure(e)
        }
    }

    /**
     * Отсоединиться от плагина
     */
    override suspend fun detachHandle(sessionId: Long, handleId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "detach",
                transaction = generateTransactionId()
            )

            httpClient.post("$janusUrl/$sessionId/$handleId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            activeHandles.remove(handleId.toString())
            logger.info { "Detached handle from Janus: handle=$handleId" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error detaching handle from Janus" }
            Result.failure(e)
        }
    }

    /**
     * Уничтожить сессию
     */
    suspend fun destroySession(sessionId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext Result.failure(Exception("Janus Gateway is disabled"))
        }

        return@withContext try {
            val request = JanusRequest(
                janus = "destroy",
                transaction = generateTransactionId()
            )

            httpClient.post("$janusUrl/$sessionId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            activeSessions.remove(sessionId.toString())
            logger.info { "Destroyed Janus session: $sessionId" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error destroying Janus session" }
            Result.failure(e)
        }
    }

    /**
     * Получить или создать сессию для камеры
     */
    override suspend fun getOrCreateSession(cameraId: String): Result<Long> = withContext(Dispatchers.IO) {
        // Ищем существующую сессию для камеры
        val existingHandle = activeHandles.values.find { it.cameraId == cameraId }
        if (existingHandle != null) {
            return@withContext Result.success(existingHandle.sessionId)
        }

        // Создаем новую сессию
        return@withContext createSession()
    }

    /**
     * Получить или создать handle для камеры
     */
    override suspend fun getOrCreateHandle(cameraId: String, sessionId: Long): Result<Long> = withContext(Dispatchers.IO) {
        // Ищем существующий handle для камеры
        val existingHandle = activeHandles.values.find {
            it.cameraId == cameraId && it.sessionId == sessionId
        }
        if (existingHandle != null) {
            return@withContext Result.success(existingHandle.handleId)
        }

        // Создаем новый handle
        return@withContext attachToRtspPlugin(sessionId, cameraId)
    }

    /**
     * Найти или создать RTSP-поток для камеры по URL ([WebRtcMediaGateway]).
     * Повторный вызов с тем же URL возвращает кэшированный streamId без нового HTTP-запроса.
     */
    override suspend fun getOrCreateRtspStream(
        sessionId: Long,
        handleId: Long,
        rtspUrl: String
    ): Result<Long> {
        streamIdsByUrl[rtspUrl]?.let { return Result.success(it) }
        return createRtspStream(sessionId, handleId, rtspUrl, video = true, audio = true)
            .onSuccess { streamIdsByUrl[rtspUrl] = it }
    }

    /**
     * Генерация уникального transaction ID для Janus
     */
    private fun generateTransactionId(): String {
        return java.util.UUID.randomUUID().toString().replace("-", "").take(12)
    }

    /**
     * Очистить все соединения
     */
    fun cleanup() {
        activeSessions.clear()
        activeHandles.clear()
        streamIdsByUrl.clear()
    }
}

/**
 * Janus Request формат
 */
@Serializable
data class JanusRequest(
    val janus: String,
    val transaction: String,
    val plugin: String? = null,
    val body: JsonObject? = null,
    val jsep: JsonObject? = null
)

/**
 * WebRTC Answer от Janus
 */
data class JanusWebRtcAnswer(
    val answer: String,
    val iceCandidates: List<com.company.ipcamera.server.service.RTCIceCandidate>
)

/**
 * Абстракция WebRTC медиа-шлюза (Janus Gateway или другой медиа-сервер).
 *
 * Позволяет [WebRtcService] работать без привязки к конкретной реализации:
 * в продакшене — [JanusGatewayService], в тестах — фейк/мок.
 */
interface WebRtcMediaGateway {
    /** Доступен ли шлюз (конфигурация + сетевая проверка). */
    suspend fun isAvailable(): Boolean

    /** Найти или создать сессию шлюза для камеры. */
    suspend fun getOrCreateSession(cameraId: String): Result<Long>

    /** Найти или создать handle (присоединение к плагину) для камеры в сессии. */
    suspend fun getOrCreateHandle(cameraId: String, sessionId: Long): Result<Long>

    /** Найти или создать RTSP-поток шлюза для камеры по URL. */
    suspend fun getOrCreateRtspStream(sessionId: Long, handleId: Long, rtspUrl: String): Result<Long>

    /** Запустить воспроизведение потока. */
    suspend fun startStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit>

    /** Обработать SDP offer клиента и получить answer шлюза. */
    suspend fun handleOffer(sessionId: Long, handleId: Long, offer: String): Result<JanusWebRtcAnswer>

    /** Остановить поток (без удаления). */
    suspend fun stopStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit>

    /** Удалить поток. */
    suspend fun destroyStream(sessionId: Long, handleId: Long, streamId: Long): Result<Unit>

    /** Отсоединить handle от плагина. */
    suspend fun detachHandle(sessionId: Long, handleId: Long): Result<Unit>
}
