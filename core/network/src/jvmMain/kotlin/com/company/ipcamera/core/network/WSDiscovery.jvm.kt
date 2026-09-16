package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.net.*
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory

private val logger = KotlinLogging.logger {}

/**
 * JVM реализация WS-Discovery через MulticastSocket (внутренняя реализация для desktop/jvm).
 * Использует UDP multicast на 239.255.255.250:3702 согласно спецификации WS-Discovery
 */
open class WSDiscoveryJvmImpl {
    private val multicastAddress = InetAddress.getByName("239.255.255.250")
    private val multicastPort = 3702
    private var socket: MulticastSocket? = null
    private val discoveredDevices = mutableSetOf<String>() // Для дедупликации по XAddrs

    open suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        var multicastGroup: NetworkInterface? = null
        try {
            logger.info { "Starting WS-Discovery on JVM..." }

            // Создание MulticastSocket для работы с multicast
            socket = MulticastSocket(multicastPort).apply {
                soTimeout = timeoutMillis.toInt()
                reuseAddress = true
                // Установка TTL для multicast (по умолчанию 1 для локальной сети)
                timeToLive = 4
            }

            // Присоединение к multicast группе на всех доступных интерфейсах
            try {
                socket?.joinGroup(InetSocketAddress(multicastAddress, multicastPort), getNetworkInterface())
                logger.debug { "Joined multicast group on network interface" }
            } catch (e: Exception) {
                // Fallback: присоединение без указания интерфейса
                try {
                    socket?.joinGroup(multicastAddress)
                    logger.debug { "Joined multicast group (fallback)" }
                } catch (e2: Exception) {
                    logger.warn(e2) { "Failed to join multicast group, continuing anyway" }
                }
            }

            val probeMessage = createProbeMessage()
            val probeBytes = probeMessage.toByteArray(StandardCharsets.UTF_8)

            // Отправка Probe запроса
            val probePacket = DatagramPacket(
                probeBytes,
                probeBytes.size,
                multicastAddress,
                multicastPort
            )
            socket?.send(probePacket)
            logger.debug { "Probe message sent to $multicastAddress:$multicastPort" }

            // Сбор ответов
            val responses = mutableListOf<DiscoveredDevice>()
            val startTime = System.currentTimeMillis()
            var lastResponseTime = startTime

            // Отправляем несколько Probe запросов с адаптивными интервалами для увеличения шансов получения ответов
            // Стратегия: широкий поиск → специфичный поиск → повторный широкий поиск
            val probeStrategies = listOf(
                Pair(0L, createProbeMessage()), // Первый запрос: широкий поиск (немедленно)
                Pair(500L, createProbeMessage()), // Второй запрос: через 500ms
                Pair(1000L, createProbeMessage()), // Третий запрос: через 1000ms (если мало ответов)
                Pair(1500L, createProbeMessage()) // Четвертый запрос: через 1500ms (если очень мало ответов)
            )

            for ((delayMs, probeMessage) in probeStrategies) {
                if (delayMs > 0) {
                    delay(delayMs)
                }

                // Проверяем, есть ли уже достаточно ответов
                if (responses.isNotEmpty() && System.currentTimeMillis() - lastResponseTime > 1000) {
                    logger.debug { "Enough responses received, skipping remaining probe requests" }
                    break
                }

                try {
                    val probeBytes = probeMessage.toByteArray(StandardCharsets.UTF_8)
                    val probePacket = DatagramPacket(probeBytes, probeBytes.size, multicastAddress, multicastPort)
                    socket?.send(probePacket)
                    logger.debug { "Probe message sent (delay: ${delayMs}ms)" }
                } catch (e: Exception) {
                    logger.warn(e) { "Error sending probe message: ${e.message}" }
                }
            }

            // Увеличиваем таймаут для сбора ответов
            val extendedTimeout = timeoutMillis + 1000
            // Оптимизация: если нет ответов в течение 2 секунд, прекращаем ожидание
            val noResponseTimeout = 2000L

            while (System.currentTimeMillis() - startTime < extendedTimeout) {
                try {
                    val buffer = ByteArray(16384) // Увеличенный буфер для больших ответов (16KB)
                    val responsePacket = DatagramPacket(buffer, buffer.size)

                    val elapsed = System.currentTimeMillis() - startTime
                    val remainingTime = extendedTimeout - elapsed
                    if (remainingTime <= 0) break

                    // Оптимизация: если прошло достаточно времени с последнего ответа, прекращаем
                    val timeSinceLastResponse = System.currentTimeMillis() - lastResponseTime
                    if (responses.isNotEmpty() && timeSinceLastResponse > noResponseTimeout) {
                        logger.debug { "No responses for ${timeSinceLastResponse}ms, stopping discovery" }
                        break
                    }

                    // Используем более короткий таймаут для каждого receive, чтобы проверять условие цикла
                    val socketTimeout = remainingTime.toInt().coerceAtMost(1000).coerceAtLeast(100)
                    socket?.soTimeout = socketTimeout
                    socket?.receive(responsePacket)

                    val responseXml = String(
                        responsePacket.data,
                        0,
                        responsePacket.length,
                        StandardCharsets.UTF_8
                    ).trim()

                    if (responseXml.isEmpty()) continue

                    lastResponseTime = System.currentTimeMillis()
                    logger.debug { "Received response (${responsePacket.length} bytes) from ${responsePacket.address}:${responsePacket.port}" }

                    // Парсинг всех ProbeMatch из ответа
                    val devices = parseProbeMatches(responseXml)
                    for (device in devices) {
                        // Дедупликация по всем XAddr для надежности
                        var found = false
                        for (xAddr in device.xAddrs) {
                            if (xAddr.isNotEmpty() && !discoveredDevices.contains(xAddr)) {
                                discoveredDevices.add(xAddr)
                                found = true
                            }
                        }

                        if (found && device.xAddrs.isNotEmpty()) {
                            responses.add(device)
                            logger.info {
                                "Discovered device: ${device.xAddrs.firstOrNull()} " +
                                    "(types: ${device.types.take(3).joinToString()}, " +
                                    "xAddrs: ${device.xAddrs.size})"
                            }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // Если прошло достаточно времени с последнего ответа, прекращаем ожидание
                    val timeSinceLastResponse = System.currentTimeMillis() - lastResponseTime
                    if (timeSinceLastResponse > 1000 || System.currentTimeMillis() - startTime >= timeoutMillis) {
                        break
                    }
                } catch (e: Exception) {
                    logger.debug(e) { "Error receiving response: ${e.message}" }
                    // Не прерываем цикл при ошибках, продолжаем слушать
                }
            }

            logger.info { "WS-Discovery completed. Found ${responses.size} devices" }
            responses.toList()
        } catch (e: Exception) {
            logger.error(e) { "Error during WS-Discovery: ${e.message}" }
            emptyList()
        } finally {
            // Покидание multicast группы и закрытие сокета
            try {
                socket?.leaveGroup(InetSocketAddress(multicastAddress, multicastPort), getNetworkInterface())
            } catch (e: Exception) {
                try {
                    socket?.leaveGroup(multicastAddress)
                } catch (e2: Exception) {
                    logger.debug(e2) { "Error leaving multicast group" }
                }
            }
            try {
                socket?.close()
            } catch (e: Exception) {
                logger.debug(e) { "Error closing socket" }
            }
            socket = null
        }
    }

    /**
     * Получить сетевой интерфейс для multicast
     */
    private fun getNetworkInterface(): NetworkInterface {
        return try {
            // Попытка найти активный интерфейс с IPv4 адресом
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .filter { it.supportsMulticast() }
                .firstOrNull()
                ?: NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
                ?: NetworkInterface.getNetworkInterfaces().nextElement()
        } catch (e: Exception) {
            logger.warn(e) { "Error getting network interface, using default" }
            NetworkInterface.getNetworkInterfaces().nextElement()
        }
    }

    open fun close() {
        socket?.close()
        socket = null
        discoveredDevices.clear()
    }

    /**
     * Создать SOAP Probe запрос
     * Использует более широкий поиск для обнаружения всех ONVIF устройств
     */
    private fun createProbeMessage(): String {
        val messageId = "urn:uuid:${java.util.UUID.randomUUID()}"
        // Используем более универсальный Probe без конкретного типа,
        // чтобы обнаружить все устройства, а затем фильтруем по ответам
        return """<?xml version="1.0" encoding="UTF-8"?>
<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope" xmlns:a="http://schemas.xmlsoap.org/ws/2004/08/addressing" xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery">
    <s:Header>
        <a:Action s:mustUnderstand="1">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</a:Action>
        <a:MessageID>$messageId</a:MessageID>
        <a:To s:mustUnderstand="1">urn:schemas-xmlsoap-org:ws:2005:04:discovery</a:To>
    </s:Header>
    <s:Body>
        <d:Probe xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery">
            <d:Types>dn:NetworkVideoTransmitter</d:Types>
        </d:Probe>
    </s:Body>
</s:Envelope>"""
    }

    /**
     * Парсинг всех ProbeMatch из ProbeMatches ответа
     */
    private fun parseProbeMatches(xml: String): List<DiscoveredDevice> {
        return try {
            // Нормализация XML - удаляем BOM и лишние пробелы
            val normalizedXml = xml.trim().replace("\uFEFF", "")

            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            factory.isIgnoringElementContentWhitespace = true
            // Отключаем валидацию для более гибкого парсинга
            factory.isValidating = false
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(java.io.ByteArrayInputStream(normalizedXml.toByteArray(StandardCharsets.UTF_8)))

            val devices = mutableListOf<DiscoveredDevice>()

            // Попытка найти ProbeMatches с namespace
            var probeMatches = doc.getElementsByTagNameNS(
                "http://schemas.xmlsoap.org/ws/2005/04/discovery",
                "ProbeMatches"
            )

            // Если не нашли, пробуем без namespace
            if (probeMatches.length == 0) {
                probeMatches = doc.getElementsByTagName("ProbeMatches")
            }

            // Если нашли ProbeMatches
            if (probeMatches.length > 0) {
                val probeMatchesElement = probeMatches.item(0)
                val probeMatchNodes = probeMatchesElement.childNodes

                // Парсинг каждого ProbeMatch
                for (i in 0 until probeMatchNodes.length) {
                    val node = probeMatchNodes.item(i)
                    if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE &&
                        (node.localName == "ProbeMatch" || node.nodeName.contains("ProbeMatch"))
                    ) {
                        val device = parseProbeMatchNode(node)
                        if (device != null) {
                            devices.add(device)
                        }
                    }
                }
            }

            // Если не нашли через ProbeMatches, ищем ProbeMatch напрямую
            if (devices.isEmpty()) {
                var probeMatchElements = doc.getElementsByTagNameNS(
                    "http://schemas.xmlsoap.org/ws/2005/04/discovery",
                    "ProbeMatch"
                )
                if (probeMatchElements.length == 0) {
                    probeMatchElements = doc.getElementsByTagName("ProbeMatch")
                }

                for (i in 0 until probeMatchElements.length) {
                    val node = probeMatchElements.item(i)
                    if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                        val device = parseProbeMatchNode(node)
                        if (device != null) {
                            devices.add(device)
                        }
                    }
                }
            }

            // Если всё еще пусто, используем fallback
            if (devices.isEmpty()) {
                logger.debug { "No devices found via DOM parsing, trying fallback" }
                parseProbeMatchesFallback(normalizedXml)
            } else {
                devices
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing ProbeMatches response, using fallback: ${e.message}" }
            parseProbeMatchesFallback(xml)
        }
    }

