package com.company.ipcamera.core.network.onvif

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Парсер для ONVIF событий (WS-Notification NotificationMessage).
 * Использует regex/строковый парсинг для совместимости с KMP (без kotlinx.serialization.xml).
 */
object OnvifEventParser {

    /**
     * Парсинг NotificationMessage из XML
     *
     * Формат WS-Notification:
     * <wsnt:NotificationMessage>
     *   <wsnt:Topic Dialect="...">...</wsnt:Topic>
     *   <wsnt:Message>...</wsnt:Message>
     * </wsnt:NotificationMessage>
     */
    fun parseNotificationMessage(xml: String): Result<OnvifEvent> {
        return try {
            val event = parseNotificationMessageFromXml(xml)
            Result.success(event)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse ONVIF notification message" }
            Result.failure(e)
        }
    }

    private fun parseNotificationMessageFromXml(xml: String): OnvifEvent {
        // Извлечение Topic (поддержка с префиксом и без)
        val topic = extractXmlElement(xml, listOf("Topic", "wsnt:Topic")) ?: "unknown"

        // Message
        val message = extractXmlElement(xml, listOf("Message", "wsnt:Message"))

        // Source
        val source = extractXmlElement(xml, listOf("Source", "wsnt:Source"))

        // Timestamp / UtcTime
        val timestampStr = extractXmlElement(xml, listOf("UtcTime", "Timestamp", "wsnt:Timestamp"))
        val timestamp = if (timestampStr != null) parseTimestamp(timestampStr) else Clock.System.now().toEpochMilliseconds()

        // Дополнительные свойства из вложенных элементов
        val properties = parseMessageContent(xml)

        return OnvifEvent(
            topic = topic,
            timestamp = timestamp,
            message = message,
            properties = properties,
            source = source
        )
    }

