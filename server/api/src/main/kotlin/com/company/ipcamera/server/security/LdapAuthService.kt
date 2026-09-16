package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.unboundid.ldap.sdk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Аутентификация через LDAP / Active Directory (4.3.1.1).
 * При включённом LDAP_ENABLED выполняет bind по логину/паролю и поиск атрибутов пользователя.
 */
class LdapAuthService(
    private val config: EnterpriseAuthConfig = EnterpriseAuthConfig,
    private val createOrGetLocalUser: suspend (String, String?, String?, UserRole) -> User
) : ExternalAuthProvider {

    override val id: String = "ldap"

    override fun isEnabled(): Boolean = config.ldapEnabled

    /**
     * Lightweight health-check LDAP подключения без аутентификации пользователя.
     * Проверяет доступность хоста/порта и базовую bind/search готовность.
     */
    override suspend fun authenticate(username: String, password: String): User? = withContext(Dispatchers.IO) {
        if (!isEnabled()) return@withContext null
        if (username.isBlank() || password.isBlank()) return@withContext null

        val parsed = parseLdapUrl(config.ldapUrl)
        val connection = try {
            LDAPConnection(parsed.first, parsed.second)
        } catch (e: LDAPException) {
            logger.warn(e) { "LDAP connection failed: ${parsed.first}:${parsed.second}" }
            return@withContext null
        }
        try {
            // Служебный bind (если настроен): необходим для поиска до user-bind,
            // т.к. схемы/ACL могут запрещать анонимный поиск.
            val serviceDn = config.ldapBindDn
            val servicePassword = config.ldapBindPassword
            if (!serviceDn.isNullOrBlank() && !servicePassword.isNullOrBlank()) {
                connection.bind(serviceDn, servicePassword)
            }

            // 1. Поиск DN пользователя по фильтру
            val userDn = searchUserDn(connection, username) ?: run {
                logger.warn { "LDAP user not found: $username" }
                return@withContext null
            }

            // 2. Authenticate (bind) по найденному DN
            val bindResult = connection.bind(userDn, password)
            if (bindResult.resultCode != ResultCode.SUCCESS) {
                logger.warn { "LDAP bind failed for $username: ${bindResult.resultCode}" }
                return@withContext null
            }

            // 3. Чтение атрибутов пользователя (под bind-учёткой подключения)
            val userEntry = lookupUserEntry(connection, username)
            val displayName = userEntry?.getAttributeValue(config.ldapDisplayNameAttribute)
            val email = userEntry?.getAttributeValue(config.ldapEmailAttribute)
            val role = if (userEntry != null) resolveRole(userEntry) else UserRole.VIEWER

            logger.info { "LDAP authentication successful for $username (role=$role)" }
            createOrGetLocalUser(username, email, displayName, role)
        } catch (e: Exception) {
            logger.warn(e) { "LDAP authentication error for $username: ${e.message}" }
            null
        } finally {
            runCatching { connection.close() }
        }
    }

    /**
     * Повторный поиск записи пользователя (после bind доступен чтение атрибутов).
     * Запись ищется по тому же фильтру в base DN.
     */
    private fun lookupUserEntry(connection: LDAPConnection, username: String): SearchResultEntry? {
        val filter = config.ldapUserSearchFilter.replace("{0}", username)
        val request = SearchRequest(
            config.ldapUserBaseDn,
            SearchScope.SUB,
            Filter.create(filter),
            config.ldapDisplayNameAttribute,
            config.ldapEmailAttribute,
            "memberOf"
        )
        val result = connection.search(request)
        return result.searchEntries.firstOrNull()
    }

    /**
     * Проверить доступность LDAP-сервера: установить подключение и, при наличии bind-учётки,
     * выполнить service-account bind для подтверждения готовности поиска.
     */
    suspend fun checkAvailability(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isEnabled()) return@withContext false to "disabled"

        val parsed = parseLdapUrl(config.ldapUrl)
        try {
            LDAPConnection(parsed.first, parsed.second).use { connection ->
                val bindDn = config.ldapBindDn
                val bindPassword = config.ldapBindPassword
                if (!bindDn.isNullOrBlank() && !bindPassword.isNullOrBlank()) {
                    val rc = connection.bind(bindDn, bindPassword).resultCode
                    if (rc != ResultCode.SUCCESS) return@withContext false to "bind_failed:${rc.intValue()}"
                }
                true to "ok"
            }
        } catch (e: Exception) {
            logger.warn(e) { "LDAP availability check failed: ${e.message}" }
            false to (e.message ?: "error")
        }
    }

    private fun searchUserDn(connection: LDAPConnection, username: String): String? {
        val filter = config.ldapUserSearchFilter.replace("{0}", username)
        val request = SearchRequest(
            config.ldapUserBaseDn,
            SearchScope.SUB,
            Filter.create(filter),
            "dn"
        )
        val result = connection.search(request)
        return result.searchEntries.firstOrNull()?.dn
    }

    private fun resolveRole(entry: SearchResultEntry): UserRole {
        val mapping = config.ldapRoleMapping
        if (mapping.isEmpty()) return UserRole.VIEWER
        val memberOf: List<String> = entry.getAttributeValues("memberOf")?.toList() ?: emptyList()
        for (groupDn in memberOf) {
            val cn = groupDn.split(",").firstOrNull()?.removePrefix("CN=") ?: continue
            mapping[cn]?.let { roleName ->
                return try {
                    UserRole.valueOf(roleName)
                } catch (_: Exception) {
                    UserRole.VIEWER
                }
            }
        }
        return UserRole.VIEWER
    }

    private fun parseLdapUrl(url: String): Pair<String, Int> {
        val u = url.replaceFirst("ldaps://", "https://").replaceFirst("ldap://", "http://")
        return try {
            val uri = java.net.URI(u)
            val port = if (uri.port > 0) uri.port else if (uri.scheme == "https") 636 else 389
            (uri.host ?: "localhost") to port
        } catch (_: Exception) {
            "localhost" to 389
        }
    }
}
