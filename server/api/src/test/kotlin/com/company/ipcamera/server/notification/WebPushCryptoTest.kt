package com.company.ipcamera.server.notification

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebPushCryptoTest {

    @Test
    fun `generated vapid keys have correct raw sizes and restore back`() {
        val pair = WebPushCrypto.generateVapidKeyPair()
        val pubRaw = WebPushCrypto.rawPublicKey(pair.public)
        val privRaw = WebPushCrypto.rawPrivateKey(pair.private)

        assertEquals(65, pubRaw.size)
        assertEquals(0x04.toByte(), pubRaw[0])
        assertEquals(32, privRaw.size)

        val restoredPub = WebPushCrypto.publicKeyFromRaw(pubRaw)
        val restoredPriv = WebPushCrypto.privateKeyFromRaw(privRaw)
        assertEquals(WebPushCrypto.rawPublicKey(restoredPub).toList(), pubRaw.toList())
        assertEquals(WebPushCrypto.rawPrivateKey(restoredPriv).toList(), privRaw.toList())
    }

    @Test
    fun `vapid jwt signature verifies with public key`() {
        val pair = WebPushCrypto.generateVapidKeyPair()
        val audience = "https://push.example.com"
        val jwt = WebPushCrypto.signVapidJwt(pair.private, audience, "mailto:ops@example.com", nowEpochSecond = 1_700_000_000L)

        val parts = jwt.split(".")
        assertEquals(3, parts.size)
        val header = String(java.util.Base64.getUrlDecoder().decode(parts[0]))
        val claims = String(java.util.Base64.getUrlDecoder().decode(parts[1]))
        assertTrue(header.contains("ES256"))
        assertTrue(claims.contains(audience))
        assertTrue(claims.contains("mailto:ops@example.com"))

        val rawSig = java.util.Base64.getUrlDecoder().decode(parts[2])
        assertEquals(64, rawSig.size)
        val r = rawSig.copyOfRange(0, 32)
        val s = rawSig.copyOfRange(32, 64)
        fun derInt(v: ByteArray): ByteArray {
            var start = 0
            while (start < v.size - 1 && v[start].toInt() == 0) start++ // убираем ведущие нули
            var arr = v.copyOfRange(start, v.size)
            if (arr[0].toInt() and 0x80 != 0) arr = byteArrayOf(0) + arr // знаковый бит
            assertTrue(arr.size <= 127, "DER int too long for short form")
            return byteArrayOf(0x02, arr.size.toByte()) + arr
        }
        val body = derInt(r) + derInt(s)
        val der = byteArrayOf(0x30, body.size.toByte()) + body

        val verifier = Signature.getInstance("SHA256withECDSA").apply {
            initVerify(pair.public)
            update((parts[0] + "." + parts[1]).toByteArray(Charsets.UTF_8))
        }
        assertTrue(verifier.verify(der), "ES256 подпись JWT должна верифицироваться публичным ключом")
    }

    @Test
    fun `hkdf rfc5869 test vector case 1`() {
        val ikm = ByteArray(22) { 0x0b }
        val salt = hexToBytes("000102030405060708090a0b0c")
        val info = hexToBytes("f0f1f2f3f4f5f6f7f8f9")
        val okm = WebPushCrypto.hkdf(salt, ikm, info, 42)
        assertEquals(
            "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
            okm.joinToString("") { "%02x".format(it) }
        )
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) {
            (((hex[it * 2].digitToInt(16)) shl 4) or hex[it * 2 + 1].digitToInt(16)).toByte()
        }
}

/** Расшифровка aes128gcm как это делает браузер: user private + as public из idh тела. */
private fun decryptAsBrowser(
    userPrivateRaw: ByteArray,
    userPublicRaw: ByteArray,
    authSecret: ByteArray,
    body: ByteArray
): ByteArray {
    org.junit.jupiter.api.Assertions.assertEquals(1.toByte(), body[0])
    val salt = body.copyOfRange(1, 17)
    val recordSize = ((body[17].toInt() and 0xFF) shl 24) or ((body[18].toInt() and 0xFF) shl 16) or
        ((body[19].toInt() and 0xFF) shl 8) or (body[20].toInt() and 0xFF)
    val idhLen = body[21].toInt() and 0xFF
    val asPublicRaw = body.copyOfRange(22, 22 + idhLen)
    val ciphertext = body.copyOfRange(22 + idhLen, body.size)

    val userPriv = WebPushCrypto.privateKeyFromRaw(userPrivateRaw)
    val senderPub = WebPushCrypto.publicKeyFromRaw(asPublicRaw)

    val agreement = KeyAgreement.getInstance("ECDH").apply { init(userPriv); doPhase(senderPub, true) }
    val ecdhSecret = agreement.generateSecret()

    val keyInfo = "WebPush: info".toByteArray() + byteArrayOf(0) +
        userPublicRaw + asPublicRaw
    val prkKey = WebPushCrypto.hkdf(authSecret, ecdhSecret, keyInfo, 32)

    val cek = WebPushCrypto.hkdf(salt, prkKey, "Content-Encoding: aes128gcm".toByteArray() + byteArrayOf(1), 16)
    val nonce = WebPushCrypto.hkdf(salt, prkKey, "Content-Encoding: nonce".toByteArray() + byteArrayOf(1), 12)

    val aad = salt +
        byteArrayOf(
            (recordSize shr 24).toByte(), (recordSize shr 16).toByte(),
            (recordSize shr 8).toByte(), recordSize.toByte()
        ) +
        byteArrayOf(idhLen.toByte()) + asPublicRaw
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(cek, "AES"), GCMParameterSpec(128, nonce))
    cipher.updateAAD(aad)
    val plain = cipher.doFinal(ciphertext)
    val delimiterIdx = plain.indexOfLast { it == 2.toByte() }
    kotlin.test.assertTrue(delimiterIdx > 0, "padding delimiter 0x02 not found")
    return plain.copyOfRange(0, delimiterIdx)
}

