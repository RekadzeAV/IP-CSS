package com.company.ipcamera.core.network.auth

import java.security.MessageDigest
import java.security.SecureRandom

actual object DigestCrypto {
    private val secureRandom = SecureRandom()

    actual fun md5Hex(input: String): String = digestHex("MD5", input)

    actual fun sha256Hex(input: String): String = digestHex("SHA-256", input)

    actual fun secureRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    private fun digestHex(algorithm: String, input: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        return bytesToHex(digest.digest(input.toByteArray(Charsets.UTF_8)))
    }
}
