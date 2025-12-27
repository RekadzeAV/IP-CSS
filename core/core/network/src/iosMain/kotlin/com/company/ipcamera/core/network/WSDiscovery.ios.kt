package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import platform.posix.*
import platform.Foundation.*
import kotlinx.cinterop.*
import platform.darwin.*

private val logger = KotlinLogging.logger {}

/**
 * iOS реализация WS-Discovery через POSIX sockets
 */
actual class WSDiscovery {
    private val multicastAddress = "239.255.255.250"
    private val multicastPort: UShort = 3702u
    private var socket: Int = -1
    private val discoveredDevices = mutableSetOf<String>() // Для дедупликации по XAddrs

    actual suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> = withContext(Dispatchers.Default) {
        memScoped {
            try {
                logger.info { "Starting WS-Discovery on iOS..." }

                // Создание UDP сокета
                socket = socket(AF_INET, SOCK_DGRAM, 0)
                if (socket < 0) {
                    logger.error { "Failed to create socket: ${strerror(errno)?.toKString()}" }
                    return@withContext emptyList()
                }

                // Настройка сокета для multicast
                val reuseAddr = alloc<IntVar>().apply { value = 1 }
                setsockopt(socket, SOL_SOCKET, SO_REUSEADDR, reuseAddr.ptr, sizeOf<IntVar>().convert())

                // Настройка TTL для multicast
                val ttl = alloc<UByteVar>().apply { value = 4u }
                setsockopt(socket, IPPROTO_IP, IP_MULTICAST_TTL, ttl.ptr, sizeOf<UByteVar>().convert())

                // Включение loopback для multicast (чтобы получать свои собственные пакеты)
                val loopback = alloc<IntVar>().apply { value = 1 }
                setsockopt(socket, IPPROTO_IP, IP_MULTICAST_LOOP, loopback.ptr, sizeOf<IntVar>().convert())

                // Присоединение к multicast группе
                val mreq = alloc<ip_mreqVar>().apply {
                    ptr.pointed.imr_multiaddr.s_addr = inet_addr(multicastAddress)
                    ptr.pointed.imr_interface.s_addr = INADDR_ANY
                }
                setsockopt(socket, IPPROTO_IP, IP_ADD_MEMBERSHIP, mreq.ptr, sizeOf<ip_mreqVar>().convert())

                // Настройка таймаута
                val timeout = alloc<timevalVar>().apply {
                    ptr.pointed.tv_sec = (timeoutMillis / 1000).convert()
                    ptr.pointed.tv_usec = ((timeoutMillis % 1000) * 1000).convert()
                }
                setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timevalVar>().convert())

                // Создание Probe сообщения
                val probeMessage = createProbeMessage()
                val probeBytes = probeMessage.encodeToByteArray()

                // Настройка адреса для отправки
                val multicastAddr = alloc<sockaddr_inVar>().apply {
                    ptr.pointed.sin_family = AF_INET.convert()
                    ptr.pointed.sin_port = htons(multicastPort)
                    ptr.pointed.sin_addr.s_addr = inet_addr(multicastAddress)
                }

                // Отправка Probe запроса
                val sendResult = probeBytes.usePinned { pinned ->
                    sendto(
                        socket,
                        pinned.addressOf(0),
                        probeBytes.size.convert(),
                        0,
                        multicastAddr.ptr.reinterpret(),
                        sizeOf<sockaddr_inVar>().convert()
                    )
                }

                if (sendResult < 0) {
                    logger.error { "Failed to send probe: ${strerror(errno)?.toKString()}" }
                    close()
                    return@withContext emptyList()
                }

                logger.debug { "Probe message sent" }

                // Сбор ответов
                val responses = mutableListOf<DiscoveredDevice>()
                val startTime = NSDate().timeIntervalSince1970 * 1000.0
                val buffer = ByteArray(4096)

                while ((NSDate().timeIntervalSince1970 * 1000.0 - startTime) < timeoutMillis) {
                    try {
                        val remainingTime = timeoutMillis - (NSDate().timeIntervalSince1970 * 1000.0 - startTime)
                        if (remainingTime <= 0) break

                        // Обновление таймаута
                        val remainingTimeout = alloc<timevalVar>().apply {
                            ptr.pointed.tv_sec = (remainingTime / 1000).convert()
                            ptr.pointed.tv_usec = ((remainingTime % 1000) * 1000).convert()
                        }
                        setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, remainingTimeout.ptr, sizeOf<timevalVar>().convert())

                        val fromAddr = alloc<sockaddr_inVar>()
                        val fromLen = alloc<platform.posix.socklen_tVar>().apply { value = sizeOf<sockaddr_inVar>().convert() }

                        val received = buffer.usePinned { pinned ->
                            recvfrom(
                                socket,
                                pinned.addressOf(0),
                                buffer.size.convert(),
                                0,
                                fromAddr.ptr,
                                fromLen.ptr
                            )
                        }

                        if (received > 0) {
                            val responseXml = buffer.sliceArray(0 until received.toInt()).decodeToString()
                            logger.debug { "Received response from ${inet_ntoa(fromAddr.ptr.pointed.sin_addr)?.toKString()}:${ntohs(fromAddr.ptr.pointed.sin_port)}" }

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
                        } else if (received == 0) {
                            // Соединение закрыто
                            break
                        } else {
                            val error = errno
                            if (error == EAGAIN || error == EWOULDBLOCK) {
                                // Таймаут - это нормально
                                break
                            } else {
                                logger.warn { "Error receiving response: ${strerror(error)?.toKString()}" }
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn(e) { "Error receiving response" }
                    }
                }

                logger.info { "WS-Discovery completed. Found ${responses.size} devices" }
                responses.toList()
            } catch (e: Exception) {
                logger.error(e) { "Error during WS-Discovery: ${e.message}" }
                emptyList()
            } finally {
                // Покидание multicast группы перед закрытием сокета
                if (socket >= 0) {
                    try {
                        val mreq = alloc<ip_mreqVar>().apply {
                            ptr.pointed.imr_multiaddr.s_addr = inet_addr(multicastAddress)
                            ptr.pointed.imr_interface.s_addr = INADDR_ANY
                        }
                        setsockopt(socket, IPPROTO_IP, IP_DROP_MEMBERSHIP, mreq.ptr, sizeOf<ip_mreqVar>().convert())
                    } catch (e: Exception) {
                        logger.debug(e) { "Error leaving multicast group" }
                    }
                }
                close()
            }
        }
    }

    actual fun close() {
        if (socket >= 0) {
            platform.posix.close(socket)
            socket = -1
        }
        discoveredDevices.clear()
    }

    /**
     * Создать SOAP Probe запрос
     */
    private fun createProbeMessage(): String {
        val messageId = "urn:uuid:${NSUUID().UUIDString}"
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
     * Использует регулярные выражения для парсинга XML
     */
    private fun parseProbeMatches(xml: String): List<DiscoveredDevice> {
        return try {
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

            devices
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing ProbeMatches response" }
            emptyList()
        }
    }
}


