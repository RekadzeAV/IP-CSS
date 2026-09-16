package com.company.ipcamera.core.common.security

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
 * Desktop/JVM реализация шифрования локальных данных
 * Использует Java KeyStore для хранения ключей шифрования
 */
actual class SecureLocalDataEncryption : LocalDataEncryption {

    private val keyStore: KeyStore by lazy {
        val keyStoreFile = File(
            System.getProperty("user.home"),
            ".ip-camera/local_data_keystore.jks"
        )

        keyStoreFile.parentFile?.mkdirs()

        val keyStore = KeyStore.getInstance("JCEKS")

        if (keyStoreFile.exists()) {
            keyStoreFile.inputStream().use { stream ->
                val password = getKeyStorePassword()
                keyStore.load(stream, password.toCharArray())
            }
        } else {
            keyStore.load(null, null)
        }

        keyStore
    }

    private val keyAlias = "local_data_encryption_key"
    private val algorithm = "AES/GCM/NoPadding"
    private val keyLength = 256 // bits
    private val ivLength = 12 // bytes for GCM
    private val tagLength = 128 // bits for GCM
    private val encryptionPrefix = "ENC:".toByteArray()

    /**
     * Получает или создает ключ шифрования из Java KeyStore
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
            logger.error(e) { "Failed to get encryption key from KeyStore, creating new key" }
            createNewKey()
        }
    }

    /**
     * Создает новый ключ и сохраняет в KeyStore
     */
    private fun createNewKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(keyLength)
        val secretKey = keyGenerator.generateKey()

        val password = getKeyStorePassword()
        val secretKeyEntry = KeyStore.SecretKeyEntry(secretKey)

        keyStore.setEntry(
            keyAlias,
            secretKeyEntry,
            KeyStore.PasswordProtection(password.toCharArray())
        )

        val keyStoreFile = File(
            System.getProperty("user.home"),
            ".ip-camera/local_data_keystore.jks"
        )
        keyStoreFile.outputStream().use { stream ->
            keyStore.store(stream, password.toCharArray())
        }

        logger.info { "Created new local data encryption key in Java KeyStore: $keyAlias" }
        return secretKey
    }

    /**
     * Получает пароль для KeyStore
     */
    private fun getKeyStorePassword(): String {
        val systemProps = System.getProperty("user.name") + System.getProperty("user.home")
        return systemProps.hashCode().toString()
    }

    actual override fun encrypt(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        if (isEncrypted(data)) return data

        return try {
            val secretKey = getOrCreateEncryptionKey()
            val cipher = Cipher.getInstance(algorithm)

            val iv = ByteArray(ivLength)
            java.security.SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val encrypted = cipher.doFinal(data)

            val combined = ByteArray(encryptionPrefix.size + iv.size + encrypted.size)
            System.arraycopy(encryptionPrefix, 0, combined, 0, encryptionPrefix.size)
            System.arraycopy(iv, 0, combined, encryptionPrefix.size, iv.size)
            System.arraycopy(encrypted, 0, combined, encryptionPrefix.size + iv.size, encrypted.size)

            combined
        } catch (e: Exception) {
            logger.error(e) { "Failed to encrypt local data" }
            data
        }
    }

    actual override fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.isEmpty()) return encryptedData
        if (!isEncrypted(encryptedData)) return encryptedData

        return try {
            val dataWithoutPrefix = encryptedData.sliceArray(encryptionPrefix.size until encryptedData.size)

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
            encryptedData
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
