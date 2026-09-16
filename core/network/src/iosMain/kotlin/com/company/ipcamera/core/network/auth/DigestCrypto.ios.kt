package com.company.ipcamera.core.network.auth

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import kotlin.random.Random

actual object DigestCrypto {
    actual fun md5Hex(input: String): String = md5HexPure(input)

    actual fun sha256Hex(input: String): String = sha256HexPure(input)

    actual fun secureRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        val status = bytes.usePinned {
            SecRandomCopyBytes(kSecRandomDefault, byteCount.toULong(), it.addressOf(0))
        }
        if (status != errSecSuccess) {
            Random.nextBytes(bytes)
        }
        return bytesToHex(bytes)
    }
}
