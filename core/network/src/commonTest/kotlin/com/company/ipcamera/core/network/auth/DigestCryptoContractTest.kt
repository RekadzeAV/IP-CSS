package com.company.ipcamera.core.network.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DigestCryptoContractTest {

    @Test
    fun md5PureMatchesKnownVector() {
        assertEquals(
            "900150983cd24fb0d6963f7d28e17f72",
            md5HexPure("abc")
        )
    }

    @Test
    fun sha256PureMatchesKnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256HexPure("abc")
        )
    }

    @Test
    fun bytesToHexUsesLowercaseHex() {
        assertEquals("00abff", bytesToHex(byteArrayOf(0x00, 0xAB.toByte(), 0xFF.toByte())))
    }

    @Test
    fun secureRandomHexHasExpectedLength() {
        val value = DigestCrypto.secureRandomHex(16)
        assertEquals(32, value.length)
        assertTrue(value.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
