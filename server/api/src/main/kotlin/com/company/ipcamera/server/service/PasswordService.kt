package com.company.ipcamera.server.service

import org.mindrot.jbcrypt.BCrypt

/**
 * Сервис для хеширования и проверки паролей пользователей
 * Использует BCrypt для безопасного хеширования паролей
 */
object PasswordService {
    // Количество раундов BCrypt (чем больше, тем безопаснее, но медленнее)
    private const val BCRYPT_ROUNDS = 12
    
    /**
     * Хеширует пароль пользователя
     * @param password Пароль в открытом виде
     * @return Хеш пароля для хранения в базе данных
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS))
    }
    
    /**
     * Проверяет, соответствует ли пароль хешу
     * @param password Пароль в открытом виде
     * @param hashedPassword Хеш пароля из базы данных
     * @return true, если пароль соответствует хешу
     */
    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return try {
            BCrypt.checkpw(password, hashedPassword)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Проверяет, является ли строка BCrypt хешем
     */
    fun isHashed(value: String): Boolean {
        return value.startsWith("\$2a\$") || 
               value.startsWith("\$2b\$") || 
               value.startsWith("\$2y\$")
    }
}

