package com.company.ipcamera.core.network.auth

import kotlin.random.Random
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.getrandom

actual object DigestCrypto {
    actual fun md5Hex(input: String): String = md5HexPure(input)

    actual fun sha256Hex(input: String): String = sha256HexPure(input)

    actual fun secureRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        val read = bytes.usePinned { getrandom(it.addressOf(0), byteCount.toULong(), 0u) }
        if (read.toInt() != byteCount) {
            Random.nextBytes(bytes)
        }
        return bytesToHex(bytes)
    }
}
