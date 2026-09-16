package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import mu.KotlinLogging
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}

/**
 * Сервис шифрования данных (4.3.4.1).
 * AES-GCM для конфиденциальных полей (при DATA_ENCRYPTION_KEY задан).
 */
object DataEncryptionService {

    private const val ALG = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LEN = 128
    private const val GCM_IV_LEN = 12

    private fun keyBytes(): ByteArray? {
        val keyHex = EnterpriseAuthConfig.dataEncryptionKey ?: return null
        if (keyHex.length != 32 && keyHex.length != 64) {
            logger.warn { "DATA_ENCRYPTION_KEY should be 16 or 32 bytes (32/64 hex chars)" }
            return null
        }
        return keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * Зашифровать строку. Возвращает Base64(IV + ciphertext) или null если шифрование отключено.
     */
    fun encrypt(plaintext: String): String? {
        val key = keyBytes() ?: return null
        return try {
            val iv = ByteArray(GCM_IV_LEN).also { java.security.SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, ALG), GCMParameterSpec(GCM_TAG_LEN, iv))
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(iv + encrypted)
        } catch (e: Exception) {
            logger.error(e) { "Encryption failed" }
            null
        }
    }

    /**
     * Расшифровать строку. Возвращает plaintext или null при ошибке/отключённом шифровании.
     */
    fun decrypt(base64CipherText: String): String? {
        val key = keyBytes() ?: return null
        return try {
            val decoded = Base64.getDecoder().decode(base64CipherText) ?: return null
            if (decoded.size < GCM_IV_LEN + 1) return null
            val iv = decoded.copyOfRange(0, GCM_IV_LEN)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, ALG), GCMParameterSpec(GCM_TAG_LEN, iv))
            val decrypted = cipher.doFinal(decoded.copyOfRange(GCM_IV_LEN, decoded.size))
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.debug(e) { "Decryption failed" }
            null
        }
    }

    fun isEncryptionEnabled(): Boolean = EnterpriseAuthConfig.dataEncryptionEnabled
}
