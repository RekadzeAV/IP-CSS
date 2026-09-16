package com.company.ipcamera.server.notification

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

/**
 * Учётные данные сервис-аккаунта Google (service account) для FCM HTTP v1 API.
 * Парсится стандартный JSON-ключ сервис-аккаунта (без Firebase Admin SDK).
 */
@Serializable
data class FcmServiceAccountJson(
    val project_id: String? = null,
    val client_email: String? = null,
    val private_key: String? = null,
    val type: String? = null
)

class FcmServiceAccountCredentials(
    val projectId: String,
    val clientEmail: String,
    private val privateKeyPem: String
) {
    val privateKey: PrivateKey by lazy { parsePkcs8Pem(privateKeyPem) }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        /** Парсинг полного JSON ключа сервис-аккаунта. */
        fun fromJson(jsonText: String): FcmServiceAccountCredentials? {
            return try {
                val parsed = json.decodeFromString(FcmServiceAccountJson.serializer(), jsonText)
                if (parsed.project_id.isNullOrBlank() || parsed.client_email.isNullOrBlank() ||
                    parsed.private_key.isNullOrBlank()
                ) {
                    null
                } else {
                    FcmServiceAccountCredentials(parsed.project_id, parsed.client_email, parsed.private_key)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parsePkcs8Pem(pem: String): PrivateKey {
        val base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\n", "\n")
            .lines()
            .joinToString("") { it.trim() }
        val der = Base64.getMimeDecoder().decode(base64)
        return KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der))
    }

    /**
     * Строит подписанный RS256 JWT для обмена на OAuth2 access token
     * (scope: firebase.messaging).
     */
    fun buildAssertionJwt(nowMillis: Long): String {
        val scope = "https://www.googleapis.com/auth/firebase.messaging"
        val aud = "https://oauth2.googleapis.com/token"
        val header = ApnsJwtUtil.jsonOf(mapOf("alg" to "RS256", "typ" to "JWT"))
        val claims = ApnsJwtUtil.jsonOf(
            mapOf(
                "iss" to clientEmail,
                "scope" to scope,
                "aud" to aud,
                "iat" to nowMillis / 1000,
                "exp" to nowMillis / 1000 + 3600
            )
        )
        val signingInput = ApnsJwtUtil.b64url(header) + "." + ApnsJwtUtil.b64url(claims)

        val signature = Signature.getInstance("SHA256withRSA").apply {
            initSign(privateKey)
            update(signingInput.toByteArray(Charsets.UTF_8))
        }.sign()

        return signingInput + "." + ApnsJwtUtil.b64url(signature)
    }
}