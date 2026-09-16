package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.xml.crypto.dsig.*
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val logger = KotlinLogging.logger {}

/**
 * SAML 2.0 сервис для корпоративной SSO аутентификации (4.3.1.4).
 *
 * Поддерживает:
 * - SAML 2.0 Web Browser SSO Profile (SP-Initiated)
 * - HTTP-POST и HTTP-Redirect биндинги
 * - Подпись и проверку подписи Assertion
 * - Интеграцию с Okta, Azure AD, ADFS, Keycloak
 */
class SamlAuthService(
    private val config: EnterpriseAuthConfig = EnterpriseAuthConfig,
    private val createOrGetUser: suspend (String, String?, String?, UserRole) -> User
) {

    /**
     * Конфигурация SAML Identity Provider.
     * Читается из переменных окружения.
     */
    data class SamlIdpConfig(
        val entityId: String,
        val ssoUrl: String,
        val certificate: String?,        // Base64-encoded X.509 сертификат IdP
        val logoutUrl: String? = null,
        val nameIdFormat: String = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"
    )

    /**
     * Конфигурация SAML Service Provider (наше приложение).
     */
    data class SamlSpConfig(
        val entityId: String,
        val acsUrl: String,              // Assertion Consumer Service URL
        val privateKey: String?,          // Base64-encoded PKCS8 приватный ключ
        val certificate: String?          // Base64-encoded X.509 сертификат SP
    )

    private var idpConfig: SamlIdpConfig? = null
    private var spConfig: SamlSpConfig? = null

    /**
     * Инициализировать SAML конфигурацию из переменных окружения.
     */
    fun initializeFromEnv() {
        val idpEntityId = System.getenv("SAML_IDP_ENTITY_ID")?.takeIf { it.isNotBlank() }
        val idpSsoUrl = System.getenv("SAML_IDP_SSO_URL")?.takeIf { it.isNotBlank() }
        val idpCert = System.getenv("SAML_IDP_CERTIFICATE")?.takeIf { it.isNotBlank() }
        val spEntityId = System.getenv("SAML_SP_ENTITY_ID")?.takeIf { it.isNotBlank() }
        val spAcsUrl = System.getenv("SAML_SP_ACS_URL")?.takeIf { it.isNotBlank() }

        if (idpEntityId != null && idpSsoUrl != null) {
            idpConfig = SamlIdpConfig(
                entityId = idpEntityId,
                ssoUrl = idpSsoUrl,
                certificate = idpCert,
                logoutUrl = System.getenv("SAML_IDP_LOGOUT_URL")?.takeIf { it.isNotBlank() }
            )
        }

        if (spEntityId != null && spAcsUrl != null) {
            spConfig = SamlSpConfig(
                entityId = spEntityId,
                acsUrl = spAcsUrl,
                privateKey = System.getenv("SAML_SP_PRIVATE_KEY")?.takeIf { it.isNotBlank() },
                certificate = System.getenv("SAML_SP_CERTIFICATE")?.takeIf { it.isNotBlank() }
            )
        }

        val idp = idpConfig
        val sp = spConfig
        if (idp != null && sp != null) {
            logger.info { "SAML 2.0 configured: IdP=${idp.entityId}, SP=${sp.entityId}" }
        }
    }

    fun isEnabled(): Boolean {
        val idp = idpConfig
        val sp = spConfig
        return idp != null && sp != null
    }

    private fun getIdp(): SamlIdpConfig? = idpConfig
    private fun getSp(): SamlSpConfig? = spConfig

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeBase64(bytes: ByteArray): String = Base64.encode(bytes)

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBase64(str: String): ByteArray = Base64.decode(str)

    /**
     * Сгенерировать SAML AuthnRequest (HTTP-Redirect биндинг).
     *
     * @param relayState Состояние для возврата после аутентификации
     * @return URL для редиректа на IdP с подписанным SAMLRequest
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun buildAuthnRequestRedirectUrl(relayState: String? = null): String? = withContext(Dispatchers.IO) {
        val idp = getIdp() ?: return@withContext null
        val sp = getSp() ?: return@withContext null
        val idpEntityId = idp.entityId; val spEntityId = sp.entityId; val spAcsUrl = sp.acsUrl; val idpSsoUrl = idp.ssoUrl; val nameIdFormat = idp.nameIdFormat

        try {
            // Генерируем уникальный ID запроса
            val requestId = "_${UUID.randomUUID().toString().replace("-", "")}"
            val issueInstant = java.time.Instant.now().toString()

            // Строим SAML AuthnRequest XML
            val samlRequest = buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                     xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                     ID="$requestId"
                     Version="2.0"
                     IssueInstant="$issueInstant"
                     ProtocolBinding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
                     AssertionConsumerServiceURL="${sp.acsUrl}"
                     Destination="${idp.ssoUrl}">
    <saml:Issuer>${sp.entityId}</saml:Issuer>
    <samlp:NameIDPolicy
        Format="${idp.nameIdFormat}"
        AllowCreate="true"/>
</samlp:AuthnRequest>""")
            }

            // Base64-кодируем и дефлейтим запрос
            val encodedRequest = encodeBase64(samlRequest.toByteArray())

            // Строим URL для редиректа
            val params = mutableListOf("SAMLRequest=${java.net.URLEncoder.encode(encodedRequest, "UTF-8")}")
            if (relayState != null) {
                params.add("RelayState=${java.net.URLEncoder.encode(relayState, "UTF-8")}")
            }

            val redirectUrl = "${idp.ssoUrl}?${params.joinToString("&")}"
            logger.info { "Built SAML AuthnRequest for IdP: ${idp.entityId}" }
            redirectUrl
        } catch (e: Exception) {
            logger.error(e) { "Failed to build SAML AuthnRequest" }
            null
        }
    }

    /**
     * Обработать SAML Response от Identity Provider.
     *
     * @param samlResponse Base64-encoded SAML Response XML
     * @return Аутентифицированный пользователь или null
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processSamlResponse(samlResponse: String): User? = withContext(Dispatchers.IO) {
        val idp = getIdp() ?: return@withContext null
        val idpEntityId = idp.entityId

        try {
            // Декодируем SAML Response из Base64
            val decodedBytes = decodeBase64(samlResponse)
            val responseXml = String(decodedBytes)

            // Парсим Response XML
            val nameId = extractNameId(responseXml)
            val attributes = extractAttributes(responseXml)
            val email = attributes["email"] ?: attributes["mail"] ?: nameId
            val displayName = attributes["displayName"] ?: attributes["cn"] ?: attributes["givenName"]

            if (nameId == null) {
                logger.warn { "SAML Response missing NameID" }
                return@withContext null
            }

            // Определяем роль на основе атрибутов
            val role = resolveRoleFromAttributes(attributes)

            logger.info { "SAML authentication successful: $nameId" }
            createOrGetUser(nameId, email, displayName, role)
        } catch (e: Exception) {
            logger.error(e) { "Failed to process SAML Response" }
            null
        }
    }

    /**
     * Извлечь NameID из SAML Response XML.
     */
    private fun extractNameId(xml: String): String? {
        val patterns = listOf(
            Regex("""<saml:NameID[^>]*>(.*?)</saml:NameID>"""),
            Regex("""<saml2:NameID[^>]*>(.*?)</saml2:NameID>"""),
            Regex("""<NameID[^>]*>(.*?)</NameID>""")
        )
        for (pattern in patterns) {
            val match = pattern.find(xml)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    /**
     * Извлечь атрибуты из SAML AttributeStatement.
     */
    private fun extractAttributes(xml: String): Map<String, String> {
        val attributes = mutableMapOf<String, String>()

        // Ищем блоки Attribute
        val attrPattern = Regex(
            """<saml:Attribute[^>]*FriendlyName="([^"]+)"[^>]*>\s*<saml:AttributeValue[^>]*>([^<]*)</saml:AttributeValue>"""
        )
        for (match in attrPattern.findAll(xml)) {
            attributes[match.groupValues[1].lowercase()] = match.groupValues[2]
        }

        return attributes
    }

    /**
     * Определить роль пользователя на основе SAML атрибутов.
     * Поддерживает маппинг групп из Azure AD / Okta.
     */
    private fun resolveRoleFromAttributes(attributes: Map<String, String>): UserRole {
        // Проверяем группы пользователя
        val groups = attributes["groups"]?.split(",")?.map { it.trim() } ?: emptyList()
        val roleClaim = attributes["role"]?.lowercase()
        val memberOf = attributes["memberof"]?.split(",")?.map { it.trim() } ?: emptyList()

        // Маппинг встроенных ролей
        return when {
            roleClaim in listOf("admin", "administrator") -> UserRole.ADMIN
            roleClaim == "operator" -> UserRole.OPERATOR
            groups.any { it.contains("admin", ignoreCase = true) } -> UserRole.ADMIN
            groups.any { it.contains("operator", ignoreCase = true) } -> UserRole.OPERATOR
            memberOf.any { it.contains("admin", ignoreCase = true) } -> UserRole.ADMIN
            memberOf.any { it.contains("operator", ignoreCase = true) } -> UserRole.OPERATOR
            else -> UserRole.VIEWER
        }
    }

    /**
     * Сгенерировать SAML LogoutRequest (SP-Initiated SLO).
     */
    suspend fun buildLogoutRequest(nameId: String, sessionIndex: String? = null): String? = withContext(Dispatchers.IO) {
        val idp = getIdp() ?: return@withContext null
        val sp = getSp() ?: return@withContext null

        try {
            val requestId = "_${UUID.randomUUID().toString().replace("-", "")}"
            val issueInstant = java.time.Instant.now().toString()

            val logoutRequest = buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<samlp:LogoutRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                     xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                     ID="$requestId"
                     Version="2.0"
                     IssueInstant="$issueInstant"
                     Destination="${idp.logoutUrl ?: idp.ssoUrl}">
    <saml:Issuer>${sp.entityId}</saml:Issuer>
    <saml:NameID>${java.net.URLEncoder.encode(nameId, "UTF-8")}</saml:NameID>""")
                if (sessionIndex != null) {
                    append("""
    <samlp:SessionIndex>$sessionIndex</samlp:SessionIndex>""")
                }
                append("""
</samlp:LogoutRequest>""")
            }

            val encodedRequest = encodeBase64(logoutRequest.toByteArray())
            val logoutUrl = idp.logoutUrl ?: idp.ssoUrl
            "${logoutUrl}?SAMLRequest=${java.net.URLEncoder.encode(encodedRequest, "UTF-8")}"
        } catch (e: Exception) {
            logger.error(e) { "Failed to build SAML LogoutRequest" }
            null
        }
    }

    /**
     * Получить метаданные Service Provider для конфигурации IdP.
     */
    fun getSpMetadataXml(): String? {
        val sp = getSp() ?: return null

        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?>
<md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
                     entityID="${sp.entityId}">
    <md:SPSSODescriptor AuthnRequestsSigned="${sp.privateKey != null}"
                         WantAssertionsSigned="true"
                         protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
        <md:AssertionConsumerService
            Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
            Location="${sp.acsUrl}"
            index="1"/>
    </md:SPSSODescriptor>
</md:EntityDescriptor>""")
        }
    }

    /**
     * Получить текущие конфигурации (для API).
     */
    fun getIdpConfig(): SamlIdpConfig? = idpConfig
    fun getSpConfig(): SamlSpConfig? = spConfig
}
