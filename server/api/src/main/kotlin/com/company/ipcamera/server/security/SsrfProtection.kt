package com.company.ipcamera.server.security

import mu.KotlinLogging
import java.net.InetAddress
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * Утилита для защиты от SSRF (Server-Side Request Forgery) атак
 *
 * Блокирует запросы к внутренним IP адресам и метаданным сервисам
 */
object SsrfProtection {
    private fun isProductionEnvironment(): Boolean {
        val env = System.getenv("ENVIRONMENT")?.trim()?.lowercase()
        val nodeEnv = System.getenv("NODE_ENV")?.trim()?.lowercase()
        return env == "production" || nodeEnv == "production"
    }

    private fun allowPrivateCameraUrls(): Boolean {
        val raw = System.getenv("ALLOW_PRIVATE_CAMERA_URLS")?.trim()?.lowercase() ?: return false
        return raw == "true" || raw == "1" || raw == "yes"
    }

    // Приватные IP диапазоны (RFC 1918 и другие)
    private val privateIpRanges = listOf(
        IpRange("127.0.0.0", "127.255.255.255"),      // localhost
        IpRange("10.0.0.0", "10.255.255.255"),       // Class A private
        IpRange("172.16.0.0", "172.31.255.255"),     // Class B private
        IpRange("192.168.0.0", "192.168.255.255"),    // Class C private
        IpRange("169.254.0.0", "169.254.255.255"),   // Link-local
        IpRange("0.0.0.0", "0.255.255.255"),         // "This network"
        IpRange("224.0.0.0", "239.255.255.255"),     // Multicast
        IpRange("240.0.0.0", "255.255.255.255")      // Reserved
    )

    // Запрещенные hostnames
    private val forbiddenHostnames = setOf(
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1",
        "metadata.google.internal",           // GCP metadata service
        "169.254.169.254",                    // AWS/OpenStack metadata service
        "metadata.azure.com",                  // Azure metadata service
        "metadata.azure.net",
        "metadata.microsoft.com",
        "100.100.100.200",                    // Alibaba Cloud metadata
        "fd00:ec2::254"                       // AWS IPv6 metadata
    )

    /**
     * Проверяет, является ли IP адрес приватным
     */
    fun isPrivateIp(ip: String): Boolean {
        return try {
            val inetAddress = InetAddress.getByName(ip)
            val ipBytes = inetAddress.address

            if (ipBytes.size == 4) { // IPv4
                val ipInt = (ipBytes[0].toInt() and 0xFF shl 24) or
                           (ipBytes[1].toInt() and 0xFF shl 16) or
                           (ipBytes[2].toInt() and 0xFF shl 8) or
                           (ipBytes[3].toInt() and 0xFF)

                privateIpRanges.any { range ->
                    val startInt = ipToInt(range.start)
                    val endInt = ipToInt(range.end)
                    ipInt >= startInt && ipInt <= endInt
                }
            } else {
                // IPv6 - проверяем на localhost и link-local
                inetAddress.isLoopbackAddress || inetAddress.isLinkLocalAddress
            }
        } catch (e: Exception) {
            // Если не удалось распарсить IP, считаем опасным
            true
        }
    }

    /**
     * Проверяет, является ли hostname запрещенным
     */
    fun isForbiddenHostname(hostname: String): Boolean {
        val lowerHostname = hostname.lowercase()

        // Прямое совпадение
        if (forbiddenHostnames.contains(lowerHostname)) {
            return true
        }

        // Проверка на localhost варианты
        if (lowerHostname == "localhost" ||
            lowerHostname.startsWith("localhost.") ||
            lowerHostname.endsWith(".localhost")) {
            return true
        }

        // Проверка на метаданные сервисы
        if (lowerHostname.contains("metadata") &&
            (lowerHostname.contains("google") ||
             lowerHostname.contains("azure") ||
             lowerHostname.contains("microsoft") ||
             lowerHostname.contains("aws") ||
             lowerHostname.contains("amazon"))) {
            return true
        }

        return false
    }

