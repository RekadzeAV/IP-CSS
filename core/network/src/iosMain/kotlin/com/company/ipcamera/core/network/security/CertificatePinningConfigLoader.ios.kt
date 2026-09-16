package com.company.ipcamera.core.network.security

import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS: загрузка конфигурации pinning из main bundle.
 * Файл должен быть добавлен в Xcode target (Copy Bundle Resources), например config/certificate-pins.json.
 * Путь может быть "config/certificate-pins.json" (subdirectory + name) или "certificate-pins.json".
 */
actual object CertificatePinningConfigLoader {

    actual fun readFile(path: String): String? {
        val (directory, name, ext) = parsePath(path) ?: return null
        val bundle = NSBundle.mainBundle
        val fullPath = when {
            directory != null -> bundle.pathForResource(name, ext, directory)
            else -> bundle.pathForResource(name, ext)
        } ?: bundle.pathForResource(name, ext)
        if (fullPath == null) return null
        return NSString.stringWithContentsOfFile(fullPath, NSUTF8StringEncoding, null)
    }

    private fun parsePath(path: String): Triple<String?, String, String>? {
        val normalized = path.trim().removePrefix("/")
        if (normalized.isEmpty()) return null
        val lastSlash = normalized.lastIndexOf('/')
        val filePart = if (lastSlash >= 0) normalized.substring(lastSlash + 1) else normalized
        val directory = if (lastSlash >= 0) normalized.substring(0, lastSlash) else null
        val lastDot = filePart.lastIndexOf('.')
        val (name, ext) = if (lastDot > 0) {
            filePart.substring(0, lastDot) to filePart.substring(lastDot + 1)
        } else {
            filePart to ""
        }
        return Triple(if (directory?.isNotEmpty() == true) directory else null, name, ext)
    }

    actual fun getEnv(name: String): String? {
        return platform.posix.getenv(name)?.toKString()
    }

    actual fun getEnvMap(): Map<String, String>? = null

    actual fun calculateSha256Pin(certificateData: ByteArray): String {
        // На iOS расчёт pin для справки/тестов не реализован в shared-коде; используйте нативные API при необходимости.
        return ""
    }
}
