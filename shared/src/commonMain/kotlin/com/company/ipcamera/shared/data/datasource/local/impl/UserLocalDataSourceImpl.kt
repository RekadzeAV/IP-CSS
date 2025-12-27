package com.company.ipcamera.shared.data.datasource.local.impl

import com.company.ipcamera.shared.data.datasource.local.UserLocalDataSource
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.UserEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация UserLocalDataSource с использованием SQLDelight
 */
class UserLocalDataSourceImpl(
    private val databaseFactory: DatabaseFactory
) : UserLocalDataSource {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = UserEntityMapper()

    override suspend fun getUsers(): List<User> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectAllUsers()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting users from local database" }
            emptyList()
        }
    }

    override suspend fun getUserById(id: String): User? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectUserById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting user by id from local database: $id" }
            null
        }
    }

    override suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectUserByUsername(username)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting user by username from local database: $username" }
            null
        }
    }

    override suspend fun getUsersByRole(role: String): List<User> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectUsersByRole(role)
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting users by role from local database: $role" }
            emptyList()
        }
    }

    override suspend fun getActiveUsers(): List<User> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries
                .selectActiveUsers()
                .executeAsList()
                .map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting active users from local database" }
            emptyList()
        }
    }

    override suspend fun saveUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            val dbUser = mapper.toDatabase(user)
            database.cameraDatabaseQueries.insertUser(
                id = dbUser.id,
                username = dbUser.username,
                email = dbUser.email,
                full_name = dbUser.full_name,
                role = dbUser.role,
                permissions = dbUser.permissions,
                created_at = dbUser.created_at,
                last_login_at = dbUser.last_login_at,
                is_active = dbUser.is_active
            )
            Result.success(user)
        } catch (e: Exception) {
            logger.error(e) { "Error saving user to local database: ${user.id}" }
            Result.failure(e)
        }
    }

    override suspend fun saveUsers(users: List<User>): Result<List<User>> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.transaction {
                users.forEach { user ->
                    val dbUser = mapper.toDatabase(user)
                    database.cameraDatabaseQueries.insertUser(
                        id = dbUser.id,
                        username = dbUser.username,
                        email = dbUser.email,
                        full_name = dbUser.full_name,
                        role = dbUser.role,
                        permissions = dbUser.permissions,
                        created_at = dbUser.created_at,
                        last_login_at = dbUser.last_login_at,
                        is_active = dbUser.is_active
                    )
                }
            }
            Result.success(users)
        } catch (e: Exception) {
            logger.error(e) { "Error saving users to local database" }
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            val dbUser = mapper.toDatabase(user)
            database.cameraDatabaseQueries.updateUser(
                username = dbUser.username,
                email = dbUser.email,
                full_name = dbUser.full_name,
                role = dbUser.role,
                permissions = dbUser.permissions,
                last_login_at = dbUser.last_login_at,
                is_active = dbUser.is_active,
                id = dbUser.id
            )
            Result.success(user)
        } catch (e: Exception) {
            logger.error(e) { "Error updating user in local database: ${user.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateUserLastLogin(id: String, timestamp: Long): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.updateUserLastLogin(timestamp, id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating user last login in local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun updateUserStatus(id: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.updateUserStatus(if (isActive) 1L else 0L, id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error updating user status in local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteUser(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user from local database: $id" }
            Result.failure(e)
        }
    }

    override suspend fun deleteAllUsers(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val allUsers = database.cameraDatabaseQueries.selectAllUsers().executeAsList()
            database.cameraDatabaseQueries.transaction {
                allUsers.forEach { user ->
                    database.cameraDatabaseQueries.deleteUser(user.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting all users from local database" }
            Result.failure(e)
        }
    }

    override suspend fun userExists(id: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectUserById(id).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking user existence in local database: $id" }
            false
        }
    }

    override suspend fun userExistsByUsername(username: String): Boolean = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectUserByUsername(username).executeAsOneOrNull() != null
        } catch (e: Exception) {
            logger.error(e) { "Error checking user existence by username in local database: $username" }
            false
        }
    }
}