    /**
     * Валидирует URL на SSRF уязвимости
     *
     * @param urlString URL для проверки
     * @return ValidationResult с ошибкой если URL опасен
     */
    fun validateUrlForSsrf(urlString: String): com.company.ipcamera.server.validation.ValidationResult {
        return try {
            val uri = URI(urlString)
            val host = uri.host

            if (host.isNullOrBlank()) {
                return com.company.ipcamera.server.validation.ValidationResult.Error("URL host cannot be blank")
            }

            validateForbiddenHostname(host, urlString)
                ?: validateResolvedIp(host, urlString)
                ?: validateIpv4Address(host, urlString)
                ?: validateIpv6Address(host, urlString)
                ?: com.company.ipcamera.server.validation.ValidationResult.Success
        } catch (e: Exception) {
            com.company.ipcamera.server.validation.ValidationResult.Error("Invalid URL format: ${e.message}")
        }
    }

    private fun validateForbiddenHostname(host: String, urlString: String): com.company.ipcamera.server.validation.ValidationResult? {
        if (isForbiddenHostname(host)) {
            logger.warn { "SSRF attempt detected: forbidden hostname '$host' in URL '$urlString'" }
            SecurityLogger.logSsrfAttempt(ipAddress = null, userId = null, username = null, url = urlString, reason = "forbidden_hostname: $host")
            return com.company.ipcamera.server.validation.ValidationResult.Error("URL host is not allowed (internal or metadata service)")
        }
        return null
    }

    private fun validateResolvedIp(host: String, urlString: String): com.company.ipcamera.server.validation.ValidationResult? {
        if (!shouldCheckPrivateIp()) return null
        
        return try {
            InetAddress.getAllByName(host).forEach { address ->
                val ip = address.hostAddress
                if (ip != null && isPrivateIp(ip)) {
                    logger.warn { "SSRF attempt detected: private IP address '$ip' resolved from host '$host' in URL '$urlString'" }
                    SecurityLogger.logSsrfAttempt(ipAddress = null, userId = null, username = null, url = urlString, reason = "private_ip_address: $ip")
                    return com.company.ipcamera.server.validation.ValidationResult.Error("URL points to private/internal IP address: $ip")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun validateIpv4Address(host: String, urlString: String): com.company.ipcamera.server.validation.ValidationResult? {
        if (!shouldCheckPrivateIp()) return null
        
        val ipv4Pattern = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
        if (ipv4Pattern.matches(host) && isPrivateIp(host)) {
            logger.warn { "SSRF attempt detected: private IPv4 address '$host' in URL '$urlString'" }
            SecurityLogger.logSsrfAttempt(ipAddress = null, userId = null, username = null, url = urlString, reason = "private_ipv4_address: $host")
            return com.company.ipcamera.server.validation.ValidationResult.Error("URL points to private IP address: $host")
        }
        return null
    }

    private fun validateIpv6Address(host: String, urlString: String): com.company.ipcamera.server.validation.ValidationResult? {
        if (!shouldCheckPrivateIp()) return null
        if (!host.contains(":") || host.contains(".")) return null
        
        return try {
            val inetAddress = InetAddress.getByName(host)
            if (inetAddress.isLoopbackAddress || inetAddress.isLinkLocalAddress || inetAddress.isSiteLocalAddress) {
                logger.warn { "SSRF attempt detected: private IPv6 address '$host' in URL '$urlString'" }
                SecurityLogger.logSsrfAttempt(ipAddress = null, userId = null, username = null, url = urlString, reason = "private_ipv6_address: $host")
                return com.company.ipcamera.server.validation.ValidationResult.Error("URL points to private IPv6 address: $host")
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun shouldCheckPrivateIp(): Boolean {
        return !allowPrivateCameraUrls() && isProductionEnvironment()
    }

    /**
     * Конвертирует IP адрес в целое число для сравнения
     */
    private fun ipToInt(ip: String): Int {
        val parts = ip.split(".")
        return (parts[0].toInt() shl 24) or
               (parts[1].toInt() shl 16) or
               (parts[2].toInt() shl 8) or
               parts[3].toInt()
    }

    /**
     * Представляет диапазон IP адресов
     */
    private data class IpRange(
        val start: String,
        val end: String
    )
}
