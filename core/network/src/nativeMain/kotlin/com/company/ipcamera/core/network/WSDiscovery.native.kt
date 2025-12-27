package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import kotlinx.cinterop.*
import platform.posix.*
import platform.darwin.*

private val logger = KotlinLogging.logger {}

/**
 * Native реализация WS-Discovery через POSIX sockets
 * Поддерживает Linux, macOS и другие POSIX-совместимые системы
 */
actual class WSDiscovery {
    private val multicastAddress = "239.255.255.250"
    private val multicastPort: UShort = 3702u
    private var socket: Int = -1
    private val discoveredDevices = mutableSetOf<String>() // Для дедупликации по XAddrs

    actual suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> = withContext(Dispatchers.Default) {
        memScoped {
            try {
                logger.info { "Starting WS-Discovery on Native platform..." }

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

                // Включение loopback для multicast
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

                // Отправка Probe запроса (несколько раз для надежности)
                repeat(2) { attempt ->
                    if (attempt > 0) {
                        delay(500)
                    }
                    probeBytes.usePinned { pinned ->
                        sendto(
                            socket,
                            pinned.addressOf(0),
                            probeBytes.size.convert(),
                            0,
                            multicastAddr.ptr.reinterpret(),
                            sizeOf<sockaddr_inVar>().convert()
                        )
                    }
                    logger.debug { "Probe message sent (attempt ${attempt + 1})" }
                }

                // Сбор ответов
                val responses = mutableListOf<DiscoveredDevice>()
                val startTime = getCurrentTimeMillis()
                val buffer = ByteArray(16384) // 16KB буфер

                while ((getCurrentTimeMillis() - startTime) < timeoutMillis) {
                    try {
                        val remainingTime = timeoutMillis - (getCurrentTimeMillis() - startTime)
                        if (remainingTime <= 0) break

                        // Обновление таймаута
                        val remainingTimeout = alloc<timevalVar>().apply {
                            ptr.pointed.tv_sec = (remainingTime / 1000).convert()
                            ptr.pointed.tv_usec = ((remainingTime % 1000) * 1000).convert()
                        }
                        setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, remainingTimeout.ptr, sizeOf<timevalVar>().convert())

                        val fromAddr = alloc<sockaddr_inVar>()
                        val fromLen = alloc<socklen_tVar>().apply { value = sizeOf<sockaddr_inVar>().convert() }

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
                            logger.debug {
                                "Received response from ${inet_ntoa(fromAddr.ptr.pointed.sin_addr)?.toKString()}:${ntohs(fromAddr.ptr.pointed.sin_port)}"
                            }

                            // Парсинг всех ProbeMatch из ответа
                            val devices = parseProbeMatches(responseXml)
                            for (device in devices) {
                                // Дедупликация по первому XAddr
                                val key = device.xAddrs.firstOrNull() ?: ""
                                if (key.isNotEmpty() && !discoveredDevices.contains(key)) {
                                    discoveredDevices.add(key)
                                    responses.add(device)
                                    logger.info {
                                        "Discovered device: ${device.xAddrs.firstOrNull()} " +
                                        "(types: ${device.types.take(3).joinToString()}, " +
                                        "xAddrs: ${device.xAddrs.size})"
                                    }
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
     * Получить текущее время в миллисекундах
     */
    private fun getCurrentTimeMillis(): Long {
        memScoped {
            val tv = alloc<timevalVar>()
            gettimeofday(tv.ptr, null)
            return tv.ptr.pointed.tv_sec * 1000L + tv.ptr.pointed.tv_usec / 1000L
        }
    }

    /**
     * Создать SOAP Probe запрос
     */
    private fun createProbeMessage(): String {
        val messageId = "urn:uuid:${generateUUID()}"
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
     * Генерация UUID (упрощенная версия)
     */
    private fun generateUUID(): String {
        // Упрощенная генерация UUID для нативных платформ
        val random = kotlin.random.Random
        return buildString {
            repeat(8) { append(random.nextInt(0, 16).toString(16)) }
            append("-")
            repeat(4) { append(random.nextInt(0, 16).toString(16)) }
            append("-")
            repeat(4) { append(random.nextInt(0, 16).toString(16)) }
            append("-")
            repeat(4) { append(random.nextInt(0, 16).toString(16)) }
            append("-")
            repeat(12) { append(random.nextInt(0, 16).toString(16)) }
        }
    }

    /**
     * Парсинг всех ProbeMatch из ProbeMatches ответа
     * Использует регулярные выражения для парсинга XML
     */
    private fun parseProbeMatches(xml: String): List<DiscoveredDevice> {
        return try {
            val devices = mutableListOf<DiscoveredDevice>()

            // Поиск всех ProbeMatch блоков
            val probeMatchRegex = Regex(
                "<[^:]*:ProbeMatch[^>]*>(.*?)</[^:]*:ProbeMatch>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
            )
            val matches = probeMatchRegex.findAll(xml)

            for (match in matches) {
                val probeMatchXml = match.groupValues.getOrNull(1) ?: continue
                val xAddrs = mutableListOf<String>()
                val types = mutableListOf<String>()
                val scopes = mutableListOf<String>()

                // Извлечение XAddrs
                val xAddrsRegex = Regex("<[^:]*:XAddrs[^>]*>([^<]+)</[^:]*:XAddrs>", RegexOption.MULTILINE)
                val xAddrsMatches = xAddrsRegex.findAll(probeMatchXml)
                for (xMatch in xAddrsMatches) {
                    val text = xMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        xAddrs.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Извлечение Types
                val typesRegex = Regex("<[^:]*:Types[^>]*>([^<]+)</[^:]*:Types>", RegexOption.MULTILINE)
                val typesMatches = typesRegex.findAll(probeMatchXml)
                for (tMatch in typesMatches) {
                    val text = tMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        types.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Извлечение Scopes
                val scopesRegex = Regex("<[^:]*:Scopes[^>]*>([^<]+)</[^:]*:Scopes>", RegexOption.MULTILINE)
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
                val xAddrsRegex = Regex("<[^:]*:XAddrs[^>]*>([^<]+)</[^:]*:XAddrs>", RegexOption.MULTILINE)
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

            devices
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing ProbeMatches response" }
            emptyList()
        }
    }
}

