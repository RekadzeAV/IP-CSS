package com.company.ipcamera.core.network.utils

import com.company.ipcamera.core.network.DiscoveredCamera
import com.company.ipcamera.core.network.NetworkScanProgress
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import java.net.InetAddress

private val logger = KotlinLogging.logger {}

/**
 * Утилиты для работы с NetworkScanner и результатами сканирования
 */
object ScannerUtils {

    /**
     * Парсинг диапазона подсети в список IP адресов
     */
    fun parseSubnetToIps(subnetRange: String): List<String> {
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
            logger.error(e) { "Ошибка парсинга подсети: $subnetRange" }
            emptyList()
        }
    }

    /**
     * Проверка валидности формата подсети
     */
    fun isValidSubnet(subnetRange: String): Boolean {
        return try {
            val parts = subnetRange.split("/")
            if (parts.size != 2) return false

            val (network, prefix) = parts
            val prefixLength = prefix.toInt()

            if (prefixLength < 1 || prefixLength > 32) return false

            InetAddress.getByName(network)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Оценка времени сканирования
     */
    fun estimateScanTime(
        subnetRange: String,
        timeoutPerHost: Long,
        ports: Int,
        maxConcurrent: Int
    ): Long {
        return try {
            val ips = parseSubnetToIps(subnetRange)
            val totalHosts = ips.size

            // Время на один хост = timeout * ports
            val timePerHost = timeoutPerHost * ports

            // Параллельное выполнение уменьшает общее время
            val batches = (totalHosts + maxConcurrent - 1) / maxConcurrent
            val totalTime = batches * timePerHost

            totalTime
        } catch (e: Exception) {
            -1L
        }
    }

    /**
     * Группировка камер по производителю
     */
    fun groupCamerasByManufacturer(cameras: List<DiscoveredCamera>): Map<String?, List<DiscoveredCamera>> {
        return cameras.groupBy { it.manufacturer }
    }

    /**
     * Группировка камер по подсети
     */
    fun groupCamerasBySubnet(cameras: List<DiscoveredCamera>): Map<String, List<DiscoveredCamera>> {
        return cameras.groupBy { camera ->
            // Извлекаем подсеть из URL
            val url = camera.url
            val ip = url.substringAfter("://").substringBefore(":")
            val parts = ip.split(".")
            if (parts.size >= 3) {
                "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
            } else {
                "unknown"
            }
        }
    }

    /**
     * Фильтрация камер по производителю
     */
    fun filterByManufacturer(
        cameras: List<DiscoveredCamera>,
        manufacturer: String
    ): List<DiscoveredCamera> {
        return cameras.filter {
            it.manufacturer?.contains(manufacturer, ignoreCase = true) == true
        }
    }

    /**
     * Фильтрация камер по модели
     */
    fun filterByModel(
        cameras: List<DiscoveredCamera>,
        model: String
    ): List<DiscoveredCamera> {
        return cameras.filter {
            it.model?.contains(model, ignoreCase = true) == true
        }
    }

    /**
     * Фильтрация камер по диапазону IP
     */
    fun filterByIpRange(
        cameras: List<DiscoveredCamera>,
        startIp: String,
        endIp: String
    ): List<DiscoveredCamera> {
        val startIpInt = ipToInt(startIp)
        val endIpInt = ipToInt(endIp)

        return cameras.filter { camera ->
            val ip = camera.url.substringAfter("://").substringBefore(":")
            val ipInt = ipToInt(ip)
            ipInt in startIpInt..endIpInt
        }
    }

    /**
     * Дедупликация камер по URL
     */
    fun deduplicateCameras(cameras: List<DiscoveredCamera>): List<DiscoveredCamera> {
        return cameras.distinctBy { it.url }
    }

    /**
     * Сортировка камер по имени
     */
    fun sortCamerasByName(cameras: List<DiscoveredCamera>): List<DiscoveredCamera> {
        return cameras.sortedBy { it.name?.lowercase() ?: "" }
    }

    /**
     * Сортировка камер по IP адресу
     */
    fun sortCamerasByIp(cameras: List<DiscoveredCamera>): List<DiscoveredCamera> {
        return cameras.sortedBy {
            val ip = it.url.substringAfter("://").substringBefore(":")
            ipToInt(ip)
        }
    }

    /**
     * Получение статистики сканирования
     */
    fun getScanStatistics(cameras: List<DiscoveredCamera>): ScanStatistics {
        val byManufacturer = groupCamerasByManufacturer(cameras)
        val bySubnet = groupCamerasBySubnet(cameras)

        return ScanStatistics(
            totalCameras = cameras.size,
            uniqueManufacturers = byManufacturer.keys.filterNotNull().size,
            uniqueSubnets = bySubnet.keys.size,
            manufacturers = byManufacturer.mapValues { it.value.size },
            subnets = bySubnet.mapValues { it.value.size }
        )
    }

    /**
     * Конвертация IP строки в int
     */
    private fun ipToInt(ip: String): Int {
        return try {
            val parts = ip.split(".")
            if (parts.size != 4) throw IllegalArgumentException("Invalid IP format")

            ((parts[0].toInt() and 0xFF) shl 24) or
                ((parts[1].toInt() and 0xFF) shl 16) or
                ((parts[2].toInt() and 0xFF) shl 8) or
                (parts[3].toInt() and 0xFF)
        } catch (e: Exception) {
            logger.error(e) { "Ошибка конвертации IP: $ip" }
            0
        }
    }

    /**
     * Конвертация int в IP строку
     */
    private fun intToIp(ipInt: Int): String {
        return "${(ipInt shr 24) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 8) and 0xFF}.${ipInt and 0xFF}"
    }

    /**
     * Конвертация байтов в int
     */
    private fun bytesToInt(bytes: ByteArray): Int {
        return ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)
    }
}

