package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.unboundid.ldap.listener.InMemoryDirectoryServer
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig
import com.unboundid.ldap.sdk.LDAPConnection
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Интеграционные тесты LDAP-аутентификации на InMemoryDirectoryServer (unboundid).
 * Проверяют реальный поиск DN, bind, чтение атрибутов и role-mapping.
 */
class LdapAuthServiceIntegrationTest {

    private lateinit var server: InMemoryDirectoryServer
    private lateinit var config: EnterpriseAuthConfig
    private lateinit var createdUser: User

    @BeforeTest
    fun setup() {
        runBlocking {
            val baseDn = "dc=test,dc=local"
        val serverConfig = InMemoryDirectoryServerConfig(baseDn)
        serverConfig.addAdditionalBindCredentials("cn=admin,dc=test,dc=local", "password")
        serverConfig.setSchema(null) // отключаем schema-валидацию (memberOf и гибкие объектные классы)
        serverConfig.setListenerConfigs(com.unboundid.ldap.listener.InMemoryListenerConfig.createLDAPConfig("default", 0))
        server = InMemoryDirectoryServer(serverConfig)
        server.startListening()

        fun entry(dn: String, vararg attrs: Pair<String, String>) =
            com.unboundid.ldap.sdk.Entry(
                dn,
                attrs.map { com.unboundid.ldap.sdk.Attribute(it.first, it.second) }
            )

        server.add(entry("dc=test,dc=local", "objectClass" to "top", "objectClass" to "domain", "dc" to "test"))
        server.add(entry("ou=people,dc=test,dc=local", "objectClass" to "top", "objectClass" to "organizationalUnit", "ou" to "people"))
        server.add(
            entry(
                "uid=alice,ou=people,dc=test,dc=local",
                "objectClass" to "organizationalPerson",
                "objectClass" to "inetOrgPerson",
                "cn" to "Alice Smith",
                "uid" to "alice",
                "mail" to "alice@test.local",
                "sn" to "Smith",
                "userPassword" to "secret123",
                "memberOf" to "CN=ops,dc=test,dc=local"
            )
        )
        server.add(
            entry(
                "uid=bob,ou=people,dc=test,dc=local",
                "objectClass" to "organizationalPerson",
                "objectClass" to "inetOrgPerson",
                "cn" to "Bob Brown",
                "uid" to "bob",
                "mail" to "bob@test.local",
                "sn" to "Brown",
                "userPassword" to "letmein"
            )
        )

        val port = server.getListenPort()
        println("LDAP-TEST: entries=${server.countEntries()} port=$port peopleExists=${server.getEntry("ou=people,dc=test,dc=local") != null}")

        config = mockk()
        every { config.ldapEnabled } returns true
        every { config.ldapUrl } returns "ldap://localhost:$port"
        every { config.ldapUserBaseDn } returns "ou=people,dc=test,dc=local"
        every { config.ldapBindDn } returns "cn=admin,dc=test,dc=local"
        every { config.ldapBindPassword } returns "password"
        every { config.ldapUserSearchFilter } returns "(uid={0})"
        every { config.ldapDisplayNameAttribute } returns "cn"
        every { config.ldapEmailAttribute } returns "mail"
        every { config.ldapRoleMapping } returns mapOf(
            "ops" to "OPERATOR",
            "admins" to "ADMIN"
        )
        }
    }

    @AfterTest
    fun teardown() {
        server.shutDown(true)
    }

    @Test
    fun `authenticate succeeds and maps role via group`() = runBlocking {
        createdUser = User(id = "local-1", username = "alice", email = "alice@test.local", role = UserRole.OPERATOR, createdAt = 0L)
        val createLocal: suspend (String, String?, String?, UserRole) -> User = { _, e, n, r ->
            User(id = "local-1", username = "alice", email = e, fullName = n, role = r, createdAt = 0L)
        }
        val service = LdapAuthService(config, createLocal)

        val user = service.authenticate("alice", "secret123")

        assertNotNull(user)
        assertEquals("alice", user.username)
        assertEquals(UserRole.OPERATOR, user.role) // из группы cn=ops -> OPERATOR
        assertEquals("Alice Smith", user.fullName)
        assertEquals("alice@test.local", user.email)
    }

    @Test
    fun `authenticate returns viewer when no group maps`() = runBlocking {
        val createLocal: suspend (String, String?, String?, UserRole) -> User = { u, e, n, r ->
            User(id = "local-2", username = u, email = e, fullName = n, role = r, createdAt = 0L)
        }
        val service = LdapAuthService(config, createLocal)

        val user = service.authenticate("bob", "letmein")

        assertNotNull(user)
        assertEquals(UserRole.VIEWER, user.role)
    }

    @Test
    fun `authenticate returns null on wrong password`() = runBlocking {
        val createLocal: suspend (String, String?, String?, UserRole) -> User =
            { _, _, _, _ -> throw AssertionError("not reached") }
        val service = LdapAuthService(config, createLocal)

        val user = service.authenticate("alice", "wrong-password")

        assertNull(user)
    }

    @Test
    fun `authenticate returns null for unknown user`() = runBlocking {
        val createLocal: suspend (String, String?, String?, UserRole) -> User =
            { _, _, _, _ -> throw AssertionError("not reached") }
        val service = LdapAuthService(config, createLocal)

        val user = service.authenticate("ghost", "whatever")

        assertNull(user)
    }

    @Test
    fun `checkAvailability reports ok when reachable`() = runBlocking {
        val service = LdapAuthService(config, { _, _, _, _ ->
            User(id = "x", username = "x", role = UserRole.VIEWER, createdAt = 0L)
        })

        val (ok, msg) = service.checkAvailability()

        assertTrue(ok, "LDAP should be reachable, msg=$msg")
    }
}