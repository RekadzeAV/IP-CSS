package com.company.ipcamera.server.service

import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Сервис для работы с WebRTC соединениями
 *
 * Интегрирован с Janus Gateway Media Server для обработки RTSP-to-WebRTC потоков.
 */
open class WebRtcService(
    private val videoStreamService: VideoStreamService,
    private val janusGatewayService: WebRtcMediaGateway? = null
) {
    /**
     * Резолвер RTSP URL камеры. Вынесен для тестируемости:
     * [VideoStreamService.getRtspUrl] в тестах всегда возвращает null.
     * Подклассы могут переопределить.
     */
    protected open fun resolveRtspUrl(cameraId: String): String? =
        videoStreamService.getRtspUrl(cameraId)
    private val activeConnections = ConcurrentHashMap<String, WebRtcConnection>()
    private val cameraSessions = ConcurrentHashMap<String, CameraWebRtcSession>() // cameraId -> session info

    /**
     * WebRTC соединение
     */
    data class WebRtcConnection(
        val cameraId: String,
        val connectionId: String,
        val janusSessionId: Long? = null,
        val janusHandleId: Long? = null,
        val janusStreamId: Long? = null,
        val createdAt: Long = System.currentTimeMillis()
    )

    /**
     * WebRTC сессия для камеры
     */
    data class CameraWebRtcSession(
        val cameraId: String,
        val janusSessionId: Long,
        val janusHandleId: Long,
        val janusStreamId: Long,
        val rtspUrl: String
    )

    /**
     * Обработать WebRTC offer от клиента через Janus Gateway
     *
     * @param cameraId ID камеры
     * @param offer SDP offer от клиента
     * @return SDP answer и ICE candidates
     */
    suspend fun handleOffer(
        cameraId: String,
        offer: String
    ): Result<WebRtcAnswer> {
        return try {
            // Проверяем доступность Janus Gateway
            if (janusGatewayService == null || !janusGatewayService.isAvailable()) {
                logger.warn { "Janus Gateway is not available, falling back to placeholder" }
                return createPlaceholderAnswer(offer)
            }

            // Получаем RTSP URL для камеры
            val rtspUrl = resolveRtspUrl(cameraId)
                ?: return Result.failure(Exception("RTSP URL not available for camera: $cameraId"))

            // Получаем или создаем Janus сессию для камеры
            val sessionId = janusGatewayService.getOrCreateSession(cameraId).getOrElse {
                return Result.failure(Exception("Failed to create Janus session: ${it.message}"))
            }

            // Получаем или создаем handle для камеры
            val handleId = janusGatewayService.getOrCreateHandle(cameraId, sessionId).getOrElse {
                return Result.failure(Exception("Failed to create Janus handle: ${it.message}"))
            }

            // Проверяем, есть ли уже активный поток для этой камеры
            val existingSession = cameraSessions[cameraId]
            val streamId = if (existingSession != null && existingSession.janusSessionId == sessionId) {
                existingSession.janusStreamId
            } else {
                // Создаем RTSP поток в Janus (или берём кэшированный по URL)
                val createStreamResult = janusGatewayService.getOrCreateRtspStream(
                    sessionId = sessionId,
                    handleId = handleId,
                    rtspUrl = rtspUrl
                )

                createStreamResult.getOrElse {
                    return Result.failure(Exception("Failed to create RTSP stream in Janus: ${it.message}"))
                }
            }

            // Запускаем поток если еще не запущен
            if (existingSession == null) {
                val startResult = janusGatewayService.startStream(sessionId, handleId, streamId)
                startResult.getOrElse {
                    return Result.failure(Exception("Failed to start stream in Janus: ${it.message}"))
                }

                // Сохраняем информацию о сессии
                cameraSessions[cameraId] = CameraWebRtcSession(
                    cameraId = cameraId,
                    janusSessionId = sessionId,
                    janusHandleId = handleId,
                    janusStreamId = streamId,
                    rtspUrl = rtspUrl
                )
            }

            // Обрабатываем WebRTC offer через Janus
            val janusAnswer = janusGatewayService.handleOffer(sessionId, handleId, offer)
                .getOrElse {
                    return Result.failure(Exception("Failed to handle offer via Janus: ${it.message}"))
                }

            val connectionId = java.util.UUID.randomUUID().toString()
            activeConnections[connectionId] = WebRtcConnection(
                cameraId = cameraId,
                connectionId = connectionId,
                janusSessionId = sessionId,
                janusHandleId = handleId,
                janusStreamId = streamId
            )

            logger.info { "WebRTC offer processed via Janus for camera: $cameraId, session=$sessionId, handle=$handleId" }

            Result.success(
                WebRtcAnswer(
                    answer = janusAnswer.answer,
                    iceCandidates = janusAnswer.iceCandidates
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error handling WebRTC offer for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Создать fallback answer (без медиа-сервера): динамический SDP из offer клиента
     * через [OfferSdpAnswerFactory] — валидный setattrDescription-совместимый answer
     * с кодеками/ICE/DTLS-параметрами offer (без реальной медиа-ноги).
     */
    private suspend fun createPlaceholderAnswer(offer: String): Result<WebRtcAnswer> {
        logger.warn { "Using fallback WebRTC answer (media gateway not available); SDP built from client offer" }
        val answer = OfferSdpAnswerFactory.buildAnswer(offer)
        val iceCandidates = emptyList<RTCIceCandidate>()

        val connectionId = java.util.UUID.randomUUID().toString()
        activeConnections[connectionId] = WebRtcConnection(
            cameraId = "",
            connectionId = connectionId
        )

        return Result.success(
            WebRtcAnswer(
                answer = answer,
                iceCandidates = iceCandidates
            )
        )
    }

    /**
     * Закрыть WebRTC соединение
     */
    suspend fun closeConnection(connectionId: String) {
        val connection = activeConnections.remove(connectionId)
        if (connection != null) {
            // Если используется Janus, останавливаем поток
            if (connection.janusSessionId != null && connection.janusHandleId != null && connection.janusStreamId != null) {
                try {
                    janusGatewayService?.stopStream(
                        connection.janusSessionId,
                        connection.janusHandleId,
                        connection.janusStreamId
                    )
                } catch (e: Exception) {
                    logger.warn(e) { "Error stopping Janus stream during connection close" }
                }
            }
            logger.info { "Closed WebRTC connection: $connectionId" }
        }
    }

    /**
     * Закрыть все соединения для камеры
     */
    suspend fun closeConnectionsForCamera(cameraId: String) {
        val connectionsToClose = activeConnections.values.filter { it.cameraId == cameraId }
        connectionsToClose.forEach { connection ->
            activeConnections.remove(connection.connectionId)

            // Останавливаем Janus поток если используется
            if (connection.janusSessionId != null && connection.janusHandleId != null && connection.janusStreamId != null) {
                try {
                    janusGatewayService?.stopStream(
                        connection.janusSessionId,
                        connection.janusHandleId,
                        connection.janusStreamId
                    )
                } catch (e: Exception) {
                    logger.warn(e) { "Error stopping Janus stream" }
                }
            }
        }

        // Удаляем сессию камеры
        val cameraSession = cameraSessions.remove(cameraId)
        if (cameraSession != null && janusGatewayService != null) {
            try {
                // Отсоединяемся от плагина
                janusGatewayService.detachHandle(cameraSession.janusSessionId, cameraSession.janusHandleId)

                // Уничтожаем поток
                janusGatewayService.destroyStream(
                    cameraSession.janusSessionId,
                    cameraSession.janusHandleId,
                    cameraSession.janusStreamId
                )
            } catch (e: Exception) {
                logger.warn(e) { "Error cleaning up Janus resources for camera: $cameraId" }
            }
        }

        logger.info { "Closed ${connectionsToClose.size} WebRTC connections for camera: $cameraId" }
    }

    /**
     * Очистить старые соединения (старше 30 минут)
     */
    fun cleanupOldConnections() {
        val now = System.currentTimeMillis()
        val timeout = 30 * 60 * 1000L // 30 минут

        val oldConnections = activeConnections.values.filter {
            now - it.createdAt > timeout
        }

        oldConnections.forEach { connection ->
            activeConnections.remove(connection.connectionId)
        }

        if (oldConnections.isNotEmpty()) {
            logger.info { "Cleaned up ${oldConnections.size} old WebRTC connections" }
        }
    }
}

/**
 * WebRTC Answer с SDP и ICE candidates
 */
data class WebRtcAnswer(
    val answer: String,
    val iceCandidates: List<RTCIceCandidate>
)

/**
 * RTC Ice Candidate
 */
data class RTCIceCandidate(
    val candidate: String,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null
)
