package com.company.ipcamera.core.security

/**
 * Интерфейс для хеширования паролей
 * Использует безопасные алгоритмы хеширования (bcrypt, argon2, scrypt)
 */
interface PasswordHasher {
    /**
     * Хеширует пароль
     *
     * @param password Исходный пароль
     * @return Хешированный пароль с солью
     */
    fun hash(password: CharArray): String

    /**
     * Проверяет пароль против хеша
     *
     * @param password Исходный пароль
     * @param hashedPassword Хешированный пароль
     * @return true если пароль верный
     */
    fun verify(password: CharArray, hashedPassword: String): Boolean

    /**
     * Проверяет, является ли строка валидным хешем
     */
    fun isValidHash(hashedPassword: String): Boolean

    /**
     * Получает алгоритм хеширования
     */
    fun getAlgorithm(): String
}

/**
 * expect класс для кросс-платформенного хеширования паролей
 */
expect class SecurePasswordHasher : PasswordHasher {
    constructor()
    override fun hash(password: CharArray): String
    override fun verify(password: CharArray, hashedPassword: String): Boolean
    override fun isValidHash(hashedPassword: String): Boolean
    override fun getAlgorithm(): String
}

/**
 * Фабрика для создания PasswordHasher
 */
object PasswordHasherFactory {
    fun create(): PasswordHasher {
        return SecurePasswordHasher()
    }

    /**
     * Создание hasher с кастомной конфигурацией
     */
    fun create(algorithm: PasswordHashAlgorithm): PasswordHasher {
        return SecurePasswordHasher()
    }
}

/**
 * Алгоритмы хеширования паролей
 */
enum class PasswordHashAlgorithm {
    /**
     * Argon2id - рекомендованный алгоритм (победитель Password Hashing Competition)
     */
    ARGON2ID,

    /**
     * bcrypt - классический безопасный алгоритм
     */
    BCRYPT,

    /**
     * scrypt - алгоритм устойчивый к GPU/ASIC атакам
     */
    SCRYPT
}
