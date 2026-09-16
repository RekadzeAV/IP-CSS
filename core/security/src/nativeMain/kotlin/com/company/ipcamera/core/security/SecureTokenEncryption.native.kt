package com.company.ipcamera.core.security

import kotlin.random.Random

/**
 * Native (Linux/Windows/macOS) реализация обфускации токенов.
 *
 * NB: В офлайн-окружении нет KMP-криптопровайдера с AES для нативных таргетов.
 * На время используется детерминированная XOR-обфускация с общим проектированным ключом
 * (reversible, consistent между платформами) — это защита от открытого хранения/логов,
 * НО НЕ полноценное шифрование. Для production перейти на KMP AES (напр. Krypto-lang).
 */
actual class SecureTokenEncryption : TokenEncryption {

    private val keyBytes: ByteArray = XOR_KEY

    override fun encrypt(token: String): String {
        val data = token.encodeToByteArray()
        val out = ByteArray(data.size)
        for (i in data.indices) {
            out[i] = (data[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return Base64Url.encode(out)
    }

    override fun decrypt(encryptedToken: String): String {
        val data = Base64Url.decode(encryptedToken)
        val out = ByteArray(data.size)
        for (i in data.indices) {
            out[i] = (data[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return out.decodeToString()
    }

    override fun isValid(encryptedToken: String): Boolean {
        return try {
            decrypt(encryptedToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        // Общий детерминированный ключ обфускации (ci: XOR key). В production заменить на KMP AES.
        private val XOR_KEY: ByteArray = buildKey()

        private fun buildKey(): ByteArray {
            val seed = 0x5EEDC0DE
            val rnd = Random(seed)
            return ByteArray(32) { rnd.nextInt(256).toByte() }
        }
    }
}

/**
 * Утилита Base64URL (KMP, без платформенных зависимостей).
 */
private object Base64Url {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun encode(data: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < data.size) {
            val b0 = data[i].toInt() and 0xFF
            if (i + 1 < data.size) {
                val b1 = data[i + 1].toInt() and 0xFF
                sb.append(ALPHABET[b0 ushr 2])
                sb.append(ALPHABET[((b0 shl 4) or (b1 ushr 4)) and 0x3F])
                if (i + 2 < data.size) {
                    val b2 = data[i + 2].toInt() and 0xFF
                    sb.append(ALPHABET[((b1 shl 2) or (b2 ushr 6)) and 0x3F])
                    sb.append(ALPHABET[b2 and 0x3F])
                } else {
                    sb.append(ALPHABET[(b1 shl 2) and 0x3F])
                }
            } else {
                sb.append(ALPHABET[b0 ushr 2])
                sb.append(ALPHABET[(b0 shl 4) and 0x3F])
            }
            i += 3
        }
        return sb.toString()
    }

    fun decode(s: String): ByteArray {
        val out = ArrayList<Byte>(s.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (c in s) {
            val v = ALPHABET.indexOf(c)
            if (v < 0) continue
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer ushr bits) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}