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
 * JVM реализация WS-Discovery через Java NIO
 */
actual class WSDiscovery {
    private val multicastAddress = InetAddress.getByName("239.255.255.250")
    private val multicastPort = 3702
    private var socket: DatagramSocket? = null
    private val discoveredDevices = mutableSetOf<String>() // Для дедупликации по XAddrs

    actual suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Starting WS-Discovery on JVM..." }

            socket = DatagramSocket().apply {
                soTimeout = timeoutMillis.toInt()
                reuseAddress = true
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

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                try {
                    val buffer = ByteArray(4096)
                    val responsePacket = DatagramPacket(buffer, buffer.size)

                    val remainingTime = timeoutMillis - (System.currentTimeMillis() - startTime)
                    if (remainingTime <= 0) break

                    socket?.soTimeout = remainingTime.toInt().coerceAtLeast(100)
                    socket?.receive(responsePacket)

                    val responseXml = String(responsePacket.data, 0, responsePacket.length, StandardCharsets.UTF_8)
                    logger.debug { "Received response from ${responsePacket.address}" }

                    val device = parseProbeMatch(responseXml)
                    if (device != null) {
                        // Дедупликация по первому XAddr
                        val key = device.xAddrs.firstOrNull() ?: ""
                        if (key.isNotEmpty() && !discoveredDevices.contains(key)) {
                            discoveredDevices.add(key)
                            responses.add(device)
                            logger.info { "Discovered device: ${device.xAddrs.firstOrNull()}" }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // Таймаут ожидания ответа - это нормально
                    break
                } catch (e: Exception) {
                    logger.warn(e) { "Error receiving response" }
                }
            }

            logger.info { "WS-Discovery completed. Found ${responses.size} devices" }
            responses.toList()
        } catch (e: Exception) {
            logger.error(e) { "Error during WS-Discovery" }
            emptyList()
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
     * Парсинг ProbeMatches ответа
     */
    private fun parseProbeMatch(xml: String): DiscoveredDevice? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(java.io.ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))

            val probeMatches = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2005/04/discovery", "ProbeMatches")
            if (probeMatches.length == 0) return null

            val probeMatch = probeMatches.item(0)
            val xAddrs = mutableListOf<String>()
            val types = mutableListOf<String>()
            val scopes = mutableListOf<String>()

            // Извлечение XAddrs
            val xAddrNodes = probeMatch.childNodes
            for (i in 0 until xAddrNodes.length) {
                val node = xAddrNodes.item(i)
                if (node.nodeName.contains("XAddrs") || node.localName == "XAddrs") {
                    val text = node.textContent?.trim()
                    if (text != null && text.isNotEmpty()) {
                        xAddrs.addAll(text.split(" ").filter { it.isNotEmpty() })
                    }
                }
                if (node.nodeName.contains("Types") || node.localName == "Types") {
                    val text = node.textContent?.trim()
                    if (text != null && text.isNotEmpty()) {
                        types.addAll(text.split(" ").filter { it.isNotEmpty() })
                    }
                }
                if (node.nodeName.contains("Scopes") || node.localName == "Scopes") {
                    val text = node.textContent?.trim()
                    if (text != null && text.isNotEmpty()) {
                        scopes.addAll(text.split(" ").filter { it.isNotEmpty() })
                    }
                }
            }

            // Альтернативный способ парсинга через getTextContent
            if (xAddrs.isEmpty()) {
                val xAddrsText = getElementText(doc, "XAddrs")
                if (xAddrsText.isNotEmpty()) {
                    xAddrs.addAll(xAddrsText.split(" ").filter { it.isNotEmpty() })
                }
            }

            if (xAddrs.isNotEmpty()) {
                DiscoveredDevice(
                    xAddrs = xAddrs,
                    types = types,
                    scopes = scopes
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing ProbeMatch response" }
            // Fallback: попытка извлечь XAddrs через regex
            val xAddrsRegex = Regex("<[^:]*:XAddrs[^>]*>([^<]+)</[^:]*:XAddrs>")
            val xAddrsMatch = xAddrsRegex.find(xml)
            val xAddrs = xAddrsMatch?.groupValues?.get(1)?.trim()?.split(" ")?.filter { it.isNotEmpty() } ?: emptyList()

            if (xAddrs.isNotEmpty()) {
                DiscoveredDevice(xAddrs = xAddrs, types = emptyList(), scopes = emptyList())
            } else {
                null
            }
        }
    }

    private fun getElementText(doc: Document, tagName: String): String {
        val nodes = doc.getElementsByTagName("*")
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.localName == tagName || node.nodeName.contains(tagName)) {
                return node.textContent?.trim() ?: ""
            }
        }
        return ""
    }
}

