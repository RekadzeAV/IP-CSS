package com.company.ipcamera.server.notification

import java.security.KeyPairGenerator
import java.security.Signature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit-тесты криптографических примитивов APNs JWT (ApnsJwtUtil).
 * Проверяют base64url, минимальный JSON и конвертацию DER-подписи EC P-256 в raw R||S.
 */
class ApnsJwtUtilTest {

    @Test
    fun `b64url encodes without padding and url-safe`() {
        val encoded = ApnsJwtUtil.b64url("iss")
        assertTrue(!encoded.contains('='), "No padding expected, got: $encoded")
        assertTrue(!encoded.contains('+') && !encoded.contains('/'), "Not url-safe: $encoded")
        assertEquals("aXNz", encoded)
    }

    @Test
    fun `jsonOf builds simple object`() {
        val json = ApnsJwtUtil.jsonOf(mapOf("alg" to "ES256", "kid" to "ABC123", "iat" to 5L))
        assertEquals("{\"alg\":\"ES256\",\"kid\":\"ABC123\",\"iat\":5}", json)
    }

    @Test
    fun `derToRawJws converts P-256 DER signature to 64-byte raw`() {
        val kp = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val der = sign("test-input".toByteArray(Charsets.US_ASCII), kp)

        val raw = ApnsJwtUtil.derToRawJws(der)
        assertEquals(64, raw.size, "raw R||S must be 64 bytes for P-256")
    }

    @Test
    fun `derToRawJws result re-encoded as DER verifies with public key`() {
        val kp = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val data = "verify-me".toByteArray(Charsets.US_ASCII)

        val der = sign(data, kp)
        val raw = ApnsJwtUtil.derToRawJws(der)

        val r = raw.copyOfRange(0, 32)
        val s = raw.copyOfRange(32, 64)
        val rebuiltDer = derSequence(r, s)

        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(kp.public)
        verifier.update(data)
        assertTrue(verifier.verify(rebuiltDer), "Re-built DER signature must verify")
    }

    private fun sign(data: ByteArray, kp: java.security.KeyPair): ByteArray {
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(kp.private)
        signer.update(data)
        return signer.sign()
    }

    /** Собирает DER-последовательность 30 <len> <int(R)> <int(S)>. */
    private fun derSequence(r: ByteArray, s: ByteArray): ByteArray {
        val rInt = encodeInt(r)
        val sInt = encodeInt(s)
        val body = rInt + sInt
        return byteArrayOf(0x30, body.size.toByte()) + body
    }

    private fun encodeInt(raw: ByteArray): ByteArray {
        var i = 0
        while (i < raw.size - 1 && raw[i].toInt() == 0 && raw[i + 1].toInt() and 0x80 != 0) i++
        val trimmed = raw.copyOfRange(i, raw.size)
        val positive = if (trimmed[0].toInt() and 0x80 != 0) byteArrayOf(0) + trimmed else trimmed
        return byteArrayOf(0x02, positive.size.toByte()) + positive
    }

    private fun ByteArray.plus(other: ByteArray): ByteArray {
        val out = ByteArray(this.size + other.size)
        this.copyInto(out, 0)
        other.copyInto(out, this.size)
        return out
    }
}