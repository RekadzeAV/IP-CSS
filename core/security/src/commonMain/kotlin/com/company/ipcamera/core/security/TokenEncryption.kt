package com.company.ipcamera.core.security

/**
 * Интерфейс для шифрования/дешифрования токенов
 * Используется для защиты JWT, session tokens и других чувствительных данных
 */
interface TokenEncryption {
    /**
     * Шифрует токен
     */
    fun encrypt(token: String): String

    /**
     * Расшифровывает токен
     */
    fun decrypt(encryptedToken: String): String

    /**
     * Проверяет валидность токена
     */
    fun isValid(encryptedToken: String): Boolean
}

/**
 * expect класс для шифрования токенов
 */
expect class SecureTokenEncryption : TokenEncryption {
    constructor()
}

/**
 * Фабрика для создания TokenEncryption
 */
object TokenEncryptionFactory {
    fun create(): TokenEncryption {
        return SecureTokenEncryption()
    }
}
