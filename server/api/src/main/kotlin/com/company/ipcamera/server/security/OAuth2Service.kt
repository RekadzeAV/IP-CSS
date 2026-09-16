package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

/**
 * OAuth2/OIDC: обмен кода на токены и получение userinfo (4.3.1.2).
 */
class OAuth2Service(
    private val config: EnterpriseAuthConfig = EnterpriseAuthConfig,
    private val stateStore: OAuth2StateStore,
    private val createOrGetUser: suspend (String, String?, String?, UserRole) -> User
) {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
    }

    fun isEnabled(): Boolean = config.oauth2Enabled &&
        config.oauth2ClientId != null &&
        config.oauth2AuthorizationUrl != null &&
        config.oauth2TokenUrl != null &&
        config.oauth2RedirectUri != null

    /** Строит URL для редиректа на IdP (authorization endpoint). */
    suspend fun buildAuthorizationUrl(state: String): String {
        val authUrl = config.oauth2AuthorizationUrl!!
        val params = listOf(
            "response_type" to "code",
            "client_id" to (config.oauth2ClientId!!),
            "redirect_uri" to (config.oauth2RedirectUri!!),
            "scope" to config.oauth2Scopes.joinToString(" "),
            "state" to state
        )
        val query = params.joinToString("&") { (k, v) -> "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}" }
        return if (authUrl.contains("?")) "$authUrl&$query" else "$authUrl?$query"
    }

    /** Обмен authorization code на access_token (и опционально refresh_token). */
    suspend fun exchangeCodeForTokens(code: String, redirectUri: String): OAuth2TokenResult = withContext(Dispatchers.IO) {
        val tokenUrl = config.oauth2TokenUrl!!
        val body = listOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri,
            "client_id" to (config.oauth2ClientId!!),
            "client_secret" to (config.oauth2ClientSecret ?: "")
        )
        val formBody = body.joinToString("&") { (k, v) -> "${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}" }
        val response = httpClient.post(tokenUrl) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(formBody)
        }
        if (!response.status.isSuccess()) {
            val text = response.bodyAsText()
            logger.warn { "OAuth2 token exchange failed: ${response.status} $text" }
            return@withContext OAuth2TokenResult.Error(text)
        }
        val jsonObj = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val accessToken = jsonObj["access_token"]?.jsonPrimitive?.content
        if (accessToken.isNullOrBlank()) {
            return@withContext OAuth2TokenResult.Error("No access_token in response")
        }
        OAuth2TokenResult.Success(
            accessToken = accessToken,
            refreshToken = jsonObj["refresh_token"]?.jsonPrimitive?.content,
            expiresIn = jsonObj["expires_in"]?.jsonPrimitive?.content?.toLongOrNull()
        )
    }

    /** Запрос userinfo по access_token. */
    suspend fun getUserInfo(accessToken: String): OAuth2UserInfoResult = withContext(Dispatchers.IO) {
        val userInfoUrl = config.oauth2UserInfoUrl ?: run {
            logger.warn { "OAUTH2_USERINFO_URL not set" }
            return@withContext OAuth2UserInfoResult.Error("UserInfo URL not configured")
        }
        val response = httpClient.get(userInfoUrl) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        if (!response.status.isSuccess()) {
            return@withContext OAuth2UserInfoResult.Error("UserInfo request failed: ${response.status}")
        }
        val obj = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val sub = obj["sub"]?.jsonPrimitive?.content ?: ""
        val username = obj["preferred_username"]?.jsonPrimitive?.content
            ?: obj["username"]?.jsonPrimitive?.content
            ?: obj["email"]?.jsonPrimitive?.content
            ?: sub
        val email = obj["email"]?.jsonPrimitive?.content
        val name = obj["name"]?.jsonPrimitive?.content ?: obj["given_name"]?.jsonPrimitive?.content
        if (sub.isBlank()) {
            return@withContext OAuth2UserInfoResult.Error("Missing sub in userinfo")
        }
        OAuth2UserInfoResult.Success(
            sub = sub,
            username = username,
            email = email,
            fullName = name
        )
    }

    /** Полный цикл: по коду и redirect_uri получить пользователя системы. */
    suspend fun authenticateWithCode(code: String, redirectUri: String): User? {
        val tokenResult = exchangeCodeForTokens(code, redirectUri)
        val tokens = (tokenResult as? OAuth2TokenResult.Success) ?: return null
        val userInfoResult = getUserInfo(tokens.accessToken)
        val info = (userInfoResult as? OAuth2UserInfoResult.Success) ?: return null
        return createOrGetUser(info.username, info.email, info.fullName, UserRole.VIEWER)
    }

    fun close() {
        httpClient.close()
    }
}

sealed class OAuth2TokenResult {
    data class Success(val accessToken: String, val refreshToken: String?, val expiresIn: Long?) : OAuth2TokenResult()
    data class Error(val message: String) : OAuth2TokenResult()
}

sealed class OAuth2UserInfoResult {
    data class Success(val sub: String, val username: String, val email: String?, val fullName: String?) : OAuth2UserInfoResult()
    data class Error(val message: String) : OAuth2UserInfoResult()
}
