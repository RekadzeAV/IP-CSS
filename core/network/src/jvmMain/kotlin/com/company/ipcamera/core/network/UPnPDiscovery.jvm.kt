package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.net.*
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

/**
 * JVM реализация UPnP Discovery (внутренняя реализация для desktop/jvm).
 * Использует SSDP на 239.255.255.250:1900
 */
open class UPnPDiscoveryJvmImpl {
    private val multicastAddress = InetAddress.getByName("239.255.255.250")
    private val multicastPort = 1900
    private var socket: MulticastSocket? = null
    private val discoveredDevices = mutableSetOf<String>() // Для дедупликации по USN

    open suspend fun discover(timeoutMillis: Long): List<UPnPDevice> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Starting UPnP discovery on JVM..." }

            // Создание MulticastSocket для работы с multicast
            socket = MulticastSocket().apply {
                soTimeout = timeoutMillis.toInt()
                reuseAddress = true
                timeToLive = 4
            }

            // Присоединение к multicast группе
            try {
                socket?.joinGroup(InetSocketAddress(multicastAddress, multicastPort), getNetworkInterface())
                logger.debug { "Joined UPnP multicast group on network interface" }
            } catch (e: Exception) {
                try {
                    socket?.joinGroup(multicastAddress)
                    logger.debug { "Joined UPnP multicast group (fallback)" }
                } catch (e2: Exception) {
                    logger.warn(e2) { "Failed to join UPnP multicast group, continuing anyway" }
                }
            }

            // Создание M-SEARCH запроса
            val mSearchMessage = createMSearchMessage()
            val mSearchBytes = mSearchMessage.toByteArray(StandardCharsets.UTF_8)

            // Отправка M-SEARCH запроса
            val mSearchPacket = DatagramPacket(
                mSearchBytes,
                mSearchBytes.size,
                multicastAddress,
                multicastPort
            )
            socket?.send(mSearchPacket)
            logger.debug { "M-SEARCH message sent to $multicastAddress:$multicastPort" }

            // Сбор ответов
            val responses = mutableListOf<UPnPDevice>()
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                try {
                    val buffer = ByteArray(4096)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet)

                    val response = String(packet.data, 0, packet.length, StandardCharsets.UTF_8)
                    logger.debug { "Received UPnP response from ${packet.address}:${packet.port}" }

                    val device = parseUPnPResponse(response)
                    if (device != null) {
                        // Дедупликация по USN
                        if (!discoveredDevices.contains(device.usn)) {
                            discoveredDevices.add(device.usn)
                            responses.add(device)
                            logger.debug { "Added UPnP device: ${device.usn}" }
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    // Таймаут - это нормально, продолжаем сбор ответов
                    break
                } catch (e: Exception) {
                    logger.warn(e) { "Error receiving UPnP response" }
                }
            }

            logger.info { "UPnP discovery completed. Found ${responses.size} devices" }
            responses
        } catch (e: Exception) {
            logger.error(e) { "Error during UPnP discovery" }
            emptyList()
        }
    }

    open fun close() {
        try {
            socket?.leaveGroup(multicastAddress)
            socket?.close()
            socket = null
            discoveredDevices.clear()
            logger.debug { "UPnP discovery socket closed" }
        } catch (e: Exception) {
            val isExpectedLeaveGroupNoise =
                e is SocketException && (e.message?.contains("Not a member of group", ignoreCase = true) == true)
            if (isExpectedLeaveGroupNoise) {
                logger.debug { "UPnP socket already left multicast group, skipping duplicate leaveGroup" }
            } else {
                logger.warn(e) { "Error closing UPnP discovery socket" }
            }
        }
    }

    /**
     * Создает M-SEARCH запрос для UPnP
     */
    private fun createMSearchMessage(): String {
        return buildString {
            appendLine("M-SEARCH * HTTP/1.1")
            appendLine("HOST: 239.255.255.250:1900")
            appendLine("MAN: \"ssdp:discover\"")
            appendLine("ST: urn:schemas-upnp-org:device:MediaServer:1") // Ищем медиа-серверы (камеры)
            appendLine("MX: 3") // Максимальное время ожидания ответа (секунды)
            appendLine() // Пустая строка для завершения заголовков
        }
    }

    /**
     * Парсит UPnP ответ
     */
    private fun parseUPnPResponse(response: String): UPnPDevice? {
        try {
            val lines = response.lines()
            if (lines.isEmpty()) return null

            // Проверяем, что это валидный HTTP ответ
            val statusLine = lines[0]
            if (!statusLine.contains("HTTP/1.1") && !statusLine.contains("HTTP/1.0")) {
                return null
            }

            var location: String? = null
            var server: String? = null
            var usn: String? = null
            var st: String? = null
            var cacheControl: String? = null

            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) break

                when {
                    line.startsWith("LOCATION:", ignoreCase = true) -> {
                        location = line.substringAfter(":").trim()
                    }
                    line.startsWith("SERVER:", ignoreCase = true) -> {
                        server = line.substringAfter(":").trim()
                    }
                    line.startsWith("USN:", ignoreCase = true) -> {
                        usn = line.substringAfter(":").trim()
                    }
                    line.startsWith("ST:", ignoreCase = true) -> {
                        st = line.substringAfter(":").trim()
                    }
                    line.startsWith("CACHE-CONTROL:", ignoreCase = true) -> {
                        cacheControl = line.substringAfter(":").trim()
                    }
                }
            }

            if (location != null && usn != null) {
                return UPnPDevice(
                    location = location,
                    server = server ?: "Unknown",
                    usn = usn,
                    st = st ?: "Unknown",
                    cacheControl = cacheControl
                )
            }
        } catch (e: Exception) {
            logger.warn(e) { "Error parsing UPnP response" }
        }
        return null
    }

    /**
     * Получает сетевой интерфейс для multicast
     */
    private fun getNetworkInterface(): NetworkInterface? {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .filter { it.supportsMulticast() }
                .firstOrNull()
        } catch (e: Exception) {
            logger.warn(e) { "Error getting network interface" }
            null
        }
    }
}

actual class UPnPDiscovery actual constructor() : UPnPDiscoveryJvmImpl() {
    actual override suspend fun discover(timeoutMillis: Long): List<UPnPDevice> = super.discover(timeoutMillis)
    actual override fun close() = super.close()
}
