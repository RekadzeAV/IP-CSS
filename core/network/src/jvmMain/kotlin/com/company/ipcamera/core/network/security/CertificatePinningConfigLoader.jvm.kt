package com.company.ipcamera.core.network.security

import java.io.File
import java.security.MessageDigest
import java.util.Base64

/**
 * JVM/Desktop: загрузка конфигурации pinning из файловой системы.
 * Путь может быть относительным (к текущей рабочей директории) или абсолютным.
 * Рекомендуемый путь: config/certificate-pins.json (рядом с config/certificate-pins.example.json).
 */
actual object CertificatePinningConfigLoader {

    actual fun readFile(path: String): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null
            file.readText()
        } catch (_: Exception) {
            null
        }
    }

    actual fun getEnv(name: String): String? = System.getenv(name)

    actual fun getEnvMap(): Map<String, String>? = System.getenv()

    actual fun calculateSha256Pin(certificateData: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(certificateData)
        return "sha256/${Base64.getEncoder().encodeToString(hash)}"
    }
}
