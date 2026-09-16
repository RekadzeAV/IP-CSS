package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.shared.domain.model.User
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Заглушка аутентификации через Kerberos (4.3.1.3).
 * Полная реализация потребует GSS-API (javax.security.sasl) и интеграции с KDC.
 */
class KerberosAuthService : ExternalAuthProvider {

    override val id: String = "kerberos"

    override fun isEnabled(): Boolean = EnterpriseAuthConfig.kerberosEnabled

    /**
     * Базовая проверка готовности Kerberos конфигурации.
     * Полная network-проверка KDC не выполняется в MVP-режиме.
     */
    fun checkAvailability(): Pair<Boolean, String> {
        if (!isEnabled()) return false to "disabled"
        val hasRealm = !EnterpriseAuthConfig.kerberosRealm.isNullOrBlank()
        val hasKdc = !EnterpriseAuthConfig.kerberosKdc.isNullOrBlank()
        return if (hasRealm && hasKdc) {
            false to "configured_but_not_implemented"
        } else {
            false to "missing_realm_or_kdc"
        }
    }

    override suspend fun authenticate(username: String, password: String): User? {
        if (!isEnabled()) return null
        logger.warn { "Kerberos auth is not implemented yet (stub). Configure KERBEROS_REALM and KERBEROS_KDC." }
        return null
    }
}
