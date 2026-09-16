package com.company.ipcamera.core.network.onvif

import com.company.ipcamera.core.network.auth.DigestAuthHelper
import com.company.ipcamera.core.network.auth.DigestAuthParams
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}
private val Dispatchers.IO get() = Dispatchers.Default

/**
 * Реализация OnvifEventService
 */
class OnvifEventServiceImpl(
    private val engine: HttpClientEngine,
    private val notificationConsumerBaseUrl: String = "http://localhost:8080/api/v1/onvif/events/notification"
) : OnvifEventService {

    private val client: HttpClient by lazy {
        HttpClient(engine) {
            install(ContentNegotiation)
        }
    }

    // Кэш для Digest Authentication
    private val digestAuthCache = mutableMapOf<String, DigestAuthParams>()

    // Хранилище активных подписок
    private val subscriptions = mutableMapOf<String, OnvifEventSubscription>()

    override suspend fun subscribeToEvents(
        cameraUrl: String,
        username: String?,
        password: String?,
        filter: OnvifEventFilter?,
        notificationConsumerUrl: String?,
        subscriptionTime: Long
    ): Result<OnvifEventSubscription> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)

            // 1. Получить Event Service URL через GetCapabilities
            val eventServiceUrl = getEventServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Event Service URL not found in capabilities")
                )

            // 2. Создать URL для получения уведомлений
            val consumerUrl = notificationConsumerUrl ?: "$notificationConsumerBaseUrl/${newRandomId()}"

            // 3. Создать Subscribe SOAP запрос
            val subscribeRequest = OnvifEventParser.createSubscribeRequest(
                eventServiceUrl = eventServiceUrl,
                notificationConsumerUrl = consumerUrl,
                subscriptionTime = subscriptionTime,
                filter = filter
            )

            // 4. Отправить запрос с Digest Authentication
            val response = sendSoapRequest(
                url = eventServiceUrl,
                soapMessage = subscribeRequest,
                username = username,
                password = password
            )

            // 5. Парсить ответ
            val subscribeResponse = OnvifEventParser.parseSubscribeResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            // 6. Создать подписку
            val subscription = OnvifEventSubscription(
                id = newRandomId(),
                cameraUrl = normalizedUrl,
                notificationConsumerUrl = consumerUrl,
                subscriptionReference = subscribeResponse.subscriptionReference,
                expirationTime = subscribeResponse.expirationTime,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                username = username,
                password = password,
                filter = filter,
                status = SubscriptionStatus.ACTIVE
            )

            // 7. Сохранить подписку
            subscriptions[subscription.id] = subscription

            logger.info { "Subscribed to events for camera: $normalizedUrl, subscription ID: ${subscription.id}" }

            Result.success(subscription)
        } catch (e: IllegalStateException) {
            logger.error(e) { "Event service not available for camera: $cameraUrl" }
            Result.failure(com.company.ipcamera.core.network.OnvifNotSupportedException("Event service", e))
        } catch (e: Exception) {
            logger.error(e) { "Failed to subscribe to events for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun unsubscribe(subscriptionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val subscription = subscriptions[subscriptionId]
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Subscription not found: $subscriptionId")
                )

            if (subscription.status == SubscriptionStatus.CANCELLED) {
                return@withContext Result.success(Unit)
            }

            // Создать Unsubscribe SOAP запрос
            val unsubscribeRequest = createUnsubscribeRequest(subscription.subscriptionReference)

            // Отправить запрос
            sendSoapRequest(
                url = subscription.subscriptionReference,
                soapMessage = unsubscribeRequest,
                username = subscription.username,
                password = subscription.password
            )

            // Обновить статус подписки
            subscriptions[subscriptionId] = subscription.copy(status = SubscriptionStatus.CANCELLED)

            logger.info { "Unsubscribed from events, subscription ID: $subscriptionId" }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to unsubscribe: $subscriptionId" }
            Result.failure(e)
        }
    }

    override suspend fun renewSubscription(
        subscriptionId: String,
        renewalTime: Long
    ): Result<OnvifEventSubscription> = withContext(Dispatchers.IO) {
        try {
            val subscription = subscriptions[subscriptionId]
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Subscription not found: $subscriptionId")
                )

            if (subscription.status != SubscriptionStatus.ACTIVE) {
                return@withContext Result.failure(
                    IllegalStateException("Subscription is not active: ${subscription.status}")
                )
            }

            // Создать Renew SOAP запрос
            val renewRequest = createRenewRequest(subscription.subscriptionReference, renewalTime)

            // Отправить запрос
            val response = sendSoapRequest(
                url = subscription.subscriptionReference,
                soapMessage = renewRequest,
                username = subscription.username,
                password = subscription.password
            )

            // Парсить новый expiration time
            val newExpirationTime = parseRenewResponse(response)

            // Обновить подписку
            val updatedSubscription = subscription.copy(
                expirationTime = newExpirationTime,
                status = SubscriptionStatus.ACTIVE
            )
            subscriptions[subscriptionId] = updatedSubscription

            logger.info { "Renewed subscription: $subscriptionId, new expiration: $newExpirationTime" }

            Result.success(updatedSubscription)
        } catch (e: Exception) {
            logger.error(e) { "Failed to renew subscription: $subscriptionId" }
            Result.failure(e)
        }
    }

    override suspend fun getEventProperties(
        cameraUrl: String,
        username: String?,
        password: String?
    ): Result<OnvifEventProperties> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val eventServiceUrl = getEventServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Event Service URL not found")
                )

            // Создать GetEventProperties SOAP запрос
            val request = createGetEventPropertiesRequest()

            // Отправить запрос
            val response = sendSoapRequest(
                url = eventServiceUrl,
                soapMessage = request,
                username = username,
                password = password
            )

            // Парсить ответ через OnvifEventParser
            val properties = OnvifEventParser.parseGetEventPropertiesResponse(response, eventServiceUrl)
                .getOrElse { error ->
                    logger.warn(error) { "Failed to parse GetEventProperties response, using defaults" }
                    // Fallback на упрощенную версию
                    OnvifEventProperties(
                        eventServiceUrl = eventServiceUrl,
                        supportedTopics = emptyList(),
                        supportedFilters = emptyList(),
                        maxSubscriptionTime = null,
                        minSubscriptionTime = null,
                        supportsPullPoint = false,
                        supportsPush = true
                    )
                }

            Result.success(properties)
        } catch (e: IllegalStateException) {
            logger.error(e) { "Event service not available for camera: $cameraUrl" }
            Result.failure(com.company.ipcamera.core.network.OnvifNotSupportedException("Event service", e))
        } catch (e: Exception) {
            logger.error(e) { "Failed to get event properties for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun getSubscription(subscriptionId: String): OnvifEventSubscription? {
        return subscriptions[subscriptionId]
    }

    override suspend fun getSubscriptionsForCamera(cameraUrl: String): List<OnvifEventSubscription> {
        val normalizedUrl = normalizeUrl(cameraUrl)
        return subscriptions.values.filter { it.cameraUrl == normalizedUrl && it.status == SubscriptionStatus.ACTIVE }
    }

    override suspend fun unsubscribeAll(cameraUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)
            val cameraSubscriptions = getSubscriptionsForCamera(normalizedUrl)

            val results = cameraSubscriptions.map { subscription ->
                unsubscribe(subscription.id)
            }

            val failures = results.filter { it.isFailure }
            if (failures.isNotEmpty()) {
                val error = failures.first().exceptionOrNull()
                return@withContext Result.failure(
                    Exception("Failed to unsubscribe some subscriptions: ${error?.message}")
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to unsubscribe all for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    // Вспомогательные методы

    private suspend fun getEventServiceUrl(
        cameraUrl: String,
        username: String?,
        password: String?
    ): String? {
        val normalizedUrl = normalizeUrl(cameraUrl)

        // Попытка получить Event Service URL через GetCapabilities
        try {
            val capabilitiesRequest = createGetCapabilitiesRequest()
            val response = sendSoapRequest(
                url = normalizedUrl,
                soapMessage = capabilitiesRequest,
                username = username,
                password = password
            )

            // Парсинг Event Service URL из capabilities
            val eventServiceUrl = extractEventServiceUrl(response)
            if (eventServiceUrl != null) {
                logger.debug { "Found Event Service URL in capabilities: $eventServiceUrl" }
                return eventServiceUrl
            }
        } catch (e: Exception) {
            logger.debug(e) { "Failed to get Event Service URL from capabilities, trying fallback" }
        }

        // Fallback: Стандартные пути для Event Service
        val possiblePaths = listOf(
            "$normalizedUrl/onvif/event_service",
            "$normalizedUrl/onvif/events",
            "$normalizedUrl/onvif/EventService",
            "$normalizedUrl/onvif/event"
        )

        return possiblePaths.firstOrNull()
    }

    private fun createGetCapabilitiesRequest(): String {
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
    <s:Body>
        <tds:GetCapabilities xmlns:tds="http://www.onvif.org/ver10/device/wsdl">
            <tds:Category>All</tds:Category>
        </tds:GetCapabilities>
    </s:Body>
</s:Envelope>"""
    }

    private fun extractEventServiceUrl(xml: String): String? {
        // Парсинг Event Service URL из capabilities XML
        val patterns = listOf(
            Regex("""<tds:Events[^>]*XAddr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""<Events[^>]*XAddr\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""(?s)<tds:Events[^>]*>.*?<tds:XAddr[^>]*>([^<]+)</tds:XAddr>"""),
            Regex("""(?s)<tt:Events[^>]*>.*?<tt:XAddr[^>]*>([^<]+)</tt:XAddr>"""),
            Regex("""(?s)<(?:\w+:)?Events[^>]*>.*?<(?:\w+:)?XAddr[^>]*>([^<]+)</(?:\w+:)?XAddr>"""),
            Regex("""Events.*?XAddr[^>]*>([^<]+)<""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(xml)
            if (match != null) {
                val url = match.groupValues.getOrNull(1)?.trim()
                if (!url.isNullOrBlank()) {
                    return url
                }
            }
        }

        return null
    }

    private suspend fun sendSoapRequest(
        url: String,
        soapMessage: String,
        username: String?,
        password: String?
    ): String {
        var digestParams = digestAuthCache[url]
        var useDigestAuth = digestParams != null
        var nc = 1
        var attempt = 0
        val maxRetries = 2

        while (attempt <= maxRetries) {
            try {
                // Извлекаем путь из URL для Digest Authentication
                val uriPath = try {
                    extractPathFromUrl(url)
                } catch (e: Exception) {
                    "/"
                }

                val response = client.post(url) {
                    contentType(ContentType.Text.Xml)
                    header("SOAPAction", "")

                    // Аутентификация
                    if (username != null && password != null) {
                        if (useDigestAuth && digestParams != null) {
                            val digestHeader = DigestAuthHelper.generateDigestAuthHeader(
                                username = username,
                                password = password,
                                method = "POST",
                                uri = uriPath,
                                params = digestParams,
                                nc = nc.toString(16).padStart(8, '0'),
                                entityBody = soapMessage
                            )
                            header(HttpHeaders.Authorization, digestHeader)
                            nc++
                        } else {
                            val credentials = "$username:$password"
                            val encoded = credentials.encodeToByteArray().encodeBase64()
                            header(HttpHeaders.Authorization, "Basic $encoded")
                        }
                    }

                    setBody(soapMessage)
                }

                if (response.status.value in 200..299) {
                    return response.body<String>()
                } else if (response.status.value == HttpStatusCode.Unauthorized.value) {
                    val wwwAuthenticate = response.headers[HttpHeaders.WWWAuthenticate]
                        ?: response.headers.getAll("WWW-Authenticate")?.firstOrNull()

                    if (wwwAuthenticate != null && wwwAuthenticate.startsWith("Digest", ignoreCase = true)) {
                        val parsedParams = DigestAuthHelper.parseWWWAuthenticate(wwwAuthenticate)
                        if (parsedParams != null) {
                            if (parsedParams.stale) {
                                digestAuthCache.remove(url)
                                digestParams = null
                                useDigestAuth = false
                            } else {
                                digestParams = parsedParams
                                digestAuthCache[url] = digestParams
                                useDigestAuth = true
                            }
                            attempt++
                            continue
                        }
                    }
                }

                // Если не удалось обработать, выбрасываем ошибку
                throw IllegalStateException("SOAP request failed with status: ${response.status}")
            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    logger.error(e) { "Failed to send SOAP request after $maxRetries retries" }
                    throw e
                }
                attempt++
            }
        }

        throw IllegalStateException("Failed to send SOAP request")
    }

    private fun createUnsubscribeRequest(subscriptionReference: String): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2">
            <soap:Body>
                <wsnt:Unsubscribe>
                    <wsnt:SubscriptionReference>
                        <wsa:Address xmlns:wsa="http://www.w3.org/2005/08/addressing">$subscriptionReference</wsa:Address>
                    </wsnt:SubscriptionReference>
                </wsnt:Unsubscribe>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createRenewRequest(subscriptionReference: String, renewalTime: Long): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2">
            <soap:Body>
                <wsnt:Renew>
                    <wsnt:TerminationTime>PT${renewalTime}S</wsnt:TerminationTime>
                </wsnt:Renew>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createGetEventPropertiesRequest(): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tev="http://www.onvif.org/ver10/events/wsdl">
            <soap:Body>
                <tev:GetEventProperties/>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun parseRenewResponse(xml: String): Long {
        // Парсинг нового expiration time из ответа Renew
        val terminationTimeRegex = Regex(
            """<wsnt:TerminationTime[^>]*>([^<]+)</wsnt:TerminationTime>"""
        )
        val terminationTimeStr = terminationTimeRegex.find(xml)?.groupValues?.get(1)
        return parseTerminationTime(terminationTimeStr)
    }

    private fun parseTerminationTime(terminationTimeStr: String?): Long {
        if (terminationTimeStr == null) {
            return Clock.System.now().toEpochMilliseconds() + (3600 * 1000)
        }

        return try {
            // ISO 8601 duration: PT3600S (3600 секунд)
            val durationRegex = Regex("""PT(\d+)S""")
            val seconds = durationRegex.find(terminationTimeStr)?.groupValues?.get(1)?.toLongOrNull()
                ?: 3600L
            Clock.System.now().toEpochMilliseconds() + (seconds * 1000)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse termination time: $terminationTimeStr" }
            Clock.System.now().toEpochMilliseconds() + (3600 * 1000)
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.trimEnd('/')
    }

    // === PullPoint Subscription методы ===

    override suspend fun createPullPointSubscription(
        cameraUrl: String,
        username: String?,
        password: String?,
        filter: OnvifEventFilter?,
        subscriptionTime: Long
    ): Result<OnvifEventSubscription> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(cameraUrl)

            // 1. Получить Event Service URL
            val eventServiceUrl = getEventServiceUrl(normalizedUrl, username, password)
                ?: return@withContext Result.failure(
                    IllegalStateException("Event Service URL not found in capabilities")
                )

            // 2. Создать PullPoint через CreatePullPointSubscription
            val pullPointRequest = createPullPointSubscriptionRequest(
                eventServiceUrl = eventServiceUrl,
                subscriptionTime = subscriptionTime,
                filter = filter
            )

            // 3. Отправить запрос
            val response = sendSoapRequest(
                url = eventServiceUrl,
                soapMessage = pullPointRequest,
                username = username,
                password = password
            )

            // 4. Парсить ответ
            val pullPointResponse = parsePullPointSubscriptionResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            // 5. Создать подписку
            val subscription = OnvifEventSubscription(
                id = newRandomId(),
                cameraUrl = normalizedUrl,
                notificationConsumerUrl = pullPointResponse.pullPointUrl,
                subscriptionReference = pullPointResponse.pullPointUrl,
                expirationTime = pullPointResponse.expirationTime,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                username = username,
                password = password,
                filter = filter,
                status = SubscriptionStatus.ACTIVE
            )

            // 6. Сохранить подписку
            subscriptions[subscription.id] = subscription

            logger.info { "Created PullPoint subscription for camera: $normalizedUrl, subscription ID: ${subscription.id}" }

            Result.success(subscription)
        } catch (e: Exception) {
            logger.error(e) { "Failed to create PullPoint subscription for camera: $cameraUrl" }
            Result.failure(e)
        }
    }

    override suspend fun pullMessages(
        subscriptionId: String,
        timeout: Long,
        maxMessages: Int
    ): Result<List<OnvifEvent>> = withContext(Dispatchers.IO) {
        try {
            val subscription = subscriptions[subscriptionId]
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Subscription not found: $subscriptionId")
                )

            if (subscription.status != SubscriptionStatus.ACTIVE) {
                return@withContext Result.failure(
                    IllegalStateException("Subscription is not active: ${subscription.status}")
                )
            }

            // Создать PullMessages SOAP запрос
            val pullMessagesRequest = createPullMessagesRequest(timeout, maxMessages)

            // Отправить запрос на PullPoint URL
            val response = sendSoapRequest(
                url = subscription.subscriptionReference,
                soapMessage = pullMessagesRequest,
                username = subscription.username,
                password = subscription.password
            )

            // Парсить события из ответа
            val events = OnvifEventParser.parsePullMessagesResponse(response)
                .getOrElse { error ->
                    return@withContext Result.failure(error)
                }

            logger.debug { "Pulled ${events.size} events from subscription: $subscriptionId" }

            Result.success(events)
        } catch (e: Exception) {
            logger.error(e) { "Failed to pull messages from subscription: $subscriptionId" }
            Result.failure(e)
        }
    }

    override suspend fun setSynchronizationPoint(subscriptionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val subscription = subscriptions[subscriptionId]
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Subscription not found: $subscriptionId")
                )

            // Создать SetSynchronizationPoint SOAP запрос
            val syncPointRequest = createSetSynchronizationPointRequest()

            // Отправить запрос
            sendSoapRequest(
                url = subscription.subscriptionReference,
                soapMessage = syncPointRequest,
                username = subscription.username,
                password = subscription.password
            )

            logger.info { "Set synchronization point for subscription: $subscriptionId" }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to set synchronization point for subscription: $subscriptionId" }
            Result.failure(e)
        }
    }

    private fun createPullPointSubscriptionRequest(
        eventServiceUrl: String,
        subscriptionTime: Long,
        filter: OnvifEventFilter?
    ): String {
        val topicExpression = filter?.topicExpression ?: ""
        val filterXml = if (filter != null) {
            """
            <wsnt:Filter>
                <wsnt:TopicExpression Dialect="http://www.onvif.org/ver10/tev/topicExpression/ConcreteSet">
                    $topicExpression
                </wsnt:TopicExpression>
            </wsnt:Filter>
            """.trimIndent()
        } else {
            ""
        }

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tev="http://www.onvif.org/ver10/events/wsdl">
            <soap:Body>
                <tev:CreatePullPointSubscription>
                    <wsnt:InitialTerminationTime xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2">PT${subscriptionTime}S</wsnt:InitialTerminationTime>
                    $filterXml
                </tev:CreatePullPointSubscription>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createPullMessagesRequest(timeout: Long, maxMessages: Int): String {
        // Конвертируем миллисекунды в секунды для ISO 8601 duration
        val timeoutSeconds = timeout / 1000
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tev="http://www.onvif.org/ver10/events/wsdl">
            <soap:Body>
                <tev:PullMessages>
                    <wsnt:Timeout xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2">PT${timeoutSeconds}S</wsnt:Timeout>
                    <wsnt:MessageLimit xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2">$maxMessages</wsnt:MessageLimit>
                </tev:PullMessages>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun createSetSynchronizationPointRequest(): String {
        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                       xmlns:tev="http://www.onvif.org/ver10/events/wsdl">
            <soap:Body>
                <tev:SetSynchronizationPoint/>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    private fun parsePullPointSubscriptionResponse(xml: String): Result<PullPointSubscriptionResponse> {
        return try {
            // Извлечение PullPoint URL из ответа
            val pullPointUrlRegex = Regex(
                """(?s)<tev:SubscriptionReference[^>]*>.*?<wsa:Address[^>]*>([^<]+)</wsa:Address>.*?</tev:SubscriptionReference>"""
            )
            val pullPointUrl = pullPointUrlRegex.find(xml)?.groupValues?.get(1)
                ?: return Result.failure(IllegalArgumentException("PullPoint URL not found"))

            // Извлечение времени истечения
            val terminationTimeRegex = Regex(
                """<wsnt:TerminationTime[^>]*>([^<]+)</wsnt:TerminationTime>"""
            )
            val terminationTimeStr = terminationTimeRegex.find(xml)?.groupValues?.get(1)
            val expirationTime = parseTerminationTime(terminationTimeStr)

            Result.success(
                PullPointSubscriptionResponse(
                    pullPointUrl = pullPointUrl,
                    expirationTime = expirationTime
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse PullPoint subscription response" }
            Result.failure(e)
        }
    }

    private data class PullPointSubscriptionResponse(
        val pullPointUrl: String,
        val expirationTime: Long
    )

    private fun extractPathFromUrl(url: String): String {
        val schemeIndex = url.indexOf("://")
        val hostStart = if (schemeIndex >= 0) schemeIndex + 3 else 0
        val pathStart = url.indexOf('/', hostStart)
        if (pathStart < 0) return "/"
        val pathEnd = url.indexOf('?', pathStart).takeIf { it >= 0 } ?: url.length
        return url.substring(pathStart, pathEnd).ifEmpty { "/" }
    }

    private fun newRandomId(): String {
        val chars = "0123456789abcdef"
        return buildString(32) {
            repeat(32) { append(chars[Random.nextInt(chars.length)]) }
        }
    }
}
