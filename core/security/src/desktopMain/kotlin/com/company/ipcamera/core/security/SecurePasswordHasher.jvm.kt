package com.company.ipcamera.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * JVM (desktop) реализация хеширования паролей.
 * Использует PBKDF2WithHmacSHA256 (аналогично Android/iOS), формат:
 * $pbkdf2-sha256$iterations$salt$hash
 */
actual class SecurePasswordHasher : PasswordHasher {
    actual constructor()

    private val iterations = 100000
    private val keyLength = 32
    private val saltLength = 16

    actual override fun hash(password: CharArray): String {
        val salt = ByteArray(saltLength).apply { SecureRandom().nextBytes(this) }
        val hash = pbkdf2(password, salt, iterations, keyLength)
        val encodedSalt = Base64.getEncoder().encodeToString(salt)
        val encodedHash = Base64.getEncoder().encodeToString(hash)
        return "\$pbkdf2-sha256\$$iterations\$$encodedSalt\$$encodedHash"
    }

    actual override fun verify(password: CharArray, hashedPassword: String): Boolean {
        return try {
            val parts = hashedPassword.split("$")
            if (parts.size != 5 || parts[1] != "pbkdf2-sha256") {
                return false
            }
            val iter = parts[2].toInt()
            val salt = Base64.getDecoder().decode(parts[3])
            val expectedHash = Base64.getDecoder().decode(parts[4])
            val computedHash = pbkdf2(password, salt, iter, expectedHash.size)
            computedHash.contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }
    }

    actual override fun isValidHash(hashedPassword: String): Boolean {
        return hashedPassword.matches(
            Regex("\\\$pbkdf2-sha256\\\$\\d+\\\$[A-Za-z0-9+/=]+\\\$[A-Za-z0-9+/=]+")
        )
    }

    actual override fun getAlgorithm(): String = "PBKDF2WithHmacSHA256"

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }
}
