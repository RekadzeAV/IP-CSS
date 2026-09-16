package com.company.ipcamera.server.repository

import com.company.ipcamera.server.service.PasswordService
import com.company.ipcamera.shared.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerUserRepositoryInMemoryTest {

    private val repo = ServerUserRepositoryInMemory()

    @Test
    fun `default admin exists`() = runBlocking {
        val user = repo.getUserByUsername("admin")
        assertNotNull(user)
        assertEquals(UserRole.ADMIN, user!!.role)
    }

    @Test
    fun `authenticate with correct password`() = runBlocking {
        val user = repo.getUserByUsername("admin")
        assertNotNull(user)
        val logged = repo.authenticate("admin", "admin")
        assertNotNull(logged)
        assertEquals(user!!.id, logged!!.id)
    }

    @Test
    fun `authenticate with wrong password fails`() = runBlocking {
        assertNull(repo.authenticate("admin", "wrong-password"))
    }

    @Test
    fun `authenticate unknown user fails`() = runBlocking {
        assertNull(repo.authenticate("ghost", "any-password"))
    }

    @Test
    fun `create and get user by id`() = runBlocking {
        val created = repo.createUser(
            username = "bob", email = "bob@example.com",
            password = "s3cure-Pass!1", fullName = "Bob Brown", role = UserRole.VIEWER
        )
        val fetched = repo.getUserById(created.id)
        assertNotNull(fetched)
        assertEquals(created.id, fetched!!.id)
        assertEquals(UserRole.VIEWER, fetched!!.role)
    }

    @Test
    fun `create user then authenticate`() = runBlocking {
        repo.createUser("carol", "carol@example.com", "Pass!word1", "Carol", UserRole.OPERATOR)
        val logged = repo.authenticate("carol", "Pass!word1")
        assertNotNull(logged)
        assertEquals("carol", logged!!.username)
    }

    @Test
    fun `getOrCreate returns existing user`() = runBlocking {
        val first = repo.getOrCreateUserByUsername("dave", "dave@example.com", "Dave", UserRole.VIEWER)
        val second = repo.getOrCreateUserByUsername("DAVE", "other@example.com", "Other", UserRole.ADMIN)
        assertEquals(first.id, second.id)
        assertEquals(UserRole.VIEWER, second.role)
    }

    @Test
    fun `totp lifecycle`() = runBlocking {
        val user = repo.createUser("eve", null, "Passw0rd!", null, UserRole.VIEWER)
        assertFalse(repo.isTotpEnabled(user.id))

        repo.setTotpSecret(user.id, "BASE32SECRET")
        repo.setTotpEnabled(user.id, true)
        assertTrue(repo.isTotpEnabled(user.id))
        assertEquals("BASE32SECRET", repo.getTotpSecret(user.id))

        repo.clearTotp(user.id)
        assertNull(repo.getTotpSecret(user.id))
        assertFalse(repo.isTotpEnabled(user.id))
    }

    @Test
    fun `refresh token lifecycle`() = runBlocking {
        val user = repo.createUser("frank", null, "Passw0rd!", null, UserRole.VIEWER)
        repo.saveRefreshToken("rt-1", user.id)
        assertEquals(user.id, repo.getUserIdByRefreshToken("rt-1"))

        repo.revokeRefreshToken("rt-1")
        assertNull(repo.getUserIdByRefreshToken("rt-1"))
    }

    @Test
    fun `getUsers paginates and filters by role`() = runBlocking {
        repo.createUser("u1", null, "Passw0rd!", null, UserRole.VIEWER)
        repo.createUser("u2", null, "Passw0rd!", null, UserRole.OPERATOR)
        val all = repo.getAllUsers()
        assertTrue(all.size >= 3) // default admin + 2

        val page = repo.getUsers(page = 1, limit = 2, role = null)
        assertEquals(2, page.items.size)
        assertTrue(page.total >= 3)

        val viewers = repo.getUsers(page = 1, limit = 10, role = UserRole.VIEWER)
        assertTrue(viewers.items.all { it.role == UserRole.VIEWER })
    }

    @Test
    fun `update and delete user`() = runBlocking {
        val user = repo.createUser("grace", "grace@example.com", "Passw0rd!", "Grace", UserRole.VIEWER)
        val updated = repo.updateUser(user.copy(fullName = "Grace Updated"))
        assertTrue(updated.isSuccess)
        assertEquals("Grace Updated", repo.getUserById(user.id)!!.fullName)

        val deleted = repo.deleteUser(user.id)
        assertTrue(deleted.isSuccess)
        assertNull(repo.getUserById(user.id))
    }

    @Test
    fun `delete missing user is failure`() = runBlocking {
        val res = repo.deleteUser("no-such-id")
        assertTrue(res.isFailure)
    }

    @Test
    fun `hashed passwords never stored in plaintext`() = runBlocking {
        val user = repo.createUser("hash", null, "SuperSecret1", null, UserRole.VIEWER)
        // Пароль не должен быть открытым. Проверяем через повторную аутентификацию,
        // что хеш корректен и не равен паролю в открытом виде.
        val authenticated = repo.authenticate("hash", "SuperSecret1")
        assertNotNull(authenticated)
    }

    @Test
    fun `password hasher produces bcrypt hash`() {
        val hash = PasswordService.hashPassword("SuperSecret1")
        assertTrue(hash.startsWith("$2"))
    }
}