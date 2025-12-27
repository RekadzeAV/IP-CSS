package com.company.ipcamera.core.common.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.GCMParameterSpec

/**
 * Desktop/JVM реализация шифрования паролей
 * Использует AES-256-GCM
 */
actual class SecurePasswordEncryption : PasswordEncryption {

    private val encryptionKey: ByteArray by lazy {
        // В продакшене использовать системный KeyStore или файл с ключом
        // Для MVP используем производный ключ
        generateDerivedKey()
    }

    private val algorithm = "AES/GCM/NoPadding"
    private val keyAlgorithm = "AES"
    private val keyLength = 256 // bits
    private val ivLength = 12 // bytes for GCM
    private val tagLength = 128 // bits for GCM

    /**
     * Генерирует производный ключ
     * В продакшене должен использовать системный KeyStore
     */
    private fun generateDerivedKey(): ByteArray {
        // TODO: Использовать Java KeyStore для генерации/хранения ключа
        // Временная реализация для MVP
        val keyBytes = ByteArray(32) // 256 bits
        val masterKeyAlias = "camera_password_key"
        System.arraycopy(masterKeyAlias.toByteArray(), 0, keyBytes, 0,
            masterKeyAlias.toByteArray().size.coerceAtMost(32))
        return keyBytes
    }

    actual override fun encrypt(password: String): String {
        if (password.isEmpty()) return ""
        if (isEncrypted(password)) return password // Уже зашифрован

        return try {
            val cipher = Cipher.getInstance(algorithm)
            val secretKey = SecretKeySpec(encryptionKey, keyAlgorithm)

            // Генерируем случайный IV
            val iv = ByteArray(ivLength)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

            // Объединяем IV и зашифрованные данные
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            // Кодируем в Base64 и добавляем префикс
            "ENC:" + Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            // В случае ошибки возвращаем исходный пароль
            password
        }
    }

    actual override fun decrypt(encryptedPassword: String): String {
        if (encryptedPassword.isEmpty()) return ""
        if (!isEncrypted(encryptedPassword)) return encryptedPassword // Не зашифрован

        return try {
            // Удаляем префикс "ENC:"
            val base64Data = encryptedPassword.removePrefix("ENC:")
            val combined = Base64.getDecoder().decode(base64Data)

            // Извлекаем IV и зашифрованные данные
            val iv = ByteArray(ivLength)
            System.arraycopy(combined, 0, iv, 0, ivLength)

            val encrypted = ByteArray(combined.size - ivLength)
            System.arraycopy(combined, ivLength, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(algorithm)
            val secretKey = SecretKeySpec(encryptionKey, keyAlgorithm)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decrypted = cipher.doFinal(encrypted)

            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // В случае ошибки возвращаем исходную строку
            encryptedPassword
        }
    }

    actual override fun isEncrypted(value: String): Boolean {
        return value.startsWith("ENC:")
    }
}


