package com.company.ipcamera.server.service

import java.security.SecureRandom

/**
 * Генератор SDP answer из SDP offer клиента (fallback-режим без медиа-сервера).
 *
 * Ранее возвращался статический «placeholder» SDP с фиксированным payload-type 96 (H264)
 * и фиктивными ICE-атрибутами — браузер отклонял такой answer из-за несовпадения кодеков
 * и ICE-параметров с offer. Теперь answer строится из offer:
 *  - media-профиль и список payload-type наследуются из `m=` строки offer;
 *  - `a=rtpmap`/`a=fmtp` кодеков копируются из соответствующей media-секции offer;
 *  - DTLS fingerprint, ice-ufrag/pwd, mid наследуются из offer (если заданы);
 *  - направление зеркалится: offer `sendonly` → answer `recvonly` и наоборот;
 *  - `setup:actpass` → `setup:active` (сервер — DTLS-клиент);
 *  - session-id переиспользуется, version увеличивается на 1.
 *
 * Ограничение: это сигнальный answer без реальной медиа-транспортной ноги
 * (без медиа-сервера поток идти не будет), но SDP валиден и проходит
 * setattrDescription в браузере, что делает fallback-ветку тестируемой и корректной.
 */
object OfferSdpAnswerFactory {

    private val random = SecureRandom()

    /** Секция SDP: строки от одной `m=` до следующей `m=` (или конца). */
    private data class MediaSection(
        val mLine: String,
        val attributes: List<String>
    )

    fun buildAnswer(offer: String): String {
        val lines = offer.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val sessionAttrs = mutableListOf<String>()
        val mediaSections = mutableListOf<MediaSection>()
        var currentMLine: String? = null
        var currentAttrs: MutableList<String> = mutableListOf()

        for (line in lines) {
            if (line.startsWith("m=")) {
                currentMLine?.let { mediaSections += MediaSection(it, currentAttrs.toList()) }
                currentMLine = line
                currentAttrs = mutableListOf()
            } else if (currentMLine == null) {
                // Сессионные атрибуты до первой m=
                sessionAttrs += line
            } else {
                currentAttrs += line
            }
        }
        currentMLine?.let { mediaSections += MediaSection(it, currentAttrs.toList()) }

        val (sessId, sessVersionBase) = parseOLine(offer) ?: (randomSessionId() to 1L)
        val sessVersion = sessVersionBase + 1

        val out = StringBuilder()
        out.append("v=0\r\n")
        out.append("o=- ").append(sessId).append(' ').append(sessVersion).append(" IN IP4 0.0.0.0\r\n")
        out.append("s=-\r\n")
        out.append("t=0 0\r\n")

        // BUNDLE-группа: зеркалим mids из offer
        val bundleGroup = sessionAttrs.firstOrNull { it.startsWith("a=group:") }
        if (bundleGroup != null) out.append(bundleGroup).append("\r\n")
        out.append("a=msid-semantic: WMS\r\n")

        // Выбираем video-секцию; если её нет — первую media-секцию.
        val section = mediaSections.firstOrNull { it.mLine.startsWith("m=video") }
            ?: mediaSections.firstOrNull()

        if (section == null) {
            // Offer без media-строк: минимальный синтаксически валидный answer.
            out.append("m=video 9 UDP/TLS/RTP/SAVPF 96\r\n")
            out.append("c=IN IP4 0.0.0.0\r\n")
            out.append("a=recvonly\r\n")
            out.append("a=rtcp-mux\r\n")
            return out.toString()
        }

        val mParts = section.mLine.split(Regex("\\s+"))
        // m=<media> <port> <proto> <fmt>...
        val mediaType = mParts.getOrNull(0)?.removePrefix("m=")?.takeIf { it.isNotEmpty() } ?: "video"
        val proto = mParts.getOrNull(2) ?: "UDP/TLS/RTP/SAVPF"
        val payloadTypes = mParts.drop(3).filter { it.toIntOrNull() != null }

        val attrs = section.attributes
        val mid = attrs.firstOrNull { it.startsWith("a=mid:") }?.removePrefix("a=mid:") ?: "0"
        val offerDirection = attrs.firstOrNull { it.startsWith("a=sendonly") || it.startsWith("a=recvonly") || it.startsWith("a=inactive") }
        val answerDirection = when (offerDirection) {
            "a=sendonly" -> "a=recvonly"
            "a=recvonly" -> "a=sendonly"
            "a=inactive" -> "a=inactive"
            else -> "a=recvonly"
        }
        val ufrag = attrs.firstOrNull { it.startsWith("a=ice-ufrag:") }?.removePrefix("a=ice-ufrag:") ?: randomHex(8)
        val pwd = attrs.firstOrNull { it.startsWith("a=ice-pwd:") }?.removePrefix("a=ice-pwd:") ?: randomHex(24)
        val fingerprint = attrs.firstOrNull { it.startsWith("a=fingerprint:") }
        val setup = when (attrs.firstOrNull { it.startsWith("a=setup:") }?.removePrefix("a=setup:")) {
            "passive" -> "a=setup:passive"
            "holdconn" -> "a=setup:holdconn"
            else -> "a=setup:active" // offer actpass или active → сервер берёт active/пассивную роль согласованно
        }
        val rtcpMux = attrs.any { it == "a=rtcp-mux" }

        // Кодеки секции: rtpmap/fmtp для payload-type из m=строки
        val rtpmaps = attrs.filter { it.startsWith("a=rtpmap:") && it.payloadTypeIn(payloadTypes) }
        val fmtps = attrs.filter { it.startsWith("a=fmtp:") && it.payloadTypeIn(payloadTypes) }

        out.append("m=").append(mediaType).append(" 9 ").append(proto).append(' ')
        out.append((payloadTypes.ifEmpty { listOf("96") }).joinToString(" "))
        out.append("\r\n")
        out.append("c=IN IP4 0.0.0.0\r\n")
        out.append("a=rtcp:9 IN IP4 0.0.0.0\r\n")
        out.append("a=ice-ufrag:").append(ufrag).append("\r\n")
        out.append("a=ice-pwd:").append(pwd).append("\r\n")
        if (fingerprint != null) out.append(fingerprint).append("\r\n")
        out.append(setup).append("\r\n")
        out.append("a=mid:").append(mid).append("\r\n")
        out.append(answerDirection).append("\r\n")
        if (rtcpMux) out.append("a=rtcp-mux\r\n")
        rtpmaps.forEach { out.append(it).append("\r\n") }
        fmtps.forEach { out.append(it).append("\r\n") }

        return out.toString()
    }

    private fun parseOLine(offer: String): Pair<String, Long>? =
        offer.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("o=") }
            ?.split(Regex("\\s+"))
            ?.takeIf { it.size >= 3 }
            ?.let { parts ->
                val id = parts.getOrNull(1)?.takeIf { it.isNotEmpty() && it != "-" }
                val version = parts.getOrNull(2)?.toLongOrNull()
                if (id != null && version != null) id to version else null
            }

    private fun String.payloadTypeIn(pts: List<String>): Boolean {
        val pt = substringAfter(':', "").substringBefore(' ')
        return pt in pts
    }

    private fun randomHex(length: Int): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return buildString {
            repeat(length) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
    }

    private fun randomSessionId(): String = (1..16).map { "0123456789"[random.nextInt(10)] }.joinToString("")
}
