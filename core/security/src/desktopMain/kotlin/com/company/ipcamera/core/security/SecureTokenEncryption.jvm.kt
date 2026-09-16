package com.company.ipcamera.core.security

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

/**
 * JVM реализация шифрования токенов
 * Временная заглушка - использует AES
 */
actual class SecureTokenEncryption : TokenEncryption {
    
    private val key: SecretKey = generateKey()
    
    private fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }
    
    override fun encrypt(token: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(encrypted)
    }
    
    override fun decrypt(encryptedToken: String): String {
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedToken))
        return String(decrypted, Charsets.UTF_8)
    }
    
    override fun isValid(encryptedToken: String): Boolean {
        return try {
            decrypt(encryptedToken)
            true
        } catch (e: Exception) {
            false
        }
    }
}