    /** Пустая реализация NodeList (интерфейс не имеет конструктора). */
    private fun emptyNodeList(): NodeList = object : NodeList {
        override fun item(index: Int): Node? = null
        override fun getLength(): Int = 0
    }

    /**
     * Парсинг одного ProbeMatch узла
     */
    private fun parseProbeMatchNode(probeMatchNode: Node): DiscoveredDevice? {
        val xAddrs = mutableListOf<String>()
        val types = mutableListOf<String>()
        val scopes = mutableListOf<String>()

        // Рекурсивный поиск элементов с улучшенной обработкой различных форматов
        fun searchElement(parent: Node, localName: String, resultList: MutableList<String>) {
            val childNodes = parent.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val nodeLocalName = node.localName ?: node.nodeName
                    // Поддержка различных форматов: XAddrs, xAddrs, XAddrs, xaddrs
                    val normalizedLocalName = nodeLocalName.lowercase()
                    val normalizedSearchName = localName.lowercase()

                    if (normalizedLocalName == normalizedSearchName ||
                        normalizedLocalName.endsWith(normalizedSearchName) ||
                        normalizedLocalName.contains(normalizedSearchName)
                    ) {
                        val text = node.textContent?.trim()
                        if (text != null && text.isNotEmpty()) {
                            // Разделяем по пробелам, табам и переносам строк, фильтруем пустые строки
                            val values = text.split(Regex("[\\s\\t\\n\\r]+"))
                                .filter { it.isNotEmpty() && it.isNotBlank() }
                                .map { it.trim() }
                            resultList.addAll(values)
                        }
                    }
                    // Рекурсивно ищем в дочерних элементах
                    searchElement(node, localName, resultList)
                }
            }
        }

        // Поиск XAddrs, Types, Scopes с поддержкой различных namespace префиксов
        searchElement(probeMatchNode, "XAddrs", xAddrs)
        searchElement(probeMatchNode, "Types", types)
        searchElement(probeMatchNode, "Scopes", scopes)

        // Также проверяем атрибуты (некоторые устройства могут использовать атрибуты)
        val attributes = probeMatchNode.attributes
        if (attributes != null) {
            for (i in 0 until attributes.length) {
                val attr = attributes.item(i)
                val attrName = attr.nodeName.lowercase()
                if (attrName.contains("xaddr") || attrName.contains("address")) {
                    val value = attr.nodeValue?.trim()
                    if (value != null && value.isNotEmpty()) {
                        xAddrs.add(value)
                    }
                }
            }
        }

        // Также пробуем найти через getElementsByTagName для надежности
        if (xAddrs.isEmpty()) {
            val xAddrsElements = probeMatchNode.ownerDocument?.getElementsByTagNameNS(
                "http://schemas.xmlsoap.org/ws/2005/04/discovery",
                "XAddrs"
            ) ?: emptyNodeList()

            if (xAddrsElements.length == 0) {
                val allElements = probeMatchNode.ownerDocument?.getElementsByTagName("XAddrs")
                if (allElements != null) {
                    for (i in 0 until allElements.length) {
                        val text = allElements.item(i).textContent?.trim()
                        if (text != null && text.isNotEmpty()) {
                            xAddrs.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() })
                        }
                    }
                }
            } else {
                for (i in 0 until xAddrsElements.length) {
                    val text = xAddrsElements.item(i).textContent?.trim()
                    if (text != null && text.isNotEmpty()) {
                        xAddrs.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() })
                    }
                }
            }
        }

        // Валидация и очистка данных
        val validXAddrs = xAddrs
            .distinct()
            .filter { it.isNotBlank() }
            .filter { url ->
                // Базовая валидация URL
                try {
                    val uri = java.net.URI(url)
                    uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https")
                } catch (e: Exception) {
                    false
                }
            }

        return if (validXAddrs.isNotEmpty()) {
            DiscoveredDevice(
                xAddrs = validXAddrs,
                types = types.distinct().filter { it.isNotBlank() },
                scopes = scopes.distinct().filter { it.isNotBlank() }
            )
        } else {
            null
        }
    }

    /**
     * Fallback парсинг через regex (более надежный)
     */
    private fun parseProbeMatchesFallback(xml: String): List<DiscoveredDevice> {
        val devices = mutableListOf<DiscoveredDevice>()

        try {
            // Улучшенный regex для поиска ProbeMatch блоков (поддерживает разные форматы namespace)
            val probeMatchRegex = Regex(
                "<[^>]*:?ProbeMatch[^>]*>(.*?)</[^>]*:?ProbeMatch>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
            )
            val matches = probeMatchRegex.findAll(xml)

            for (match in matches) {
                val probeMatchXml = match.groupValues.getOrNull(1) ?: continue
                val xAddrs = mutableListOf<String>()
                val types = mutableListOf<String>()
                val scopes = mutableListOf<String>()

                // Извлечение XAddrs (более гибкий regex)
                val xAddrsRegex = Regex("<[^>]*:?XAddrs[^>]*>([^<]+)</[^>]*:?XAddrs>", RegexOption.MULTILINE)
                val xAddrsMatches = xAddrsRegex.findAll(probeMatchXml)
                for (xMatch in xAddrsMatches) {
                    val text = xMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        xAddrs.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Извлечение Types
                val typesRegex = Regex("<[^>]*:?Types[^>]*>([^<]+)</[^>]*:?Types>", RegexOption.MULTILINE)
                val typesMatches = typesRegex.findAll(probeMatchXml)
                for (tMatch in typesMatches) {
                    val text = tMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        types.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Извлечение Scopes
                val scopesRegex = Regex("<[^>]*:?Scopes[^>]*>([^<]+)</[^>]*:?Scopes>", RegexOption.MULTILINE)
                val scopesMatches = scopesRegex.findAll(probeMatchXml)
                for (sMatch in scopesMatches) {
                    val text = sMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        scopes.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                if (xAddrs.isNotEmpty()) {
                    devices.add(
                        DiscoveredDevice(
                            xAddrs = xAddrs.distinct(),
                            types = types.distinct(),
                            scopes = scopes.distinct()
                        )
                    )
                }
            }

            // Если не нашли через ProbeMatch блоки, ищем XAddrs напрямую в документе
            if (devices.isEmpty()) {
                val xAddrsRegex = Regex("<[^>]*:?XAddrs[^>]*>([^<]+)</[^>]*:?XAddrs>", RegexOption.MULTILINE)
                val xAddrsMatches = xAddrsRegex.findAll(xml)
                val allXAddrs = mutableListOf<String>()

                for (match in xAddrsMatches) {
                    val text = match.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        allXAddrs.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                if (allXAddrs.isNotEmpty()) {
                    devices.add(
                        DiscoveredDevice(
                            xAddrs = allXAddrs.distinct(),
                            types = emptyList(),
                            scopes = emptyList()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.debug(e) { "Error in fallback parsing: ${e.message}" }
        }

        return devices
    }
}

actual class WSDiscovery actual constructor() : WSDiscoveryJvmImpl() {
    actual override suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> = super.discover(timeoutMillis)
    actual override fun close() = super.close()
}
