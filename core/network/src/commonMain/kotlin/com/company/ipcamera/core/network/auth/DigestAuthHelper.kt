package com.company.ipcamera.core.network.auth

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Параметры Digest Authentication из WWW-Authenticate заголовка
 */
data class DigestAuthParams(
    val realm: String,
    val nonce: String,
    val algorithm: String = "MD5", // MD5 или MD5-sess
    val qop: String? = null, // auth, auth-int, или null
    val opaque: String? = null,
    val stale: Boolean = false
)

/**
 * Helper класс для работы с Digest Authentication
 * Реализует RFC 2617 - HTTP Digest Authentication
 */
object DigestAuthHelper {
    /**
     * Парсинг WWW-Authenticate заголовка
     * Формат: Digest realm="...", nonce="...", algorithm=MD5, qop="auth"
     */
    fun parseWWWAuthenticate(wwwAuthenticateHeader: String): DigestAuthParams? {
        return try {
            // Проверяем, что это Digest Authentication
            if (!wwwAuthenticateHeader.startsWith("Digest", ignoreCase = true)) {
                logger.debug { "Not a Digest authentication header: $wwwAuthenticateHeader" }
                return null
            }

            // Удаляем префикс "Digest " без учета регистра
            val digestPart = wwwAuthenticateHeader.substringAfter(' ', missingDelimiterValue = "").trim()

            var realm: String? = null
            var nonce: String? = null
            var algorithm: String = "MD5"
            var qop: String? = null
            var opaque: String? = null
            var stale: Boolean = false

            // Парсинг параметров (realm="...", nonce="...", algorithm=MD5, qop="auth")
            // Поддержка как с кавычками, так и без
            val paramRegex = Regex("""(\w+)=(?:"([^"]+)"|([^,\s]+))""")
            val matches = paramRegex.findAll(digestPart)

            for (match in matches) {
                val paramName = match.groupValues[1].lowercase()
                val paramValue = match.groupValues[2].ifEmpty { match.groupValues[3] }

                when (paramName) {
                    "realm" -> realm = paramValue
                    "nonce" -> nonce = paramValue
                    "algorithm" -> algorithm = paramValue.uppercase()
                    "qop" -> qop = normalizeQop(paramValue)
                    "opaque" -> opaque = paramValue
                    "stale" -> stale = paramValue.equals("true", ignoreCase = true)
                }
            }

            if (realm == null || nonce == null) {
                logger.warn { "Missing required Digest parameters: realm=$realm, nonce=$nonce" }
                return null
            }

            DigestAuthParams(
                realm = realm,
                nonce = nonce,
                algorithm = algorithm,
                qop = qop,
                opaque = opaque,
                stale = stale
            )
        } catch (e: Exception) {
            logger.error(e) { "Error parsing WWW-Authenticate header: ${e.message}" }
            null
        }
    }

    /**
     * Генерация MD5 hash
     */
    private fun md5(input: String): String {
        return DigestCrypto.md5Hex(input)
    }

    /**
     * Генерация SHA-256 hash
     */
    private fun sha256(input: String): String {
        return DigestCrypto.sha256Hex(input)
    }

    /**
     * Генерация HA1 (username:realm:password)
     * Поддерживает MD5 и SHA-256 алгоритмы
     */
    private fun generateHA1(
        username: String,
        realm: String,
        password: String,
        algorithm: String,
        nonce: String? = null,
        cnonce: String? = null
    ): String {
        val ha1Input = "$username:$realm:$password"

        return when (algorithm.uppercase()) {
            "MD5", "MD5-SESS" -> {
                val ha1 = md5(ha1Input)
                // Для MD5-sess: MD5(HA1:nonce:cnonce)
                if (algorithm.uppercase() == "MD5-SESS" && nonce != null && cnonce != null) {
                    md5("$ha1:$nonce:$cnonce")
                } else {
                    ha1
                }
            }
            "SHA-256", "SHA-256-SESS" -> {
                val ha1 = sha256(ha1Input)
                // Для SHA-256-sess: SHA-256(HA1:nonce:cnonce)
                if (algorithm.uppercase() == "SHA-256-SESS" && nonce != null && cnonce != null) {
                    sha256("$ha1:$nonce:$cnonce")
                } else {
                    ha1
                }
            }
            else -> {
                // По умолчанию используем MD5
                logger.warn { "Unknown algorithm: $algorithm, using MD5" }
                md5(ha1Input)
            }
        }
    }

