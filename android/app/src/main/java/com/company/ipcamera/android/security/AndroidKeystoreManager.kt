package com.company.ipcamera.android.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Менеджер для работы с Android Keystore
 *
 * Обеспечивает безопасное хранение и шифрование данных:
 * - Пароли камер
 * - API ключи
 * - Токены аутентификации
 */
class AndroidKeystoreManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        // Алиасы ключей
        private const val KEY_ALIAS_PASSWORD = "ipcamera_password_key"
        private const val KEY_ALIAS_API = "ipcamera_api_key"
        private const val KEY_ALIAS_TOKEN = "ipcamera_token_key"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    /**
     * Проверить, существует ли ключ
     */
    fun keyExists(alias: String): Boolean {
        return try {
            keyStore.containsAlias(alias)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Генерировать ключ
     */
    fun generateKey(alias: String, requireAuth: Boolean = false): Boolean {
        return try {
            if (keyExists(alias)) {
                return true // Ключ уже существует
            }

            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM, KEYSTORE_PROVIDER)

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setKeySize(256)
                .apply {
                    if (requireAuth) {
                        setUserAuthenticationRequired(true)
                        setUserAuthenticationValidityDurationSeconds(300) // 5 минут
                    }
                }
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Удалить ключ
     */
    fun deleteKey(alias: String): Boolean {
        return try {
            if (keyExists(alias)) {
                keyStore.deleteEntry(alias)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Получить список всех ключей
     */
    fun listKeys(): List<String> {
        return try {
            keyStore.aliases().toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Зашифровать данные
     */
    fun encrypt(alias: String, data: String): String? {
        return try {
            // Генерируем ключ, если его нет
            if (!keyExists(alias)) {
                generateKey(alias)
            }

            val secretKey = keyStore.getKey(alias, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Объединяем IV и зашифрованные данные
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            // Кодируем в Base64
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Расшифровать данные
     */
    fun decrypt(alias: String, encryptedData: String): String? {
        return try {
            if (!keyExists(alias)) {
                return null // Ключ не найден
            }

            val secretKey = keyStore.getKey(alias, null) as SecretKey
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)

            // Извлекаем IV
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            // Извлекаем зашифрованные данные
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Зашифровать пароль камеры
     */
    fun encryptPassword(password: String): String? {
        return encrypt(KEY_ALIAS_PASSWORD, password)
    }

    /**
     * Расшифровать пароль камеры
     */
    fun decryptPassword(encryptedPassword: String): String? {
        return decrypt(KEY_ALIAS_PASSWORD, encryptedPassword)
    }

    /**
     * Зашифровать API ключ
     */
    fun encryptApiKey(apiKey: String): String? {
        return encrypt(KEY_ALIAS_API, apiKey)
    }

    /**
     * Расшифровать API ключ
     */
    fun decryptApiKey(encryptedApiKey: String): String? {
        return decrypt(KEY_ALIAS_API, encryptedApiKey)
    }

    /**
     * Зашифровать токен
     */
    fun encryptToken(token: String): String? {
        return encrypt(KEY_ALIAS_TOKEN, token)
    }

    /**
     * Расшифровать токен
     */
    fun decryptToken(encryptedToken: String): String? {
        return decrypt(KEY_ALIAS_TOKEN, encryptedToken)
    }

    /**
     * Инициализировать ключи (вызывать при первом запуске)
     */
    fun initializeKeys(): Boolean {
        return try {
            generateKey(KEY_ALIAS_PASSWORD)
            generateKey(KEY_ALIAS_API)
            generateKey(KEY_ALIAS_TOKEN)
            true
        } catch (e: Exception) {
            false
        }
    }
}
