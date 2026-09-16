package com.company.ipcamera.server.repository

import com.company.ipcamera.server.service.PasswordService
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Контракт серверного хранилища пользователей и авторизации.
 * Реализации: ServerUserRepositoryInMemory (MVP), ServerUserRepositoryPostgres (продакшен).
 */
interface ServerUserRepository {

    suspend fun authenticate(username: String, password: String): User?
    suspend fun getUserById(id: String): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun getOrCreateUserByUsername(username: String, email: String?, fullName: String?, role: UserRole): User
    suspend fun getTotpSecret(userId: String): String?
    suspend fun isTotpEnabled(userId: String): Boolean
    suspend fun setTotpSecret(userId: String, secret: String)
    suspend fun setTotpEnabled(userId: String, enabled: Boolean)
    suspend fun clearTotp(userId: String)
    suspend fun saveRefreshToken(refreshToken: String, userId: String)
    suspend fun getUserIdByRefreshToken(refreshToken: String): String?
    suspend fun revokeRefreshToken(refreshToken: String)
    suspend fun createUser(username: String, email: String?, password: String, fullName: String?, role: UserRole): User
    suspend fun getAllUsers(): List<User>
    suspend fun getUsers(page: Int, limit: Int, role: UserRole?): PaginatedResult<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun deleteUser(id: String): Result<Unit>
}

/**
 * In-memory реализация только для dev/test окружений (без PostgreSQL).
 * В production должна использоваться ServerUserRepositoryPostgres.
 */
class ServerUserRepositoryInMemory : ServerUserRepository {
    private val users = mutableMapOf<String, User>()
    private val passwordHashes = mutableMapOf<String, String>() // userId -> hashedPassword
    private val refreshTokens = mutableMapOf<String, String>() // refreshToken -> userId
    private val totpSecrets = mutableMapOf<String, String>() // userId -> base32 secret
    private val totpEnabled = mutableMapOf<String, Boolean>() // userId -> enabled
    private val mutex = Mutex()

    init {
        // Создаем дефолтного администратора для MVP
        // В продакшене это должно быть через миграцию БД или переменные окружения
        createDefaultAdmin()
    }

    /**
     * Создает дефолтного администратора.
     * Поддерживаются переменные окружения:
     * - ADMIN_PASSWORD_HASH — готовый BCrypt-хеш (рекомендуется для production, совместимо с docker-compose).
     * - ADMIN_PASSWORD — пароль в открытом виде (хешируется при старте; для разработки).
     * Если ни одна не задана — используется "admin" (только для разработки!).
     */
    private fun createDefaultAdmin() {
        val adminId = UUID.randomUUID().toString()
        val hashedPassword = when {
            System.getenv("ADMIN_PASSWORD_HASH")?.isNotBlank() == true -> {
                logger.info { "Default admin user: using ADMIN_PASSWORD_HASH from environment" }
                System.getenv("ADMIN_PASSWORD_HASH")
            }
            else -> {
                val adminPassword = System.getenv("ADMIN_PASSWORD") ?: "admin"
                logger.warn { "Default admin user created. Username: admin, password from ADMIN_PASSWORD or default. CHANGE IN PRODUCTION!" }
                PasswordService.hashPassword(adminPassword)
            }
        }

        val admin = User(
            id = adminId,
            username = "admin",
            email = "admin@example.com",
            fullName = "Administrator",
            role = UserRole.ADMIN,
            permissions = listOf("*"), // Все разрешения
            createdAt = System.currentTimeMillis(),
            lastLoginAt = null,
            isActive = true
        )

        users[adminId] = admin
        passwordHashes[adminId] = hashedPassword
    }

    /**
     * Аутентификация пользователя
     *
     * Защита от timing атак:
     * - Всегда выполняем проверку пароля, даже если пользователь не существует
     * - Используем фиктивный валидный BCrypt хеш для константного времени выполнения
     * - Это предотвращает утечку информации о существовании пользователя через время ответа
     */
    override suspend fun authenticate(username: String, password: String): User? = mutex.withLock {
        val user = users.values.find { it.username == username && it.isActive }

        // Фиктивный валидный BCrypt хеш для защиты от timing атак
        // Хеш от пароля "dummy_password_for_timing_attack_protection"
        // Это гарантирует, что BCrypt.checkpw всегда выполняется с одинаковой сложностью
        val dummyHash = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"

        val hashedPassword = if (user != null) {
            passwordHashes[user.id] ?: dummyHash
        } else {
            dummyHash
        }

        // Всегда выполняем проверку пароля для защиты от timing атак
        // BCrypt.checkpw занимает константное время независимо от результата
        val passwordValid = PasswordService.verifyPassword(password, hashedPassword)

        // Проверяем, что пользователь существует и пароль правильный
        if (user == null || !passwordValid) {
            // Используем одинаковое сообщение для защиты от перечисления пользователей
            logger.warn { "Authentication failed: invalid credentials for username: $username" }
            return null
        }

        // Обновляем время последнего входа
        val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
        users[user.id] = updatedUser

        logger.info { "User authenticated successfully: $username" }
        return updatedUser
    }

    /**
     * Получение пользователя по ID
     */
    override suspend fun getUserById(id: String): User? = mutex.withLock {
        return users[id]
    }

