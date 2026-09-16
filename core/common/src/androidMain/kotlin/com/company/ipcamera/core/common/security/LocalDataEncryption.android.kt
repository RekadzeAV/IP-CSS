package com.company.ipcamera.core.common.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import mu.KotlinLogging
import java.io.File
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val logger = KotlinLogging.logger {}

/**
 * Android реализация шифрования локальных данных
 * Использует Android Keystore для хранения ключей шифрования
 */
actual class SecureLocalDataEncryption : LocalDataEncryption {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }

    private val keyAlias = "local_data_encryption_key"
    private val algorithm = "AES/GCM/NoPadding"
    private val keyLength = 256 // bits
    private val ivLength = 12 // bytes for GCM
    private val tagLength = 128 // bits for GCM
    private val encryptionPrefix = "ENC:".toByteArray()

    /**
     * Получает или создает ключ шифрования из Android Keystore
     */
    private fun getOrCreateEncryptionKey(): SecretKey {
        return try {
            if (keyStore.containsAlias(keyAlias)) {
                val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey ?: createNewKey()
            } else {
                createNewKey()
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get encryption key from Keystore, creating new key" }
            createNewKey()
        }
    }

    /**
     * Создает новый ключ в Android Keystore
     */
    private fun createNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(keyLength)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        val key = keyGenerator.generateKey()
        logger.info { "Created new local data encryption key in Android Keystore: $keyAlias" }
        return key
    }

    actual override fun encrypt(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        if (isEncrypted(data)) return data // Уже зашифрован

        return try {
            val secretKey = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(algorithm)

            // Генерируем случайный IV
            val iv = ByteArray(ivLength)
            java.security.SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encrypted = cipher.doFinal(data)

            // Объединяем префикс, IV и зашифрованные данные
            val combined = ByteArray(encryptionPrefix.size + iv.size + encrypted.size)
            System.arraycopy(encryptionPrefix, 0, combined, 0, encryptionPrefix.size)
            System.arraycopy(iv, 0, combined, encryptionPrefix.size, iv.size)
            System.arraycopy(encrypted, 0, combined, encryptionPrefix.size + iv.size, encrypted.size)

            combined
        } catch (e: Exception) {
            logger.error(e) { "Failed to encrypt local data" }
            data // Возвращаем исходные данные в случае ошибки
        }
    }

    actual override fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.isEmpty()) return encryptedData
        if (!isEncrypted(encryptedData)) return encryptedData // Не зашифрован

        return try {
            // Пропускаем префикс
            val dataWithoutPrefix = encryptedData.sliceArray(encryptionPrefix.size until encryptedData.size)

            // Извлекаем IV и зашифрованные данные
            val iv = ByteArray(ivLength)
            System.arraycopy(dataWithoutPrefix, 0, iv, 0, ivLength)

            val encrypted = ByteArray(dataWithoutPrefix.size - ivLength)
            System.arraycopy(dataWithoutPrefix, ivLength, encrypted, 0, encrypted.size)

            val secretKey = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(algorithm)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            cipher.doFinal(encrypted)
        } catch (e: Exception) {
            logger.error(e) { "Failed to decrypt local data" }
            encryptedData // Возвращаем исходные данные в случае ошибки
        }
    }

    actual override fun encryptString(data: String): String {
        val encrypted = encrypt(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }

    actual override fun decryptString(encryptedData: String): String {
        val encrypted = Base64.getDecoder().decode(encryptedData)
        val decrypted = decrypt(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    actual override fun isEncrypted(data: ByteArray): Boolean {
        if (data.size < encryptionPrefix.size) return false
        return data.sliceArray(0 until encryptionPrefix.size).contentEquals(encryptionPrefix)
    }
}
