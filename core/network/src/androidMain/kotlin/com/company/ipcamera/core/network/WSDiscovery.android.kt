package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.net.*
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document

private val logger = KotlinLogging.logger {}

/**
 * Android реализация WS-Discovery через MulticastSocket
 * Использует UDP multicast на 239.255.255.250:3702 согласно спецификации WS-Discovery
 */
actual class WSDiscovery {
    private val multicastAddress = InetAddress.getByName("239.255.255.250")
    private val multicastPort = 3702
    private var socket: MulticastSocket? = null
    private val discoveredDevices = mutableSetOf<String>() // Для дедупликации по XAddrs

    actual suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Starting WS-Discovery on Android..." }

            // Создание MulticastSocket для работы с multicast
            socket = MulticastSocket(multicastPort).apply {
                soTimeout = timeoutMillis.toInt()
                reuseAddress = true
                // Установка TTL для multicast
                timeToLive = 4
            }

            // Присоединение к multicast группе
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
            logger.debug { "Probe message sent" }

            // Сбор ответов
            val responses = mutableListOf<DiscoveredDevice>()
            val startTime = System.currentTimeMillis()
            var lastResponseTime = startTime

            // Отправляем несколько Probe запросов
            repeat(2) { attempt ->
                if (attempt > 0) {
                    kotlinx.coroutines.delay(500)
                }
                try {
                    val probeBytes = createProbeMessage().toByteArray(StandardCharsets.UTF_8)
                    val probePacket = DatagramPacket(probeBytes, probeBytes.size, multicastAddress, multicastPort)
                    socket?.send(probePacket)
                    logger.debug { "Probe message sent (attempt ${attempt + 1})" }
                } catch (e: Exception) {
                    logger.warn(e) { "Error sending probe on attempt ${attempt + 1}" }
                }
            }

            val extendedTimeout = timeoutMillis + 1000

            while (System.currentTimeMillis() - startTime < extendedTimeout) {
                try {
                    val buffer = ByteArray(16384)
                    val responsePacket = DatagramPacket(buffer, buffer.size)

                    val elapsed = System.currentTimeMillis() - startTime
                    val remainingTime = extendedTimeout - elapsed
                    if (remainingTime <= 0) break

                    socket?.soTimeout = remainingTime.toInt().coerceAtMost(1000).coerceAtLeast(100)
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

                    val devices = parseProbeMatches(responseXml)
                    for (device in devices) {
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
                    val timeSinceLastResponse = System.currentTimeMillis() - lastResponseTime
                    if (timeSinceLastResponse > 1000 || System.currentTimeMillis() - startTime >= timeoutMillis) {
                        break
                    }
                } catch (e: Exception) {
                    logger.debug(e) { "Error receiving response: ${e.message}" }
                }
            }

            logger.info { "WS-Discovery completed. Found ${responses.size} devices" }
            responses.toList()
        } catch (e: Exception) {
            logger.error(e) { "Error during WS-Discovery: ${e.message}" }
            emptyList()
        } finally {
            // Покидание multicast группы
            try {
                socket?.leaveGroup(InetSocketAddress(multicastAddress, multicastPort), getNetworkInterface())
            } catch (e: Exception) {
                try {
                    socket?.leaveGroup(multicastAddress)
                } catch (e2: Exception) {
                    logger.debug(e2) { "Error leaving multicast group" }
                }
            }
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

    actual fun close() {
        socket?.close()
        socket = null
        discoveredDevices.clear()
    }

    /**
     * Создать SOAP Probe запрос
     */
    private fun createProbeMessage(): String {
        val messageId = "urn:uuid:${java.util.UUID.randomUUID()}"
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
            factory.isValidating = false
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(java.io.ByteArrayInputStream(normalizedXml.toByteArray(StandardCharsets.UTF_8)))

            val devices = mutableListOf<DiscoveredDevice>()

            // Попытка найти ProbeMatches с namespace
            var probeMatches = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2005/04/discovery", "ProbeMatches")

            // Если не нашли, пробуем без namespace
            if (probeMatches.length == 0) {
                probeMatches = doc.getElementsByTagName("ProbeMatches")
            }

            // Если нашли ProbeMatches
            if (probeMatches.length > 0) {
                val probeMatchesElement = probeMatches.item(0)
                val probeMatchNodes = probeMatchesElement.childNodes

                for (i in 0 until probeMatchNodes.length) {
                    val node = probeMatchNodes.item(i)
                    if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE &&
                        (node.localName == "ProbeMatch" || node.nodeName.contains("ProbeMatch"))) {
                        val device = parseProbeMatchNode(node)
                        if (device != null) {
                            devices.add(device)
                        }
                    }
                }
            }

            // Если не нашли через ProbeMatches, ищем ProbeMatch напрямую
            if (devices.isEmpty()) {
                var probeMatchElements = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2005/04/discovery", "ProbeMatch")
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

    /**
     * Парсинг одного ProbeMatch узла
     */
    private fun parseProbeMatchNode(probeMatchNode: org.w3c.dom.Node): DiscoveredDevice? {
        val xAddrs = mutableListOf<String>()
        val types = mutableListOf<String>()
        val scopes = mutableListOf<String>()

        // Рекурсивный поиск элементов
        fun searchElement(parent: org.w3c.dom.Node, localName: String, resultList: MutableList<String>) {
            val childNodes = parent.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val nodeLocalName = node.localName ?: node.nodeName
                    if (nodeLocalName == localName || nodeLocalName.endsWith(localName)) {
                        val text = node.textContent?.trim()
                        if (text != null && text.isNotEmpty()) {
                            val values = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
                            resultList.addAll(values)
                        }
                    }
                    searchElement(node, localName, resultList)
                }
            }
        }

        searchElement(probeMatchNode, "XAddrs", xAddrs)
        searchElement(probeMatchNode, "Types", types)
        searchElement(probeMatchNode, "Scopes", scopes)

        return if (xAddrs.isNotEmpty()) {
            DiscoveredDevice(
                xAddrs = xAddrs.distinct(),
                types = types.distinct(),
                scopes = scopes.distinct()
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
            // Улучшенный regex для поиска ProbeMatch блоков
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

                // Извлечение XAddrs
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

            // Если не нашли через ProbeMatch блоки, ищем XAddrs напрямую
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


