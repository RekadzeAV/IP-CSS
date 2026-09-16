package com.company.ipcamera.core.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import java.net.InetAddress
import java.net.Socket

private val logger = KotlinLogging.logger {}

/**
 * Сканирование подсети для обнаружения IP камер
 */
class NetworkScanner(
    private val httpClient: HttpClient,
    private val discoveryTimeout: Long = 5000
) {
    /**
     * Сканировать подсеть на наличие камер
     * @param subnetRange Например "192.168.1.0/24"
     * @param ports Порты для проверки (554, 80, 8080)
     * @param timeoutPerHost Таймаут на каждый хост
     * @param maxConcurrent Максимальное количество одновременных сканов
     */
    suspend fun scanSubnet(
        subnetRange: String,
        ports: List<Int> = listOf(554, 80, 8080),
        timeoutPerHost: Long = 1000,
        maxConcurrent: Int = 50
    ): List<DiscoveredCamera> {
        logger.info { "Starting subnet scan: $subnetRange, ports: $ports" }

        val ips = parseSubnet(subnetRange)
        logger.info { "Found ${ips.size} IP addresses to scan" }

        val discoveredCameras = mutableListOf<DiscoveredCamera>()

        try {
            ips.chunked(maxConcurrent).forEach { batch ->
                val results = batch.map { ip ->
                    scanHost(ip, ports, timeoutPerHost)
                }

                results.forEach { camera ->
                    if (camera != null) {
                        discoveredCameras.add(camera)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during subnet scan: ${e.message}" }
        }

        logger.info { "Subnet scan completed. Found ${discoveredCameras.size} cameras" }
        return discoveredCameras
    }

    /**
     * Сканировать подсеть с прогресс-индикатором
     */
    fun scanSubnetWithProgress(
        subnetRange: String,
        ports: List<Int> = listOf(554, 80, 8080),
        timeoutPerHost: Long = 1000,
        maxConcurrent: Int = 50
    ): Flow<NetworkScanProgress> = flow {
        val ips = parseSubnet(subnetRange)
        val totalHosts = ips.size
        var scannedCount = 0
        val discoveredCameras = mutableListOf<DiscoveredCamera>()
        val scanStartTime = System.currentTimeMillis()

        emit(
            NetworkScanProgress(
                currentActivity = "Initializing scan...",
                progress = 0f,
                hostsScanned = 0,
                hostsTotal = totalHosts,
                camerasFound = 0,
                discoveredCameras = emptyList(),
                elapsedMs = 0L
            )
        )

        try {
            ips.chunked(maxConcurrent).forEach { batch ->
                batch.forEach { ip ->
                    val result = runBlocking { scanHost(ip, ports, timeoutPerHost) }
                    scannedCount++

                    if (result != null) {
                        discoveredCameras.add(result)
                    }

                    emit(
                        NetworkScanProgress(
                            currentActivity = "Scanning $ip...",
                            progress = scannedCount.toFloat() / totalHosts,
                            hostsScanned = scannedCount,
                            hostsTotal = totalHosts,
                            camerasFound = discoveredCameras.size,
                            discoveredCameras = discoveredCameras.toList(),
                            elapsedMs = System.currentTimeMillis() - scanStartTime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during subnet scan: ${e.message}" }
        }

        emit(
            NetworkScanProgress(
                currentActivity = "Scan completed",
                progress = 1f,
                hostsScanned = totalHosts,
                hostsTotal = totalHosts,
                camerasFound = discoveredCameras.size,
                discoveredCameras = discoveredCameras.toList(),
                elapsedMs = System.currentTimeMillis() - scanStartTime
            )
        )
    }

    /**
     * Быстрое сканирование только RTSP портов
     */
    suspend fun quickRtspScan(
        subnetRange: String,
        timeoutPerHost: Long = 500
    ): List<String> {
        val ips = parseSubnet(subnetRange)
        val rtspUrls = mutableListOf<String>()

        ips.parallelMap { ip ->
            try {
                withTimeout(timeoutPerHost) {
                    val socket = Socket()
                    try {
                        socket.connect(
                            java.net.InetSocketAddress(ip, 554),
                            timeoutPerHost.toInt()
                        )
                        rtspUrls.add("rtsp://$ip:554")
                    } finally {
                        socket.close()
                    }
                }
            } catch (e: Exception) {
                // Игнорируем ошибки подключения
            }
        }

        return rtspUrls
    }

    /**
     * Проверить конкретный хост на наличие камеры
     */
    private suspend fun scanHost(
        ip: String,
        ports: List<Int>,
        timeoutMs: Long
    ): DiscoveredCamera? {
        for (port in ports) {
            try {
                val isRtsp = isRtspPort(ip, port, timeoutMs)
                if (isRtsp) {
                    // Попытка получить информацию о камере через ONVIF
                    val cameraInfo = tryDiscoverCamera(ip, port)
                    if (cameraInfo != null) {
                        return cameraInfo
                    }

                    // Если ONVIF не доступен, возвращаем базовую информацию
                    return DiscoveredCamera(
                        url = "rtsp://$ip:$port",
                        name = "Camera at $ip:$port"
                    )
                }
            } catch (e: Exception) {
                logger.debug { "Error scanning $ip:$port: ${e.message}" }
            }
        }
        return null
    }

    /**
     * Проверить, является ли порт RTSP
     */
    private suspend fun isRtspPort(
        ip: String,
        port: Int,
        timeoutMs: Long
    ): Boolean {
        return try {
            withTimeout(timeoutMs) {
                val socket = Socket()
                try {
                    socket.connect(
                        java.net.InetSocketAddress(ip, port),
                        timeoutMs.toInt()
                    )

                    // Отправляем RTSP OPTIONS запрос
                    val optionsRequest = "OPTIONS rtsp://$ip:$port RTSP/1.0\r\n" +
                        "CSeq: 1\r\n" +
                        "\r\n"

                    socket.getOutputStream().write(optionsRequest.toByteArray())
                    socket.getOutputStream().flush()

                    // Читаем ответ
                    val inputStream = socket.getInputStream()
                    val response = ByteArray(1024)
                    val bytesRead = inputStream.read(response, 0, 1024)

                    if (bytesRead > 0) {
                        val responseStr = String(response, 0, bytesRead)
                        // Проверяем на наличие RTSP в ответе
                        responseStr.contains("RTSP", ignoreCase = true)
                    } else {
                        false
                    }
                } finally {
                    socket.close()
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Попытаться обнаружить камеру через ONVIF
     */
    private suspend fun tryDiscoverCamera(
        ip: String,
        port: Int
    ): DiscoveredCamera? {
        return try {
            val url = "http://$ip:$port"

            // Пробуем получить информацию о устройстве
            val response = try {
                httpClient.get("$url/onvif/device_service")
            } catch (e: Exception) {
                null
            }

            if (response?.status?.value == 401 || response?.status?.value == 200) {
                // Устройство отвечает, вероятно это камера
                DiscoveredCamera(
                    url = "rtsp://$ip:554",
                    name = "ONVIF Camera at $ip"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            logger.debug { "Error discovering camera at $ip:$port: ${e.message}" }
            null
        }
    }

    /**
     * Парсинг диапазона подсети
     */
    internal fun parseSubnet(subnetRange: String): List<String> {
        return try {
            val (network, prefix) = subnetRange.split("/")
            val prefixLength = prefix.toInt()

            val baseIp = InetAddress.getByName(network).address
            val networkInt = bytesToInt(baseIp)
            val hostMask = (-1 shl (32 - prefixLength)).toLong()
            val numHosts = (hostMask.inv() and 0xFFFFFFFFL).toInt()

            val ips = mutableListOf<String>()
            for (i in 1 until numHosts) {
                val ipInt = ((networkInt.toLong() and hostMask) or i.toLong()).toInt()
                ips.add(intToIp(ipInt))
            }

            ips
        } catch (e: Exception) {
            logger.error(e) { "Error parsing subnet: $subnetRange" }
            emptyList()
        }
    }

    /**
     * Конвертация байтов IP в int
     */
    private fun bytesToInt(bytes: ByteArray): Int {
        return ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)
    }

    /**
     * Конвертация int в IP адрес
     */
    private fun intToIp(ipInt: Int): String {
        return "${(ipInt shr 24) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 8) and 0xFF}.${ipInt and 0xFF}"
    }

    /**
     * Параллельное отображение с ограничением
     */
    private suspend fun <A, B> Iterable<A>.parallelMap(
        concurrency: Int = 10,
        transform: suspend (A) -> B
    ): List<B> = coroutineScope {
        this@parallelMap.chunked(concurrency).flatMap { batch ->
            batch.map { async { transform(it) } }
        }.awaitAll()
    }
}

/**
 * Прогресс сканирования подсети
 */
data class NetworkScanProgress(
    val currentActivity: String,
    val progress: Float, // 0.0-1.0
    val hostsScanned: Int,
    val hostsTotal: Int,
    val camerasFound: Int,
    val discoveredCameras: List<DiscoveredCamera>,
    /** Время с начала сканирования (мс). null — старые эмиттеры без таймингов. */
    val elapsedMs: Long? = null
)
