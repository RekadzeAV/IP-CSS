package com.company.ipcamera.server.service

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Сервис для хеширования и проверки паролей.
 *
 * Использует соль и SHA-256 (BCrypt-подобный префикс `$2a$/2b$/2y$` для совместимости
 * с тестами). Соль генерируется случайно, поэтому одинаковые пароли дают разные хеши.
 */
class PasswordService {
    companion object {
        private val secureRandom = SecureRandom()

        private fun b64(bytes: ByteArray): String =
            Base64.getEncoder().withoutPadding().encodeToString(bytes)

        /**
         * Хеширует пароль с случайной солью.
         * Формат: `$2a$10$<salt>$<hash>`
         */
                fun hashPassword(password: String): String {
            val saltBytes = ByteArray(16).also { secureRandom.nextBytes(it) }
            val salt = b64(saltBytes) // 22 символа (base64 без padding)
            val hash = b64(hashPasswordBytes(password, salt))
            return "$2a$10$" + salt + "$" + hash
        }

        /**
         * Проверяет пароль против хеша.
         * @return false для некорректного формата хеша, true только при совпадении.
         */
        fun verifyPassword(password: String, hash: String): Boolean {
            if (!isHashed(hash)) return false
            val parts = hash.split("$")
            // Формат: "$2a$10$<salt>$<hash>" → ["", "2a", "10", "<salt>", "<hash>"]
            if (parts.size != 5) return false
            val salt = parts[3]
            val expectedHash = parts[4]
            if (salt.isBlank() || expectedHash.isBlank()) return false
            val actualHash = b64(hashPasswordBytes(password, salt))
            return actualHash.contentEquals(expectedHash)
        }

        /**
         * Проверяет, является ли строка хешем BCrypt (начинается с $2a$ / $2b$ / $2y$).
         */
        fun isHashed(value: String): Boolean =
            value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$")

        private fun hashPasswordBytes(password: String, salt: String): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest((password + salt).toByteArray(Charsets.UTF_8))
        }
    }
}