    /**
     * Генерация HA2 (method:uri)
     * Для qop=auth-int включает entity-body hash
     */
    private fun generateHA2(
        method: String,
        uri: String,
        qop: String?,
        entityBody: String? = null,
        algorithm: String = "MD5"
    ): String {
        val hashFunction = when (algorithm.uppercase()) {
            "SHA-256", "SHA-256-SESS" -> ::sha256
            else -> ::md5
        }

        val ha2Input = if (qop == "auth-int" && entityBody != null) {
            // Для auth-int: method:uri:MD5(entity-body)
            val entityHash = hashFunction(entityBody)
            "$method:$uri:$entityHash"
        } else {
            // Для auth или без qop: method:uri
            "$method:$uri"
        }

        return hashFunction(ha2Input)
    }

    /**
     * Генерация response hash для Digest Authentication
     *
     * @param username имя пользователя
     * @param password пароль
     * @param method HTTP метод (POST, GET, etc.)
     * @param uri URI запроса
     * @param params параметры Digest Authentication из WWW-Authenticate
     * @param cnonce клиентский nonce (генерируется автоматически, если не указан)
     * @param nc счетчик запросов (обычно 00000001 для первого запроса)
     */
    fun generateDigestAuthHeader(
        username: String,
        password: String,
        method: String,
        uri: String,
        params: DigestAuthParams,
        cnonce: String? = null,
        nc: String = "00000001",
        entityBody: String? = null
    ): String {
        val actualCnonce = cnonce ?: generateCNonce()
        val effectiveQop = params.qop?.let { normalizeQop(it) }

        // Проверка на stale nonce - если stale=true, нужно запросить новый nonce
        if (params.stale) {
            logger.warn { "Received stale nonce, client should request new nonce from server" }
        }

        // Генерация HA1 (с поддержкой MD5-sess и SHA-256-sess)
        val ha1 = if (params.algorithm.uppercase().endsWith("-SESS")) {
            generateHA1(username, params.realm, password, params.algorithm, params.nonce, actualCnonce)
        } else {
            generateHA1(username, params.realm, password, params.algorithm)
        }

        // Генерация HA2 (с поддержкой auth-int)
        val ha2 = generateHA2(method, uri, effectiveQop, entityBody, params.algorithm)

        // Выбор функции хеширования в зависимости от алгоритма
        val hashFunction = when (params.algorithm.uppercase()) {
            "SHA-256", "SHA-256-SESS" -> ::sha256
            else -> ::md5
        }

        // Генерация response
        val response = if (effectiveQop != null) {
            // С qop: HASH(HA1:nonce:nc:cnonce:qop:HA2)
            hashFunction("$ha1:${params.nonce}:$nc:$actualCnonce:$effectiveQop:$ha2")
        } else {
            // Без qop: HASH(HA1:nonce:HA2)
            hashFunction("$ha1:${params.nonce}:$ha2")
        }

        // Формирование Authorization заголовка
        val authHeader = buildString {
            append("Digest ")
            append("username=\"$username\", ")
            append("realm=\"${params.realm}\", ")
            append("nonce=\"${params.nonce}\", ")
            append("uri=\"$uri\", ")
            append("response=\"$response\"")

            if (effectiveQop != null) {
                append(", qop=\"$effectiveQop\"")
                append(", nc=$nc")
                append(", cnonce=\"$actualCnonce\"")
            }

            if (params.algorithm.isNotEmpty()) {
                append(", algorithm=${params.algorithm}")
            }

            if (params.opaque != null) {
                append(", opaque=\"${params.opaque}\"")
            }
        }

        logger.debug { "Generated Digest Authorization header for user: $username" }
        return authHeader
    }

    /**
     * Генерация клиентского nonce (cnonce)
     */
    private fun generateCNonce(): String {
        return DigestCrypto.secureRandomHex(16)
    }

    /**
     * Извлечение WWW-Authenticate заголовка из HTTP ответа
     * Поддерживает множественные заголовки
     */
    fun extractWWWAuthenticate(headers: Map<String, List<String>>): String? {
        // Собираем все значения WWW-Authenticate без учета регистра ключа
        val authValues = headers.entries
            .filter { it.key.equals("WWW-Authenticate", ignoreCase = true) }
            .flatMap { it.value }
            .filter { it.isNotBlank() }

        if (authValues.isEmpty()) return null

        // Предпочитаем Digest challenge, если он присутствует.
        return authValues.firstOrNull { it.trimStart().startsWith("Digest", ignoreCase = true) }
            ?: authValues.first()
    }

    /**
     * Normalizes qop from WWW-Authenticate.
     * Servers may send a CSV list (e.g. "auth,auth-int"), but
     * Authorization must contain exactly one selected token.
     */
    private fun normalizeQop(rawQop: String): String? {
        val tokens = rawQop.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        return when {
            "auth" in tokens -> "auth"
            "auth-int" in tokens -> "auth-int"
            else -> tokens.first()
        }
    }
}
