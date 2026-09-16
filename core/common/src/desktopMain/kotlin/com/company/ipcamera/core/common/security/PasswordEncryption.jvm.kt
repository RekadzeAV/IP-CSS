package com.company.ipcamera.core.common.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.GCMParameterSpec
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Desktop/JVM реализация шифрования паролей
 *
 * Использует:
 * - AES-256-GCM для шифрования (аутентифицированное шифрование)
 * - Java KeyStore (JCEKS) для безопасного хранения ключей
 * - Случайный IV (12 bytes) для каждого шифрования
 * - GCM tag (16 bytes) для проверки целостности
 *
 * Формат зашифрованных данных: IV (12 bytes) + Encrypted Data + GCM Tag (16 bytes)
 */
actual class SecurePasswordEncryption : PasswordEncryption {

    private val encryptionKey: ByteArray by lazy {
        getOrCreateEncryptionKey()
    }

    private val algorithm = "AES/GCM/NoPadding"
    private val keyAlgorithm = "AES"
    private val keyLength = 256 // bits
    private val ivLength = 12 // bytes for GCM
    private val tagLength = 128 // bits for GCM

    private val keyStore: java.security.KeyStore by lazy {
        val keyStoreFile = java.io.File(
            System.getProperty("user.home"),
            ".ip-camera/keystore.jks"
        )

        // Создаем директорию если не существует
        keyStoreFile.parentFile?.mkdirs()

        val keyStore = java.security.KeyStore.getInstance("JCEKS")

        if (keyStoreFile.exists()) {
            // Загружаем существующий KeyStore
            keyStoreFile.inputStream().use { stream ->
                val password = getKeyStorePassword()
                keyStore.load(stream, password.toCharArray())
            }
        } else {
            // Создаем новый KeyStore
            keyStore.load(null, null)
        }

        keyStore
    }

    private val keyAlias = "camera_password_encryption_key"

    /**
     * Получает или создает ключ шифрования из Java KeyStore
     */
    private fun getOrCreateEncryptionKey(): ByteArray {
        return try {
            if (keyStore.containsAlias(keyAlias)) {
                // Ключ существует, получаем его
                val entry = keyStore.getEntry(keyAlias, null) as? java.security.KeyStore.SecretKeyEntry
                entry?.secretKey?.encoded ?: createNewKey()
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
    private fun createNewKey(): ByteArray {
        val keyGenerator = javax.crypto.KeyGenerator.getInstance(keyAlgorithm)
        keyGenerator.init(keyLength)
        val secretKey = keyGenerator.generateKey()

        // Сохраняем ключ в KeyStore
        val password = getKeyStorePassword()
        val secretKeyEntry = java.security.KeyStore.SecretKeyEntry(secretKey)

        keyStore.setEntry(
            keyAlias,
            secretKeyEntry,
            java.security.KeyStore.PasswordProtection(password.toCharArray())
        )

        // Сохраняем KeyStore в файл
        val keyStoreFile = java.io.File(
            System.getProperty("user.home"),
            ".ip-camera/keystore.jks"
        )
        keyStoreFile.outputStream().use { stream ->
            keyStore.store(stream, password.toCharArray())
        }

        logger.info { "Created new encryption key in Java KeyStore: $keyAlias" }
        return secretKey.encoded
    }

    /**
     * Получает пароль для KeyStore
     *
     * Использует переменную окружения или генерирует стабильный пароль
     * на основе системных свойств пользователя.
     *
     * Приоритет:
     * 1. Переменная окружения KEYSTORE_PASSWORD (рекомендуется для production)
     * 2. Генерация на основе системных свойств (для development)
     *
     * В production рекомендуется:
     * - Использовать переменную окружения KEYSTORE_PASSWORD
     * - Или системное хранилище паролей (Windows Credential Manager, macOS Keychain, Linux Secret Service)
     * - Или запрашивать пароль у пользователя при первом запуске
     */
    private fun getKeyStorePassword(): String {
        // Проверяем переменную окружения (приоритет для production)
        val envPassword = System.getenv("KEYSTORE_PASSWORD")
        if (!envPassword.isNullOrBlank()) {
            logger.debug { "Using KeyStore password from environment variable" }
            return envPassword
        }

        // Используем комбинацию системных свойств для генерации стабильного пароля
        // Это обеспечивает, что пароль будет одинаковым для одного пользователя,
        // но разным для разных пользователей
        val systemProps = buildString {
            append(System.getProperty("user.name", "user"))
            append(System.getProperty("user.home", "/home"))
            append(System.getProperty("os.name", "unknown"))
            // Добавляем дополнительную соль для безопасности
            append("ip-camera-keystore-2026")
        }

        // Используем SHA-256 для генерации пароля из системных свойств
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(systemProps.toByteArray())

        // Конвертируем в Base64 и берем первые 32 символа
        val base64 = java.util.Base64.getEncoder().encodeToString(hash)
        val password = base64.substring(0, minOf(32, base64.length))

        logger.debug { "Generated KeyStore password from system properties (development mode)" }
        logger.warn { "Using auto-generated KeyStore password. For production, set KEYSTORE_PASSWORD environment variable" }

        return password
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
            // GCM автоматически добавляет tag в конец encrypted данных
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            // Кодируем в Base64 и добавляем префикс
            val result = "ENC:" + Base64.getEncoder().encodeToString(combined)
            logger.debug { "Password encrypted successfully using AES-256-GCM" }
            result
        } catch (e: Exception) {
            logger.error(e) { "Failed to encrypt password" }
            // В случае ошибки шифрования выбрасываем исключение для безопасности
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

            // Проверяем минимальный размер (IV + данные + GCM tag)
            if (combined.size < ivLength + 16) {
                logger.warn { "Invalid encrypted data size: ${combined.size} bytes" }
                throw SecurityException("Invalid encrypted data format")
            }

            // Извлекаем IV и зашифрованные данные (включая GCM tag)
            val iv = ByteArray(ivLength)
            System.arraycopy(combined, 0, iv, 0, ivLength)

            val encrypted = ByteArray(combined.size - ivLength)
            System.arraycopy(combined, ivLength, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(algorithm)
            val secretKey = SecretKeySpec(encryptionKey, keyAlgorithm)
            val gcmSpec = GCMParameterSpec(tagLength, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val decrypted = cipher.doFinal(encrypted)

            logger.debug { "Password decrypted successfully using AES-256-GCM" }
            String(decrypted, Charsets.UTF_8)
        } catch (e: SecurityException) {
            logger.error(e) { "Security error during password decryption" }
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to decrypt password" }
            // В случае ошибки расшифровки выбрасываем исключение для безопасности
            throw SecurityException("Failed to decrypt password: ${e.message}", e)
        }
    }

    actual override fun isEncrypted(value: String): Boolean {
        return value.startsWith("ENC:")
    }
}



