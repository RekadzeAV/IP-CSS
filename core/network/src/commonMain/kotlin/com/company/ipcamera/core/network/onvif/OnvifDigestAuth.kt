package com.company.ipcamera.core.network.onvif

import com.company.ipcamera.core.network.auth.DigestCrypto
import mu.KotlinLogging
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * ONVIF Digest Authentication (RFC 2617).
 *
 * ONVIF Profile S требует поддержки Digest аутентификации для SOAP-запросов.
 * Алгоритм:
 * 1. Отправляем запрос без аутентификации → получаем WWW-Authenticate: Digest realm="...", nonce="...", opaque="..."
 * 2. Вычисляем HA1, HA2, response
 * 3. Отправляем запрос с Authorization: Digest ...
 *
 * Поддерживает:
 * - MD5 и MD5-sess алгоритмы
 * - qop=auth (рекомендуемый ONVIF)
 * - UTF-8 кодировку username/password
 */
class OnvifDigestAuth {

    companion object {
        private const val DIGEST_REALM = "ONVIF"
        private const val DIGEST_QOP = "auth"
        private const val DIGEST_ALGORITHM = "MD5"
    }

    /**
     * Парсит заголовок WWW-Authenticate из ответа сервера.
     *
     * @param wwwAuthHeader Значение заголовка WWW-Authenticate
     * @return DigestChallenge или null, если не удалось распарсить
     */
    fun parseWwwAuthenticate(wwwAuthHeader: String?): DigestChallenge? {
        if (wwwAuthHeader == null || !wwwAuthHeader.startsWith("Digest", ignoreCase = true)) {
            return null
        }

        try {
            val params = parseDigestParams(wwwAuthHeader.removePrefix("Digest").trim())

            val realm = params["realm"]
            val nonce = params["nonce"]
            val opaque = params["opaque"]
            val qop = params["qop"]
            val algorithm = params["algorithm"] ?: "MD5"
            val stale = params["stale"]?.lowercase() == "true"

            if (realm == null || nonce == null) {
                logger.warn { "Invalid Digest challenge: missing realm or nonce" }
                return null
            }

            return DigestChallenge(
                realm = realm,
                nonce = nonce,
                opaque = opaque,
                qop = qop,
                algorithm = algorithm,
                stale = stale
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to parse WWW-Authenticate header: ${e.message}" }
            return null
        }
    }

    /**
     * Формирует заголовок Authorization для Digest аутентификации.
     *
     * @param challenge Параметры challenge от сервера
     * @param username Имя пользователя
     * @param password Пароль
     * @param method HTTP метод (GET, POST, ...)
     * @param uri URI запроса (например, "/onvif/device_service")
     * @param nc Счётчик nonce (шестнадцатеричный, 8 цифр)
     * @param cnonce Клиентский nonce (уникальная строка)
     * @return Значение заголовка Authorization
     */
    fun buildAuthorizationHeader(
        challenge: DigestChallenge,
        username: String,
        password: String,
        method: String,
        uri: String,
        nc: String = "00000001",
        cnonce: String = generateCnonce()
    ): String {
        val ha1 = calculateHa1(username, challenge.realm, password, challenge.algorithm)
        val ha2 = calculateHa2(method, uri, challenge.qop)
        val response = calculateResponse(ha1, ha2, challenge.nonce, nc, cnonce, challenge.qop)

        return buildString {
            append("Digest ")
            append("username=\"$username\", ")
            append("realm=\"${challenge.realm}\", ")
            append("nonce=\"${challenge.nonce}\", ")
            append("uri=\"$uri\", ")
            append("response=\"$response\", ")
            append("algorithm=${challenge.algorithm}, ")
            append("cnonce=\"$cnonce\", ")
            append("nc=$nc, ")
            append("qop=${challenge.qop ?: DIGEST_QOP}")
            if (challenge.opaque != null) {
                append(", opaque=\"${challenge.opaque}\"")
            }
        }
    }

    /**
     * Вычисляет HA1 = MD5(username:realm:password).
     */
    private fun calculateHa1(
        username: String,
        realm: String,
        password: String,
        algorithm: String?
    ): String {
        val input = "$username:$realm:$password"
        return md5Hex(input)
    }

    /**
     * Вычисляет HA2 = MD5(method:uri).
     * Если qop=auth-int, то HA2 = MD5(method:uri:MD5(entityBody)).
     */
    private fun calculateHa2(
        method: String,
        uri: String,
        qop: String?
    ): String {
        val input = "$method:$uri"
        return md5Hex(input)
    }

    /**
     * Вычисляет response = MD5(HA1:nonce:nc:cnonce:qop:HA2).
     */
    private fun calculateResponse(
        ha1: String,
        ha2: String,
        nonce: String,
        nc: String,
        cnonce: String,
        qop: String?
    ): String {
        val input = if (qop != null) {
            "$ha1:$nonce:$nc:$cnonce:$qop:$ha2"
        } else {
            "$ha1:$nonce:$ha2"
        }
        return md5Hex(input)
    }

    /**
     * MD5 хеш в hex-строке (чистый Kotlin, без платформенных крипто-API).
     */
    private fun md5Hex(input: String): String = DigestCrypto.md5Hex(input)

    /**
     * Парсит параметры Digest из строки.
     * Пример: realm="ONVIF", nonce="abc123", opaque="def456"
     */
    private fun parseDigestParams(input: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val regex = Regex("""(\w+)\s*=\s*"([^"]*)""")
        regex.findAll(input).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2]
            params[key] = value
        }
        return params
    }

    /**
     * Генерирует клиентский nonce (cnonce). Использует kotlin.random.Random.
     */
    private fun generateCnonce(): String {
        val random = Random.Default
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Выполняет полный цикл Digest аутентификации:
     * 1. Парсит WWW-Authenticate
     * 2. Формирует Authorization
     *
     * @param wwwAuthHeader Заголовок WWW-Authenticate от сервера
     * @param username Имя пользователя
     * @param password Пароль
     * @param method HTTP метод
     * @param uri URI запроса
     * @return Заголовок Authorization или null, если challenge невалидный
     */
    fun authenticate(
        wwwAuthHeader: String?,
        username: String,
        password: String,
        method: String,
        uri: String
    ): String? {
        val challenge = parseWwwAuthenticate(wwwAuthHeader) ?: return null
        return buildAuthorizationHeader(challenge, username, password, method, uri)
    }
}

/**
 * Параметры Digest challenge от сервера.
 */
data class DigestChallenge(
    val realm: String,
    val nonce: String,
    val opaque: String? = null,
    val qop: String? = null,
    val algorithm: String = "MD5",
    val stale: Boolean = false
)
