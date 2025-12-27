package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.UserEntityMapper
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.LoginResult
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация UserRepository с использованием SQLDelight для локальных операций
 * и API сервиса для аутентификации
 */
class UserRepositoryImplSqlDelight(
    private val databaseFactory: DatabaseFactory,
    private val userApiService: UserApiService
) : UserRepository {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = UserEntityMapper()

    override suspend fun login(username: String, password: String): Result<LoginResult> = withContext(Dispatchers.Default) {
        try {
            val request = LoginRequest(username = username, password = password)
            val result = userApiService.login(request)
            result.fold(
                onSuccess = { response ->
                    // Сохраняем пользователя в локальную БД после успешного входа
                    val user = response.user.toDomain()
                    saveUserLocally(user)

                    Result.success(
                        LoginResult(
                            user = user,
                            token = response.token,
                            refreshToken = response.refreshToken,
                            expiresIn = response.expiresIn
                        )
                    )
                },
                onError = { error ->
                    logger.error(error) { "Error logging in user: $username" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error logging in user: $username" }
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = userApiService.logout()
            result.fold(
                onSuccess = {
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error logging out" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error logging out" }
            Result.failure(e)
        }
    }

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String?
    ): Result<User> = withContext(Dispatchers.Default) {
        try {
            val request = RegisterRequest(username = username, email = email, password = password, fullName = fullName)
            val result = userApiService.register(request)
            result.fold(
                onSuccess = { response ->
                    response.user?.let { userDto ->
                        val user = userDto.toDomain()
                        // Сохраняем пользователя в локальную БД после успешной регистрации
                        saveUserLocally(user)
                        Result.success(user)
                    } ?: Result.failure(Exception(response.message ?: "Registration failed"))
                },
                onError = { error ->
                    logger.error(error) { "Error registering user: $username" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error registering user: $username" }
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.Default) {
        try {
            // Сначала пытаемся получить из API
            val result = userApiService.getCurrentUser()
            result.fold(
                onSuccess = { dto ->
                    val user = dto.toDomain()
                    saveUserLocally(user)
                    user
                },
                onError = { error ->
                    logger.warn(error) { "Error getting current user from API, trying local DB" }
                    // Если API недоступен, пытаемся получить из локальной БД
                    // Возвращаем первого активного пользователя (упрощенная логика)
                    database.cameraDatabaseQueries
                        .selectActiveUsers()
                        .executeAsList()
                        .firstOrNull()
                        ?.let { mapper.toDomain(it) }
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting current user" }
            null
        }
    }

    override suspend fun updateCurrentUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            val request = UpdateUserRequest(
                email = user.email,
                fullName = user.fullName,
                password = null // Password update should be separate endpoint
            )
            val result = userApiService.updateCurrentUser(request)
            result.fold(
                onSuccess = { dto ->
                    val updatedUser = dto.toDomain()
                    saveUserLocally(updatedUser)
                    Result.success(updatedUser)
                },
                onError = { error ->
                    logger.error(error) { "Error updating current user: ${user.id}" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error updating current user: ${user.id}" }
            Result.failure(e)
        }
    }

    override suspend fun getUsers(
        page: Int,
        limit: Int,
        role: String?
    ): PaginatedResult<User> = withContext(Dispatchers.Default) {
        try {
            val offset = (page - 1) * limit

            val allUsers = if (role != null) {
                database.cameraDatabaseQueries
                    .selectUsersByRole(role)
                    .executeAsList()
            } else {
                database.cameraDatabaseQueries
                    .selectAllUsers()
                    .executeAsList()
            }

            val total = allUsers.size
            val paginatedUsers = allUsers.drop(offset).take(limit)

            PaginatedResult(
                items = paginatedUsers.map { mapper.toDomain(it) },
                total = total,
                page = page,
                limit = limit,
                hasMore = offset + limit < total
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting users from local DB" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getUserById(id: String): User? = withContext(Dispatchers.Default) {
        try {
            // Сначала пытаемся получить из локальной БД
            database.cameraDatabaseQueries
                .selectUserById(id)
                .executeAsOneOrNull()
                ?.let { mapper.toDomain(it) }
                // Если не найдено локально, пытаемся через API
                ?: run {
                    try {
                        val result = userApiService.getUserById(id)
                        result.fold(
                            onSuccess = { dto ->
                                val user = dto.toDomain()
                                saveUserLocally(user)
                                user
                            },
                            onError = { error ->
                                logger.warn(error) { "User not found: $id" }
                                null
                            }
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error getting user by id from API: $id" }
                        null
                    }
                }
        } catch (e: Exception) {
            logger.error(e) { "Error getting user by id: $id" }
            null
        }
    }

    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            // Обновляем локально
            saveUserLocally(user)

            // Пытаемся синхронизировать с API
            try {
                val request = UpdateUserRequest(
                    email = user.email,
                    fullName = user.fullName,
                    password = null
                )
                val result = userApiService.updateUser(user.id, request)
                result.fold(
                    onSuccess = { dto ->
                        val updatedUser = dto.toDomain()
                        saveUserLocally(updatedUser)
                        Result.success(updatedUser)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to sync user update to API, but saved locally" }
                        Result.success(user)
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Error syncing user update to API, but saved locally" }
                Result.success(user)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating user: ${user.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Удаляем из локальной БД
            database.cameraDatabaseQueries.deleteUser(id)

            // Пытаемся удалить через API
            try {
                val result = userApiService.deleteUser(id)
                result.fold(
                    onSuccess = {
                        Result.success(Unit)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to delete user from API, but deleted locally" }
                        Result.success(Unit)
                    }
                )
            } catch (e: Exception) {
                logger.warn(e) { "Error deleting user from API, but deleted locally" }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user: $id" }
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<LoginResult> = withContext(Dispatchers.Default) {
        try {
            val result = userApiService.refreshToken(refreshToken)
            result.fold(
                onSuccess = { response ->
                    // Обновляем пользователя в локальной БД
                    val user = response.user.toDomain()
                    saveUserLocally(user)

                    Result.success(
                        LoginResult(
                            user = user,
                            token = response.token,
                            refreshToken = response.refreshToken,
                            expiresIn = response.expiresIn
                        )
                    )
                },
                onError = { error ->
                    logger.error(error) { "Error refreshing token" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error refreshing token" }
            Result.failure(e)
        }
    }

    /**
     * Сохраняет пользователя в локальную БД
     */
    private fun saveUserLocally(user: User) {
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
        } catch (e: Exception) {
            logger.error(e) { "Error saving user locally: ${user.id}" }
        }
    }

    private fun UserResponse.toDomain(): User {
        return User(
            id = id,
            username = username,
            email = email,
            fullName = fullName,
            role = UserRole.valueOf(role.uppercase()),
            permissions = permissions,
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            isActive = isActive
        )
    }
}


