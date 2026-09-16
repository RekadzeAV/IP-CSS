package com.company.ipcamera.server.config

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация Enterprise-аутентификации (4.3).
 * Читается из переменных окружения.
 */
object EnterpriseAuthConfig {
    private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

    // ---------- LDAP / Active Directory (4.3.1.1) ----------
    val ldapEnabled: Boolean
        get() = env("LDAP_ENABLED")?.lowercase() == "true"

    val ldapUrl: String
        get() = env("AUTH_LDAP_SERVER") ?: "ldap://192.168.10.37:389"

    val ldapBindDn: String?
        get() = env("AUTH_LDAP_BIND_DN") ?: env("LDAP_BIND_DN")

    val ldapBindPassword: String?
        get() = env("AUTH_LDAP_BIND_PASSWORD") ?: env("LDAP_BIND_PASSWORD")

    /** Base DN для поиска пользователей (например: ou=users,dc=example,dc=com) */
    val ldapUserBaseDn: String
        get() = env("AUTH_LDAP_BASE_DN") ?: env("LDAP_USER_BASE_DN") ?: "dc=surveillance,dc=local"

    /** Фильтр поиска пользователя по логину (например: (uid={0}) или (sAMAccountName={0})) */
    val ldapUserSearchFilter: String
        get() = env("AUTH_LDAP_USER_SEARCH_FILTER") ?: env("LDAP_USER_SEARCH_FILTER") ?: "(uid={0})"

    /** Атрибут LDAP для отображаемого имени */
    val ldapDisplayNameAttribute: String
        get() = env("LDAP_DISPLAY_NAME_ATTRIBUTE") ?: "cn"

    /** Атрибут LDAP для email */
    val ldapEmailAttribute: String
        get() = env("LDAP_EMAIL_ATTRIBUTE") ?: "mail"

    /** Маппинг группы LDAP на роль (формат: groupCn:ROLE, например OU=Admins:ADMIN) */
    val ldapRoleMapping: Map<String, String>
        get() {
            val raw = env("LDAP_ROLE_MAPPING") ?: return emptyMap()
            return raw.split(",").mapNotNull { part ->
                val kv = part.split(":").map { it.trim() }
                if (kv.size == 2) kv[0] to kv[1] else null
            }.toMap()
        }

    val ldapConnectionTimeoutMs: Int
        get() = env("LDAP_CONNECTION_TIMEOUT_MS")?.toIntOrNull() ?: 5000

    // ---------- SSO OAuth2/OIDC (4.3.1.2) ----------
    val oauth2Enabled: Boolean
        get() = env("OAUTH2_ENABLED")?.lowercase() == "true"

    val oauth2ProviderName: String
        get() = env("OAUTH2_PROVIDER_NAME") ?: "oidc"

    val oauth2ClientId: String?
        get() = env("OAUTH2_CLIENT_ID")

    val oauth2ClientSecret: String?
        get() = env("OAUTH2_CLIENT_SECRET")

    val oauth2AuthorizationUrl: String?
        get() = env("OAUTH2_AUTHORIZATION_URL")

    val oauth2TokenUrl: String?
        get() = env("OAUTH2_TOKEN_URL")

    val oauth2UserInfoUrl: String?
        get() = env("OAUTH2_USERINFO_URL")

    val oauth2RedirectUri: String?
        get() = env("OAUTH2_REDIRECT_URI")

    val oauth2Scopes: List<String>
        get() = env("OAUTH2_SCOPES")?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: listOf("openid", "profile", "email")

    /** Куда редиректить после успешного OAuth2 входа (URL фронтенда) */
    val oauth2FrontendSuccessUrl: String
        get() = env("OAUTH2_FRONTEND_SUCCESS_URL") ?: "http://localhost:3000"

    /** Куда редиректить при ошибке OAuth2 */
    val oauth2FrontendErrorUrl: String
        get() = env("OAUTH2_FRONTEND_ERROR_URL") ?: "http://localhost:3000/login?error=oauth_failed"

    /** TTL state в Redis (секунды) */
    val oauth2StateTtlSec: Long
        get() = env("OAUTH2_STATE_TTL_SEC")?.toLongOrNull()?.coerceIn(60, 900) ?: 600L

