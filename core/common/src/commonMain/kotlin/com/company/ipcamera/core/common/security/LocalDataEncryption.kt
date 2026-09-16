package com.company.ipcamera.core.common.security

/**
 * Интерфейс для шифрования локальных данных
 * Используется для шифрования чувствительных данных, хранящихся локально
 * (база данных, файлы настроек, кэш и т.д.)
 */
interface LocalDataEncryption {
    /**
     * Шифрует данные перед сохранением
     */
    fun encrypt(data: ByteArray): ByteArray

    /**
     * Расшифровывает данные после чтения
     */
    fun decrypt(encryptedData: ByteArray): ByteArray

    /**
     * Шифрует строку
     */
    fun encryptString(data: String): String

    /**
     * Расшифровывает строку
     */
    fun decryptString(encryptedData: String): String

    /**
     * Проверяет, зашифрованы ли данные
     */
    fun isEncrypted(data: ByteArray): Boolean
}

/**
 * Общий класс для шифрования локальных данных
 * Использует платформо-специфичные реализации
 */
expect class SecureLocalDataEncryption() : LocalDataEncryption {
    override fun encrypt(data: ByteArray): ByteArray
    override fun decrypt(encryptedData: ByteArray): ByteArray
    override fun encryptString(data: String): String
    override fun decryptString(encryptedData: String): String
    override fun isEncrypted(data: ByteArray): Boolean
}

/**
 * Фабрика для создания экземпляра шифрования локальных данных
 */
object LocalDataEncryptionFactory {
    fun create(): LocalDataEncryption {
        return SecureLocalDataEncryption()
    }
}
