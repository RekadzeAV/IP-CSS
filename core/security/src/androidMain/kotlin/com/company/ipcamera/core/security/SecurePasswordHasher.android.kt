package com.company.ipcamera.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.SecureRandom
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import android.util.Base64

/**
 * Android реализация хеширования паролей с использованием Android Keystore
 */
actual class SecurePasswordHasher : PasswordHasher {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val keyAlias = "password_hash_key"
    private val saltLength = 32
    private val iterations = 100000

    init {
        initKey()
    }

    private fun initKey() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(keyAlias, null) as SecretKey
    }

    actual override fun hash(password: CharArray): String {
        val salt = ByteArray(saltLength).apply { SecureRandom().nextBytes(this) }
        val passwordBytes = String(password).toByteArray()
        
        // PBKDF2 для хеширования
        val hash = pbkdf2(passwordBytes, salt, iterations, 32)
        
        // Формат: $pbkdf2-sha256$iterations$salt$hash (Base64 флаги: NO_WRAP | NO_PADDING — совместимо с java.util.Base64)
        val encodedSalt = Base64.encodeToString(salt, Base64.NO_WRAP or Base64.NO_PADDING)
        val encodedHash = Base64.encodeToString(hash, Base64.NO_WRAP or Base64.NO_PADDING)
        
        return "$\$pbkdf2-sha256\$$iterations\$$encodedSalt\$$encodedHash"
    }

    actual override fun verify(password: CharArray, hashedPassword: String): Boolean {
        return try {
            val parts = hashedPassword.split("$")
            if (parts.size != 5 || parts[1] != "pbkdf2-sha256") {
                return false
            }

            val iterations = parts[2].toInt()
            val salt = Base64.decode(parts[3], Base64.NO_WRAP or Base64.NO_PADDING)
            val expectedHash = Base64.decode(parts[4], Base64.NO_WRAP or Base64.NO_PADDING)
            val passwordBytes = String(password).toByteArray()

            val computedHash = pbkdf2(passwordBytes, salt, iterations, expectedHash.size)
            computedHash.contentEquals(expectedHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(
            String(password).toCharArray(),
            salt,
            iterations,
            keyLength * 8
        )
        val skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    actual override fun isValidHash(hashedPassword: String): Boolean {
        return hashedPassword.matches(Regex("\\\$pbkdf2-sha256\\\$\\d+\\\$[A-Za-z0-9+/=]+\\\$[A-Za-z0-9+/=]+"))
    }

    actual override fun getAlgorithm(): String = "PBKDF2WithHmacSHA256"
}