    /**
     * Получение пользователя по username
     */
    override suspend fun getUserByUsername(username: String): User? = mutex.withLock {
        return users.values.find { it.username == username }
    }

    /**
     * Получить существующего пользователя по username или создать нового (для LDAP/SSO).
     * При создании пароль не задаётся (внешняя аутентификация).
     */
    override suspend fun getOrCreateUserByUsername(
        username: String,
        email: String?,
        fullName: String?,
        role: UserRole
    ): User = mutex.withLock {
        users.values.find { it.username.equals(username, ignoreCase = true) }?.let { return it }
        val userId = UUID.randomUUID().toString()
        val user = User(
            id = userId,
            username = username,
            email = email,
            fullName = fullName,
            role = role,
            permissions = emptyList(),
            createdAt = System.currentTimeMillis(),
            lastLoginAt = null,
            isActive = true
        )
        users[userId] = user
        // LDAP/SSO пользователи без локального пароля — аутентификация только через провайдера
        passwordHashes[userId] = PasswordService.hashPassword(UUID.randomUUID().toString())
        logger.info { "Created user from external auth: $username (id: $userId)" }
        user
    }

    // ---------- 2FA TOTP (4.3.2) ----------
    override suspend fun getTotpSecret(userId: String): String? = mutex.withLock { totpSecrets[userId] }

    override suspend fun isTotpEnabled(userId: String): Boolean = mutex.withLock { totpEnabled[userId] == true }

    override suspend fun setTotpSecret(userId: String, secret: String) = mutex.withLock {
        totpSecrets[userId] = secret
    }

    override suspend fun setTotpEnabled(userId: String, enabled: Boolean) = mutex.withLock {
        totpEnabled[userId] = enabled
    }

    override suspend fun clearTotp(userId: String) = mutex.withLock {
        totpSecrets.remove(userId)
        totpEnabled.remove(userId)
        Unit
    }

    override suspend fun saveRefreshToken(refreshToken: String, userId: String) = mutex.withLock {
        refreshTokens[refreshToken] = userId
    }

    override suspend fun getUserIdByRefreshToken(refreshToken: String): String? = mutex.withLock {
        return refreshTokens[refreshToken]
    }

    override suspend fun revokeRefreshToken(refreshToken: String) = mutex.withLock {
        refreshTokens.remove(refreshToken)
        Unit
    }

    override suspend fun createUser(
        username: String,
        email: String?,
        password: String,
        fullName: String?,
        role: UserRole
    ): User {
        return mutex.withLock {
            // Проверяем, что пользователь с таким username не существует
            if (users.values.any { it.username == username }) {
                throw IllegalArgumentException("User with username $username already exists")
            }

            val userId = UUID.randomUUID().toString()
            val hashedPassword = PasswordService.hashPassword(password)

            val user = User(
                id = userId,
                username = username,
                email = email,
                fullName = fullName,
                role = role,
                permissions = emptyList(),
                createdAt = System.currentTimeMillis(),
                lastLoginAt = null,
                isActive = true
            )

            users[userId] = user
            passwordHashes[userId] = hashedPassword

            logger.info { "User created: $username (id: $userId)" }
            user
        }
    }

    /**
     * Получение всех пользователей (для администрирования)
     */
    override suspend fun getAllUsers(): List<User> = mutex.withLock {
        return users.values.toList()
    }

    /**
     * Получение пользователей с пагинацией
     * @param page номер страницы (начинается с 1)
     * @param limit количество элементов на странице
     * @param role фильтр по роли (опционально)
     * @return результат с пагинацией
     */
    override suspend fun getUsers(
        page: Int,
        limit: Int,
        role: UserRole?
    ): com.company.ipcamera.shared.domain.repository.PaginatedResult<User> = mutex.withLock {
        var filteredUsers = users.values.toList()

        // Фильтрация по роли
        if (role != null) {
            filteredUsers = filteredUsers.filter { it.role == role }
        }

        val total = filteredUsers.size
        val offset = (page - 1) * limit
        val paginatedItems = filteredUsers.drop(offset).take(limit)
        val hasMore = offset + limit < total

        return com.company.ipcamera.shared.domain.repository.PaginatedResult(
            items = paginatedItems,
            total = total,
            page = page,
            limit = limit,
            hasMore = hasMore
        )
    }

    /**
     * Обновление пользователя
     */
    override suspend fun updateUser(user: User): Result<User> = mutex.withLock {
        try {
            if (!users.containsKey(user.id)) {
                return Result.failure(IllegalArgumentException("User not found: ${user.id}"))
            }
            users[user.id] = user
            logger.info { "User updated: ${user.username} (id: ${user.id})" }
            Result.success(user)
        } catch (e: Exception) {
            logger.error(e) { "Error updating user: ${user.id}" }
            Result.failure(e)
        }
    }

    /**
     * Удаление пользователя
     */
    override suspend fun deleteUser(id: String): Result<Unit> = mutex.withLock {
        try {
            if (!users.containsKey(id)) {
                return Result.failure(IllegalArgumentException("User not found: $id"))
            }
            users.remove(id)
            passwordHashes.remove(id)
            logger.info { "User deleted: $id" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user: $id" }
            Result.failure(e)
        }
    }
}

