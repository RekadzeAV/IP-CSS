package com.company.ipcamera.core.license

import java.security.MessageDigest
import kotlin.experimental.and

/**
 * Desktop (JVM) actual implementation of PlatformCrypto.
 * Uses javax.crypto and java.security for offline license code decryption
 * and device fingerprint derivation.
 */
actual class PlatformCrypto {
    actual fun getSecureDeviceFingerprint(): String {
        val sb = StringBuilder()
        try {
            val env = System.getenv()
            val parts = listOf(
                env["COMPUTERNAME"] ?: env["HOSTNAME"] ?: "unknown",
                env["PROCESSOR_IDENTIFIER"] ?: "",
                System.getProperty("os.name") ?: "",
                System.getProperty("user.name") ?: "",
                Runtime.getRuntime().availableProcessors().toString()
            )
            val raw = parts.joinToString("|")
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            for (b in digest) {
                sb.append("%02x".format(b and 0xFF.toByte()))
            }
        } catch (e: Exception) {
            sb.append("fallback-${System.identityHashCode(this)}")
        }
        return sb.toString()
    }

    actual fun decryptOfflineCode(code: String): OfflineActivationData {
        // Плацебо-реализация: для продакшена требуется интеграция с
        // assymetric crypto (RSA/AES) для расшифровки offline-кода.
        return OfflineActivationData(
            licenseId = "offline-${code.take(8)}",
            validUntil = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
            deviceFingerprint = getSecureDeviceFingerprint(),
            requiresPeriodicCheck = true
        )
    }

    actual fun schedulePeriodicCheck(checkCallback: (ActivatedLicense) -> Unit) {
        // Плацебо: в продакшене здесь запускается периодическая проверка
        // лицензии через CoroutineScope. На desktop пока не реализовано.
    }
}