class WebPushCryptoRoundtripTest {

    @Test
    fun `aes128gcm roundtrip - browser can decrypt`() {
        val userPair = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair()
        val userPubRaw = WebPushCrypto.rawPublicKey(userPair.public)
        val userPrivRaw = WebPushCrypto.rawPrivateKey(userPair.private)
        val authSecret = ByteArray(16) { it.toByte() }

        val payload = """{"title":"Motion","body":"Front door"}""".toByteArray(Charsets.UTF_8)

        val ephemeral = WebPushCrypto.generateVapidKeyPair()
        val salt = ByteArray(16) { (it * 7 + 1).toByte() }

        val encrypted = WebPushCrypto.encryptAes128Gcm(
            userPublicKeyRaw = userPubRaw,
            authSecretRaw = authSecret,
            payload = payload,
            ephemeralPair = ephemeral,
            salt = salt
        )

        assertTrue(encrypted.size > 22 + 65)
        org.junit.jupiter.api.Assertions.assertEquals(1.toByte(), encrypted[0])
        assertTrue(encrypted.copyOfRange(1, 17).contentEquals(salt), "salt в теле должен совпадать")

        val decrypted = decryptAsBrowser(userPrivRaw, userPubRaw, authSecret, encrypted)
        org.junit.jupiter.api.Assertions.assertEquals(String(payload, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))
    }
}

/** Независимая (эталонная) реализация RFC 8291 для перекрёстной проверки. */
class WebPushCryptoCrossCheckTest {

    private fun hkdf(salt: ByteArray, ikm: ByteArray, info: ByteArray, len: Int): ByteArray {
        val hmac = javax.crypto.Mac.getInstance("HmacSHA256")
        fun mac(key: ByteArray): (ByteArray) -> ByteArray { val m = javax.crypto.Mac.getInstance("HmacSHA256"); m.init(javax.crypto.spec.SecretKeySpec(key, "HmacSHA256")); return { d -> m.doFinal(d) } }
        val prk = mac(if (salt.isEmpty()) ByteArray(32) else salt)(ikm)
        var t = ByteArray(0)
        val okm = ByteArray(len)
        var off = 0
        var counter = 1
        while (off < len) {
            t = mac(prk)(t + info + byteArrayOf(counter.toByte()))
            val n = minOf(t.size, len - off)
            System.arraycopy(t, 0, okm, off, n)
            off += n
            counter++
        }
        return okm
    }

    @Test
    fun `cross check encrypt against independent implementation`() {
        val userPair = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val userPubRaw = WebPushCrypto.rawPublicKey(userPair.public)
        val authSecret = ByteArray(16) { (it * 3).toByte() }
        val payload = """{"msg":"hello"}""".toByteArray()
        val eph = WebPushCrypto.generateVapidKeyPair()
        val salt = ByteArray(16) { (it * 11).toByte() }

        // Эталонный расчёт
        val asPubRaw = WebPushCrypto.rawPublicKey(eph.public)
        val ka = KeyAgreement.getInstance("ECDH").apply {
            init(eph.private); doPhase(WebPushCrypto.publicKeyFromRaw(userPubRaw), true)
        }
        val secret = ka.generateSecret()
        val keyInfo = "WebPush: info".toByteArray() + byteArrayOf(0) + userPubRaw + asPubRaw
        val prk = hkdf(authSecret, secret, keyInfo, 32)
        val cek = hkdf(salt, prk, "Content-Encoding: aes128gcm".toByteArray() + byteArrayOf(1), 16)
        val nonce = hkdf(salt, prk, "Content-Encoding: nonce".toByteArray() + byteArrayOf(1), 12)
        val rs = 4096
        val maxPlain = rs - 17 - 1
        val plain = payload + byteArrayOf(2) + ByteArray(maxPlain - payload.size)
        val aad = salt + byteArrayOf(0, 0, 16, 0) + byteArrayOf(65.toByte()) + asPubRaw
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(cek, "AES"), GCMParameterSpec(128, nonce))
        c.updateAAD(aad)
        val expectedBody = byteArrayOf(1) + salt + byteArrayOf(0, 0, 16, 0) +
            byteArrayOf(65.toByte()) + asPubRaw + c.doFinal(plain)

        val actual = WebPushCrypto.encryptAes128Gcm(userPubRaw, authSecret, payload, eph, salt)

        

        
        // Детальный отчёт о расхождениях
        val diffs = StringBuilder()
        var mismatchCount = 0
        for (idx in expectedBody.indices) {
            val a = actual.getOrNull(idx)
            if (a == null || a != expectedBody[idx]) {
                mismatchCount++
                if (mismatchCount <= 6) diffs.append("[$idx] exp=${expectedBody[idx]} act=$a; ")
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(expectedBody.size, actual.size,
            "size mismatch: exp=${expectedBody.size} act=${actual.size}; first diffs: $diffs")
        org.junit.jupiter.api.Assertions.assertTrue(mismatchCount == 0, "byte diffs=$mismatchCount: $diffs")
    }
}