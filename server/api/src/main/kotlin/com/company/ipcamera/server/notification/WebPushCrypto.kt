package com.company.ipcamera.server.notification

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Криптография Web Push:
 * - ES256 (P-256) подпись VAPID JWT;
 * - шифрование payload по RFC 8291 (aes128gcm): ECDH + HKDF + AES-128-GCM.
 * Реализовано на чистом JCA, без внешних web-push библиотек.
 */
internal object WebPushCrypto {

    private val random = SecureRandom()

    /** Параметры кривой P-256 (prime256v1). */
    private val p256Params: ECParameterSpec by lazy {
        AlgorithmParameters.getInstance("EC").apply {
            init(ECGenParameterSpec("secp256r1"))
        }.getParameterSpec(ECParameterSpec::class.java)
    }

    // ---------- Генерация VAPID ключей ----------

    /** Генерирует пару P-256 ключей; возвращает raw-представления (base64url совместимые). */
    fun generateVapidKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(p256Params) }.generateKeyPair()

    /** Raw публичный ключ (0x04 || X || Y, 65 байт). */
    fun rawPublicKey(key: PublicKey): ByteArray {
        val ec = key as ECPublicKey
        val x = toFixed32(ec.w.affineX)
        val y = toFixed32(ec.w.affineY)
        return byteArrayOf(0x04.toByte()) + x + y
    }

    /** Raw приватный ключ (32 байта big-endian). */
    fun rawPrivateKey(key: PrivateKey): ByteArray = toFixed32((key as ECPrivateKey).s)

    private fun toFixed32(v: BigInteger): ByteArray {
        val b = v.toByteArray()
        return when {
            b.size == 32 -> b
            b.size > 32 -> b.copyOfRange(b.size - 32, b.size)
            else -> ByteArray(32 - b.size) { 0 } + b
        }
    }

    // ---------- Загрузка ключей из raw/base64url ----------

    /** Восстановить приватный EC-ключ из raw 32 байт. */
    fun privateKeyFromRaw(raw32: ByteArray): PrivateKey {
        require(raw32.size == 32) { "EC private key must be 32 bytes, got ${raw32.size}" }
        val d = BigInteger(1, raw32)
        return KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(d, p256Params))
    }

    /** Восстановить публичный EC-ключ из uncompressed raw (0x04||X||Y). */
    fun publicKeyFromRaw(raw: ByteArray): PublicKey {
        require(raw.size == 65 && raw[0] == 0x04.toByte()) { "Public key must be uncompressed 65-byte point" }
        val x = BigInteger(1, raw.copyOfRange(1, 33))
        val y = BigInteger(1, raw.copyOfRange(33, 65))
        val w = ECPoint(x, y)
        return KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(w, p256Params))
    }

    /** Декодирование base64url (с паддинг-толерантностью). */
    fun b64UrlDecode(s: String): ByteArray {
        val normalized = s.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return Base64.getDecoder().decode(padded)
    }

    /** Кодирование base64url без паддинга. */
    fun b64UrlEncode(data: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data)

    // ---------- VAPID ES256 ----------

    /**
     * Подписывает VAPID JWT (ES256, raw R||S) приватным ключом.
     * @param audience origin push-сервиса (например https://fcm.googleapis.com)
     */
    fun signVapidJwt(
        privateKey: PrivateKey,
        audience: String,
        subject: String,
        nowEpochSecond: Long,
        expiresInSeconds: Long = 12 * 3600
    ): String {
        val header = """{"typ":"JWT","alg":"ES256"}"""
        val claims = """{"aud":"$audience","exp":${nowEpochSecond + expiresInSeconds},"sub":"$subject"}"""
        val signingInput = b64UrlEncode(header.toByteArray()) + "." + b64UrlEncode(claims.toByteArray())

        val derSignature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(signingInput.toByteArray(Charsets.UTF_8))
        }.sign()

        val raw = derToRawJws(derSignature)
        return "$signingInput.${b64UrlEncode(raw)}"
    }

    /** Конвертация DER ECDSA-подписи в JWS raw R||S (делегирует общей утилите APNs). */
    fun derToRawJws(der: ByteArray): ByteArray = ApnsJwtUtil.derToRawJws(der)

    // ---------- HKDF ----------

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(if (key.isEmpty()) ByteArray(32) else key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    /** HKDF-Extract. */
    fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray = hmacSha256(salt, ikm)

    /** HKDF-Expand. */
    fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val hashLen = 32
        val n = (length + hashLen - 1) / hashLen
        require(n <= 255) { "Too many HKDF blocks" }
        var t = ByteArray(0)
        val okm = ByteArray(length)
        var offset = 0
        for (i in 1..n) {
            val input = t + info + byteArrayOf(i.toByte())
            t = hmacSha256(prk, input)
            val copyLen = minOf(hashLen, length - offset)
            System.arraycopy(t, 0, okm, offset, copyLen)
            offset += copyLen
        }
        return okm
    }

    /** HKDF (extract+expand). */
    fun hkdf(salt: ByteArray, ikm: ByteArray, info: ByteArray, length: Int): ByteArray =
        hkdfExpand(hkdfExtract(salt, ikm), info, length)

    // ---------- RFC 8291 (aes128gcm) ----------

    /** Публичная версия: генерирует ephemeral пару и salt, делегирует детерминированной перегрузке. */
    fun encryptAes128Gcm(
        userPublicKeyRaw: ByteArray,
        authSecretRaw: ByteArray,
        payload: ByteArray
    ): ByteArray {
        val ephemeral = generateVapidKeyPair()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        return encryptAes128Gcm(userPublicKeyRaw, authSecretRaw, payload, ephemeral, salt)
    }

    /**
     * Детерминированная версия (для тестов): явные ephemeral пара и salt.
     */
    fun encryptAes128Gcm(
        userPublicKeyRaw: ByteArray,
        authSecretRaw: ByteArray,
        payload: ByteArray,
        ephemeralPair: KeyPair,
        salt: ByteArray
    ): ByteArray {
        val asPrivate = ephemeralPair.private
        val asPublicRaw = rawPublicKey(ephemeralPair.public)
        val uaPublic = publicKeyFromRaw(userPublicKeyRaw)

        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(asPrivate)
        agreement.doPhase(uaPublic, true)
        val ecdhSecret = agreement.generateSecret()

        val keyInfo = "WebPush: info".toByteArray() + byteArrayOf(0) + userPublicKeyRaw + asPublicRaw
        val prkKey = hkdf(authSecretRaw, ecdhSecret, keyInfo, 32)

        val recordSize = 4096
        val cek = hkdf(salt, prkKey, "Content-Encoding: aes128gcm".toByteArray() + byteArrayOf(1), 16)
        val nonce = hkdf(salt, prkKey, "Content-Encoding: nonce".toByteArray() + byteArrayOf(1), 12)

        val maxPlain = recordSize - 17 - 1
        require(payload.size <= maxPlain) { "Payload too large for single aes128gcm record" }
        val paddingLen = maxPlain - payload.size
        val plain = payload + byteArrayOf(2.toByte()) + ByteArray(paddingLen)

        val idhLen = asPublicRaw.size
        val aad = salt +
            byteArrayOf(
                (recordSize shr 24).toByte(), (recordSize shr 16).toByte(),
                (recordSize shr 8).toByte(), recordSize.toByte()
            ) +
            byteArrayOf(idhLen.toByte()) + asPublicRaw
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(cek, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
                val ciphertext = cipher.doFinal(plain)

        return byteArrayOf(1.toByte()) + salt +
            byteArrayOf(
                (recordSize shr 24).toByte(), (recordSize shr 16).toByte(),
                (recordSize shr 8).toByte(), recordSize.toByte()
            ) +
            byteArrayOf(idhLen.toByte()) + asPublicRaw + ciphertext
    }

    /** Заголовок Authorization VAPID: "vapid t=<jwt>, k=<raw pubkey b64url>". */
    fun vapidAuthorizationHeader(jwt: String, vapidPublicKeyRaw: ByteArray): String =
        "vapid t=$jwt, k=${b64UrlEncode(vapidPublicKeyRaw)}"
}