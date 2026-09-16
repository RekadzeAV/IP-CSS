package com.company.ipcamera.server.notification

import java.util.Base64

/**
 * Утилиты построения JWS-компонентов для APNs (ES256).
 * Вынесены отдельно от ApnsNotificationSender для unit-тестирования криптографических примитивов.
 */
internal object ApnsJwtUtil {

    /** base64url без padding. */
    fun b64url(s: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray(Charsets.UTF_8))

    /** base64url без padding для байтов (подпись). */
    fun b64url(data: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(data)

    /**
     * Минимальный JSON-сериализатор для небольшого фиксированного набора полей JWT
     * (alg/kid/typ/iss/iat/exp). Не предназначен для общего назначения.
     */
    fun jsonOf(map: Map<String, Any>): String {
        val sb = StringBuilder("{")
        map.entries.forEachIndexed { idx, (k, v) ->
            if (idx > 0) sb.append(',')
            sb.append('"').append(k).append("\":")
            when (v) {
                is String -> sb.append('"').append(v).append('"')
                is Number -> sb.append(v)
                else -> sb.append(v)
            }
        }
        sb.append('}')
        return sb.toString()
    }

    /**
     * Конвертирует DER-кодированную ECDSA-подпись (SHA256withECDSA, P-256) в JWS raw R||S (64 байта).
     * DER: 30 <len> 02 <lenR> R... 02 <lenS> S...
     */
    fun derToRawJws(der: ByteArray): ByteArray {
        if (der.size < 8 || der[0].toInt() != 0x30) throw IllegalArgumentException("Invalid DER sequence")
        var i = 2 // skip 30 + length byte(s) для коротких P-256 подписей
        fun readInt(): ByteArray {
            if (der[i].toInt() != 0x02) throw IllegalArgumentException("Invalid DER integer tag")
            i++
            var len = der[i].toInt() and 0xFF
            i++
            if (len and 0x80 != 0) {
                val numBytes = len and 0x7F
                len = 0
                repeat(numBytes) { len = (len shl 8) or (der[i].toInt() and 0xFF); i++ }
            }
            val start = i
            i += len
            var arr = der.copyOfRange(start, i)
            // удаляем ведущий нуль-байт, добавленный для положительного знака
            while (arr.size > 1 && arr[0].toInt() == 0 && arr[1].toInt() and 0x80 != 0) {
                arr = arr.copyOfRange(1, arr.size)
            }
            if (arr.size > 32) throw IllegalArgumentException("EC coordinate too long")
            if (arr.size < 32) arr = ByteArray(32 - arr.size) { 0 } + arr
            return arr
        }
        val r = readInt()
        val s = readInt()
        return r + s
    }
}