/**
 * Статистика сканирования
 */
data class ScanStatistics(
    val totalCameras: Int,
    val uniqueManufacturers: Int,
    val uniqueSubnets: Int,
    val manufacturers: Map<String?, Int>,
    val subnets: Map<String, Int>
)

/**
 * Расширение для создания сканера с предустановленными настройками
 */
fun createDefaultScanner(
    httpClient: HttpClient? = null
): NetworkScanner {
    return NetworkScanner(
        httpClient ?: HttpClient(),
        discoveryTimeout = 5000
    )
}

/**
 * Расширение для получения прогресса в процентах
 */
val NetworkScanProgress.progressPercent: Int
    get() = (progress * 100).toInt()

/**
 * Расширение для получения оставшегося времени в секундах.
 *
 * Формула: (elapsed / progress) × (1 - progress) — линейная экстраполяция по фактическому
 * темпу сканирования. Требует [NetworkScanProgress.elapsedMs]; если elapsedMs = null
 * (старые эмиттеры) — возвращает null.
 */
val NetworkScanProgress.remainingTimeSeconds: Long?
    get() {
        val elapsed = elapsedMs ?: return null
        if (progress <= 0f) return null
        val total = elapsed / progress
        return ((total - elapsed) / 1000.0).toLong().coerceAtLeast(0L)
    }

/**
 * Форматированное оставшееся время ("2м 30с", "45с", "<1с").
 */
val NetworkScanProgress.remainingTimeFormatted: String?
    get() {
        val seconds = remainingTimeSeconds ?: return null
        return when {
            seconds < 1 -> "<1с"
            seconds < 60 -> "${seconds}с"
            else -> "${seconds / 60}м ${seconds % 60}с"
        }
    }

/**
 * Расширение для проверки завершения сканирования
 */
val NetworkScanProgress.isCompleted: Boolean
    get() = progress >= 1f

/**
 * Расширение для получения процента завершённости хостов
 */
val NetworkScanProgress.hostsProgressPercent: Int
    get() = if (hostsTotal > 0) {
        (hostsScanned * 100 / hostsTotal)
    } else {
        0
    }

/**
 * Преобразовать Flow прогресса в Flow только изменений
 */
fun Flow<NetworkScanProgress>.filterProgressChanges(
    threshold: Float = 0.05f
): Flow<NetworkScanProgress> {
    var lastProgress = 0f

    return this.map { progress ->
        if (progress.progress - lastProgress >= threshold) {
            lastProgress = progress.progress
            progress
        } else {
            progress // Возвращаем прогресс вместо null
        }
    }.filter { it.progress - lastProgress >= threshold }
}

/**
 * Преобразовать Flow прогресса в список snapshots
 */
suspend fun Flow<NetworkScanProgress>.collectSnapshots(): List<NetworkScanProgress> {
    val snapshots = mutableListOf<NetworkScanProgress>()
    collect { progress ->
        snapshots.add(progress)
    }
    return snapshots
}

/**
 * Найти камеру по IP адресу
 */
fun List<DiscoveredCamera>.findByIp(ipAddress: String): DiscoveredCamera? {
    return find { it.url.contains(ipAddress) }
}

/**
 * Найти камеру по URL
 */
fun List<DiscoveredCamera>.findByUrl(url: String): DiscoveredCamera? {
    return find { it.url == url }
}

/**
 * Найти камеру по имени
 */
fun List<DiscoveredCamera>.findByName(name: String): DiscoveredCamera? {
    return find { it.name.equals(name, ignoreCase = true) }
}

/**
 * Проверить содержит ли список камер конкретный IP
 */
fun List<DiscoveredCamera>.containsIp(ipAddress: String): Boolean {
    return any { it.url.contains(ipAddress) }
}
