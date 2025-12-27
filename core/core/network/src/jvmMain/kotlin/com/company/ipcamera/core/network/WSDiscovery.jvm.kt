package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.net.*
import java.nio.channels.DatagramChannel
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList

private val logger = KotlinLogging.logger {}

/**
 * JVM реализация WS-Discovery через MulticastSocket
 * Использует UDP multicast на 239.255.255.250:3702 согласно спецификации WS-Discovery
 */
actual class WSDiscovery {
    private val multicastAddress = InetAddress.getByName("239.255.255.250")
    private val multicastPort = 3702
    private var socket: MulticastSocket? = null
    private val discoveredDevices = mutableSetOf<String>() // Для дедупликации по XAddrs

    actual suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
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
            logger.debug { "Probe message sent to ${multicastAddress}:${multicastPort}" }

            // Сбор ответов
            val responses = mutableListOf<DiscoveredDevice>()
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                try {
                    val buffer = ByteArray(8192) // Увеличенный буфер для больших ответов
                    val responsePacket = DatagramPacket(buffer, buffer.size)

                    val remainingTime = timeoutMillis - (System.currentTimeMillis() - startTime)
                    if (remainingTime <= 0) break

                    socket?.soTimeout = remainingTime.toInt().coerceAtLeast(100)
                    socket?.receive(responsePacket)

                    val responseXml = String(responsePacket.data, 0, responsePacket.length, StandardCharsets.UTF_8)
                    logger.debug { "Received response from ${responsePacket.address}:${responsePacket.port}" }

                    // Парсинг всех ProbeMatch из ответа
                    val devices = parseProbeMatches(responseXml)
                    for (device in devices) {
                        // Дедупликация по первому XAddr
                        val key = device.xAddrs.firstOrNull() ?: ""
                        if (key.isNotEmpty() && !discoveredDevices.contains(key)) {
                            discoveredDevices.add(key)
                            responses.add(device)
                            logger.info { "Discovered device: ${device.xAddrs.firstOrNull()} (types: ${device.types.joinToString()})" }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // Таймаут ожидания ответа - это нормально
                    break
                } catch (e: Exception) {
                    logger.warn(e) { "Error receiving response: ${e.message}" }
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
        <d:Probe>
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
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            factory.isIgnoringElementContentWhitespace = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(java.io.ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))

            val probeMatches = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2005/04/discovery", "ProbeMatches")
            if (probeMatches.length == 0) {
                // Попытка найти ProbeMatch напрямую
                return parseProbeMatchesFallback(xml)
            }

            val probeMatchesElement = probeMatches.item(0)
            val probeMatchNodes = probeMatchesElement.childNodes
            val devices = mutableListOf<DiscoveredDevice>()

            // Парсинг каждого ProbeMatch
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

            if (devices.isEmpty()) {
                parseProbeMatchesFallback(xml)
            } else {
                devices
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing ProbeMatches response, using fallback" }
            parseProbeMatchesFallback(xml)
        }
    }

    /**
     * Парсинг одного ProbeMatch узла
     */
    private fun parseProbeMatchNode(probeMatchNode: Node): DiscoveredDevice? {
        val xAddrs = mutableListOf<String>()
        val types = mutableListOf<String>()
        val scopes = mutableListOf<String>()

        val childNodes = probeMatchNode.childNodes
        for (i in 0 until childNodes.length) {
            val node = childNodes.item(i)
            if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                when {
                    node.localName == "XAddrs" || node.nodeName.contains("XAddrs") -> {
                        val text = node.textContent?.trim()
                        if (text != null && text.isNotEmpty()) {
                            xAddrs.addAll(text.split(" ").filter { it.isNotEmpty() })
                        }
                    }
                    node.localName == "Types" || node.nodeName.contains("Types") -> {
                        val text = node.textContent?.trim()
                        if (text != null && text.isNotEmpty()) {
                            types.addAll(text.split(" ").filter { it.isNotEmpty() })
                        }
                    }
                    node.localName == "Scopes" || node.nodeName.contains("Scopes") -> {
                        val text = node.textContent?.trim()
                        if (text != null && text.isNotEmpty()) {
                            scopes.addAll(text.split(" ").filter { it.isNotEmpty() })
                        }
                    }
                }
            }
        }

        return if (xAddrs.isNotEmpty()) {
            DiscoveredDevice(
                xAddrs = xAddrs,
                types = types,
                scopes = scopes
            )
        } else {
            null
        }
    }

    /**
     * Fallback парсинг через regex
     */
    private fun parseProbeMatchesFallback(xml: String): List<DiscoveredDevice> {
        val devices = mutableListOf<DiscoveredDevice>()

        // Поиск всех ProbeMatch блоков
        val probeMatchRegex = Regex("<[^:]*:ProbeMatch[^>]*>(.*?)</[^:]*:ProbeMatch>", RegexOption.DOT_MATCHES_ALL)
        val matches = probeMatchRegex.findAll(xml)

        for (match in matches) {
            val probeMatchXml = match.groupValues[1]
            val xAddrs = mutableListOf<String>()
            val types = mutableListOf<String>()
            val scopes = mutableListOf<String>()

            // Извлечение XAddrs
            val xAddrsRegex = Regex("<[^:]*:XAddrs[^>]*>([^<]+)</[^:]*:XAddrs>")
            val xAddrsMatch = xAddrsRegex.find(probeMatchXml)
            if (xAddrsMatch != null) {
                val text = xAddrsMatch.groupValues[1].trim()
                if (text.isNotEmpty()) {
                    xAddrs.addAll(text.split(" ").filter { it.isNotEmpty() })
                }
            }

            // Извлечение Types
            val typesRegex = Regex("<[^:]*:Types[^>]*>([^<]+)</[^:]*:Types>")
            val typesMatch = typesRegex.find(probeMatchXml)
            if (typesMatch != null) {
                val text = typesMatch.groupValues[1].trim()
                if (text.isNotEmpty()) {
                    types.addAll(text.split(" ").filter { it.isNotEmpty() })
                }
            }

            // Извлечение Scopes
            val scopesRegex = Regex("<[^:]*:Scopes[^>]*>([^<]+)</[^:]*:Scopes>")
            val scopesMatch = scopesRegex.find(probeMatchXml)
            if (scopesMatch != null) {
                val text = scopesMatch.groupValues[1].trim()
                if (text.isNotEmpty()) {
                    scopes.addAll(text.split(" ").filter { it.isNotEmpty() })
                }
            }

            if (xAddrs.isNotEmpty()) {
                devices.add(DiscoveredDevice(xAddrs = xAddrs, types = types, scopes = scopes))
            }
        }

        // Если не нашли через regex, попробуем простой поиск XAddrs
        if (devices.isEmpty()) {
            val xAddrsRegex = Regex("<[^:]*:XAddrs[^>]*>([^<]+)</[^:]*:XAddrs>")
            val xAddrsMatch = xAddrsRegex.find(xml)
            if (xAddrsMatch != null) {
                val text = xAddrsMatch.groupValues[1].trim()
                if (text.isNotEmpty()) {
                    val xAddrs = text.split(" ").filter { it.isNotEmpty() }
                    if (xAddrs.isNotEmpty()) {
                        devices.add(DiscoveredDevice(xAddrs = xAddrs, types = emptyList(), scopes = emptyList()))
                    }
                }
            }
        }

        return devices
    }

}


