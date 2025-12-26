package com.company.ipcamera.core.common.security


/**
 * Интерфейс для шифрования паролей
 * Использует платформо-специфичные реализации для безопасного хранения
 */
interface PasswordEncryption {
    /**
     * Шифрует пароль перед сохранением в БД
     */
    fun encrypt(password: String): String

    /**
     * Расшифровывает пароль из БД
     */
    fun decrypt(encryptedPassword: String): String

    /**
     * Проверяет, является ли строка зашифрованной
     */
    fun isEncrypted(value: String): Boolean
}

/**
 * Общий класс для шифрования паролей
 * Использует платформо-специфичные реализации
 */
expect class SecurePasswordEncryption() : PasswordEncryption

/**
 * Фабрика для создания экземпляра шифрования
 */
object PasswordEncryptionFactory {
    fun create(): PasswordEncryption {
        return SecurePasswordEncryption()
    }
}

