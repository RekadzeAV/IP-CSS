package com.company.ipcamera.server.security

import com.company.ipcamera.shared.domain.model.User

/**
 * Провайдер внешней аутентификации (LDAP/AD, SSO, Kerberos).
 * Используется в цепочке аутентификации при логине.
 */
interface ExternalAuthProvider {

    /** Уникальный идентификатор провайдера (ldap, oauth2, kerberos) */
    val id: String

    /** Включён ли провайдер (из конфига) */
    fun isEnabled(): Boolean

    /**
     * Попытка аутентификации.
     * @return User при успехе, null если провайдер не смог аутентифицировать (не его пользователь или ошибка)
     */
    suspend fun authenticate(username: String, password: String): User?
}
