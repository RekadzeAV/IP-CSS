package com.company.ipcamera.core.security

import platform.Foundation.*
import platform.Security.*
import kotlinx.cinterop.*
import platform.posix.memcpy

/**
 * iOS реализация хеширования паролей с использованием CommonCrypto
 */
actual class SecurePasswordHasher : PasswordHasher {

    private val iterations = 100000
    private val keyLength = 32

    actual override fun hash(password: CharArray): String {
        val passwordString = String(password)
        val salt = generateSalt()
        
        val data = passwordString.toNSData()
        val saltData = salt.toNSData()
        
        val key = pbkdf2Hash(data, saltData, iterations, keyLength)
        
        // Формат: $pbkdf2-sha256$iterations$salt$hash
        val encodedSalt = saltData.base64EncodedString()
        val encodedHash = key.base64EncodedString()
        
        return "$\$pbkdf2-sha256\$$iterations\$$encodedSalt\$$encodedHash"
    }

    actual override fun verify(password: CharArray, hashedPassword: String): Boolean {
        return try {
            val parts = hashedPassword.split("$")
            if (parts.size != 5 || parts[1] != "pbkdf2-sha256") {
                return false
            }

            val iter = parts[2].toInt()
            val salt = NSData.base64EncodedData(parts[3])
            val expectedHash = NSData.base64EncodedData(parts[4])
            val passwordData = String(password).toNSData()

            val computedHash = pbkdf2Hash(passwordData, salt, iter, expectedHash.length.toInt())
            computedHash.isEqualToData(expectedHash)
        } catch (e: Exception) {
            false
        }
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        memScoped {
            val ptr = allocArray<UInt8Var>(16)
            SecRandomCopyBytes(kSecRandomDefault, 16UL, ptr)
            for (i in 0..15) {
                salt[i] = ptr[i].toByte()
            }
        }
        return salt
    }

    private fun pbkdf2Hash(data: NSData, salt: NSData, iterations: Int, keyLength: Int): NSData {
        memScoped {
            val derivedKey = allocArray<UInt8Var>(keyLength)
            let ccKeyLen = keyLength.toUInt()
            
            val result = CCKeyDerivationPBKDF(
                kCCPBKDF2,
                data.bytes?.reinterpret<CharVar>() ?: return NSData.data(),
                data.length.toUInt(),
                salt.bytes?.reinterpret<UInt8Var>() ?: return NSData.data(),
                salt.length.toUInt(),
                kCCPRFHmacAlgSHA256,
                iterations.toUInt(),
                derivedKey,
                ccKeyLen
            )
            
            if (result == 0) {
                return NSData.dataWithData(
                    derivedKey.readBytes(keyLength)
                )
            }
            return NSData.data()
        }
    }

    private fun String.toNSData(): NSData {
        return this.toByteArray(Charsets.UTF_8).toNSData()
    }

    private fun ByteArray.toNSData(): NSData {
        return NSData.dataWithBytes(this, this.size.toLong())
    }

    private fun NSData.base64EncodedString(): String {
        return this.base64EncodedStringWithOptions(0UL)
    }

    private fun NSData.isEqualToData(other: NSData): Boolean {
        return this.isEqualToData(other)
    }

    actual override fun isValidHash(hashedPassword: String): Boolean {
        return hashedPassword.matches(Regex("\\\$pbkdf2-sha256\\\$\\d+\\\$[A-Za-z0-9+/=]+\\\$[A-Za-z0-9+/=]+"))
    }

    actual override fun getAlgorithm(): String = "PBKDF2-SHA256"
}