    private fun extractXmlElement(xml: String, tagNames: List<String>): String? {
        for (tag in tagNames) {
            // Элемент с префиксом namespace: <wsnt:Topic>value</wsnt:Topic> или <Topic>value</Topic>
            val regex = Regex("""<$tag[^>]*>([^<]*)</$tag>""", RegexOption.IGNORE_CASE)
            regex.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    /**
     * Парсинг timestamp из различных форматов
     */
    private fun parseTimestamp(timestampStr: String): Long {
        return try {
            // ISO 8601 формат: 2024-01-26T12:00:00Z
            if (timestampStr.contains("T")) {
                val dateTime = Instant.parse(timestampStr)
                dateTime.toEpochMilliseconds()
            } else {
                // Unix timestamp (секунды или миллисекунды)
                val timestamp = timestampStr.toLongOrNull() ?: return Clock.System.now().toEpochMilliseconds()
                if (timestamp > 1_000_000_000_000L) {
                    // Миллисекунды
                    timestamp
                } else {
                    // Секунды
                    timestamp * 1000
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse timestamp: $timestampStr" }
            Clock.System.now().toEpochMilliseconds()
        }
    }

    /**
     * Парсинг Topic Expression
     *
     * Формат: Dialect="http://www.onvif.org/ver10/tev/topicExpression/ConcreteSet"
     *         Topic="tns1:VideoSource/MotionAlarm"
     */
    fun parseTopicExpression(xml: String): String? {
        return try {
            val attributeRegex = Regex("""Topic\s*[=:]\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            attributeRegex.find(xml)?.groupValues?.get(1)
                ?: extractXmlElement(xml, listOf("Topic", "wsnt:Topic"))
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse topic expression" }
            null
        }
    }

    /**
     * Парсинг Message Content
     */
    fun parseMessageContent(xml: String): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        // Топ-уровневые служебные теги WS-Notification, извлекаемые отдельно
        val reservedKeys = setOf(
            "Topic", "Message", "Source", "UtcTime", "Timestamp",
            "NotificationMessage", "Envelope", "Body", "Header"
        )

        try {
            // Простой парсинг через регулярные выражения
            val propertyRegex = Regex("""<(?:\w+:)?(\w+)[^>]*>([^<]+)</(?:\w+:)?\1>""")
            propertyRegex.findAll(xml).forEach { matchResult ->
                val key = matchResult.groupValues[1]
                val value = matchResult.groupValues[2].trim()
                if (key.isNotBlank() && value.isNotBlank() && key !in reservedKeys) {
                    properties[key] = value
                }
            }

            // ONVIF события используют <SimpleItem Name="State" Value="true"/> (атрибуты).
            val simpleItemRegex = Regex(
                """<(?:\w+:)?SimpleItem\s+Name\s*=\s*["']([^"']+)["']\s+Value\s*=\s*["']([^"']*)["']""",
                RegexOption.IGNORE_CASE
            )
            simpleItemRegex.findAll(xml).forEach { matchResult ->
                val name = matchResult.groupValues[1].trim()
                val value = matchResult.groupValues[2].trim()
                if (name.isNotBlank()) {
                    properties[name] = value
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse message content" }
        }

        return properties
    }

    /**
     * Создание SOAP запроса для Subscribe
     */
    fun createSubscribeRequest(
        eventServiceUrl: String,
        notificationConsumerUrl: String,
        subscriptionTime: Long,
        filter: OnvifEventFilter? = null
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
                       xmlns:wsnt="http://docs.oasis-open.org/wsn/b-2"
                       xmlns:tev="http://www.onvif.org/ver10/events/wsdl">
            <soap:Header>
                <wsnt:SubscriptionManager>
                    <wsa:Address xmlns:wsa="http://www.w3.org/2005/08/addressing">$notificationConsumerUrl</wsa:Address>
                </wsnt:SubscriptionManager>
            </soap:Header>
            <soap:Body>
                <wsnt:Subscribe>
                    <wsnt:ConsumerReference>
                        <wsa:Address xmlns:wsa="http://www.w3.org/2005/08/addressing">$notificationConsumerUrl</wsa:Address>
                    </wsnt:ConsumerReference>
                    <wsnt:InitialTerminationTime>PT${subscriptionTime}S</wsnt:InitialTerminationTime>
                    $filterXml
                </wsnt:Subscribe>
            </soap:Body>
        </soap:Envelope>
        """.trimIndent()
    }

    /**
     * Парсинг ответа Subscribe
     */
    fun parseSubscribeResponse(xml: String): Result<SubscribeResponse> {
        return try {
            // Извлечение SubscriptionReference из ответа
            val subscriptionRefRegex = Regex(
                """(?s)<wsnt:SubscriptionReference[^>]*>.*?<wsa:Address[^>]*>([^<]+)</wsa:Address>.*?</wsnt:SubscriptionReference>"""
            )
            val subscriptionRef = subscriptionRefRegex.find(xml)?.groupValues?.get(1)
                ?: return Result.failure(IllegalArgumentException("SubscriptionReference not found"))

            // Извлечение времени истечения
            val terminationTimeRegex = Regex(
                """<wsnt:TerminationTime[^>]*>([^<]+)</wsnt:TerminationTime>"""
            )
            val terminationTimeStr = terminationTimeRegex.find(xml)?.groupValues?.get(1)
            val expirationTime = parseTerminationTime(terminationTimeStr)

            Result.success(
                SubscribeResponse(
                    subscriptionReference = subscriptionRef,
                    expirationTime = expirationTime
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse Subscribe response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг времени истечения из ISO 8601 duration (PT3600S)
     */
    private fun parseTerminationTime(terminationTimeStr: String?): Long {
        if (terminationTimeStr == null) {
            return Clock.System.now().toEpochMilliseconds() + (3600 * 1000) // По умолчанию 1 час
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

    /**
     * Парсинг ответа PullMessages
     *
     * Формат:
     * <tev:PullMessagesResponse>
     *   <wsnt:NotificationMessage>...</wsnt:NotificationMessage>
     *   ...
     * </tev:PullMessagesResponse>
     */
    fun parsePullMessagesResponse(xml: String): Result<List<OnvifEvent>> {
        return try {
            val events = mutableListOf<OnvifEvent>()

            // Парсинг всех NotificationMessage элементов
            val notificationMessageRegex = Regex(
                """(?s)<wsnt:NotificationMessage[^>]*>(.*?)</wsnt:NotificationMessage>"""
            )

            notificationMessageRegex.findAll(xml).forEach { match ->
                val messageXml = match.groupValues[1]
                val eventResult = parseNotificationMessage(
                    "<wsnt:NotificationMessage>$messageXml</wsnt:NotificationMessage>"
                )
                eventResult.onSuccess { event ->
                    events.add(event)
                }
            }

            Result.success(events)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse PullMessages response" }
            Result.failure(e)
        }
    }

    /**
     * Парсинг ответа GetEventProperties
     *
     * Формат:
     * <tev:GetEventPropertiesResponse>
     *   <tev:TopicNamespaceLocation>...</tev:TopicNamespaceLocation>
     *   <wsnt:FixedTopicSet>true</wsnt:FixedTopicSet>
     *   <wsnt:TopicSet>...</wsnt:TopicSet>
     *   <wsnt:TopicExpressionDialects>...</wsnt:TopicExpressionDialects>
     *   <tev:MessageContentFilterDialects>...</tev:MessageContentFilterDialects>
     *   <tev:ProducerPropertiesFilterDialects>...</tev:ProducerPropertiesFilterDialects>
     *   <tev:MessageContentSchemaLocation>...</tev:MessageContentSchemaLocation>
     *   <wsnt:MaxNotificationProducers>...</wsnt:MaxNotificationProducers>
     * </tev:GetEventPropertiesResponse>
     */
    fun parseGetEventPropertiesResponse(
        xml: String,
        eventServiceUrl: String
    ): Result<OnvifEventProperties> {
        return try {
            val supportedTopics = mutableListOf<String>()
            val supportedFilters = mutableListOf<String>()
            var maxSubscriptionTime: Long? = null
            var minSubscriptionTime: Long? = null
            var supportsPullPoint = false
            var supportsPush = true

            // Парсинг TopicSet - извлечение всех топиков
            val topicSetRegex = Regex(
                """(?s)<wsnt:TopicSet[^>]*>(.*?)</wsnt:TopicSet>"""
            )
            val topicSetMatch = topicSetRegex.find(xml)
            if (topicSetMatch != null) {
                val topicSetXml = topicSetMatch.groupValues[1]
                // Парсинг топиков из TopicSet
                val topicRegex = Regex(
                    """<tt:Topic[^>]*Name\s*=\s*["']([^"']+)["']""",
                    RegexOption.IGNORE_CASE
                )
                topicRegex.findAll(topicSetXml).forEach { match ->
                    val topic = match.groupValues[1]
                    if (topic.isNotBlank()) {
                        supportedTopics.add(topic)
                    }
                }
            }

            // Альтернативный способ парсинга топиков (если TopicSet не найден)
            if (supportedTopics.isEmpty()) {
                val topicRegex = Regex(
                    """<tt:Topic[^>]*Name\s*=\s*["']([^"']+)["']""",
                    RegexOption.IGNORE_CASE
                )
                topicRegex.findAll(xml).forEach { match ->
                    val topic = match.groupValues[1]
                    if (topic.isNotBlank() && !supportedTopics.contains(topic)) {
                        supportedTopics.add(topic)
                    }
                }
            }

            // Парсинг TopicExpressionDialects
            val topicExpressionDialectsRegex = Regex(
                """(?s)<wsnt:TopicExpressionDialects[^>]*>(.*?)</wsnt:TopicExpressionDialects>"""
            )
            topicExpressionDialectsRegex.findAll(xml).forEach { match ->
                val dialectsXml = match.groupValues[1]
                val dialectRegex = Regex("""<wsnt:TopicExpressionDialect[^>]*>([^<]+)</wsnt:TopicExpressionDialect>""")
                dialectRegex.findAll(dialectsXml).forEach { dialectMatch ->
                    val dialect = dialectMatch.groupValues[1].trim()
                    if (dialect.isNotBlank() && !supportedFilters.contains(dialect)) {
                        supportedFilters.add(dialect)
                    }
                }
            }

            // Парсинг MessageContentFilterDialects
            val messageContentFilterDialectsRegex = Regex(
                """(?s)<tev:MessageContentFilterDialects[^>]*>(.*?)</tev:MessageContentFilterDialects>"""
            )
            messageContentFilterDialectsRegex.findAll(xml).forEach { match ->
                val dialectsXml = match.groupValues[1]
                val dialectRegex = Regex(
                    """<tev:MessageContentFilterDialect[^>]*>([^<]+)</tev:MessageContentFilterDialect>"""
                )
                dialectRegex.findAll(dialectsXml).forEach { dialectMatch ->
                    val dialect = dialectMatch.groupValues[1].trim()
                    if (dialect.isNotBlank() && !supportedFilters.contains(dialect)) {
                        supportedFilters.add(dialect)
                    }
                }
            }

            // Парсинг MaxNotificationProducers (может указывать на поддержку PullPoint)
            val maxNotificationProducersRegex = Regex(
                """<wsnt:MaxNotificationProducers[^>]*>(\d+)</wsnt:MaxNotificationProducers>"""
            )
            val maxNotificationProducers = maxNotificationProducersRegex.find(xml)?.groupValues?.get(1)?.toIntOrNull()
            if (maxNotificationProducers != null && maxNotificationProducers > 0) {
                supportsPullPoint = true
            }

            // Проверка наличия PullPoint в capabilities или ответе
            if (xml.contains("PullPoint", ignoreCase = true) ||
                xml.contains("CreatePullPointSubscription", ignoreCase = true)
            ) {
                supportsPullPoint = true
            }

            // Парсинг времени подписки (если указано)
            // Некоторые камеры могут указывать максимальное время подписки
            val maxSubscriptionTimeRegex = Regex(
                """<.*?MaxSubscriptionTime[^>]*>PT(\d+)S</.*?>""",
                RegexOption.IGNORE_CASE
            )
            val maxSubscriptionTimeMatch = maxSubscriptionTimeRegex.find(xml)
            if (maxSubscriptionTimeMatch != null) {
                maxSubscriptionTime = maxSubscriptionTimeMatch.groupValues[1].toLongOrNull()
            }

            val minSubscriptionTimeRegex = Regex(
                """<.*?MinSubscriptionTime[^>]*>PT(\d+)S</.*?>""",
                RegexOption.IGNORE_CASE
            )
            val minSubscriptionTimeMatch = minSubscriptionTimeRegex.find(xml)
            if (minSubscriptionTimeMatch != null) {
                minSubscriptionTime = minSubscriptionTimeMatch.groupValues[1].toLongOrNull()
            }

            // Проверка поддержки Push (по умолчанию true, если не указано иное)
            if (xml.contains("Subscribe", ignoreCase = true) ||
                xml.contains("NotificationConsumer", ignoreCase = true)
            ) {
                supportsPush = true
            }

            val properties = OnvifEventProperties(
                eventServiceUrl = eventServiceUrl,
                supportedTopics = supportedTopics,
                supportedFilters = supportedFilters,
                maxSubscriptionTime = maxSubscriptionTime,
                minSubscriptionTime = minSubscriptionTime,
                supportsPullPoint = supportsPullPoint,
                supportsPush = supportsPush
            )

            Result.success(properties)
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse GetEventProperties response" }
            Result.failure(e)
        }
    }
}

/**
 * Ответ на Subscribe запрос
 */
data class SubscribeResponse(
    val subscriptionReference: String,
    val expirationTime: Long
)
