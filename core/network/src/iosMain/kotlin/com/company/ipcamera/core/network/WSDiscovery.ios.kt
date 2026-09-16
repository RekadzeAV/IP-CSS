package com.company.ipcamera.core.network

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import platform.Foundation.*
import platform.darwin.*
import platform.posix.*

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

                // Отправка Probe запроса с адаптивной стратегией
                val probeStrategies = listOf(
                    Pair(0L, createProbeMessage()), // Первый запрос: широкий поиск (немедленно)
                    Pair(500L, createProbeMessage()), // Второй запрос: через 500ms
                    Pair(1000L, createProbeMessage()), // Третий запрос: через 1000ms
                    Pair(1500L, createProbeMessage()) // Четвертый запрос: через 1500ms
                )

                var lastResponseTime = startTime
                for ((delayMs, probeMessage) in probeStrategies) {
                    if (delayMs > 0) {
                        delay(delayMs)
                    }

                    // Проверяем, есть ли уже достаточно ответов
                    val currentTime = NSDate().timeIntervalSince1970 * 1000.0
                    if (responses.isNotEmpty() && (currentTime - lastResponseTime) > 1000) {
                        logger.debug { "Enough responses received, skipping remaining probe requests" }
                        break
                    }

                    val probeBytes = probeMessage.encodeToByteArray()
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
                        logger.warn { "Failed to send probe (delay: ${delayMs}ms): ${strerror(errno)?.toKString()}" }
                        // Продолжаем с другими запросами
                    } else {
                        logger.debug { "Probe message sent (delay: ${delayMs}ms)" }
                    }
                }

                // Сбор ответов
                val responses = mutableListOf<DiscoveredDevice>()
                val startTime = NSDate().timeIntervalSince1970 * 1000.0
                var lastResponseTime = startTime
                val buffer = ByteArray(16384) // Увеличенный буфер для больших ответов (16KB)

                while ((NSDate().timeIntervalSince1970 * 1000.0 - startTime) < timeoutMillis) {
                    try {
                        val remainingTime = timeoutMillis - (NSDate().timeIntervalSince1970 * 1000.0 - startTime)
                        if (remainingTime <= 0) break

                        // Обновление таймаута
                        val remainingTimeout = alloc<timevalVar>().apply {
                            ptr.pointed.tv_sec = (remainingTime / 1000).convert()
                            ptr.pointed.tv_usec = ((remainingTime % 1000) * 1000).convert()
                        }
                        setsockopt(
                            socket,
                            SOL_SOCKET,
                            SO_RCVTIMEO,
                            remainingTimeout.ptr,
                            sizeOf<timevalVar>().convert()
                        )

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
                            logger.debug {
                                "Received response from ${inet_ntoa(fromAddr.ptr.pointed.sin_addr)?.toKString()}:${ntohs(
                                    fromAddr.ptr.pointed.sin_port
                                )}"
                            }

                            // Парсинг всех ProbeMatch из ответа
                            val devices = parseProbeMatches(responseXml)
                            for (device in devices) {
                                // Дедупликация по всем XAddrs
                                var found = false
                                for (xAddr in device.xAddrs) {
                                    if (xAddr.isNotEmpty() && !discoveredDevices.contains(xAddr)) {
                                        discoveredDevices.add(xAddr)
                                        found = true
                                    }
                                }

                                if (found && device.xAddrs.isNotEmpty()) {
                                    responses.add(device)
                                    lastResponseTime = NSDate().timeIntervalSince1970 * 1000.0
                                    logger.info {
                                        "Discovered device: ${device.xAddrs.firstOrNull()} " +
                                            "(types: ${device.types.take(3).joinToString()}, " +
                                            "xAddrs: ${device.xAddrs.size})"
                                    }
                                }
                            }

                            // Проверка на раннее прекращение, если нет ответов долгое время
                            val timeSinceLastResponse = NSDate().timeIntervalSince1970 * 1000.0 - lastResponseTime
                            if (responses.isNotEmpty() && timeSinceLastResponse > 2000) {
                                logger.debug { "No responses for ${timeSinceLastResponse}ms, stopping discovery" }
                                break
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
     * Использует улучшенные регулярные выражения для парсинга XML с поддержкой различных форматов
     */
    private fun parseProbeMatches(xml: String): List<DiscoveredDevice> {
        return try {
            // Нормализация XML - удаляем BOM и лишние пробелы
            val normalizedXml = xml.trim().replace("\uFEFF", "")
            val devices = mutableListOf<DiscoveredDevice>()

            // Улучшенный regex для поиска ProbeMatch блоков (поддерживает разные форматы namespace)
            val probeMatchRegex = Regex(
                "<[^>]*:?ProbeMatch[^>]*>(.*?)</[^>]*:?ProbeMatch>",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)
            )
            val matches = probeMatchRegex.findAll(normalizedXml)

            for (match in matches) {
                val probeMatchXml = match.groupValues.getOrNull(1) ?: continue
                val xAddrs = mutableListOf<String>()
                val types = mutableListOf<String>()
                val scopes = mutableListOf<String>()

                // Извлечение XAddrs (более гибкий regex с поддержкой разных namespaces)
                val xAddrsRegex = Regex("<[^>]*:?XAddrs[^>]*>([^<]+)</[^>]*:?XAddrs>", RegexOption.MULTILINE)
                val xAddrsMatches = xAddrsRegex.findAll(probeMatchXml)
                for (xMatch in xAddrsMatches) {
                    val text = xMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        // Разделяем по пробелам и фильтруем пустые строки
                        xAddrs.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Извлечение Types (более гибкий regex)
                val typesRegex = Regex("<[^>]*:?Types[^>]*>([^<]+)</[^>]*:?Types>", RegexOption.MULTILINE)
                val typesMatches = typesRegex.findAll(probeMatchXml)
                for (tMatch in typesMatches) {
                    val text = tMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        types.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Извлечение Scopes (более гибкий regex)
                val scopesRegex = Regex("<[^>]*:?Scopes[^>]*>([^<]+)</[^>]*:?Scopes>", RegexOption.MULTILINE)
                val scopesMatches = scopesRegex.findAll(probeMatchXml)
                for (sMatch in scopesMatches) {
                    val text = sMatch.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        scopes.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Валидация и очистка данных
                val validXAddrs = xAddrs
                    .distinct()
                    .filter { it.isNotBlank() }
                    .filter { url ->
                        // Базовая валидация URL (проверка на http/https)
                        url.startsWith("http://", ignoreCase = true) ||
                            url.startsWith("https://", ignoreCase = true)
                    }

                if (validXAddrs.isNotEmpty()) {
                    devices.add(
                        DiscoveredDevice(
                            xAddrs = validXAddrs,
                            types = types.distinct().filter { it.isNotBlank() },
                            scopes = scopes.distinct().filter { it.isNotBlank() }
                        )
                    )
                }
            }

            // Если не нашли через ProbeMatch блоки, ищем XAddrs напрямую в документе
            if (devices.isEmpty()) {
                val xAddrsRegex = Regex("<[^>]*:?XAddrs[^>]*>([^<]+)</[^>]*:?XAddrs>", RegexOption.MULTILINE)
                val xAddrsMatches = xAddrsRegex.findAll(normalizedXml)
                val allXAddrs = mutableListOf<String>()

                for (match in xAddrsMatches) {
                    val text = match.groupValues.getOrNull(1)?.trim()
                    if (text != null && text.isNotEmpty()) {
                        allXAddrs.addAll(text.split(Regex("\\s+")).filter { it.isNotEmpty() && it.isNotBlank() })
                    }
                }

                // Валидация URL
                val validXAddrs = allXAddrs
                    .distinct()
                    .filter { it.isNotBlank() }
                    .filter { url ->
                        url.startsWith("http://", ignoreCase = true) ||
                            url.startsWith("https://", ignoreCase = true)
                    }

                if (validXAddrs.isNotEmpty()) {
                    devices.add(
                        DiscoveredDevice(
                            xAddrs = validXAddrs,
                            types = emptyList(),
                            scopes = emptyList()
                        )
                    )
                }
            }

            devices
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing ProbeMatches response: ${e.message}" }
            emptyList()
        }
    }
}
