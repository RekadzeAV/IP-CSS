package com.company.ipcamera.server.repository

import com.company.ipcamera.server.service.PasswordService
import com.company.ipcamera.server.config.DatabaseConfig
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.LoginResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.util.*
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val json = Json { encodeDefaults = true }

/**
 * РЎРµСЂРІРµСЂРЅР°СЏ СЂРµР°Р»РёР·Р°С†РёСЏ С…СЂР°РЅРёР»РёС‰Р° РїРѕР»СЊР·РѕРІР°С‚РµР»РµР№ Рё Р°РІС‚РѕСЂРёР·Р°С†РёРё РЅР° PostgreSQL.
 * РСЃРїРѕР»СЊР·СѓРµС‚ С‚Р°Р±Р»РёС†С‹: "user", user_password_hash, refresh_token, user_totp (РјРёРіСЂР°С†РёСЏ V3).
 */
class ServerUserRepositoryPostgres(
    private val dataSource: DataSource
) : ServerUserRepository {

    init {
        createDefaultAdminIfNeeded()
    }

    private fun createDefaultAdminIfNeeded() {
        dataSource.connection.use { conn ->
            val hasAdmin = conn.prepareStatement("SELECT 1 FROM \"user\" WHERE username = ?")
                .use { ps ->
                    ps.setString(1, "admin")
                    ps.executeQuery().next()
                }
            if (hasAdmin) return

            val adminId = UUID.randomUUID().toString()
            val nowEpochSeconds = System.currentTimeMillis() / 1000
            val isProduction = DatabaseConfig.isProductionEnvironment()
            val hashedPassword = when {
                System.getenv("ADMIN_PASSWORD_HASH")?.isNotBlank() == true -> {
                    logger.info { "Default admin user: using ADMIN_PASSWORD_HASH from environment" }
                    System.getenv("ADMIN_PASSWORD_HASH")
                }
                System.getenv("ADMIN_PASSWORD")?.isNotBlank() == true -> {
                    logger.warn { "Default admin user: using ADMIN_PASSWORD from environment. Prefer ADMIN_PASSWORD_HASH in production." }
                    PasswordService.hashPassword(System.getenv("ADMIN_PASSWORD"))
                }
                isProduction -> {
                    throw IllegalStateException(
                        "Production startup blocked: set ADMIN_PASSWORD_HASH or ADMIN_PASSWORD for initial admin bootstrap."
                    )
                }
                else -> {
                    val devPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                    logger.warn {
                        "Default admin user created with DEVELOPMENT password '$devPassword'. CHANGE IN PRODUCTION!"
                    }
                    PasswordService.hashPassword(devPassword)
                }
            }

            conn.autoCommit = false
            try {
                conn.prepareStatement("""
                    INSERT INTO "user" (id, username, email, full_name, role, permissions, created_at, last_login_at, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { ps ->
                    ps.setString(1, adminId)
                    ps.setString(2, "admin")
                    ps.setString(3, "admin@example.com")
                    ps.setString(4, "Administrator")
                    ps.setString(5, UserRole.ADMIN.name)
                    ps.setString(6, json.encodeToString(listOf("*")))
                    ps.setLong(7, nowEpochSeconds)
                    ps.setObject(8, null)
                    ps.setInt(9, 1)
                    ps.executeUpdate()
                }
                conn.prepareStatement("""
                    INSERT INTO user_password_hash (user_id, password_hash, updated_at)
                    VALUES (?, ?, ?)
                """.trimIndent()).use { ps ->
                    ps.setString(1, adminId)
                    ps.setString(2, hashedPassword)
                    ps.setLong(3, nowEpochSeconds)
                    ps.executeUpdate()
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    override suspend fun authenticate(username: String, password: String): User? = withContext(Dispatchers.IO) {
        val dummyHash = "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
        dataSource.connection.use { conn ->
            val user = conn.prepareStatement("SELECT id, username, email, full_name, role, permissions, created_at, last_login_at, is_active FROM \"user\" WHERE username = ? AND is_active = 1")
                .use { ps ->
                    ps.setString(1, username)
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) {
                            PasswordService.verifyPassword(password, dummyHash)
                            logger.warn { "Authentication failed: invalid credentials for username: $username" }
                            return@withContext null
                        }
                        mapRowToUser(rs)
                    }
                } ?: return@withContext null

            val hash = conn.prepareStatement("SELECT password_hash FROM user_password_hash WHERE user_id = ?")
                .use { ps ->
                    ps.setString(1, user.id)
                    ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else dummyHash }
                }

            if (!PasswordService.verifyPassword(password, hash)) {
                logger.warn { "Authentication failed: invalid credentials for username: $username" }
                return@withContext null
            }

            val now = System.currentTimeMillis() / 1000
            conn.prepareStatement("UPDATE \"user\" SET last_login_at = ? WHERE id = ?").use { ps ->
                ps.setLong(1, now)
                ps.setString(2, user.id)
                ps.executeUpdate()
            }
            logger.info { "User authenticated successfully: $username" }
            user.copy(lastLoginAt = now)
        }
    }

    override suspend fun getUserById(id: String): User? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, username, email, full_name, role, permissions, created_at, last_login_at, is_active FROM \"user\" WHERE id = ?")
                .use { ps ->
                    ps.setString(1, id)
                    ps.executeQuery().use { rs -> if (rs.next()) mapRowToUser(rs) else null }
                }
        }
    }

    override suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, username, email, full_name, role, permissions, created_at, last_login_at, is_active FROM \"user\" WHERE username = ?")
                .use { ps ->
                    ps.setString(1, username)
                    ps.executeQuery().use { rs -> if (rs.next()) mapRowToUser(rs) else null }
                }
        }
    }

    override suspend fun getOrCreateUserByUsername(
        username: String,
        email: String?,
        fullName: String?,
        role: UserRole
    ): User = withContext(Dispatchers.IO) {
        getUserByUsername(username)?.let { return@withContext it }
        val userId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis() / 1000
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("""
                    INSERT INTO "user" (id, username, email, full_name, role, permissions, created_at, last_login_at, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { ps ->
                    ps.setString(1, userId)
                    ps.setString(2, username)
                    ps.setString(3, email)
                    ps.setString(4, fullName)
                    ps.setString(5, role.name)
                    ps.setString(6, "[]")
                    ps.setLong(7, now)
                    ps.setObject(8, null)
                    ps.setInt(9, 1)
                    ps.executeUpdate()
                }
                conn.prepareStatement("INSERT INTO user_password_hash (user_id, password_hash, updated_at) VALUES (?, ?, ?)")
                    .use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, PasswordService.hashPassword(UUID.randomUUID().toString()))
                        ps.setLong(3, now)
                        ps.executeUpdate()
                    }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
        getUserById(userId)!!.also { logger.info { "Created user from external auth: $username (id: $userId)" } }
    }

    override suspend fun getTotpSecret(userId: String): String? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT secret FROM user_totp WHERE user_id = ? AND enabled = true")
                .use { ps ->
                    ps.setString(1, userId)
                    ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
                }
        }
    }

    override suspend fun isTotpEnabled(userId: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT 1 FROM user_totp WHERE user_id = ? AND enabled = true")
                .use { ps ->
                    ps.setString(1, userId)
                    ps.executeQuery().next()
                }
        }
    }

    override suspend fun setTotpSecret(userId: String, secret: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dataSource.connection.use { conn ->
            conn.prepareStatement("""
                INSERT INTO user_totp (user_id, secret, enabled, updated_at) VALUES (?, ?, false, ?)
                ON CONFLICT (user_id) DO UPDATE SET secret = EXCLUDED.secret, updated_at = EXCLUDED.updated_at
            """.trimIndent()).use { ps ->
                ps.setString(1, userId)
                ps.setString(2, secret)
                ps.setLong(3, now)
                ps.executeUpdate()
            }
        }
        Unit
    }

    override suspend fun setTotpEnabled(userId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dataSource.connection.use { conn ->
            conn.prepareStatement("UPDATE user_totp SET enabled = ?, updated_at = ? WHERE user_id = ?")
                .use { ps ->
                    ps.setBoolean(1, enabled)
                    ps.setLong(2, now)
                    ps.setString(3, userId)
                    ps.executeUpdate()
                }
        }
        Unit
    }

    override suspend fun clearTotp(userId: String) = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM user_totp WHERE user_id = ?").use { ps ->
                ps.setString(1, userId)
                ps.executeUpdate()
            }
        }
        Unit
    }

    override suspend fun saveRefreshToken(refreshToken: String, userId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dataSource.connection.use { conn ->
            conn.prepareStatement("INSERT INTO refresh_token (token, user_id, created_at) VALUES (?, ?, ?)")
                .use { ps ->
                    ps.setString(1, refreshToken)
                    ps.setString(2, userId)
                    ps.setLong(3, now)
                    ps.executeUpdate()
                }
        }
        Unit
    }

    override suspend fun getUserIdByRefreshToken(refreshToken: String): String? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT user_id FROM refresh_token WHERE token = ? AND (expires_at IS NULL OR expires_at > ?)")
                .use { ps ->
                    ps.setString(1, refreshToken)
                    ps.setLong(2, System.currentTimeMillis())
                    ps.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
                }
        }
    }

    override suspend fun revokeRefreshToken(refreshToken: String) = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("DELETE FROM refresh_token WHERE token = ?").use { ps ->
                ps.setString(1, refreshToken)
                ps.executeUpdate()
            }
        }
        Unit
    }

    override suspend fun createUser(
        username: String,
        email: String?,
        password: String,
        fullName: String?,
        role: UserRole
    ): User = withContext(Dispatchers.IO) {
        if (getUserByUsername(username) != null) {
            throw IllegalArgumentException("User with username $username already exists")
        }
        val userId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis() / 1000
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                conn.prepareStatement("""
                    INSERT INTO "user" (id, username, email, full_name, role, permissions, created_at, last_login_at, is_active)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()).use { ps ->
                    ps.setString(1, userId)
                    ps.setString(2, username)
                    ps.setString(3, email)
                    ps.setString(4, fullName)
                    ps.setString(5, role.name)
                    ps.setString(6, "[]")
                    ps.setLong(7, now)
                    ps.setObject(8, null)
                    ps.setInt(9, 1)
                    ps.executeUpdate()
                }
                conn.prepareStatement("INSERT INTO user_password_hash (user_id, password_hash, updated_at) VALUES (?, ?, ?)")
                    .use { ps ->
                        ps.setString(1, userId)
                        ps.setString(2, PasswordService.hashPassword(password))
                        ps.setLong(3, now)
                        ps.executeUpdate()
                    }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
        getUserById(userId)!!.also { logger.info { "User created: $username (id: $userId)" } }
    }

    override suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.prepareStatement("SELECT id, username, email, full_name, role, permissions, created_at, last_login_at, is_active FROM \"user\" ORDER BY created_at DESC")
                .use { ps ->
                    ps.executeQuery().use { rs ->
                        sequence { while (rs.next()) yield(mapRowToUser(rs)) }.toList()
                    }
                }
        }
    }

    override suspend fun getUsers(
        page: Int,
        limit: Int,
        role: UserRole?
    ): com.company.ipcamera.shared.domain.repository.PaginatedResult<User> = withContext(Dispatchers.IO) {
        val all = getAllUsers()
        val filtered = if (role != null) all.filter { it.role == role } else all
        val total = filtered.size
        val offset = (page - 1) * limit
        val items = filtered.drop(offset).take(limit)
        com.company.ipcamera.shared.domain.repository.PaginatedResult(
            items = items,
            total = total,
            page = page,
            limit = limit,
            hasMore = offset + items.size < total
        )
    }

    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.IO) {
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("""
                    UPDATE "user" SET username = ?, email = ?, full_name = ?, role = ?, permissions = ?, last_login_at = ?, is_active = ?
                    WHERE id = ?
                """.trimIndent()).use { ps ->
                    ps.setString(1, user.username)
                    ps.setString(2, user.email)
                    ps.setString(3, user.fullName)
                    ps.setString(4, user.role.name)
                    ps.setString(5, json.encodeToString(user.permissions))
                    if (user.lastLoginAt != null) ps.setLong(6, user.lastLoginAt!!) else ps.setNull(6, java.sql.Types.BIGINT)
                    ps.setInt(7, if (user.isActive) 1 else 0)
                    ps.setString(8, user.id)
                    if (ps.executeUpdate() == 0) return@withContext Result.failure(IllegalArgumentException("User not found: ${user.id}"))
                }
            }
            logger.info { "User updated: ${user.username} (id: ${user.id})" }
            Result.success(user)
        } catch (e: Exception) {
            logger.error(e) { "Error updating user: ${user.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("DELETE FROM \"user\" WHERE id = ?").use { ps ->
                    ps.setString(1, id)
                    if (ps.executeUpdate() == 0) return@withContext Result.failure(IllegalArgumentException("User not found: $id"))
                }
            }
            logger.info { "User deleted: $id" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user: $id" }
            Result.failure(e)
        }
    }

    private fun mapRowToUser(rs: java.sql.ResultSet): User {
        val perms = rs.getString(6)?.takeIf { it.isNotBlank() }?.let {
            try { json.decodeFromString<List<String>>(it) } catch (_: Exception) { emptyList() }
        } ?: emptyList()
        return User(
            id = rs.getString(1),
            username = rs.getString(2),
            email = rs.getString(3),
            fullName = rs.getString(4),
            role = UserRole.valueOf(rs.getString(5)),
            permissions = perms,
            createdAt = rs.getLong(7),
            lastLoginAt = rs.getLong(8).takeIf { rs.getObject(8) != null },
            isActive = rs.getInt(9) == 1
        )
    }
}
