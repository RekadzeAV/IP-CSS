package com.company.ipcamera.core.network.auth

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.arc4random_buf

actual object DigestCrypto {
    actual fun md5Hex(input: String): String = md5HexPure(input)

    actual fun sha256Hex(input: String): String = sha256HexPure(input)

    actual fun secureRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        bytes.usePinned { arc4random_buf(it.addressOf(0), byteCount.toULong()) }
        return bytesToHex(bytes)
    }
}
