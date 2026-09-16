package com.company.ipcamera.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/**
 * Android реализация шифрования токенов с использованием Android Keystore
 */
actual class SecureTokenEncryption : TokenEncryption {
    
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }
    
    private val keyAlias = "token_encryption_key"
    
    private val key: SecretKey = generateKey()
    
    private fun generateKey(): SecretKey {
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
                .build()
            )
            keyGenerator.generateKey()
        }
        return keyStore.getKey(keyAlias, null) as SecretKey
    }
    
    override fun encrypt(token: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP or Base64.NO_PADDING)
    }
    
    override fun decrypt(encryptedToken: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decrypted = cipher.doFinal(Base64.decode(encryptedToken, Base64.NO_WRAP or Base64.NO_PADDING))
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