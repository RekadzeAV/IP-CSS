package com.company.ipcamera.shared

import com.company.ipcamera.core.common.security.PasswordEncryption

/**
 * Тестовая реализация PasswordEncryption для unit тестов
 * Не использует KeyStore или другие платформо-зависимые компоненты
 * Просто добавляет префикс "ENC:" для имитации шифрования
 */
class TestPasswordEncryption : PasswordEncryption {
    override fun encrypt(password: String): String {
        if (password.isEmpty()) return ""
        if (isEncrypted(password)) return password
        return "ENC:$password"
    }

    override fun decrypt(encryptedPassword: String): String {
        if (encryptedPassword.isEmpty()) return ""
        if (!isEncrypted(encryptedPassword)) return encryptedPassword
        return encryptedPassword.removePrefix("ENC:")
    }

    override fun isEncrypted(value: String): Boolean {
        return value.startsWith("ENC:")
    }
}