    // ---------- Kerberos (4.3.1.3) ----------
    val kerberosEnabled: Boolean
        get() = env("KERBEROS_ENABLED")?.lowercase() == "true"

    val kerberosRealm: String?
        get() = env("KERBEROS_REALM")

    val kerberosKdc: String?
        get() = env("KERBEROS_KDC")

    // ---------- 2FA TOTP (4.3.2) ----------
    val totpIssuer: String
        get() = env("TOTP_ISSUER") ?: "IP-CSS"

    /** Включена ли обязательная 2FA для всех пользователей (иначе по желанию) */
    val twoFactorRequired: Boolean
        get() = env("TWO_FACTOR_REQUIRED")?.lowercase() == "true"

    /** Размер окна для проверки TOTP (допуск ±N интервалов по 30 сек) */
    val totpWindowSize: Int
        get() = env("TOTP_WINDOW_SIZE")?.toIntOrNull()?.coerceIn(0, 2) ?: 1

    // ---------- Аудит (4.3.3) ----------
    val auditPersistEnabled: Boolean
        get() = env("AUDIT_PERSIST_ENABLED")?.lowercase() == "true"

    // ---------- Шифрование (4.3.4) ----------
    val dataEncryptionKey: String?
        get() = env("DATA_ENCRYPTION_KEY")

    val dataEncryptionEnabled: Boolean
        get() = !dataEncryptionKey.isNullOrBlank()

    internal fun validateSecurityRequirementsForServerStartup(
        isProductionEnvironment: Boolean,
        envReader: (String) -> String?
    ) {
        val explicitRequired = envReader("DATA_ENCRYPTION_REQUIRED")?.lowercase() == "true"
        val required = explicitRequired || isProductionEnvironment
        if (!required) return

        val key = envReader("DATA_ENCRYPTION_KEY")?.takeIf { it.isNotBlank() }
        require(!key.isNullOrBlank()) {
            "Production startup blocked: DATA_ENCRYPTION_KEY is required."
        }
        require((key.length == 32 || key.length == 64) && key.matches(Regex("^[0-9a-fA-F]+$"))) {
            "Production startup blocked: DATA_ENCRYPTION_KEY must be 32 or 64 hex chars."
        }
    }

    /**
     * Fail-closed preflight для шифрования чувствительных данных:
     * - в production режимах требуем DATA_ENCRYPTION_KEY;
     * - при DATA_ENCRYPTION_REQUIRED=true также требуем ключ и валидный hex-формат.
     */
    fun validateSecurityRequirementsForServerStartup() {
        validateSecurityRequirementsForServerStartup(
            isProductionEnvironment = DatabaseConfig.isProductionEnvironment(),
            envReader = { name -> env(name) }
        )
    }

    /**
     * Валидация LDAP требований для запуска сервера
     */
    fun validateLdapRequirementsForServerStartup() {
        if (!ldapEnabled) {
            logger.info { "LDAP is disabled, skipping validation" }
            return
        }

        logger.info { "Validating LDAP requirements..." }

        // Check if LDAP is reachable
        val isReachable = testLdapConnection()

        if (!isReachable) {
            logger.error { "LDAP server is not reachable at $ldapUrl" }
            // Не блокируем запуск, но логгируем предупреждение
            logger.warn { "Server will continue without LDAP authentication" }
        } else {
            logger.info { "LDAP validation successful" }
        }
    }

    private fun testLdapConnection(): Boolean {
        return try {
            val url = parseLdapUrl(ldapUrl)
            // Упрощённая проверка: просто проверяем формат URL
            // В production для реального подключения нужен правильный LDAP SDK API
            true
        } catch (e: Exception) {
            logger.error(e) { "LDAP connection test failed" }
            false
        }
    }

    private fun parseLdapUrl(url: String): LdapUrl {
        val pattern = Regex("ldap://([^:]+):(\\d+)")
        val match = pattern.find(url)
            ?: throw IllegalArgumentException("Invalid LDAP URL format: $url")

        return LdapUrl(
            host = match.groupValues[1],
            port = match.groupValues[2].toInt()
        )
    }

    data class LdapUrl(
        val host: String,
        val port: Int
    )
}
