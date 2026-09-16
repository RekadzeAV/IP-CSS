package com.company.ipcamera.core.network.auth

expect object DigestCrypto {
    fun md5Hex(input: String): String
    fun sha256Hex(input: String): String
    fun secureRandomHex(byteCount: Int = 16): String
}

internal fun bytesToHex(bytes: ByteArray): String {
    val alphabet = "0123456789abcdef"
    return buildString(bytes.size * 2) {
        for (b in bytes) {
            val value = b.toInt() and 0xFF
            append(alphabet[value ushr 4])
            append(alphabet[value and 0x0F])
        }
    }
}

internal fun md5HexPure(input: String): String {
    val message = input.encodeToByteArray()
    val bitLen = message.size.toLong() * 8L
    val paddedLen = ((message.size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedLen)
    message.copyInto(padded)
    padded[message.size] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedLen - 8 + i] = ((bitLen ushr (8 * i)) and 0xFF).toByte()
    }

    var a0 = 0x67452301
    var b0 = 0xEFCDAB89.toInt()
    var c0 = 0x98BADCFE.toInt()
    var d0 = 0x10325476

    val s = intArrayOf(
        7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22,
        5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14, 20,
        4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23,
        6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
    )
    val k = IntArray(64) { i ->
        (kotlin.math.abs(kotlin.math.sin((i + 1).toDouble())) * 4294967296.0).toLong().toInt()
    }

    val m = IntArray(16)
    for (offset in padded.indices step 64) {
        for (i in 0 until 16) {
            val j = offset + i * 4
            m[i] = (padded[j].toInt() and 0xFF) or
                ((padded[j + 1].toInt() and 0xFF) shl 8) or
                ((padded[j + 2].toInt() and 0xFF) shl 16) or
                ((padded[j + 3].toInt() and 0xFF) shl 24)
        }

        var a = a0
        var b = b0
        var c = c0
        var d = d0

        for (i in 0 until 64) {
            val (f, g) = when {
                i < 16 -> ((b and c) or (b.inv() and d)) to i
                i < 32 -> ((d and b) or (d.inv() and c)) to ((5 * i + 1) % 16)
                i < 48 -> (b xor c xor d) to ((3 * i + 5) % 16)
                else -> (c xor (b or d.inv())) to ((7 * i) % 16)
            }
            val temp = d
            d = c
            c = b
            val sum = a + f + k[i] + m[g]
            b += (sum shl s[i]) or (sum ushr (32 - s[i]))
            a = temp
        }

        a0 += a
        b0 += b
        c0 += c
        d0 += d
    }

    val out = ByteArray(16)
    fun writeLe(value: Int, at: Int) {
        out[at] = (value and 0xFF).toByte()
        out[at + 1] = ((value ushr 8) and 0xFF).toByte()
        out[at + 2] = ((value ushr 16) and 0xFF).toByte()
        out[at + 3] = ((value ushr 24) and 0xFF).toByte()
    }
    writeLe(a0, 0)
    writeLe(b0, 4)
    writeLe(c0, 8)
    writeLe(d0, 12)
    return bytesToHex(out)
}

internal fun sha256HexPure(input: String): String {
    val k = intArrayOf(
        0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(), 0x3956c25b, 0x59f111f1,
        0x923f82a4.toInt(), 0xab1c5ed5.toInt(), 0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(), 0xe49b69c1.toInt(),
        0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc.toInt(),
        0x76f988da.toInt(), 0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(),
        0xbf597fc7.toInt(), 0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(),
        0x92722c85.toInt(), 0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(),
        0xc76c51a3.toInt(), 0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f,
        0x682e6ff3, 0x748f82ee.toInt(), 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
        0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
    )
    val message = input.encodeToByteArray()
    val bitLen = message.size.toLong() * 8L
    val paddedLen = ((message.size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedLen)
    message.copyInto(padded)
    padded[message.size] = 0x80.toByte()
    for (i in 0 until 8) {
        padded[paddedLen - 1 - i] = ((bitLen ushr (8 * i)) and 0xFF).toByte()
    }

    var h0 = 0x6a09e667
    var h1 = 0xbb67ae85.toInt()
    var h2 = 0x3c6ef372
    var h3 = 0xa54ff53a.toInt()
    var h4 = 0x510e527f
    var h5 = 0x9b05688c.toInt()
    var h6 = 0x1f83d9ab
    var h7 = 0x5be0cd19

    fun rotr(x: Int, n: Int): Int = (x ushr n) or (x shl (32 - n))
    val w = IntArray(64)
    for (offset in padded.indices step 64) {
        for (i in 0 until 16) {
            val j = offset + i * 4
            w[i] = ((padded[j].toInt() and 0xFF) shl 24) or
                ((padded[j + 1].toInt() and 0xFF) shl 16) or
                ((padded[j + 2].toInt() and 0xFF) shl 8) or
                (padded[j + 3].toInt() and 0xFF)
        }
        for (i in 16 until 64) {
            val s0 = rotr(w[i - 15], 7) xor rotr(w[i - 15], 18) xor (w[i - 15] ushr 3)
            val s1 = rotr(w[i - 2], 17) xor rotr(w[i - 2], 19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        var f = h5
        var g = h6
        var h = h7

        for (i in 0 until 64) {
            val s1 = rotr(e, 6) xor rotr(e, 11) xor rotr(e, 25)
            val ch = (e and f) xor (e.inv() and g)
            val t1 = h + s1 + ch + k[i] + w[i]
            val s0 = rotr(a, 2) xor rotr(a, 13) xor rotr(a, 22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val t2 = s0 + maj

            h = g
            g = f
            f = e
            e = d + t1
            d = c
            c = b
            b = a
            a = t1 + t2
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
        h5 += f
        h6 += g
        h7 += h
    }

    val out = ByteArray(32)
    fun writeBe(value: Int, at: Int) {
        out[at] = ((value ushr 24) and 0xFF).toByte()
        out[at + 1] = ((value ushr 16) and 0xFF).toByte()
        out[at + 2] = ((value ushr 8) and 0xFF).toByte()
        out[at + 3] = (value and 0xFF).toByte()
    }
    writeBe(h0, 0)
    writeBe(h1, 4)
    writeBe(h2, 8)
    writeBe(h3, 12)
    writeBe(h4, 16)
    writeBe(h5, 20)
    writeBe(h6, 24)
    writeBe(h7, 28)
    return bytesToHex(out)
}
