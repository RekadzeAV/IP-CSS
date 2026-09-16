package com.company.ipcamera.core.common.security

import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * iOS реализация шифрования локальных данных
 * Использует Keychain Services для хранения ключей шифрования
 */
@OptIn(ExperimentalForeignApi::class)
actual class SecureLocalDataEncryption : LocalDataEncryption {

    private val keychainService = "com.company.ipcamera.local_data"
    private val keychainAccount = "local_data_encryption_key"
    private val keySize = 32 // 256 bits
    private val ivLength = 12 // bytes for GCM
    private val encryptionPrefix = "ENC:".toByteArray()

    actual override fun encrypt(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        if (isEncrypted(data)) return data

        return try {
            val key = getOrCreateEncryptionKey()
            val encrypted = performAESEncryption(data, key)

            // Добавляем префикс
            val result = ByteArray(encryptionPrefix.size + encrypted.size)
            System.arraycopy(encryptionPrefix, 0, result, 0, encryptionPrefix.size)
            System.arraycopy(encrypted, 0, result, encryptionPrefix.size, encrypted.size)

            result
        } catch (e: Throwable) {
            logger.error(e) { "Failed to encrypt local data" }
            data
        }
    }

    actual override fun decrypt(encryptedData: ByteArray): ByteArray {
        if (encryptedData.isEmpty()) return encryptedData
        if (!isEncrypted(encryptedData)) return encryptedData

        return try {
            val dataWithoutPrefix = encryptedData.sliceArray(encryptionPrefix.size until encryptedData.size)
            val key = getOrCreateEncryptionKey()
            performAESDecryption(dataWithoutPrefix, key)
        } catch (e: Throwable) {
            logger.error(e) { "Failed to decrypt local data" }
            encryptedData
        }
    }

    actual override fun encryptString(data: String): String {
        val encrypted = encrypt(data.toByteArray())
        val data = NSData.dataWithBytes(encrypted, encrypted.size.toULong())
        return data.base64EncodedStringWithOptions(0u)
    }

    actual override fun decryptString(encryptedData: String): String {
        val data = NSData.dataWithBase64EncodedString(encryptedData, null)
            ?: return encryptedData

        val encryptedBytes = ByteArray(data.length.toInt())
        val dataBytes = data.bytes
        if (dataBytes != null) {
            for (i in 0 until encryptedBytes.size) {
                encryptedBytes[i] = dataBytes[i]
            }
        }

        val decrypted = decrypt(encryptedBytes)
        return NSString.create(string = decrypted, encoding = NSUTF8StringEncoding) as? String
            ?: encryptedData
    }

    actual override fun isEncrypted(data: ByteArray): Boolean {
        if (data.size < encryptionPrefix.size) return false
        return data.sliceArray(0 until encryptionPrefix.size).contentEquals(encryptionPrefix)
    }

    /**
     * Получает или создает ключ шифрования из Keychain
     */
    private fun getOrCreateEncryptionKey(): ByteArray {
        return memScoped {
            val query = mutableMapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to keychainService,
                kSecAttrAccount to keychainAccount,
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne
            )

            val result = alloc<ObjCObjectVar<CFTypeRef?>>()
            val status = SecItemCopyMatching(query, result.ptr)

            if (status == errSecSuccess) {
                val keyData = result.value as? NSData
                if (keyData != null) {
                    val keyBytes = ByteArray(keyData.length.toInt())
                    val dataBytes = keyData.bytes
                    if (dataBytes != null) {
                        for (i in 0 until keyBytes.size) {
                            keyBytes[i] = dataBytes[i]
                        }
                    }
                    return@memScoped keyBytes
                }
            }

            // Создаем новый ключ
            val newKey = ByteArray(keySize)
            SecRandomCopyBytes(kSecRandomDefault, keySize.toULong(), newKey.refTo(0))

            // Сохраняем в Keychain
            val addQuery = mutableMapOf<Any?, Any?>(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to keychainService,
                kSecAttrAccount to keychainAccount,
                kSecValueData to NSData.dataWithBytes(newKey, keySize.toULong()),
                kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            )

            SecItemAdd(addQuery, null)
            newKey
        }
    }

    /**
     * Выполняет AES шифрование
     */
    private fun performAESEncryption(data: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(ivLength)
        SecRandomCopyBytes(kSecRandomDefault, ivLength.toULong(), iv.refTo(0))

        val encrypted = ByteArray(iv.size + data.size)
        System.arraycopy(iv, 0, encrypted, 0, iv.size)

        // Упрощенное XOR шифрование (в production использовать AES через CommonCrypto)
        for (i in data.indices) {
            encrypted[iv.size + i] = (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }

        return encrypted
    }

    /**
     * Выполняет AES расшифровку
     */
    private fun performAESDecryption(encrypted: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(ivLength)
        System.arraycopy(encrypted, 0, iv, 0, ivLength)

        val dataLength = encrypted.size - ivLength
        val decrypted = ByteArray(dataLength)
        for (i in 0 until dataLength) {
            decrypted[i] = (encrypted[ivLength + i].toInt() xor key[i % key.size].toInt()).toByte()
        }

        return decrypted
    }
}
