package com.company.ipcamera.core.common.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import mu.KotlinLogging
import com.company.ipcamera.core.common.security.MobileSecurityLoggerFactory
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val logger = KotlinLogging.logger {}
private val securityLogger = MobileSecurityLoggerFactory.create()

/**
 * Android реализация шифрования паролей
 * Использует AES-256-GCM с ключом из Android Keystore
 */
actual class SecurePasswordEncryption : PasswordEncryption {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }

    private val keyAlias = "camera_password_encryption_key"
    private val algorithm = "AES/GCM/NoPadding"
    private val keyAlgorithm = "AES"
    private val keyLength = 256 // bits
    private val ivLength = 12 // bytes for GCM
    private val tagLength = 128 // bits for GCM

    /**
     * Получает или создает ключ шифрования из Android Keystore
     */
    private fun getOrCreateEncryptionKey(): SecretKey {
        return try {
            // Проверяем, существует ли ключ
            if (keyStore.containsAlias(keyAlias)) {
                val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey ?: createNewKey()
            } else {
                createNewKey()
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get encryption key from Keystore, creating new key" }
            securityLogger.logKeystoreError("get_key", e.message ?: "Unknown error")
            createNewKey()
        }
    }

    /**
     * Создает новый ключ в Android Keystore
     */
    private fun createNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(keyAlgorithm, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(keyLength)
            .setUserAuthenticationRequired(false) // Не требуется биометрия для MVP
            .build()

        keyGenerator.init(keyGenParameterSpec)
        val key = keyGenerator.generateKey()
        logger.info { "Created new encryption key in Android Keystore: $keyAlias" }
        return key
    }

    actual override fun encrypt(password: String): String {
        if (password.isEmpty()) return ""
        if (isEncrypted(password)) return password // Уже зашифрован

        return try {
            val secretKey = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(algorithm)

            // Генерируем случайный IV
            val iv = ByteArray(ivLength)
            java.security.SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

            // Объединяем IV и зашифрованные данные
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            // Кодируем в Base64 и добавляем префикс для идентификации
            val result = "ENC:" + Base64.getEncoder().encodeToString(combined)
            securityLogger.logEncryptionSuccess("camera_password")
            result
        } catch (e: Exception) {
            logger.error(e) { "Failed to encrypt password" }
            securityLogger.logEncryptionFailure("camera_password", e.message ?: "Unknown error")
            // В случае ошибки шифрования выбрасываем исключение для безопасности
            // Не возвращаем незашифрованный пароль, так как это создает уязвимость
            throw SecurityException("Failed to encrypt password: ${e.message}", e)
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

            val secretKey = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(algorithm)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decrypted = cipher.doFinal(encrypted)

            val result = String(decrypted, Charsets.UTF_8)
            securityLogger.logDecryptionSuccess("camera_password")
            result
        } catch (e: Exception) {
            logger.error(e) { "Failed to decrypt password" }
            securityLogger.logDecryptionFailure("camera_password", e.message ?: "Unknown error")
            // В случае ошибки расшифровки выбрасываем исключение для безопасности
            // Не возвращаем исходную строку, так как это может привести к утечке данных
            throw SecurityException("Failed to decrypt password: ${e.message}", e)
        }
    }

    actual override fun isEncrypted(value: String): Boolean {
        return value.startsWith("ENC:")
    }
}



