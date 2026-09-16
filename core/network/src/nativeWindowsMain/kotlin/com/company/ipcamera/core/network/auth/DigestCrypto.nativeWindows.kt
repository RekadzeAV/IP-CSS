@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.company.ipcamera.core.network.auth

import kotlin.random.Random
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.windows.BCRYPT_USE_SYSTEM_PREFERRED_RNG
import platform.windows.BCryptGenRandom

actual object DigestCrypto {
    actual fun md5Hex(input: String): String = md5HexPure(input)

    actual fun sha256Hex(input: String): String = sha256HexPure(input)

    actual fun secureRandomHex(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        val status = bytes.usePinned {
            BCryptGenRandom(
                hAlgorithm = null,
                pbBuffer = it.addressOf(0).reinterpret(),
                cbBuffer = byteCount.convert(),
                dwFlags = BCRYPT_USE_SYSTEM_PREFERRED_RNG.convert()
            )
        }
        // STATUS_SUCCESS = 0
        if (status != 0) {
            Random.nextBytes(bytes)
        }
        return bytesToHex(bytes)
    }
}
